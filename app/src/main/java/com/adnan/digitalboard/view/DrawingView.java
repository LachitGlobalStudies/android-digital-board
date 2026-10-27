package com.adnan.digitalboard.view;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.util.Base64;
import android.view.*;
import android.graphics.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import android.content.*;
import android.util.*;
import android.widget.*;

import androidx.core.content.ContextCompat;

import com.adnan.digitalboard.R;
import com.adnan.digitalboard.activity.MainActivity;

import org.json.JSONArray;
import org.json.JSONObject;


public class DrawingView extends View {
    private List<Path> paths;   // List of paths for each page
    private List<Stack<Path>> undoStacks; // Stack to manage undo for each page
    private List<Stack<Path>> redoStacks; // Stack to manage redo for each page
    private Bitmap imageToDraw;
    private Rect imageRect;
	private String currentPdfPath = null;
    private Paint drawPaint;
    private Path currentPath;
    private Path path;
    private  List<Integer> nonErasableImageIndices=new ArrayList<>();
    private RectF shapeBounds=null;
    private String currentProjectPath = null;
    private MainActivity mainActivity;
	private Stack<Bitmap> undoStack = new Stack<>();
    private Stack<Bitmap> redoStack = new Stack<>();
	private Path erasePath=new Path();
    private List<Page> pages = new ArrayList<>();
    private Canvas drawCanvas;
    private RectF rectF;
    private Paint  selectionPaint,dotPaint;
    private int currentPageIndex = 0;
    private static float currentStrokeWidth = 3; // Default stroke width
    private boolean  ellipsMode=false, dotLineMode=false,cylinderMode=false,coneMode=false, cubeMode=false, isResizing = false, isMoving = false,eraseMode=false,circleMode=false, paintMode=true,lineMode=false,triangleMode=false,rectangleMode=false,sphereMode=false,hemisphereMode=false;
    private float lastTouchX, lastTouchY,startX,startY,endX,endY;
    private float frontLeft,frontTop,frontRight,frontBottom,backLeft,backTop,backRight,backBottom,cubeDepth,midPointX,midPointY;
    private final int resizeThreshold = 50;
    private String resizeMode = null;
    private int selectedImageIndex = -1; // Index of the selected image for resizing or moving
    private int selectedPathIndex = -1; // Index of the selected image for resizing or moving
    private int width, height;
	private GestureDetector gestureDetector;
    private int currentColor=Color.BLUE;
    private float radius=0;
    private RectF deleteIconRect;
    private Bitmap deleteIcon;
    boolean showDeletIcon=false;
    private boolean isMoveDrawing=false, cropMode = false;
    private Rect cropRect = new Rect(); // Holds the cropping area
    private Paint cropRectPaint;
    private boolean drawingMoveMode = false;
    private float drawingMoveStartX, drawingMoveStartY;
    private Bitmap currentDrawingBitmap;  // To hold the bitmap of the current drawing being moved
    private RectF drawingBounds;          // To define the bounds of the drawing being moved
    private boolean isMovingPath=false, highlightMode = false;
    private Paint highlightPaint;
    private float totalTranslateX=0f;
    private float totalTranslateY=0f;
	private boolean arrowMode = false;
	private float canvasOffsetX = 0f;
	private float canvasOffsetY = 0f;
	private boolean isPanning = false;
	private float panStartX, panStartY;
	private boolean singlePageMode = false;
	private boolean isCanvasMoving = false;
    private Matrix transformMatrix = new Matrix();
    private Matrix inverseMatrix = new Matrix();



    private static DrawingView activeInstance;

    public static DrawingView getInstance() {
        return activeInstance;
    }

    public static void setInstance(DrawingView instance) {
        activeInstance = instance;
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activeInstance = this;
        if (context instanceof MainActivity) {
            this.mainActivity = (MainActivity) context;
        }
		undoStack = new Stack<>();
        redoStack = new Stack<>();
		//image crop paint
        cropRectPaint = new Paint();
        cropRectPaint.setColor(Color.BLACK); // Semi-transparent color for non-crop area
        cropRectPaint.setAlpha(128);        // Adjust transparency
        cropRectPaint.setStyle(Paint.Style.FILL);


        // Initialize paint for drawing
        drawPaint = new Paint();
        drawPaint.setColor(currentColor);  // Current drawing color
        drawPaint.setStyle(Paint.Style.STROKE);  // Use stroke style for lines and shapes
        drawPaint.setStrokeWidth(currentStrokeWidth);  // Dynamic stroke width
        drawPaint.setAntiAlias(true);  // Enable smooth drawing
//        drawPaint.setDither(true);  // For gradient smoothing
//        drawPaint.setStrokeJoin(Paint.Join.ROUND);  // Smooth joint
//        drawPaint.setStrokeCap(Paint.Cap.ROUND);  // Smooth line ends

        shapeBounds=new RectF(frontLeft,backTop,backRight,frontBottom);
        // Initialize paint for selection rectangle
        selectionPaint = new Paint();
        selectionPaint.setColor(Color.RED); // Selection rectangle color
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(5f); // Thickness of the selection border
        setBackgroundColor(Color.WHITE);
        rectF=new RectF(10,10,300,300);
        dotPaint=new Paint();
        dotPaint.setColor(Color.BLUE);
        dotPaint.setStrokeWidth(3f);
        dotPaint.setAntiAlias(true);
        dotPaint.setPathEffect(new DashPathEffect(new float[]{10,10},0));
        deleteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.lachit_logo);
        deleteIconRect = new RectF(10, 10, 70, 70); // Adjust the size and position of the delete icon
		gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                Page currentPage = pages.get(currentPageIndex);

