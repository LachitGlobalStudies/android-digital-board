package com.adnan.digitalboard.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.adnan.digitalboard.utils.StudentTrackManager;
import com.adnan.digitalboard.view.DrawingView;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * WebRTC Streaming Service that captures and streams ONLY the DrawingView,
 * not the full device screen (preventing system UI, notifications, and outer toolbars from being seen by students).
 */
public class DrawingViewWebRtcService extends Service {

    private static final String TAG = "DrawingViewWebRtcService";
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final String DATA = "DATA";
    private static final String ACTION = "ACTION";
    private static final String START = "START";
    private static final String STOP = "STOP";
    private static final String ROOM_ID = "ROOM_ID";

    // Intent Action matching with MainActivity
    public static final String ACTION_SEND_COMMAND = "ACTION_SEND_COMMAND";
    public static final String EXTRA_TARGET_ID = "EXTRA_TARGET_ID";
    public static final String EXTRA_COMMAND = "EXTRA_COMMAND";

    private static DrawingView activeDrawingView;

    private Socket socket;
    private String roomId;

    // WebRTC Components
    private EglBase rootEglBase;
    private PeerConnectionFactory peerConnectionFactory;

    // Multiple Peer Connections to support multiple students
    private final Map<String, PeerConnection> peerConnectionMap = new HashMap<>();

    private DrawingViewCapturer drawingViewCapturer;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;
    private SurfaceTextureHelper surfaceTextureHelper;

    private boolean isStreaming = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void setDrawingView(DrawingView view) {
        activeDrawingView = view;
    }

    public static DrawingView getDrawingView() {
        return activeDrawingView;
    }

    public static Intent getStartIntent(Context context, String roomId) {
        Intent intent = new Intent(context, DrawingViewWebRtcService.class);
        intent.putExtra(ACTION, START);
        intent.putExtra(ROOM_ID, roomId);
        return intent;
    }

    public static Intent getStartIntent(Context context, int resultCode, Intent data, String roomId) {
        Intent intent = new Intent(context, DrawingViewWebRtcService.class);
        intent.putExtra(ACTION, START);
        intent.putExtra(RESULT_CODE, resultCode);
        intent.putExtra(DATA, data);
        intent.putExtra(ROOM_ID, roomId);
        return intent;
    }

