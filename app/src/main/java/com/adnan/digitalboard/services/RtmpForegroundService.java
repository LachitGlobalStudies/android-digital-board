package com.adnan.digitalboard.services;

import android.app.Service;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.adnan.digitalboard.R;

public class RtmpForegroundService extends Service {
    private static final int NOTIFICATION_ID = 101;
    private MediaProjection mediaProjection;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }

        if (intent == null) {
            stopSelf(); // Stop service if intent is null
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");

        if (resultCode == -1 || data == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (projectionManager != null) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection != null) {
                Log.d("RTMP_SERVICE", "MediaProjection started successfully!");
            } else {
                Log.e("RTMP_SERVICE", "Failed to start MediaProjection!");
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel("RTMP_CHANNEL", "Screen Streaming", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);

        return new NotificationCompat.Builder(this, "RTMP_CHANNEL")
                .setContentTitle("Screen Streaming Active")
                .setContentText("Your screen is being shared.")
                .setSmallIcon(R.drawable.ic_camera)
                .setOngoing(true)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