                for (int i = currentPage.imageRects.size() - 1; i >= 0; i--) {
                    Rect rect = currentPage.imageRects.get(i);
                    if (rect.contains((int) x, (int) y)) {
                        Bitmap originalImage = currentPage.images.get(i);
                        if (originalImage != null) {
                            // Copy the image and add it 30dp right and bottom
                            float scale = getResources().getDisplayMetrics().density;
                            int offset = (int) (30 * scale + 0.5f); // Convert 30dp to pixels

                            Rect newRect = new Rect(rect.left + offset, rect.top + offset, rect.right + offset, rect.bottom + offset);
                            currentPage.images.add(originalImage); // Reuse bitmap as it's immutable
                            currentPage.imageRects.add(newRect);
                            invalidate();
                            return true; // Image copied and added
                        }
                    }
                }
                return super.onDoubleTap(event);
            }

        });
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        // Apply transformation if in singlePageMode and canvas should move
        if (singlePageMode && isCanvasMoving) {
            canvas.concat(transformMatrix);  // Apply the transformation matrix
            canvas.translate(canvasOffsetX, canvasOffsetY); // Additional offset if needed
        }

        canvas.concat(transformMatrix); // Apply matrix
		// Apply the canvas translation
		canvas.translate(canvasOffsetX, canvasOffsetY);	
        canvas.concat(transformMatrix);
        Page currentPage = pages.get(currentPageIndex);

        for (int i = 0; i < currentPage.paths.size(); i++) {
            canvas.drawPath(currentPage.paths.get(i), currentPage.pathPaints.get(i));
        }

        // Draw all images
        for (int i = 0; i < currentPage.images.size(); i++) {
            canvas.drawBitmap(currentPage.images.get(i), null, currentPage.imageRects.get(i), null);

            // Draw selection rectangle if this image is selected
            if (i == selectedImageIndex && !isMoving) {
                canvas.drawRect(currentPage.imageRects.get(i), selectionPaint);
            }
        }

        // Draw the page's content
        if (currentPage.contentBitmap != null) {
            canvas.drawBitmap(currentPage.contentBitmap, 0, 0, null);
        }

        // Show the eraser path (if active)
        if (eraseMode) {
            Paint fillPaint = new Paint();
            fillPaint.setColor(Color.argb(128, 255, 100, 100)); // Light reddish color with transparency
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setAntiAlias(true);
            Paint dashedPaint = new Paint();
            dashedPaint.setColor(Color.RED);
            dashedPaint.setStyle(Paint.Style.STROKE);
            dashedPaint.setStrokeWidth(3f);
            dashedPaint.setAntiAlias(true);
            dashedPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
            canvas.drawPath(erasePath, fillPaint);
            canvas.drawPath(erasePath, dashedPaint);

        } else if (circleMode) {
            canvas.drawCircle(lastTouchX,lastTouchY,radius,drawPaint);
            Paint dottedLinePaint=new Paint();
            dottedLinePaint.setColor(currentColor);
            dottedLinePaint.setStyle(Paint.Style.STROKE);
            dottedLinePaint.setStrokeWidth(2f);
            dottedLinePaint.setAntiAlias(true);
            dottedLinePaint.setPathEffect(new DashPathEffect(new float[]{10,20},0));
            Paint centerPaint= new Paint();
            centerPaint.setColor(Color.RED);
            centerPaint.setAntiAlias(true);
            centerPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(lastTouchX,lastTouchY,5f,centerPaint);
            canvas.drawLine(lastTouchX,lastTouchY,lastTouchX+radius,lastTouchY,dottedLinePaint);

        } else if (triangleMode) {

            // Draw the triangle sides
            canvas.drawLine(lastTouchX, lastTouchY, endX, endY, drawPaint);      // Hypotenuse
            canvas.drawLine(lastTouchX, lastTouchY, lastTouchX-(endX-lastTouchX), endY, drawPaint); // Left side
            canvas.drawLine(lastTouchX-(endX-lastTouchX), endY, endX, endY, drawPaint);        // Right side
        }else if (hemisphereMode) {
            RectF oval = new RectF(lastTouchX - radius, lastTouchY - radius, lastTouchX + radius, lastTouchY);
            canvas.drawArc(oval, 0, 180, true, drawPaint);
        }else if (rectangleMode){
            canvas.drawRect(lastTouchX,lastTouchY,endX,endY,drawPaint);
        }else if (cubeMode) {

            // Paint for dotted lines
            Paint dottedPaint = new Paint();
            dottedPaint.setColor(currentColor); // Dotted line color
            dottedPaint.setStyle(Paint.Style.STROKE);
            dottedPaint.setStrokeWidth(4f);
            dottedPaint.setAntiAlias(true);
            dottedPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0)); // Dotted effect
            canvas.drawLine(backLeft,backTop,backRight,backTop,drawPaint);
            canvas.drawLine(backRight,backTop,backRight,backBottom,drawPaint);
            canvas.drawLine(backLeft,backTop,backLeft,backBottom,dottedPaint);
            canvas.drawLine(backLeft,backBottom,backRight,backBottom,dottedPaint);
            canvas.drawLine(backLeft,backBottom,frontLeft,frontBottom,dottedPaint);
            canvas.drawLine(frontLeft,frontTop,backLeft,backTop,drawPaint);
            canvas.drawLine(frontRight,frontTop,backRight,backTop,drawPaint);
            canvas.drawLine(frontRight,frontBottom,backRight,backBottom,drawPaint);
            canvas.drawRect(frontLeft,frontTop,frontRight,frontBottom,drawPaint);
        }else if(sphereMode){
            canvas.drawCircle(lastTouchX, lastTouchY, radius, drawPaint);
            // Dotted line for the radius
            Paint dottedLinePaint = new Paint();
            dottedLinePaint.setColor(currentColor);
            dottedLinePaint.setStyle(Paint.Style.STROKE);
            dottedLinePaint.setStrokeWidth(2f);
            dottedLinePaint.setAntiAlias(true);
            dottedLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
            Paint cp=new Paint();
            cp.setColor(Color.RED);
            cp.setAntiAlias(true);
            cp.setStyle(Paint.Style.FILL);
            canvas.drawCircle(lastTouchX,lastTouchY,5f,cp);
            canvas.drawLine(lastTouchX, lastTouchY, lastTouchX + radius, lastTouchY, dottedLinePaint);
            // Define the bounds for the equator (an ellipse)
            rectF = new RectF(lastTouchX - radius, lastTouchY - radius / 3, lastTouchX + radius, lastTouchY + radius / 3);

            // Draw the equator
            //canvas.drawOval(rectF, drawPaint);
            canvas.drawArc(rectF,0,180,false,drawPaint);
            canvas.drawArc(rectF,180,180,false,dottedLinePaint);

        }else if(lineMode){
            canvas.drawLine(lastTouchX,lastTouchY,endX,endY,drawPaint);
        }else if(dotLineMode){
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setPathEffect(new DashPathEffect(new float[]{10,10},0)); // Set dash effect
            canvas.drawLine(lastTouchX,lastTouchY,endX,endY,dotPaint);
        }else if(coneMode){
            // Dotted line for the radius
            Paint dottedLinePaint = new Paint();
            dottedLinePaint.setColor(currentColor);
            dottedLinePaint.setStyle(Paint.Style.STROKE);
            dottedLinePaint.setStrokeWidth(2f);
            dottedLinePaint.setAntiAlias(true);
            dottedLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));

            float leftX=lastTouchX-(endX-lastTouchX);
            float arcHeight=(leftX+endX)/10;
            rectF=new RectF(leftX,endY-arcHeight,endX,endY+arcHeight);
            canvas.drawLine(lastTouchX,lastTouchY,endX,endY,drawPaint);
            canvas.drawLine(lastTouchX,lastTouchY,leftX,endY,drawPaint);
            canvas.drawLine(lastTouchX,lastTouchY,lastTouchX,endY,dotPaint);
            canvas.drawLine(lastTouchX,endY,endX,endY,dotPaint);
            canvas.drawArc(rectF,0,180,false,drawPaint);
            canvas.drawArc(rectF,180,180,false,dottedLinePaint);

        }else if (cylinderMode) {
            // Calculate cylinder dimensions
            float cylinderWidth = Math.abs(endX - lastTouchX);
            float cylinderHeight = Math.abs(endY - lastTouchY);
            radius = cylinderWidth / 2;
            float centerX = (lastTouchX + endX) / 2;

            // Top oval
            RectF topOval = new RectF(
                    centerX - radius,
                    lastTouchY,
                    centerX + radius,
                    lastTouchY + radius / 3
            );

            // Bottom oval
            RectF bottomOval = new RectF(
                    centerX - radius,
                    lastTouchY + cylinderHeight,
                    centerX + radius,
                    lastTouchY + cylinderHeight + radius / 3
            );

            // Draw the cylinder
            canvas.drawOval(topOval, drawPaint);
            canvas.drawOval(bottomOval, drawPaint);
            canvas.drawLine(topOval.left, topOval.bottom, bottomOval.left, bottomOval.top, drawPaint);
            canvas.drawLine(topOval.right, topOval.bottom, bottomOval.right, bottomOval.top, drawPaint);
        } else if (ellipsMode) {
			// Calculate ellipse dimensions
			float left = Math.min(lastTouchX, endX);
			float top = Math.min(lastTouchY, endY);
			float right = Math.max(lastTouchX, endX);
			float bottom = Math.max(lastTouchY, endY);
			
			RectF oval = new RectF(left, top, right, bottom);
			canvas.drawOval(oval, drawPaint);
        } else if (showDeletIcon) {
            canvas.drawBitmap(deleteIcon, null, deleteIconRect, null);
        }else if (cropMode) {
            canvas.drawRect(0, 0, width, cropRect.top, cropRectPaint); // Top
            canvas.drawRect(0, cropRect.bottom, width, height, cropRectPaint); // Bottom
            canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, cropRectPaint); // Left
            canvas.drawRect(cropRect.right, cropRect.top, width, cropRect.bottom, cropRectPaint); // Right
            canvas.drawRect(cropRect, selectionPaint); // Highlight crop rectangle
        }else if (paintMode && path != null) {
            canvas.drawPath(path, drawPaint);
        }else if (arrowMode) {
			// Draw the arrow line
			canvas.drawLine(lastTouchX, lastTouchY, endX, endY, drawPaint);
			
			// Draw arrow head
			drawArrowHead(canvas, lastTouchX, lastTouchY, endX, endY, drawPaint);
		}