    public static Intent getStopIntent(Context context) {
        Intent intent = new Intent(context, DrawingViewWebRtcService.class);
        intent.putExtra(ACTION, STOP);
        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        rootEglBase = EglBase.create();
        initializeWebRTC();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && isStartCommand(intent)) {
            Pair<Integer, Notification> notification = NotificationUtils.getNotification(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(notification.first, notification.second, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(notification.first, notification.second);
            }

            roomId = intent.getStringExtra(ROOM_ID);

            initDrawingViewCapturer();
            connectSocketIO(roomId);

        } else if (intent != null && ACTION_SEND_COMMAND.equals(intent.getAction())) {
            String targetId = intent.getStringExtra(EXTRA_TARGET_ID);
            String command = intent.getStringExtra(EXTRA_COMMAND);
            sendCommandToStudent(targetId, command);

        } else if (intent != null && isStopCommand(intent)) {
            stopStreaming();
            disconnectSocketIO();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void initializeWebRTC() {
        try {
            PeerConnectionFactory.InitializationOptions initializationOptions =
                    PeerConnectionFactory.InitializationOptions.builder(this)
                            .setEnableInternalTracer(true)
                            .createInitializationOptions();
            PeerConnectionFactory.initialize(initializationOptions);

            AudioDeviceModule adm = JavaAudioDeviceModule.builder(this)
                    .createAudioDeviceModule();

            DefaultVideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                    rootEglBase.getEglBaseContext(), true, true);
            DefaultVideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(
                    rootEglBase.getEglBaseContext());

            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setAudioDeviceModule(adm)
                    .setVideoEncoderFactory(encoderFactory)
                    .setVideoDecoderFactory(decoderFactory)
                    .createPeerConnectionFactory();

            Log.d(TAG, "✅ WebRTC initialized successfully for DrawingView only");
        } catch (Exception e) {
            Log.e(TAG, "❌ WebRTC initialization failed: " + e.getMessage());
        }
    }

    private void initDrawingViewCapturer() {
        try {
            DrawingView view = activeDrawingView != null ? activeDrawingView : DrawingView.getInstance();
            drawingViewCapturer = new DrawingViewCapturer(view);

            surfaceTextureHelper = SurfaceTextureHelper.create("DrawingViewCaptureThread", rootEglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(drawingViewCapturer.isScreencast());

            drawingViewCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());

            // 1280x720 30 FPS for clear whiteboard viewing
            drawingViewCapturer.startCapture(1280, 720, 30);
            Log.d(TAG, "✅ DrawingView capture started (exclusive to board)");

            videoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource);
            videoTrack.setEnabled(true);

            AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
            audioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource);
            audioTrack.setEnabled(true);

            isStreaming = true;

        } catch (Exception e) {
            Log.e(TAG, "❌ DrawingView capture init failed: " + e.getMessage());
            showToastOnUI("DrawingView capture failed: " + e.getMessage());
        }
    }

    private void connectSocketIO(String roomId) {
        try {
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket", "polling"};
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.timeout = 10000;

            socket = IO.socket("https://webrtc-project-k0jx.onrender.com", options);
            Log.d(TAG, "🔌 Connecting to server for DrawingView WebRTC...");

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "✅ Connected to server");
                showToastOnUI("Connected to whiteboard stream");

                try {
                    JSONObject joinData = new JSONObject();
                    joinData.put("roomId", roomId);
                    joinData.put("role", "teacher");
                    socket.emit("join-room", joinData);
                    Log.d(TAG, "✅ Joined room as teacher: " + roomId);
                } catch (JSONException e) {
                    Log.e(TAG, "❌ Join room error: " + e.getMessage());
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "❌ Disconnected from server");
                showToastOnUI("Disconnected from server");
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "❌ Connection error: " + args[0]);
                showToastOnUI("Connection error: " + args[0]);
            });

            // Handle when students join
            socket.on("user-joined", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String userId = data.getString("userId");
                    String role = data.optString("role", "");
                    String studentName = data.optString("studentName", "Student " + userId);
                    Log.d(TAG, "👤 User joined: " + userId + " (role: " + role + ")");

                    if ("student".equalsIgnoreCase(role)) {
                        showToastOnUI("Student joined!");
                        sendStudentJoinedBroadcast(userId, studentName);
                        mainHandler.postDelayed(() -> createOffer(userId), 500);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error handling user-joined: " + e.getMessage());
                }
            });

            // Chat messages
            socket.on("chat-message", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String sender = data.optString("sender", "Unknown");
                    String message = data.optString("message", "");

                    Intent intent = new Intent("ACTION_RECEIVE_CHAT_MESSAGE");
                    intent.putExtra("sender", sender);
                    intent.putExtra("message", message);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Chat message parse error: " + e.getMessage());
                }
            });

            // Handle signals from students
            socket.on("signal", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    JSONObject signal = data.getJSONObject("signal");
                    String senderId = data.optString("sender");

                    if (signal.has("answer")) {
                        JSONObject answerJson = signal.getJSONObject("answer");
                        handleAnswer(senderId, answerJson);
                    } else if (signal.has("candidate")) {
                        JSONObject candidateJson = signal.getJSONObject("candidate");
                        handleIceCandidate(senderId, candidateJson);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error handling signal: " + e.getMessage());
                }
            });

            socket.connect();

        } catch (Exception e) {
            Log.e(TAG, "❌ Socket connection error: " + e.getMessage());
            showToastOnUI("Socket error: " + e.getMessage());
        }
    }

    private void sendStudentJoinedBroadcast(String studentId, String studentName) {
        Intent intent = new Intent("ACTION_STUDENT_JOINED");
        intent.putExtra("studentId", studentId);
        intent.putExtra("studentName", studentName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCommandToStudent(String targetStudentId, String command) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("target", targetStudentId);

                switch (command) {
                    case "mute_mic":
                        data.put("command", "mute_mic");
                        socket.emit("single-student-muted-ui", targetStudentId);
                        break;
                    case "unmute_mic":
                        data.put("command", "unmute_mic");
                        socket.emit("single-student-unmuted-ui", targetStudentId);
                        break;
                    case "switch_camera":
                        data.put("targetStudentId", targetStudentId);
                        socket.emit("switch-student-camera", data);
                        break;
                    case "send_chat":
                        JSONObject chatData = new JSONObject();
                        chatData.put("roomId", roomId);
                        chatData.put("sender", "Teacher");
                        chatData.put("message", targetStudentId);
                        socket.emit("send-chat-message", chatData);
                        break;
                    default:
                        data.put("command", command);
                        socket.emit("student-command", data);
                        break;
                }
            } catch (JSONException e) {
                Log.e(TAG, "❌ Error sending command: " + e.getMessage());
            }
        }
    }

    private void showToastOnUI(String message) {
        mainHandler.post(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show()
        );
    }

    private void createOffer(String targetUserId) {
        try {
            Log.d(TAG, "📞 Creating offer for student: " + targetUserId);

            if (peerConnectionMap.containsKey(targetUserId)) {
                PeerConnection oldPc = peerConnectionMap.get(targetUserId);
                if (oldPc != null) {
                    oldPc.close();
                }
                peerConnectionMap.remove(targetUserId);
            }

            PeerConnection peerConnection = createPeerConnection(targetUserId);
            peerConnectionMap.put(targetUserId, peerConnection);

            List<String> streamIds = Arrays.asList("ARDAMS");

            if (videoTrack != null) {
                peerConnection.addTrack(videoTrack, streamIds);
                videoTrack.setEnabled(true);
            }

            if (audioTrack != null) {
                peerConnection.addTrack(audioTrack, streamIds);
                audioTrack.setEnabled(true);
            }

            MediaConstraints constraints = new MediaConstraints();
            constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
            constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                    peerConnection.setLocalDescription(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {}

                        @Override
                        public void onSetSuccess() {
                            sendOfferToServer(sdp, targetUserId);
                        }

                        @Override
                        public void onCreateFailure(String s) {
                            Log.e(TAG, "❌ Set Local Description Failed: " + s);
                        }

                        @Override
                        public void onSetFailure(String s) {
                            Log.e(TAG, "❌ Set Local Description Failed: " + s);
                        }
                    }, sdp);
                }

                @Override
                public void onSetSuccess() {}

                @Override
                public void onCreateFailure(String s) {
                    Log.e(TAG, "❌ Create Offer Failed: " + s);
                }

                @Override
                public void onSetFailure(String s) {}
            }, constraints);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating offer: " + e.getMessage());
        }
    }

    private PeerConnection createPeerConnection(String targetUserId) {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        return peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "🧊 ICE Connection State [" + targetUserId + "]: " + iceConnectionState);
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                sendIceCandidateToServer(iceCandidate, targetUserId);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(MediaStream mediaStream) {}

            @Override
            public void onRemoveStream(MediaStream mediaStream) {}

            @Override
            public void onDataChannel(DataChannel dataChannel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                if (rtpReceiver.track() instanceof VideoTrack) {
                    VideoTrack remoteVideoTrack = (VideoTrack) rtpReceiver.track();
                    StudentTrackManager.addTrack(targetUserId, remoteVideoTrack);

                    Intent intent = new Intent("ACTION_REMOTE_TRACK_ADDED");
                    intent.putExtra("studentId", targetUserId);
                    LocalBroadcastManager.getInstance(DrawingViewWebRtcService.this).sendBroadcast(intent);
                }
            }
        });
    }

    private void handleAnswer(String senderId, JSONObject answerJson) {
        try {
            String sdp = answerJson.getString("sdp");
            SessionDescription answer = new SessionDescription(SessionDescription.Type.ANSWER, sdp);

            PeerConnection pc = peerConnectionMap.get(senderId);
            if (pc != null) {
                pc.setRemoteDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {}

                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "✅ Remote description set for student: " + senderId);
                    }

                    @Override
                    public void onCreateFailure(String s) {}

                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "❌ Set Remote Description Failed: " + s);
                    }
                }, answer);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling answer: " + e.getMessage());
        }
    }

    private void handleIceCandidate(String senderId, JSONObject candidateJson) {
        try {
            IceCandidate candidate = new IceCandidate(
                    candidateJson.getString("sdpMid"),
                    candidateJson.getInt("sdpMLineIndex"),
                    candidateJson.getString("candidate")
            );

            PeerConnection pc = peerConnectionMap.get(senderId);
            if (pc != null) {
                pc.addIceCandidate(candidate);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling ICE candidate: " + e.getMessage());
        }
    }

    private void sendOfferToServer(SessionDescription sdp, String targetUserId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject offerJson = new JSONObject();
                offerJson.put("type", "offer");
                offerJson.put("sdp", sdp.description);

                JSONObject signalJson = new JSONObject();
                signalJson.put("offer", offerJson);

                JSONObject data = new JSONObject();
                data.put("target", targetUserId);
                data.put("signal", signalJson);

                socket.emit("signal", data);
                Log.d(TAG, "📤 Offer sent to student: " + targetUserId);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error sending offer: " + e.getMessage());
            }
        }
    }

    private void sendIceCandidateToServer(IceCandidate candidate, String targetUserId) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject candidateJson = new JSONObject();
                candidateJson.put("candidate", candidate.sdp);
                candidateJson.put("sdpMid", candidate.sdpMid);
                candidateJson.put("sdpMLineIndex", candidate.sdpMLineIndex);

                JSONObject signalJson = new JSONObject();
                signalJson.put("candidate", candidateJson);

                JSONObject data = new JSONObject();
                data.put("target", targetUserId);
                data.put("signal", signalJson);

                socket.emit("signal", data);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error sending ICE candidate: " + e.getMessage());
            }
        }
    }

    private void disconnectSocketIO() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }

    private void stopStreaming() {
        try {
            isStreaming = false;

            if (drawingViewCapturer != null) {
                drawingViewCapturer.stopCapture();
                drawingViewCapturer.dispose();
                drawingViewCapturer = null;
            }

            if (surfaceTextureHelper != null) {
                surfaceTextureHelper.dispose();
                surfaceTextureHelper = null;
            }

            for (PeerConnection pc : peerConnectionMap.values()) {
                if (pc != null) {
                    pc.close();
                }
            }
            peerConnectionMap.clear();

            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error stopping DrawingView stream: " + e.getMessage());
        }
    }

    private static boolean isStartCommand(Intent intent) {
        return intent.hasExtra(ACTION) && START.equals(intent.getStringExtra(ACTION));
    }

    private static boolean isStopCommand(Intent intent) {
        return intent.hasExtra(ACTION) && STOP.equals(intent.getStringExtra(ACTION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopStreaming();
        disconnectSocketIO();

        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        if (rootEglBase != null) {
            rootEglBase.release();
        }
    }
}
