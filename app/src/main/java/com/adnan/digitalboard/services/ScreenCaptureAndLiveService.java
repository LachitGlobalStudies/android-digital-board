package com.adnan.digitalboard.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.adnan.digitalboard.activity.MainActivity;
import com.pedro.common.ConnectChecker;
import com.pedro.library.base.DisplayBase;
import com.pedro.library.generic.GenericStream;
import com.pedro.library.rtmp.RtmpDisplay;

public class ScreenCaptureAndLiveService extends Service implements ConnectChecker  {
	private static final String YOUTUBE_RTMP_URL = "rtmp://a.rtmp.youtube.com/live2/t433-915g-kuzs-xtw5-du0z";
	private static final String TAG = "ScreenCaptureService";
	private static final String RESULT_CODE = "RESULT_CODE";
	private static final String DATA = "DATA";
	private static String RTMP_URL="RTMP_URL";
	private static final String ACTION = "ACTION";
	private static final String START = "START";
	private static final String START_STREAMING = "START_STREAMING";
	private static final String STOP = "STOP";
	private static final String SCREENCAP_NAME = "screencap";
	private static final String PAUSE = "PAUSE";
	MainActivity mainActivity;
	private static final String RESUME = "RESUME";
	private boolean isPaused = false;

	private MediaProjection mMediaProjection;
	private VirtualDisplay mVirtualDisplay;
	private MediaRecorder mMediaRecorder;

	private String mVideoPath;
	private Handler mHandler;
	private int mWidth;
	private int mHeight;
	private int mDensity;
	private boolean isRecording = false;
	private DisplayBase displayBase;
	private GenericStream genericStream;
	String timestamp;
	File videoDirectory;
	private final IBinder binder=new LocalBinder();


	public static Intent getStartIntent(Context context, int resultCode, Intent data, String rtmp_url) {
		Intent intent = new Intent(context, ScreenCaptureAndLiveService.class);
		intent.putExtra(ACTION, START);
		intent.putExtra(RESULT_CODE, resultCode);
		intent.putExtra(DATA, data);
		intent.putExtra(RTMP_URL, rtmp_url);
		return intent;
	}

	public static Intent getStopIntent(Context context) {
		Intent intent = new Intent(context, ScreenCaptureAndLiveService.class);
		intent.putExtra(ACTION, STOP);
		return intent;
	}

	// Intent methods for PAUSE and RESUME
	public static Intent getPauseIntent(Context context) {
		Intent intent = new Intent(context, ScreenCaptureAndLiveService.class);
		intent.putExtra(ACTION, PAUSE);
		return intent;
	}

	public static Intent getResumeIntent(Context context) {
		Intent intent = new Intent(context, ScreenCaptureAndLiveService.class);
		intent.putExtra(ACTION, RESUME);
		return intent;
	}

	private static boolean isStartCommand(Intent intent) {
		return intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
		&& intent.hasExtra(ACTION) && START.equals(intent.getStringExtra(ACTION));
	}


	private static boolean isStopCommand(Intent intent) {
		return intent.hasExtra(ACTION) && STOP.equals(intent.getStringExtra(ACTION));
	}

	private static int getVirtualDisplayFlags() {
		return DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	public void callFromMainActivity(MainActivity mainActi) {
		this.mainActivity = mainActi;


	}



	@Override
	public void onCreate() {
		super.onCreate();
		displayBase=new RtmpDisplay(getApplicationContext(),true,this);
		//genericStream=new GenericStream(getApplicationContext(),this, VideoManager.Source.SCREEN, AudioManager.Source.MICROPHONE);
		timestamp=new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",java.util.Locale.getDefault()).format(new java.util.Date());
		videoDirectory=new File(android.os.Environment.getExternalStorageDirectory(),"Digital Board/Video Lecctures");
		if (!videoDirectory.exists()){
			videoDirectory.mkdirs();
		}
//		// Create the directory to store videos
//		File externalFilesDir = getExternalFilesDir(null);
//		if (externalFilesDir != null) {
//			mStoreDir = externalFilesDir.getAbsolutePath() + "/screenrecord/";
//			File storeDirectory = new File(mStoreDir);
//			if (!storeDirectory.exists() && !storeDirectory.mkdirs()) {
//				Log.e(TAG, "Failed to create file storage directory.");
//				stopSelf();
//				return;
//			}
//			} else {
//			Log.e(TAG, "Failed to access external files directory.");
//			stopSelf();
//			return;
//		}

		// Start handler thread
		HandlerThread handlerThread = new HandlerThread("ScreenCaptureThread");
		handlerThread.start();
		mHandler = new Handler(handlerThread.getLooper());
	}

	// Handle PAUSE and RESUME actions in onStartCommand
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (isStartCommand(intent)) {
			Pair<Integer, Notification> notification = NotificationUtils.getNotification(this);
			startForeground(notification.first, notification.second);

			int resultCode = intent.getIntExtra(RESULT_CODE, -1);
			Intent data = intent.getParcelableExtra(DATA);
			String url_rtmp=intent.getStringExtra(RTMP_URL);
			startProjection(resultCode, data);
			startStreaming(resultCode,data,url_rtmp);

		} else if (isStopCommand(intent)) {
			stopProjection();
			stopSelf();

		} else if (intent.hasExtra(ACTION)) {
			String action = intent.getStringExtra(ACTION);
			if (PAUSE.equals(action)) {
				pauseScreenRecording();
			} else if (RESUME.equals(action)) {
				resumeScreenRecording();
			}
		}

		return START_NOT_STICKY;
	}