canvas.restore();

    }
public void resetTransform() {
    canvasOffsetX = 0f;
    canvasOffsetY = 0f;
    invalidate();
}
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mainActivity.pageLayout.getVisibility()==View.VISIBLE){
            mainActivity.pageLayout.setVisibility(View.GONE);
        }
if (isMoveDrawing) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Find the image or path located at (touchX, touchY)
                selectItemAt(touchX, touchY);
                lastTouchX = touchX;
                lastTouchY = touchY;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (selectedImageIndex != null) {
                    float dx = touchX - lastTouchX;
                    float dy = touchY - lastTouchY;

                    // Translate/move the selected image or path
                    moveSelectedItem(dx, dy);

                    lastTouchX = touchX;
                    lastTouchY = touchY;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                // Finalize position
                return true;
        }
    }

    // Fall back to regular drawing/paint logic when isMoveMode is false
    return super.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();
        Page currentPage = pages.get(currentPageIndex);
		// Apply inverse transformation to get actual canvas coordinates
		float canvasX = x - canvasOffsetX;
		float canvasY = y - canvasOffsetY;
        if (singlePageMode && isCanvasMoving) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    transformMatrix.postTranslate(dx, dy);
                    transformMatrix.invert(inverseMatrix);
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                    return true;

                case MotionEvent.ACTION_UP:
                    isCanvasMoving = false;
                    singlePageMode=false;
                    paintMode=true;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    isCanvasMoving = false;
                    singlePageMode=false;
                    break;
            }
            return true;
        } else{

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                saveToUndoStack(); // Save the state when starting a new action
                if( circleMode||cubeMode||triangleMode||sphereMode||lineMode||dotLineMode||coneMode||ellipsMode||rectangleMode){
                    lastTouchX=x;
                    lastTouchY=y;
                }else if (cylinderMode) {
                    endX = x;
                    endY = y;
                    invalidate();
                } else if (paintMode) {
                    path = new Path();
                    path.moveTo(x, y);
                    lastTouchX = x;
                    lastTouchY = y;
                }else if (eraseMode) {
                    erasePath.reset();
                    erasePath.moveTo(x, y);
                }else  if (cropMode) {
                    startX = x;
                    startY = y;
                }else if (highlightMode) {
                    lastTouchX = x;
                    lastTouchY = y;
                    saveToUndoStack();  // Save state before drawing
                }else  if (arrowMode) {
						lastTouchX = x;
						lastTouchY = y;
					}
                break;

            case MotionEvent.ACTION_MOVE:
             if (isResizing && resizeMode != null && selectedImageIndex != -1) {
                    resizeImage(x, y, currentPage.imageRects.get(selectedImageIndex));
                } else if (isMoving && selectedImageIndex != -1) {
                    moveImage(x - lastTouchX, y - lastTouchY, currentPage.imageRects.get(selectedImageIndex));
                    lastTouchX = x;
                    lastTouchY = y;
                }else  if (paintMode) {
                    if (path != null) {
                        path.lineTo(x, y);
                        //drawCanvas.drawPath(path, drawPaint); // Draw on canvas
                    }
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                } else if (eraseMode) {
                    erasePath.lineTo(x, y);
                    invalidate();
                } else if(cubeMode){
                    endX=x;
                    endY=y;
                    cubeDepth=Math.abs(endX-lastTouchX)/2;
                    frontLeft=lastTouchX;
                    frontTop=lastTouchY;
                    frontRight=endX;
                    frontBottom=endY;
                    backLeft=frontLeft+cubeDepth;
                    backTop=frontTop-cubeDepth;
                    backRight=frontRight+cubeDepth;
                    backBottom=frontBottom-cubeDepth;
                    invalidate();
                }else if (circleMode) {
                    radius= (float) Math.hypot(x-lastTouchX,y-lastTouchY);
                    invalidate();
                }else if(sphereMode){
                    radius = (float) Math.hypot(x - lastTouchX, y - lastTouchY);
                    invalidate(); // Redraw to show the circle preview
                }else if ( rectangleMode ||coneMode||cylinderMode) {
                    endX = x;
                    endY = y;
                    invalidate();
                }else if (ellipsMode) {
					endX = x;
					endY = y;
					invalidate();
				}else if (lineMode || dotLineMode) {
                 endX = x;
                 endY = y;
                 // Update shape bounds continuously
                 shapeBounds = new RectF(
                         Math.min(lastTouchX, endX) - 2,
                         Math.min(lastTouchY, endY) - 2,
                         Math.max(lastTouchX, endX) + 2,
                         Math.max(lastTouchY, endY) + 2
                 );
                 invalidate();
             }else if(triangleMode){
                    endX=x;
                    endY=y;
                    invalidate();

                }else  if (cropMode) {
                    endX = x;
                    endY = y;
                    cropRect.set((int) Math.min(startX, endX), (int) Math.min(startY, endY),
                            (int) Math.max(startX, endX), (int) Math.max(startY, endY));
                    invalidate();
                }else  if (drawingMoveMode && drawingBounds != null) {
                    float dx = event.getX() - drawingMoveStartX;
                    float dy = event.getY() - drawingMoveStartY;

                    drawingBounds.offset(dx, dy);  // Move the bounds of the drawing
                    drawingMoveStartX = event.getX();
                    drawingMoveStartY = event.getY();
                    invalidate();
                }else  if (highlightMode) {
                    drawCanvas.drawLine(lastTouchX, lastTouchY, x, y, highlightPaint);  // Draw highlight
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                }else  if (arrowMode) {
					endX = x;
					endY = y;
					invalidate();
				}
			 
                break;

            case MotionEvent.ACTION_UP:
                if (arrowMode) {
                    // Calculate arrow head points
                    float angle = (float) Math.atan2(endY - lastTouchY, endX - lastTouchX);
                    float arrowHeadLength = 30f;
                    float x1 = (float) (endX - arrowHeadLength * Math.cos(angle - Math.PI/6));
                    float y1 = (float) (endY - arrowHeadLength * Math.sin(angle - Math.PI/6));
                    float x2 = (float) (endX - arrowHeadLength * Math.cos(angle + Math.PI/6));
                    float y2 = (float) (endY - arrowHeadLength * Math.sin(angle + Math.PI/6));

                    // Calculate bounds
                    float minX = Math.min(Math.min(lastTouchX, endX), Math.min(x1, x2));
                    float minY = Math.min(Math.min(lastTouchY, endY), Math.min(y1, y2));
                    float maxX = Math.max(Math.max(lastTouchX, endX), Math.max(x1, x2));
                    float maxY = Math.max(Math.max(lastTouchY, endY), Math.max(y1, y2));

                    shapeBounds = new RectF(
                            minX - currentStrokeWidth,
                            minY - currentStrokeWidth,
                            maxX + currentStrokeWidth,
                            maxY + currentStrokeWidth
                    );
                    replaceDrawingWithImage();
                    invalidate();
                }else if (eraseMode && drawCanvas != null) {
                    applyErase();
                } else if (circleMode && drawCanvas!=null) {
                   shapeBounds=new RectF(lastTouchX-radius-2,lastTouchY-radius-2,lastTouchX+radius+2,lastTouchY+radius+2);
                   replaceDrawingWithImage();

                } else if (rectangleMode) {
                    shapeBounds=new RectF(lastTouchX-2,lastTouchY-2,endX+2,endY+2);
                    replaceDrawingWithImage();
            } else if(triangleMode){
                    // Draw the triangle sides
                    shapeBounds =new RectF(lastTouchX-(endX-lastTouchX)-2,lastTouchY-2,endX+2,endY+2);
                  replaceDrawingWithImage();
                }  else if (hemisphereMode) {
                RectF oval = new RectF(startX - radius, startY - radius, startX + radius, startY);
                drawCanvas.drawArc(oval, 0, 180, true, drawPaint);
                    invalidate();
            }else if(cubeMode){
                    shapeBounds=new RectF(frontLeft-2,backTop-2,backRight+2,frontBottom+2);
                    replaceDrawingWithImage();
                }else if(sphereMode){
                   shapeBounds=new RectF(lastTouchX-radius-2,lastTouchY-radius-2,lastTouchX+radius+2,lastTouchY+radius+2);
                   replaceDrawingWithImage();
                }else if(lineMode || dotLineMode) {
                    shapeBounds = new RectF(
                            Math.min(lastTouchX, endX) - 2,
                            Math.min(lastTouchY, endY) - 2,
                            Math.max(lastTouchX, endX) + 2,
                            Math.max(lastTouchY, endY) + 2
                    );
                    replaceDrawingWithImage();
                    invalidate();
                }else if(coneMode){
                    float leftX=lastTouchX-(endX-lastTouchX);
                    float arcHeight=(leftX+endX)/10;
                    shapeBounds=new RectF(lastTouchX-(endX-lastTouchX)-2,lastTouchY-2,endX+2,endY+arcHeight+2);
                   replaceDrawingWithImage();
                }else if (cylinderMode) {
                    float cylinderWidth = Math.abs(endX - lastTouchX);
                    float cylinderHeight = Math.abs(endY - lastTouchY);
                    radius = cylinderWidth / 2;
                    float centerX = (lastTouchX + endX) / 2;

                    shapeBounds = new RectF(
                            centerX - radius - 2,
                            lastTouchY - 2,
                            centerX + radius + 2,
                            lastTouchY + cylinderHeight + radius/3 + 2
                    );
                    replaceDrawingWithImage();
                    invalidate();
                }else if (ellipsMode) {
					// Calculate bounds with some padding
					shapeBounds = new RectF(
						Math.min(lastTouchX, endX) - 2,
						Math.min(lastTouchY, endY) - 2,
						Math.max(lastTouchX, endX) + 2,
						Math.max(lastTouchY, endY) + 2
					);
					replaceDrawingWithImage();
					invalidate();
				}else  if (cropMode) {
                    applyCrop();
                    cropMode = false;

                }else  if (drawingMoveMode) {
                    applyMovedDrawing();  // Apply the new position of the drawing
                    drawingMoveMode = false;

                }else if (highlightMode) {
                    drawCanvas.drawLine(lastTouchX, lastTouchY, x, y, highlightPaint);
                    invalidate();
                } else if (paintMode && path != null) {
                    convertPathToImage(path, drawPaint);
                    path = null; // Reset for the next stroke
                }else if(selectedImageIndex != -1 && deleteIconRect.contains(x, y)) {
                    currentPage.images.remove(selectedImageIndex);

                }
                showDeletIcon=false;
                isResizing = false;
                isMoving = false;
                resizeMode = null;
                invalidate();


                break;
        }
        return true;
    }
}
    public void addImageToCanvas(Bitmap bitmap, Rect rect) {
        this.imageToDraw = bitmap;
        this.imageRect = rect;
        Page currentPage = pages.get(currentPageIndex);
        currentPage.images.add(imageToDraw);
        currentPage.imageRects.add(imageRect);
        invalidate(); // This will call onDraw
    }
    private void convertPathToImage(Path path, Paint paint) {
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        int width = (int) bounds.width() + 10;
        int height = (int) bounds.height() + 10;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Offset path to top-left corner of bitmap
        Matrix matrix = new Matrix();
        matrix.postTranslate(-bounds.left + 5, -bounds.top + 5);
        Path offsetPath = new Path();
        path.transform(matrix, offsetPath);

        canvas.drawPath(offsetPath, paint);

        RectF finalRect = new RectF(bounds.left - 5, bounds.top - 5, bounds.right + 5, bounds.bottom + 5);
        addImageToCanvas(bmp, finalRect);
    }
