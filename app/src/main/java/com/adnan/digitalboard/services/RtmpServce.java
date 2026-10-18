package com.adnan.digitalboard.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.pedro.common.ConnectChecker;
import com.pedro.library.base.DisplayBase;
import com.pedro.library.rtmp.RtmpDisplay;

public class RtmpServce extends Service implements ConnectChecker {
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final String DATA = "DATA";
    private static String RTMP_URL="RTMP_URL";
    private static final String ACTION = "ACTION";
    private static final String START = "START";
    private DisplayBase displayBase;
    public static Intent getInternt(Context context, int resultCode, Intent data, String rtmpUrl) {
        Intent intent=new Intent(context,RtmpServce.class);
        intent.putExtra(ACTION, START);
        intent.putExtra(RESULT_CODE, resultCode);
        intent.putExtra(DATA, data);
        intent.putExtra(RTMP_URL, rtmpUrl);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        displayBase=new RtmpDisplay(getApplicationContext(),true,this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStartCommand(intent)) {
            Pair<Integer, Notification> notification = NotificationUtils.getNotification(this);
            startForeground(notification.first, notification.second);
            int resultCode = intent.getIntExtra(RESULT_CODE, -1);
            Intent data = intent.getParcelableExtra(DATA);
            String rtmp_url = intent.getStringExtra(RTMP_URL);
            startSreaming(resultCode, data, rtmp_url);

        }
        return START_NOT_STICKY;
    }

    private void startSreaming(int resultCode, Intent data, String rtmpUrl) {

        displayBase.setIntentResult(resultCode,data);
        displayBase.prepareVideo();
        displayBase.prepareAudio();
        displayBase.startStream(rtmpUrl);
    }
    private static boolean isStartCommand(Intent intent) {
        return intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
                && intent.hasExtra(ACTION) && START.equals(intent.getStringExtra(ACTION));
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
