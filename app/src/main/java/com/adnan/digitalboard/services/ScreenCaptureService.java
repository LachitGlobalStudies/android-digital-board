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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;

import androidx.core.util.Pair;

public class ScreenCaptureService extends Service {
	
	private static final String TAG = "ScreenCaptureService";
	private static final String RESULT_CODE = "RESULT_CODE";
	private static final String DATA = "DATA";
	private static final String ACTION = "ACTION";
	private static final String START = "START";
	private static final String STOP = "STOP";
	private static final String SCREENCAP_NAME = "screencap";
	private static final String PAUSE = "PAUSE";
	private static final String RESUME = "RESUME";
	private boolean isPaused = false;
	
	private MediaProjection mMediaProjection;
	private VirtualDisplay mVirtualDisplay;
	private MediaRecorder mMediaRecorder;
	
	private String mStoreDir;
	private String mVideoPath;
	private Handler mHandler;
	private int mWidth;
	private int mHeight;
	private int mDensity;
	private boolean isRecording = false;
	String timestamp;
	File videoDirectory;
	
	public static Intent getStartIntent(Context context, int resultCode, Intent data) {
		Intent intent = new Intent(context, ScreenCaptureService.class);
		intent.putExtra(ACTION, START);
		intent.putExtra(RESULT_CODE, resultCode);
		intent.putExtra(DATA, data);
		return intent;
	}
	
	public static Intent getStopIntent(Context context) {
		Intent intent = new Intent(context, ScreenCaptureService.class);
		intent.putExtra(ACTION, STOP);
		return intent;
	}

	// Intent methods for PAUSE and RESUME
	public static Intent getPauseIntent(Context context) {
		Intent intent = new Intent(context, ScreenCaptureService.class);
		intent.putExtra(ACTION, PAUSE);
		return intent;
	}

	public static Intent getResumeIntent(Context context) {
		Intent intent = new Intent(context, ScreenCaptureService.class);
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
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
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
			startProjection(resultCode, data);

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
	
	private void startProjection(int resultCode, Intent data) {
		MediaProjectionManager mpManager =
		(MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		if (mMediaProjection == null) {
			mMediaProjection = mpManager.getMediaProjection(resultCode, data);
			if (mMediaProjection != null) {
				mDensity = Resources.getSystem().getDisplayMetrics().densityDpi;
				WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
				Display display = windowManager.getDefaultDisplay();
				
				mWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
				mHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
				
				try {
					prepareMediaRecorder();
					} catch (IOException e) {
					Log.e(TAG, "Failed to prepare MediaRecorder", e);
					stopSelf();
					return;
				}
				
				createVirtualDisplay();
				
				mMediaRecorder.start();
				isRecording = true;
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
				if (mMediaRecorder != null) {
					try {
						mMediaRecorder.stop();
						mMediaRecorder.reset();
						isRecording = false;
						Log.e(TAG, "Recording saved to: " + mVideoPath);
						} catch (Exception e) {
						Log.e(TAG, "Error stopping MediaRecorder", e);
					}
					mMediaRecorder.release();
					mMediaRecorder = null;
				}
				if (mVirtualDisplay != null) {
					mVirtualDisplay.release();
				}
				if (mMediaProjection != null) {
					mMediaProjection.stop();
				}
			});
		}
	}
	
	@SuppressLint("WrongConstant")
	private void createVirtualDisplay() {
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

		mVideoPath = videoDirectory.getAbsolutePath() + "/VID_" +timestamp + ".mp4";
		
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mMediaRecorder.setOutputFile(mVideoPath);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mMediaRecorder.setAudioEncodingBitRate(128*1000);
		mMediaRecorder.setAudioSamplingRate(44100);
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mMediaRecorder.setVideoSize(mWidth, mHeight);
		mMediaRecorder.setVideoFrameRate(30);
		mMediaRecorder.setVideoEncodingBitRate(8 * 1000 * 1000); // 8 Mbps for high-quality recording
		mMediaRecorder.prepare();
		isRecording=true;
	}
}