private void drawArrowHead(Canvas canvas, float startX, float startY, float endX, float endY, Paint paint) {
    float angle = (float) Math.atan2(endY - startY, endX - startX);
    float arrowHeadLength = 30f; // Length of arrow head lines
    
    // Calculate points for arrow head
    float x1 = (float) (endX - arrowHeadLength * Math.cos(angle - Math.PI/6));
    float y1 = (float) (endY - arrowHeadLength * Math.sin(angle - Math.PI/6));
    float x2 = (float) (endX - arrowHeadLength * Math.cos(angle + Math.PI/6));
    float y2 = (float) (endY - arrowHeadLength * Math.sin(angle + Math.PI/6));
    
    // Draw arrow head lines
    canvas.drawLine(endX, endY, x1, y1, paint);
    canvas.drawLine(endX, endY, x2, y2, paint);
}

    private void replaceDrawingWithImage() {
        if (shapeBounds!=null){
            try {
                int shapeWidth=(int)shapeBounds.width();
                int shapeHeight=(int) shapeBounds.height();
                Bitmap shapeBitmap=Bitmap.createBitmap(shapeWidth,shapeHeight,Bitmap.Config.ARGB_8888);
                Canvas bitmapCanvas=new Canvas(shapeBitmap);
                Paint tempPaint=new Paint(drawPaint);
                tempPaint.setStyle(Paint.Style.STROKE);
                bitmapCanvas.translate(-shapeBounds.left,-shapeBounds.top);
                drawShape(bitmapCanvas,tempPaint);
                addImageToCanvas(shapeBitmap,shapeBounds);
            }catch (Exception e){

            }
        }
    }
