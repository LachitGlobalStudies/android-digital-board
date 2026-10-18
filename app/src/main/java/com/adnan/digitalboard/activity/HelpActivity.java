package com.adnan.digitalboard.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.adnan.digitalboard.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HelpActivity extends AppCompatActivity {
    private WebView webView;
    DatabaseReference databaseReference;
    String videoLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        webView = findViewById(R.id.webView);

        // WebView setup
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true; // Prevents redirects
            }
        });

        // Load video from Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("DigitalBoardHelp");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    videoLink = snapshot.child("videoLink").getValue().toString();
                    String embedded_html = "file:///android_asset/plyr_player.html?videoId=" + videoLink;
                    webView.loadUrl(embedded_html);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Optional: show an error message
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();// Destroy the WebView to release its resources
        if (webView != null) {
            webView.destroy();
        }
    }
}