package com.adnan.digitalboard.services;

import android.app.*;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.adnan.digitalboard.R;
import com.pedro.common.ConnectChecker;
import com.pedro.library.base.DisplayBase;
import com.pedro.library.rtmp.RtmpDisplay;


public class ScreenStreamingService extends Service implements ConnectChecker{
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ScreenStreamingChannel";
    private DisplayBase displayBase;
    private static final String YOUTUBE_RTMP_URL = "rtmp://a.rtmp.youtube.com/live2/t433-915g-kuzs-xtw5-du0z";
    private MediaProjectionManager mediaProjectionManager;
    private int screenWidth, screenHeight;
    private int resultCode;
    private Intent data;
    private MyConnectChecker myConnectChecker;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        updateResolution();
        myConnectChecker=new MyConnectChecker();
        displayBase=new RtmpDisplay(getBaseContext(),true,myConnectChecker);
        displayBase.setIntentResult(resultCode,data);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        resultCode = intent.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED);
        data = intent.getParcelableExtra("DATA_INTENT");

        if (resultCode != Activity.RESULT_CANCELED && data != null) {
            startForeground(NOTIFICATION_ID, createNotification());
            startStreaming();
        } else {
            stopSelf();
        }
        return START_STICKY;
    }

    private void updateResolution() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            screenWidth = Math.max(metrics.widthPixels, metrics.heightPixels);
            screenHeight = Math.min(metrics.widthPixels, metrics.heightPixels);
        } else {
            screenWidth = Math.min(metrics.widthPixels, metrics.heightPixels);
            screenHeight = Math.max(metrics.widthPixels, metrics.heightPixels);
        }
    }

    private void startStreaming() {
        if (!displayBase.isStreaming()) {
            if (displayBase.prepareVideo()==true && displayBase.prepareAudio()){
                displayBase.startStream(YOUTUBE_RTMP_URL);
            }
        }
    }

    public void stopStreaming() {
        if (displayBase.isStreaming()) {
            displayBase.stopStream();
        }
    }

    @Override
    public void onDestroy() {
        stopStreaming();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Streaming")
                .setContentText("Your screen is being shared to YouTube Live")
                .setSmallIcon(R.drawable.apk_logo)
                .setOngoing(true);
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Screen Streaming", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
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
}