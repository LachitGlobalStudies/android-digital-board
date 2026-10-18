package com.adnan.digitalboard.view;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.adnan.digitalboard.activity.MainActivity;

public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isConnected(context)){
            showNoInternetDialog(context);
        }

    }

    private void showNoInternetDialog(Context context) {
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again")
                .setCancelable(false)
                .setPositiveButton("Ok",(dialog, which) -> {
                    dialog.dismiss();
                    if (context instanceof MainActivity){
                        ((MainActivity)context).finishAffinity();
                    }else{
                        System.exit(0);
                    }
                }).show();
    }


    private boolean isConnected(Context context) {
        ConnectivityManager connectivityManager=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork=connectivityManager.getActiveNetworkInfo();
        return activeNetwork !=null && activeNetwork.isConnected();
    }
}
