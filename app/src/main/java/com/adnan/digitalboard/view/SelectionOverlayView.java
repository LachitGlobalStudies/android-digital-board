package com.adnan.digitalboard.view;
import android.graphics.*;
import android.view.*;
import android.content.*;
import android.util.*;
import android.app.*;
import java.io.*;
import android.widget.*;

import com.adnan.digitalboard.R;


public class SelectionOverlayView extends View {
	private DrawingView drawingView;
    private Paint paint;
    private float startX, startY, endX, endY;
    private boolean drawing = false;
	

    public SelectionOverlayView(Context context) {
        super(context);
        init();
    }

    public SelectionOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

	public void setDrawingView(DrawingView drawingView)
	{
		// TODO: Implement this method
		this.drawingView=drawingView;
	}

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
		
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (drawing) {
            canvas.drawRect(startX, startY, endX, endY, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                drawing = true;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                endX = event.getX();
                endY = event.getY();
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                endX = event.getX();
                endY = event.getY();
                drawing = false;
                invalidate();
                saveSelectedAreaAsPng();
                return false;
        }
        return false;
    }

    private void saveSelectedAreaAsPng() {
		View rootView = ((Activity) getContext()).getWindow().getDecorView().getRootView();

		Bitmap fullBitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(fullBitmap);
		rootView.draw(canvas);

		// Get location of SelectionOverlayView on screen
		int[] location = new int[2];
		this.getLocationOnScreen(location);
		int offsetX = location[0];
		int offsetY = location[1];

		// Adjust the coordinates
		int left = (int) (Math.min(startX, endX) + offsetX);
		int top = (int) (Math.min(startY, endY) + offsetY);
		int right = (int) (Math.max(startX, endX) + offsetX);
		int bottom = (int) (Math.max(startY, endY) + offsetY);

		int width = right - left;
		int height = bottom - top+30;

		// Check bounds
		if (left >= 0 && top >= 0 && (left + width) <= fullBitmap.getWidth() && (top + height) <= fullBitmap.getHeight()) {
			Bitmap cropped = Bitmap.createBitmap(fullBitmap, left, top, width, height);

			if (drawingView != null) {
				// Instead of
				//drawingView.addImageToCanvas(cropped);

// Use
				Rect targetRect = new Rect(left, top, left + width, top + height);
				drawingView.addImageToCanvas(cropped, targetRect);
			}

            post(new Runnable() {
                @Override
                public void run() {

                    setVisibility(View.GONE);
                    if (drawingView!=null && drawingView.getParent() instanceof ViewGroup){
                        ViewGroup root=(ViewGroup) drawingView.getParent();
                        View webView=root.findViewById(R.id.webView);
                        if (webView!=null){
                            webView.setOnTouchListener(null);
                        }
                    }
                }
            });
		}
	}
    
}