private void saveImageStateForUndo() {
    Page currentPage = pages.get(currentPageIndex);

    // Deep copy image list
    List<Bitmap> copiedImages = new ArrayList<>();
    for (Bitmap bmp : currentPage.images) {
        copiedImages.add(bmp.copy(Bitmap.Config.ARGB_8888, true));
    }

    // Deep copy rects
    List<Rect> copiedRects = new ArrayList<>();
    for (Rect rect : currentPage.imageRects) {
        copiedRects.add(new Rect(rect));
    }

    currentPage.undoImageStack.push(copiedImages);
    currentPage.undoRectStack.push(copiedRects);

    currentPage.redoImageStack.clear();
    currentPage.redoRectStack.clear();
}
public void undoImage() {
    Page currentPage = pages.get(currentPageIndex);
    if (!currentPage.undoImageStack.isEmpty()) {
        currentPage.redoImageStack.push(new ArrayList<>(currentPage.images));
        currentPage.redoRectStack.push(new ArrayList<>(currentPage.imageRects));

        currentPage.images = currentPage.undoImageStack.pop();
        currentPage.imageRects = currentPage.undoRectStack.pop();

        invalidate();
    }
}
public void redoImage() {
    Page currentPage = pages.get(currentPageIndex);
    if (!currentPage.redoImageStack.isEmpty()) {
        currentPage.undoImageStack.push(new ArrayList<>(currentPage.images));
        currentPage.undoRectStack.push(new ArrayList<>(currentPage.imageRects));

        currentPage.images = currentPage.redoImageStack.pop();
        currentPage.imageRects = currentPage.redoRectStack.pop();

        invalidate();
    }
}

    private void addImageToCanvas(Bitmap bitmap, RectF position) {
        if (bitmap != null && position != null) {
            saveImageStateForUndo(); // <-- ADD THIS LINE BEFORE MODIFICATION
            Page currentPage = pages.get(currentPageIndex);
            currentPage.images.add(bitmap);
            currentPage.imageRects.add(new Rect(
                    (int) position.left,
                    (int) position.top,
                    (int) position.right,
                    (int) position.bottom
            ));
            invalidate();
        } else {
            Toast.makeText(getContext(), "Invalid image or position", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawShape(Canvas canvas, Paint paint) {
        if (arrowMode) {
        // Draw the arrow line
        canvas.drawLine(lastTouchX, lastTouchY, endX, endY, paint);
        
        // Draw arrow head
        drawArrowHead(canvas, lastTouchX, lastTouchY, endX, endY, paint);
    }else if (lineMode){
            // Change from dotPaint to drawPaint for regular lines
            canvas.drawLine(lastTouchX, lastTouchY, endX, endY, drawPaint); // <-- Fix here
        } else if (dotLineMode) {
            Paint dotPaint = new Paint();
            dotPaint.setColor(currentColor);
            dotPaint.setStrokeWidth(3f);
            dotPaint.setAntiAlias(true);
            dotPaint.setPathEffect(new DashPathEffect(new float[]{10,10},0));
            canvas.drawLine(lastTouchX, lastTouchY, endX, endY, dotPaint);
        } else if (ellipsMode) {
				float left = Math.min(lastTouchX, endX);
				float top = Math.min(lastTouchY, endY);
				float right = Math.max(lastTouchX, endX);
				float bottom = Math.max(lastTouchY, endY);
				
				RectF oval = new RectF(left, top, right, bottom);
				canvas.drawOval(oval, paint);
			}else if(coneMode) {
            // Dotted line for the radius
            Paint dottedLinePaint = new Paint();
            dottedLinePaint.setColor(currentColor);
            dottedLinePaint.setStyle(Paint.Style.STROKE);
            dottedLinePaint.setStrokeWidth(2f);
            dottedLinePaint.setAntiAlias(true);
            dottedLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
            float leftX=lastTouchX-(endX-lastTouchX);
            float arcHeight=(leftX+endX)/10;
            rectF=new RectF(leftX,endY-arcHeight,endX,endY+arcHeight);
            canvas.drawLine(lastTouchX,lastTouchY,endX,endY,drawPaint);
            canvas.drawLine(lastTouchX,lastTouchY,leftX,endY,drawPaint);
            canvas.drawLine(lastTouchX,lastTouchY,lastTouchX,endY,dotPaint);
            canvas.drawLine(lastTouchX,endY,endX,endY,dotPaint);
            canvas.drawArc(rectF,0,180,false,drawPaint);
            canvas.drawArc(rectF,180,180,false,dottedLinePaint);
        }else if (triangleMode){
                canvas.drawLine(lastTouchX, lastTouchY, endX, endY, drawPaint);      // Hypotenuse
                canvas.drawLine(lastTouchX, lastTouchY, lastTouchX-(endX-lastTouchX), endY, drawPaint); // Left side
                canvas.drawLine(lastTouchX-(endX-lastTouchX), endY, endX, endY, drawPaint);        // Right side
            }else if (cubeMode){
                // Paint for dotted lines
                Paint dottedPaint = new Paint();
                dottedPaint.setColor(currentColor); // Dotted line color
                dottedPaint.setStyle(Paint.Style.STROKE);
                dottedPaint.setStrokeWidth(4f);
                dottedPaint.setAntiAlias(true);
                dottedPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0)); // Dotted effect
                canvas.drawLine(backLeft,backTop,backRight,backTop,paint);
                canvas.drawLine(backRight,backTop,backRight,backBottom,paint);
                canvas.drawLine(backLeft,backTop,backLeft,backBottom,dottedPaint);
                canvas.drawLine(backLeft,backBottom,backRight,backBottom,dottedPaint);
                canvas.drawLine(backLeft,backBottom,frontLeft,frontBottom,dottedPaint);
                canvas.drawLine(frontLeft,frontTop,backLeft,backTop,paint);
                canvas.drawLine(frontRight,frontTop,backRight,backTop,paint);
                canvas.drawLine(frontRight,frontBottom,backRight,backBottom,paint);
                canvas.drawRect(frontLeft,frontTop,frontRight,frontBottom,paint);
            } else if (circleMode) {
                canvas.drawCircle(lastTouchX,lastTouchY,radius,paint);
                Paint dottedLinePaint=new Paint();
                dottedLinePaint.setColor(currentColor);
                dottedLinePaint.setStyle(Paint.Style.STROKE);
                dottedLinePaint.setStrokeWidth(2f);
                dottedLinePaint.setAntiAlias(true);
                dottedLinePaint.setPathEffect(new DashPathEffect(new float[]{10,20},0));
                Paint centerPaint= new Paint();
                centerPaint.setColor(Color.RED);
                centerPaint.setAntiAlias(true);
                centerPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(lastTouchX,lastTouchY,5f,centerPaint);
                canvas.drawLine(lastTouchX,lastTouchY,lastTouchX+radius,lastTouchY,dottedLinePaint);


            } else if (rectangleMode) {
                canvas.drawRect(lastTouchX,lastTouchY,endX,endY,paint);
            } else if (sphereMode) {

                canvas.drawCircle(lastTouchX, lastTouchY, radius, paint);
                // Dotted line for the radius
                Paint dottedLinePaint = new Paint();
                dottedLinePaint.setColor(currentColor);
                dottedLinePaint.setStyle(Paint.Style.STROKE);
                dottedLinePaint.setStrokeWidth(2f);
                dottedLinePaint.setAntiAlias(true);
                dottedLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
                Paint cp=new Paint();
                cp.setColor(Color.RED);
                cp.setAntiAlias(true);
                cp.setStyle(Paint.Style.FILL);
                canvas.drawCircle(lastTouchX,lastTouchY,5f,cp);
                canvas.drawLine(lastTouchX, lastTouchY, lastTouchX + radius, lastTouchY, dottedLinePaint);
                //drawCanvas.drawOval(rectF, drawPaint);
                canvas.drawArc(rectF,0,180,false,paint);
                canvas.drawArc(rectF,180,180,false,dottedLinePaint);

            }else if (cylinderMode) {
            float cylinderWidth = Math.abs(endX - lastTouchX);
            float cylinderHeight = Math.abs(endY - lastTouchY);
            radius = cylinderWidth / 2;
            float centerX = (lastTouchX + endX) / 2;

            // Top oval
            RectF topOval = new RectF(
                    centerX - radius,
                    lastTouchY,
                    centerX + radius,
                    lastTouchY + radius / 3
            );

            // Bottom oval
            RectF bottomOval = new RectF(
                    centerX - radius,
                    lastTouchY + cylinderHeight,
                    centerX + radius,
                    lastTouchY + cylinderHeight + radius / 3
            );

            // Draw the cylinder
            canvas.drawOval(topOval, paint);
            canvas.drawOval(bottomOval, paint);
            canvas.drawLine(topOval.left, topOval.bottom, bottomOval.left, bottomOval.top, paint);
            canvas.drawLine(topOval.right, topOval.bottom, bottomOval.right, bottomOval.top, paint);
        }
    }


    private void applyMovedDrawing() {
        if (drawingBounds != null && currentDrawingBitmap != null && drawCanvas != null) {
            // Clear the previous content
            drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            // Draw the moved bitmap at the new bounds
            drawCanvas.drawBitmap(currentDrawingBitmap, null, drawingBounds, null);
            currentDrawingBitmap = null;  // Reset temporary bitmap
            invalidate();
        }
    }


    private void applyCrop() {
        if (selectedImageIndex != -1 && cropRect != null && !cropRect.isEmpty()) {
            Page currentPage = pages.get(currentPageIndex);

            Bitmap originalBitmap = currentPage.images.remove(selectedImageIndex);
            Rect displayRect = currentPage.imageRects.remove(selectedImageIndex);

            currentPage.images.add(originalBitmap); // Add the image to the end
            currentPage.imageRects.add(displayRect);
            selectedImageIndex = currentPage.images.size() - 1;

            // --- Step 1: Calculate scaling between bitmap and display rect ---
            float scaleX = (float) originalBitmap.getWidth() / displayRect.width();
            float scaleY = (float) originalBitmap.getHeight() / displayRect.height();

            // --- Step 2: Convert cropRect (in screen coordinates) to bitmap coordinates ---
            int cropLeft = (int) ((cropRect.left - displayRect.left) * scaleX);
            int cropTop = (int) ((cropRect.top - displayRect.top) * scaleY);
            int cropWidth = (int) (cropRect.width() * scaleX);
            int cropHeight = (int) (cropRect.height() * scaleY);

            // Clamp values to avoid IllegalArgumentException
            cropLeft = Math.max(0, cropLeft);
            cropTop = Math.max(0, cropTop);
            cropWidth = Math.min(cropWidth, originalBitmap.getWidth() - cropLeft);
            cropHeight = Math.min(cropHeight, originalBitmap.getHeight() - cropTop);

            if (cropWidth > 0 && cropHeight > 0) {
                Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, cropLeft, cropTop, cropWidth, cropHeight);
                originalBitmap.recycle();

                // Update image and rect
                currentPage.images.set(selectedImageIndex, croppedBitmap);
                Rect newRect = new Rect(20, 20, 20 + cropWidth / (int)scaleX, 20 + cropHeight / (int)scaleY);
                currentPage.imageRects.set(selectedImageIndex, newRect);

                // Send to back if needed
                Bitmap croppedImage = currentPage.images.remove(selectedImageIndex);
                Rect croppedRect = currentPage.imageRects.remove(selectedImageIndex);

                currentPage.images.add(0, croppedImage);
                currentPage.imageRects.add(0, croppedRect);
            }

            invalidate();
        }
    }


    public void enableMode(String mode)
	{
		// TODO: Implement this method
		resetModes();

        switch (mode) {
            case "paint":
                paintMode = true;
                selectedImageIndex = -1;
                break;
            case "ellips":
                ellipsMode = true;
                selectedImageIndex = -1;
                break;
            case "erase":
                eraseMode = true;
                selectedImageIndex = -1;
                break;
			case "move_drawing":
				isMoveDrawing=true;
				break;
            case "circle":
                circleMode = true;
                selectedImageIndex = -1;
                break;
            case "cube":
                cubeMode = true;
                selectedImageIndex = -1;
                break;
            case "triangle":
                triangleMode = true;
                selectedImageIndex = -1;
                break;
            case "sphere":
                sphereMode = true;
                selectedImageIndex = -1;
                break;
            case "line":
                lineMode = true;
                drawPaint.setStyle(Paint.Style.STROKE); // Ensure stroke style
                drawPaint.setPathEffect(null); // Remove any path effects
                selectedImageIndex = -1;
                break;
            case "dline":
                dotLineMode = true;

                selectedImageIndex = -1;
                break;
            case "cone":
                coneMode = true;
                selectedImageIndex = -1;
                break;
            case "cylinder":
                cylinderMode = true;
                selectedImageIndex = -1;
                break;
            case "rectangle":
                rectangleMode=true;
                selectedImageIndex = -1;
                break;
            case "crop":
                cropMode=true;
                break;
            case "highlight":
                selectedImageIndex = -1;
                highlightMode = true;
                // highlight paint
                highlightPaint = new Paint();
                highlightPaint.setColor(currentColor);  // Default highlighter color
                highlightPaint.setStyle(Paint.Style.STROKE);
                highlightPaint.setStrokeWidth(60);     // Adjust width for highlighter effect
                highlightPaint.setAlpha(30);           // Semi-transparent for highlight effect
                highlightPaint.setAntiAlias(true);
                highlightPaint.setDither(true);
                //highlightPaint.setStrokeJoin(Paint.Join.ROUND);
                highlightPaint.setStrokeCap(Paint.Cap.ROUND);

                break;
            case "singlePage":
				singlePageMode = true;
				// Reset canvas position when entering single page mode
				canvasOffsetX = 0;
				canvasOffsetY = 0;
				invalidate();
    
				break;
			case "arrow":
            arrowMode = true;
            selectedImageIndex = -1;
            break;

        }

        //invalidate();
	}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Set dimensions
        width = w;
        height = h;

        // Add the first page if pages are empty
        if (pages.isEmpty()) {
            addPage();
        } else {
            updateCanvas(currentPageIndex);
        }
    }
	private void resetModes()
	{
		// TODO: Implement this method

     isMoveDrawing= arrowMode= highlightMode =drawingMoveMode= cropMode= ellipsMode=cubeMode=dotLineMode=coneMode=cylinderMode= isResizing = isMoving =eraseMode=circleMode= paintMode=lineMode=triangleMode=rectangleMode=sphereMode=hemisphereMode=false;

    }
    public void setStrokeSize(int progress) {
        currentStrokeWidth=progress;
        drawPaint.setStrokeWidth(currentStrokeWidth);


    }
    public void addPage() {
        Bitmap newPageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newPageBitmap.eraseColor(Color.TRANSPARENT); // Set background to transparent
        Page newPage = new Page(newPageBitmap);
        pages.add(newPage);
        currentPageIndex = pages.size() - 1; // Move to the new page
        updateCanvas(currentPageIndex);
        mainActivity.pagesAdapter.notifyDataSetChanged();
        mainActivity.loadPageNo();
        invalidate();
    }


    public void addNewPage() {
        Bitmap newPageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newPageBitmap.eraseColor(Color.TRANSPARENT); // Set background to transparent
        Page newPage = new Page(newPageBitmap);

        // Insert the new page right after the current page
        int newPageIndex = currentPageIndex + 1;
        pages.add(newPageIndex, newPage);

        // Update the current page index to point to the new page
        currentPageIndex = newPageIndex;

        // Refresh the canvas and RecyclerView
        updateCanvas(currentPageIndex);
        mainActivity.pagesAdapter.notifyDataSetChanged();
        mainActivity.loadPageNo();
        invalidate();
    }


    public boolean nextPage() {
        if (currentPageIndex < pages.size() - 1) {
            currentPageIndex++;
            updateCanvas(currentPageIndex);
            invalidate();
            return true;
        }
		Toast.makeText(getContext(),"You are already on last page",Toast.LENGTH_SHORT).show();
        return false; // No next page
    }

    public boolean previousPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            updateCanvas(currentPageIndex);
            invalidate();
            return true;
        }
		Toast.makeText(getContext(),"You are already on first page",Toast.LENGTH_SHORT).show();
        return false; // No previous page
    }
    public  int getTotalPage(){
        return pages.size();
    }
    public int getCurrentPage(){
        return  currentPageIndex;
    }
	
    public void updateCanvas(int pageIndex) {
        currentPageIndex=pageIndex;
        drawCanvas = new Canvas(pages.get(currentPageIndex).contentBitmap);
        mainActivity.loadPageNo();
        invalidate();
    }


	// Save the current page state to the undo stack
    private void saveToUndoStack() {
        Bitmap currentBitmap = pages.get(currentPageIndex).contentBitmap;
        undoStack.push(Bitmap.createBitmap(currentBitmap));
        redoStack.clear(); // Clear redo stack when a new action is performed
    }
	public Bitmap getCompositeBitmap(Page page){
        Bitmap compositeBitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(compositeBitmap);
        if (page.contentBitmap!=null){
            canvas.drawBitmap(page.contentBitmap,0,0,null);
        }
        for (int i = 0; i< page.images.size();i++){
            canvas.drawBitmap(page.images.get(i),null,page.imageRects.get(i),null);
        }
        return compositeBitmap;
    }
    public void showSavePdfDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Save Pdf file");

        final EditText input = new EditText(getContext());
        input.setHint("Enter pdf file name");
        input.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.bg_search_edittext));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String userFileName = input.getText().toString().trim();

            if (userFileName.isEmpty()) {
                Toast.makeText(getContext(), "File name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            File directory = new File(android.os.Environment.getExternalStorageDirectory(), "Digital Board/Pdf Notes");

            if (!directory.exists()) {
                directory.mkdirs();
            }

            File filePath = new File(directory, "Notes_" + userFileName + ".pdf");
            currentPdfPath = filePath.getAbsolutePath();
            savePdfToFile(filePath);

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    public void saveAsPdf() {
    if (currentPdfPath != null) {
        savePdfToFile(new File(currentPdfPath));
    } else {
        showSavePdfDialog();
    }
}
private void savePdfToFile(File filePath) {
    PdfDocument pdfDocument = new PdfDocument();
    try {
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            Bitmap compositeBitmap = getCompositeBitmap(page);

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    compositeBitmap.getWidth(), compositeBitmap.getHeight(), i + 1).create();

            PdfDocument.Page pdfPage = pdfDocument.startPage(pageInfo);
            Canvas canvas = pdfPage.getCanvas();
            // First, draw the background
			Drawable bg = getBackground();
			if (bg != null) {
				bg.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
				bg.draw(canvas);  // Draw background color or drawable
			} else {
				canvas.drawColor(Color.WHITE);  // Fallback if no background
			}

			// Then, draw the drawing content
			canvas.drawBitmap(compositeBitmap, 0, 0, null);

            pdfDocument.finishPage(pdfPage);
        }

        pdfDocument.writeTo(new FileOutputStream(filePath));
        Toast.makeText(getContext(), "PDF saved to " + filePath.getAbsolutePath(), Toast.LENGTH_LONG).show();
    } catch (IOException e) {
        Toast.makeText(getContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
    } finally {
        pdfDocument.close();
    }
}

	private void applyErase() {
		Paint eraserPaint = new Paint();
		eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		eraserPaint.setStyle(Paint.Style.FILL);
		eraserPaint.setAntiAlias(true);

		drawCanvas.drawPath(erasePath, eraserPaint); // Erase from canvas
        Page currentPage=pages.get(currentPageIndex);
        for (int i=0;i<currentPage.images.size();i++){
            if (nonErasableImageIndices.contains(i)){
                continue;
            }
            Bitmap image=currentPage.images.get(i);
            Rect imageRect=currentPage.imageRects.get(i);
            Bitmap mutableImage=image.copy(Bitmap.Config.ARGB_8888,true);
            Canvas imageCanvas=new Canvas(mutableImage);
            float scaleX=(float) image.getWidth()/imageRect.width();
            float scaleY=(float) image.getHeight()/imageRect.height();
            Matrix matrix=new Matrix();
            matrix.postTranslate(-imageRect.left,-imageRect.top);
            matrix.postScale(scaleX,scaleY);
            Path mappedPath=new Path();
            erasePath.transform(matrix,mappedPath);
            imageCanvas.drawPath(mappedPath,eraserPaint);
            currentPage.images.set(i,mutableImage);
        }
		erasePath.reset();
        invalidate();
	}

    private boolean isNearBorder(float x, float y, Rect rect) {
        return Math.abs(x - rect.left) < resizeThreshold || Math.abs(x - rect.right) < resizeThreshold ||
			Math.abs(y - rect.top) < resizeThreshold || Math.abs(y - rect.bottom) < resizeThreshold;
    }

    private String determineResizeMode(float x, float y, Rect rect) {
        if (Math.abs(x - rect.left) < resizeThreshold && Math.abs(y - rect.top) < resizeThreshold)
            return "TOP_LEFT";
        else if (Math.abs(x - rect.right) < resizeThreshold && Math.abs(y - rect.top) < resizeThreshold)
            return "TOP_RIGHT";
        else if (Math.abs(x - rect.left) < resizeThreshold && Math.abs(y - rect.bottom) < resizeThreshold)
            return "BOTTOM_LEFT";
        else if (Math.abs(x - rect.right) < resizeThreshold && Math.abs(y - rect.bottom) < resizeThreshold)
            return "BOTTOM_RIGHT";
        return null;
    }

    private void resizeImage(float x, float y, Rect rect) {
        switch (resizeMode) {
            case "TOP_LEFT":
                rect.left = (int) x;
                rect.top = (int) y;
                break;
            case "TOP_RIGHT":
                rect.right = (int) x;
                rect.top = (int) y;
                break;
            case "BOTTOM_LEFT":
                rect.left = (int) x;
                rect.bottom = (int) y;
                break;
            case "BOTTOM_RIGHT":
                rect.right = (int) x;
                rect.bottom = (int) y;
                break;
        }
        invalidate();
    }

    private void moveImage(float dx, float dy, Rect rect) {
        rect.offset((int) dx, (int) dy);
        invalidate();
    }

    public void addImageToCanvas(Bitmap bitmap) {
        if (bitmap != null) {
            Page currentPage = pages.get(currentPageIndex);
            currentPage.images.add(bitmap);
            currentPage.imageRects.add(new Rect(0, 0, 600, 650));
            nonErasableImageIndices.add(currentPage.images.size()-1);
            invalidate();
        }
    }


    public void setPaintColor(int color) {
        currentColor=color;
        drawPaint.setColor(currentColor);
        resetModes();
        paintMode=true;
        selectedImageIndex=-1;
        invalidate();
    }
    public void addPdf(Uri pdfUri) {
        // Show a progress dialog while the PDF is being loaded
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Loading PDF...");
        progressDialog.setCancelable(false);

        // Show the progress dialog on the UI thread
        ((Activity) getContext()).runOnUiThread(progressDialog::show);

        new Thread(() -> {
            ParcelFileDescriptor fileDescriptor = null;
            PdfRenderer pdfRenderer = null;

            try {
                // Open the PDF file
				File pdfFile = new File(pdfUri.getPath());
				currentPdfPath = pdfFile.getAbsolutePath(); // Save path for future use

                fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);

                // Use PdfRenderer to open the file
                pdfRenderer = new PdfRenderer(fileDescriptor);

                // Loop through all pages in the PDF
                for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                    PdfRenderer.Page page = pdfRenderer.openPage(i);

                    // Create a Bitmap for the current page
                    Bitmap bitmap = Bitmap.createBitmap(
                            page.getWidth() * 2, // Scaling to improve quality
                            page.getHeight() * 2,
                            Bitmap.Config.ARGB_8888
                    );

                    // Render the page to the bitmap
                    Canvas canvas = new Canvas(bitmap);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    // Add the page bitmap to the canvas
                    Bitmap finalBitmap = bitmap; // Finalize bitmap for UI thread
                    ((Activity) getContext()).runOnUiThread(() -> addImageToCanvas(finalBitmap));

                    // Close the page
                    page.close();

                    // Add a new page for the next PDF page
                    if (i < pdfRenderer.getPageCount() - 1) {
                        ((Activity) getContext()).runOnUiThread(this::addPage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                ((Activity) getContext()).runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error loading PDF: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            } finally {
                // Close resources
                try {
                    if (pdfRenderer != null) pdfRenderer.close();
                    if (fileDescriptor != null) fileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Hide the progress dialog and refresh the view
                ((Activity) getContext()).runOnUiThread(() -> {
                    progressDialog.dismiss();
                    invalidate();
                });
            }
        }).start();
    }

    public void clearCanvas() {
    if (!pages.isEmpty()) {
        Page currentPage = pages.get(currentPageIndex);

        // Clear content bitmap
        currentPage.contentBitmap.eraseColor(Color.TRANSPARENT);

        // Clear images and their rectangles
        currentPage.images.clear();
        currentPage.imageRects.clear();

        // Clear shape-based drawings
        currentPage.shapeImages.clear();
        currentPage.shapeImageRects.clear();

        // Clear undo/redo stacks
        currentPage.undoImageStack.clear();
        currentPage.redoImageStack.clear();
        currentPage.undoRectStack.clear();
        currentPage.redoRectStack.clear();

        // Clear paths (if you ever use them)
        currentPage.paths.clear();
        currentPage.pathPaints.clear();

        // Reset transformation matrix (zoom/move)
        resetTransform();

        // Clear selected image index and other states
        selectedImageIndex = -1;
        selectedPathIndex = -1;
        path = null;
        shapeBounds = null;

        invalidate(); // Redraw the canvas
    }
}



    public class Page {
        Bitmap contentBitmap;
		Stack<List<Bitmap>> undoImageStack = new Stack<>();
		Stack<List<Bitmap>> redoImageStack = new Stack<>();
		Stack<List<Rect>> undoRectStack = new Stack<>();
		Stack<List<Rect>> redoRectStack = new Stack<>();

        List<Bitmap> shapeImages = new ArrayList<>();
        List<Rect> shapeImageRects = new ArrayList<>();
        List<Bitmap> images = new ArrayList<>();
        List<Rect> imageRects = new ArrayList<>();
        List<Path> paths = new ArrayList<>();      // Store paths
        List<Paint> pathPaints = new ArrayList<>(); // Store corresponding paints

        public Page(Bitmap contentBitmap) {
            this.contentBitmap = contentBitmap;
        }
    }

	// Get the list of pages
	public List<Page> getPages() {
		return pages;
	}

// Switch to a specific page by index
	public void switchToPage(int index) {
		if (index >= 0 && index < pages.size()) {
			currentPageIndex = index;
			updateCanvas(currentPageIndex);
			invalidate();
		}
	}

    public void showSaveProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Save Project");

        final EditText input = new EditText(getContext());
        input.setHint("Enter project name");
        input.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.bg_search_edittext));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String userFileName = input.getText().toString().trim();

            if (userFileName.isEmpty()) {
                Toast.makeText(getContext(), "Project name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

           saveProjectToFile(userFileName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    public void saveProject() {
        if (currentProjectPath != null) {
            saveProjectToFile(currentProjectPath); // Save to previously loaded file
        } else {
            showSaveProjectDialog(); // Ask for new name if not previously loaded
        }
    }
    private void saveProjectToFile(String path) {
        try {
            File projectFile = new File(path);

            JSONObject projectData = new JSONObject();
            JSONArray pagesArray = new JSONArray();

            for (Page page : pages) {
                JSONObject pageObject = new JSONObject();

                ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
                page.contentBitmap.compress(Bitmap.CompressFormat.PNG, 100, contentStream);
                String contentBase64 = Base64.encodeToString(contentStream.toByteArray(), Base64.DEFAULT);
                pageObject.put("contentBitmap", contentBase64);

                JSONArray imagesArray = new JSONArray();
                for (int i = 0; i < page.images.size(); i++) {
                    JSONObject imageObject = new JSONObject();

                    ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                    page.images.get(i).compress(Bitmap.CompressFormat.PNG, 100, imageStream);
                    String imageBase64 = Base64.encodeToString(imageStream.toByteArray(), Base64.DEFAULT);

                    Rect rect = page.imageRects.get(i);
                    JSONObject rectObject = new JSONObject();
                    rectObject.put("left", rect.left);
                    rectObject.put("top", rect.top);
                    rectObject.put("right", rect.right);
                    rectObject.put("bottom", rect.bottom);

                    imageObject.put("imageBitmap", imageBase64);
                    imageObject.put("rect", rectObject);
                    imagesArray.put(imageObject);
                }

                pageObject.put("images", imagesArray);
                pagesArray.put(pageObject);
            }

            projectData.put("pages", pagesArray);
            projectData.put("currentPageIndex", currentPageIndex);

            FileOutputStream fos = new FileOutputStream(projectFile);
            fos.write(projectData.toString().getBytes());
            fos.close();

            Toast.makeText(getContext(), "Project saved to " + projectFile.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving project: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void loadProject(String filePath) {
        try {
            File projectFile = new File(filePath);
            FileInputStream fis = new FileInputStream(projectFile);
            byte[] data = new byte[(int) projectFile.length()];
            fis.read(data);
            fis.close();

            String jsonString = new String(data);
            JSONObject projectData = new JSONObject(jsonString);

            pages.clear();
            JSONArray pagesArray = projectData.getJSONArray("pages");
            for (int i = 0; i < pagesArray.length(); i++) {
                JSONObject pageObject = pagesArray.getJSONObject(i);

                // Load content bitmap
                String contentBase64 = pageObject.getString("contentBitmap");
                byte[] contentBytes = Base64.decode(contentBase64, Base64.DEFAULT);
                Bitmap originalContentBitmap = BitmapFactory.decodeByteArray(contentBytes, 0, contentBytes.length);
                Bitmap mutableContentBitmap = originalContentBitmap.copy(Bitmap.Config.ARGB_8888, true);

                Page page = new Page(mutableContentBitmap);

                // Load images
                JSONArray imagesArray = pageObject.getJSONArray("images");
                for (int j = 0; j < imagesArray.length(); j++) {
                    JSONObject imageObject = imagesArray.getJSONObject(j);
                    String imageBase64 = imageObject.getString("imageBitmap");
                    byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                    Bitmap originalImageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    Bitmap mutableImageBitmap = originalImageBitmap.copy(Bitmap.Config.ARGB_8888, true);

                    JSONObject rectObject = imageObject.getJSONObject("rect");
                    Rect rect = new Rect(
                            rectObject.getInt("left"),
                            rectObject.getInt("top"),
                            rectObject.getInt("right"),
                            rectObject.getInt("bottom")
                    );

                    page.images.add(mutableImageBitmap);
                    page.imageRects.add(rect);
                }

                pages.add(page);
            }

            currentPageIndex = projectData.getInt("currentPageIndex");
            currentProjectPath = filePath; // Store the path for future saving

            updateCanvas(currentPageIndex);
            invalidate();

            Toast.makeText(getContext(), "Project loaded successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error loading project: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
private boolean isDrawingModeActive() {
			return paintMode || lineMode || arrowMode || circleMode || 
				   rectangleMode || triangleMode || sphereMode || 
				   cylinderMode || coneMode || cubeMode || ellipsMode;
		}

		private void startDrawing(float x, float y) {
			lastTouchX = x;
			lastTouchY = y;
			if (paintMode) {
				path = new Path();
				path.moveTo(x, y);
			}
			// Similar for other drawing modes
		}

		private void continueDrawing(float x, float y) {
			if (paintMode && path != null) {
				path.lineTo(x, y);
			}
			endX = x;
			endY = y;
			invalidate();
		}

		private void finishDrawing(float x, float y) {
			endX = x;
			endY = y;
			// Handle final drawing operations
			if (paintMode && path != null) {
				convertPathToImage(path, drawPaint);
				path = null;
			} else if (lineMode || arrowMode || circleMode || rectangleMode || 
					  triangleMode || sphereMode || cylinderMode || 
					  coneMode || cubeMode || ellipsMode) {
				replaceDrawingWithImage();
			}
			invalidate();
		}
}
