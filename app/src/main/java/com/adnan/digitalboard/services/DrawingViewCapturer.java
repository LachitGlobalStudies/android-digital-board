package com.adnan.digitalboard.services;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.adnan.digitalboard.view.DrawingView;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 * Custom WebRTC VideoCapturer that captures frames exclusively from DrawingView
 * instead of capturing the entire device screen.
 */
public class DrawingViewCapturer implements VideoCapturer {
    private static final String TAG = "DrawingViewCapturer";

    private SurfaceTextureHelper surfaceTextureHelper;
    private Context applicationContext;
    private CapturerObserver capturerObserver;
    private Surface surface;
    private int width = 1280;
    private int height = 720;
    private int framerate = 30;
    private boolean isCapturing = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable captureRunnable;
    private DrawingView targetDrawingView;

    public DrawingViewCapturer() {
        this(null);
    }

    public DrawingViewCapturer(DrawingView drawingView) {
        this.targetDrawingView = drawingView;
    }

    public void setDrawingView(DrawingView drawingView) {
        this.targetDrawingView = drawingView;
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.applicationContext = applicationContext;
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int width, int height, int framerate) {
        this.width = width;
        this.height = height;
        this.framerate = framerate;

        if (surfaceTextureHelper == null) {
            Log.e(TAG, "surfaceTextureHelper is null in startCapture");
            if (capturerObserver != null) {
                capturerObserver.onCapturerStarted(false);
            }
            return;
        }

        surfaceTextureHelper.setTextureSize(width, height);
        surface = new Surface(surfaceTextureHelper.getSurfaceTexture());
        surfaceTextureHelper.startListening(new VideoSink() {
            @Override
            public void onFrame(VideoFrame frame) {
                if (capturerObserver != null) {
                    capturerObserver.onFrameCaptured(frame);
                }
            }
        });

        isCapturing = true;
        if (capturerObserver != null) {
            capturerObserver.onCapturerStarted(true);
        }

        final long frameDelayMs = 1000 / (framerate > 0 ? framerate : 30);

        captureRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isCapturing) return;

                renderDrawingViewFrame();

                if (isCapturing) {
                    uiHandler.postDelayed(this, frameDelayMs);
                }
            }
        };

        uiHandler.post(captureRunnable);
        Log.d(TAG, "✅ DrawingViewCapturer started: " + width + "x" + height + " @ " + framerate + "fps");
    }

    private void renderDrawingViewFrame() {
        DrawingView view = targetDrawingView != null ? targetDrawingView : DrawingView.getInstance();
        if (view == null || surface == null || !surface.isValid()) {
            return;
        }

        try {
            Canvas canvas = surface.lockCanvas(null);
            if (canvas != null) {
                // Default base color before DrawingView draws its own background and strokes
                canvas.drawColor(Color.WHITE);

                int vWidth = view.getWidth();
                int vHeight = view.getHeight();
                if (vWidth > 0 && vHeight > 0) {
                    float scale = Math.min((float) width / vWidth, (float) height / vHeight);
                    float dx = (width - vWidth * scale) / 2f;
                    float dy = (height - vHeight * scale) / 2f;

                    canvas.save();
                    canvas.translate(dx, dy);
                    canvas.scale(scale, scale);
                    view.draw(canvas);
                    canvas.restore();
                }

                surface.unlockCanvasAndPost(canvas);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rendering DrawingView frame: " + e.getMessage());
        }
    }

    @Override
    public void stopCapture() throws InterruptedException {
        isCapturing = false;
        if (captureRunnable != null) {
            uiHandler.removeCallbacks(captureRunnable);
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.stopListening();
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (capturerObserver != null) {
            capturerObserver.onCapturerStopped();
        }
        Log.d(TAG, "DrawingViewCapturer stopped");
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        this.width = width;
        this.height = height;
        this.framerate = framerate;
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.setTextureSize(width, height);
        }
    }

    @Override
    public void dispose() {
        isCapturing = false;
        if (captureRunnable != null) {
            uiHandler.removeCallbacks(captureRunnable);
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.stopListening();
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }

    @Override
    public boolean isScreencast() {
        return true;
    }
}