	private void startStreaming(int resultCode, Intent data, String url_rtmp) {
		MediaProjectionManager mediaProjectionManager =
				(MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

		if (mMediaProjection != null) {
			mDensity = Resources.getSystem().getDisplayMetrics().densityDpi;
			WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			Display display = windowManager.getDefaultDisplay();
			mWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
			mHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
			displayBase.setIntentResult(resultCode,data);
			displayBase.prepareVideo();
			displayBase.prepareAudio();
			displayBase.startStream(url_rtmp);


			Toast.makeText(this, "Live streaming started", Toast.LENGTH_SHORT).show();
		} else {
			Log.e(TAG, "MediaProjection is null");
		}
	}


	private void startProjection(int resultCode, Intent data) {
		if (mMediaProjection != null) {
			mMediaProjection.stop();
			mMediaProjection = null;
		}
		MediaProjectionManager mpManager =
				(MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		mMediaProjection = mpManager.getMediaProjection(resultCode, data);
		if (mMediaProjection != null) {
			mDensity = Resources.getSystem().getDisplayMetrics().densityDpi;
			WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			Display display = windowManager.getDefaultDisplay();
			mWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
			mHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
			try {
				prepareMediaRecorder();
				createVirtualDisplay();
				mMediaRecorder.start();
				isRecording = true;
			} catch (IOException e) {
				Log.e(TAG, "Failed to prepare MediaRecorder", e);
				stopSelf();
			}
		}
	}


	// Method to pause recording
	private void pauseScreenRecording() {
		if (mMediaRecorder != null && isRecording && !isPaused) {
			mMediaRecorder.pause();
			isPaused = true;
			Log.d(TAG, "Screen recording paused.");

		}

	}

	// Method to resume recording
	private void resumeScreenRecording() {
		if (mMediaRecorder != null && isPaused) {
			mMediaRecorder.resume();
			isPaused = false;
			Log.d(TAG, "Screen recording resumed.");
		}

	}
	private void stopProjection() {
		if (isRecording) {
			mHandler.post(() -> {
				try {
					if (mMediaRecorder != null) {
						mMediaRecorder.stop();
						mMediaRecorder.reset();
						mMediaRecorder.release();
						mMediaRecorder = null;
						mainActivity.chronometer.stop();
						Toast.makeText(getApplicationContext(),"Recording saved to: "+mVideoPath,Toast.LENGTH_SHORT).show();
						mainActivity.startAndStop.setChecked(false);
						mainActivity.pauseAndResume.setChecked(false);
					}
				} catch (Exception e) {
					Log.e(TAG, "Error stopping MediaRecorder", e);
				}
				if (mVirtualDisplay != null) {
					mVirtualDisplay.release();
					mVirtualDisplay = null;
				}
				if (mMediaProjection != null) {
					mMediaProjection.stop();
					mMediaProjection = null;
				}
				isRecording = false;
			});
		}
	}


	@SuppressLint("WrongConstant")
	private void createVirtualDisplay() {
		if (mVirtualDisplay != null) {
			mVirtualDisplay.release();
			mVirtualDisplay = null;
		}
		if (mMediaProjection != null) {
			mVirtualDisplay = mMediaProjection.createVirtualDisplay(
					SCREENCAP_NAME,
					mWidth,
					mHeight,
					mDensity,
					getVirtualDisplayFlags(),
					mMediaRecorder.getSurface(),
					null,
					mHandler
			);
		}
	}
	private void prepareMediaRecorder() throws IOException {
		if (mMediaRecorder != null) {
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mVideoPath = videoDirectory.getAbsolutePath() + "/VID_" + timestamp + ".mp4";
		mMediaRecorder.setOutputFile(mVideoPath);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mMediaRecorder.setAudioEncodingBitRate(128 * 1000);
		mMediaRecorder.setAudioSamplingRate(44100);
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mMediaRecorder.setVideoSize(mWidth, mHeight);
		mMediaRecorder.setVideoFrameRate(30);
		mMediaRecorder.setVideoEncodingBitRate(8 * 1000 * 1000); // 8 Mbps for high-quality recording
		mMediaRecorder.prepare();
		if (mainActivity != null && mainActivity.chronometer != null) {
			mainActivity.chronometer.setVisibility(View.VISIBLE);
			Toast.makeText(getApplicationContext(),"Recording Started",Toast.LENGTH_SHORT).show();
			mainActivity.chronometer.start();
		}
	}

	@Override
	public void onAuthError() {

	}

	@Override
	public void onAuthSuccess() {

	}

	@Override
	public void onConnectionFailed(@NonNull String s) {

	}

	@Override
	public void onConnectionStarted(@NonNull String s) {

	}

	@Override
	public void onConnectionSuccess() {

	}

	@Override
	public void onDisconnect() {

	}

	@Override
	public void onNewBitrate(long l) {

	}


	public class LocalBinder extends Binder {
		public ScreenCaptureAndLiveService getService(){
			return ScreenCaptureAndLiveService.this;
		}
	}
}