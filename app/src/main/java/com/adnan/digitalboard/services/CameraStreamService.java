package com.adnan.digitalboard.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.pedro.library.rtmp.RtmpCamera2;
import com.pedro.common.ConnectChecker;

import android.hardware.camera2.CameraManager;
import android.provider.Settings;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CameraStreamService extends Service implements ConnectChecker {

    private static final String RESULT_CODE = "RESULT_CODE";
    private static final String DATA = "DATA";
    private static String RTMP_URL = "RTMP_URL";
    private static final String ACTION = "ACTION";
    private static final String START = "START";

    private RtmpCamera2 rtmpCamera2;
    private SurfaceView surfaceView;
    private String rtmpUrl;
    public static RtmpCamera2 rtmpCameraInstance=null;
    private WindowManager windowManager;

    public static Intent getIntent(Context context, String rtmpUrl) {
        Intent intent = new Intent(context, CameraStreamService.class);
        intent.putExtra(ACTION, START);
        intent.putExtra(RTMP_URL, rtmpUrl);
        return intent;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Check if "Draw over other apps" permission is granted
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            stopSelf(); // Don't proceed without permission
            return;
        }

        // 2. Create SurfaceView and Camera
        surfaceView = new SurfaceView(this);
        rtmpCamera2 = new RtmpCamera2(surfaceView, this);
        rtmpCameraInstance = rtmpCamera2;

        // 3. Correct WindowManager usage
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                480, 640,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        // 4. Add SurfaceView to Window
        windowManager.addView(surfaceView, params);

        // 5. Start Preview
        rtmpCamera2.startPreview();
    }



    public static void switchCamera(){
        if (rtmpCameraInstance!=null){
            rtmpCameraInstance.switchCamera();

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStartCommand(intent)) {
            Pair<Integer, Notification> notification = NotificationUtils.getNotification(this);
            startForeground(notification.first, notification.second);
            rtmpUrl = intent.getStringExtra(RTMP_URL);
            startStreaming();
        }
        return START_NOT_STICKY;
    }

    private void startStreaming() {
        if (!rtmpCamera2.isStreaming()) {
            rtmpCamera2.prepareAudio();
            rtmpCamera2.prepareVideo();
            rtmpCamera2.startStream(rtmpUrl);
        }
    }

    private static boolean isStartCommand(Intent intent) {
        return intent.hasExtra(RTMP_URL)
                && intent.hasExtra(ACTION) && START.equals(intent.getStringExtra(ACTION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rtmpCamera2 != null && rtmpCamera2.isStreaming()) {
            rtmpCamera2.stopStream();
            rtmpCamera2.stopPreview();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // --- ConnectChecker methods ---
    @Override
    public void onAuthError() {
    }

    @Override
    public void onAuthSuccess() {
    }

    @Override
    public void onConnectionFailed(@NonNull String reason) {
    }

    @Override
    public void onConnectionStarted(@NonNull String url) {
    }

    @Override
    public void onConnectionSuccess() {
    }

    @Override
    public void onDisconnect() {
    }

    @Override
    public void onNewBitrate(long bitrate) {
    }
}
