package com.adnan.digitalboard.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Patterns;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.adnan.digitalboard.R;
import com.adnan.digitalboard.adapters.ChatAdapter;
import com.adnan.digitalboard.models.ChatMessage;
import com.adnan.digitalboard.services.CameraStreamService;
import com.adnan.digitalboard.services.RtmpServce;
import com.adnan.digitalboard.services.ScreenCaptureAndLiveService;
import com.adnan.digitalboard.services.ScreenCaptureService;
import com.adnan.digitalboard.services.ScreenWebRtcService;
import com.adnan.digitalboard.services.DrawingViewWebRtcService;
import com.adnan.digitalboard.utils.StudentTrackManager;
import com.adnan.digitalboard.view.DrawingView;
import com.adnan.digitalboard.adapters.FileListAdapter;
import com.adnan.digitalboard.view.NetworkChangeReceiver;
import com.adnan.digitalboard.adapters.PagesAdapter;
import com.adnan.digitalboard.view.SelectionOverlayView;
import com.adnan.digitalboard.adapters.StudentAdapter;
import com.adnan.digitalboard.models.StudentModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.gl.render.filters.ChromaFilterRender;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.utils.gl.AspectRatioMode;
import com.pedro.library.rtmp.RtmpCamera2;
import com.pedro.library.view.OpenGlView;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements PaymentResultListener , ConnectChecker {
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;
    private static final int REQUEST_CODE_LIVE = 1002;
    private static final int REQUEST_CODE_BOTH = 1003;
    private static final int REQUEST_CODE_WEBRTC_STREAM = 1004;
    private static final String YOUTUBE_RTMP_URL = "rtmp://a.rtmp.youtube.com/live2/t433-915g-kuzs-xtw5-du0z";
    private MediaProjectionManager mediaProjectionManager;
    public LinearLayout shapeLayout, pageDisplay ,pageLayout,verticalSeekbar, paymentLayout,buttonContainer,studentLayout;
    private String mobile,email,selectedPlan,rtmp_url;
	private SharedPreferences sharedPreferences;

    private DrawingView drawingView;
    private boolean isReadPermissionGranted=false;
    private  boolean isCameraStreaming=false;
    public PagesAdapter pagesAdapter;
    public StudentAdapter studentAdapter;
	public ToggleButton startAndStop,pauseAndResume,cameraToggle;
    private boolean isFullscreen=false;
    public static final int REQUEST_CODE = 100;
    private ListView listViewFiles;
    private ArrayList<File> fileList = new ArrayList<>();
    private File currentDirectory;
    private  CardView rtmp_url_card, cameraStreamView,cardViewFileExplorer, videoCard, webCard,scrollView,paymentCardView,cameraViewCardView,selectRecordCard;
    private VideoView videoView;

    private ImageView snapClip, webClose, webFullscreen,fileclose,fileFullscreen,saveProject,colorWheel, videoClose,videoFullscreen,backWard,forward;
    int originalWidth, originalHeight; // Variables to store original dimensions
    private GestureDetector gestureDetector;
    public TextView pageAll , pageCurrent;
    private AttributeSet attributeSet=null;
    private  RecyclerView recyclerView,studentRecyclerView;
    private List<StudentModel> studentList= new ArrayList<>();
    private List<ChatMessage> chatMessageList = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private Dialog chatDialog;

    // Broadcast Receiver: সার্ভিস থেকে চ্যাট মেসেজ রিসিভ করার জন্য
    private BroadcastReceiver chatMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_RECEIVE_CHAT_MESSAGE".equals(intent.getAction())) {
                String sender = intent.getStringExtra("sender");
                String message = intent.getStringExtra("message");

                chatMessageList.add(new ChatMessage(sender, message));
                if (chatAdapter != null) {
                    chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
                }
            }
        }
    };
        private BroadcastReceiver studentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("ACTION_STUDENT_JOINED".equals(intent.getAction())) {
                    String studentId = intent.getStringExtra("studentId");
                    String studentName = intent.getStringExtra("studentName");

                    // নতুন স্টুডেন্ট লিস্টে না থাকলে যুক্ত করা
                    boolean exists = false;
                    for (StudentModel s : studentList) {
                        if (s.getStudentId().equals(studentId)) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        StudentModel newStudent = new StudentModel(studentId,studentName,null);
                        studentList.add(newStudent);
                        studentAdapter.notifyDataSetChanged();
                    }
                }
            }
        };


    private boolean isRecording=false;
    private DatabaseReference databaseReference,userRef,paymentRef,AlluserRef;

    EditText mobileEditText,emailEditText;
    private Button chatBtn, paymentBtn,goLiveBtn,screenRecordBtn,bothBtn,cameraStramBtn;
    String deviceId , price="", planName="" ,browseUrl="https://www.google.com";
    public String[] getAppPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);
        perms.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        return perms.toArray(new String[0]);
    }
    private NetworkChangeReceiver networkChangeReceiver=new NetworkChangeReceiver();
    public Chronometer chronometer;
    private long pausedTime;

    private ScreenCaptureAndLiveService screenCaptureService;
    private boolean isServiceBound = false;
    private MediaProjection mediaProjection;
    private int orientation;
    private SelectionOverlayView selectionOverlayView;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ScreenCaptureAndLiveService.LocalBinder binder = (ScreenCaptureAndLiveService.LocalBinder) service;
            screenCaptureService = binder.getService();
            isServiceBound = true;

            // Pass the MainActivity reference
            if (screenCaptureService != null) {
                screenCaptureService.callFromMainActivity(MainActivity.this);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };


        private  boolean isResizing=false,isRunning=false;
        private long pauseOffset=0;
        private  static final int RESIZE_THRESHOLD=50;
        private float lastX,lastY;

        private WebView webView;
        private EditText url_text;
        private  boolean isStudent_layout_visible=false, isGolive=false, isLive=false,isScreenCapture=false,isStreaming=false,isWebRtc=false,isBothLiveAndScreen=false;
        private int lensFacing = CameraSelector.LENS_FACING_FRONT;
        private ProcessCameraProvider cameraProvider;
        private Preview preview;
        private CameraSelector cameraSelector;
        private OpenGlView openGlView;
        private RtmpCamera2 rtmpCamera2;
        private ChromaFilterRender chromaFilterRender;
        private ScrollView setteing_layout_view;
        private ImageView back_cancel,back_border,back_green,back_white,back_black,back_lines,back_tabble,back_image;

        private RelativeLayout rootLayout;
        private Button cancel,golive,webRtc_stream_btn;
        private ImageButton btnSwitchStudentCamera,btnCloseVideo;
        private EditText rtmp_editText;
        private CheckBox checkBox;
        private EglBase rootEglBase;
        private String selectedStudentId="";
        private RelativeLayout videoContainerOverlay;
        private SurfaceViewRenderer fullStudentVideoView;
    private String currentRoomId = "";

    private ImageView editImage, background, save_project,front_bak, search_imageView, colourWheel, shape_line, shape_rec, shape_tri,shape_cir,shape_dashline, shape_oval,shape_cone,shape_cube, shape_hemisphere , shape_ellipse,shape_poly ,shape_arrow_line,shape_cylinder, shape_sphere;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve orientation value from the intent
        hideSystemUI();
        Intent intent = getIntent();
         orientation = intent.getIntExtra("orientation", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Apply the selected orientation
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        }

        setContentView(R.layout.activity_main); // Set your layout for MainActivity

        View rootView = findViewById(android.R.id.content); // This is the root view of your activity
        deviceId= Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);



        // canvasView=new CanvasView(this,attributeSet);
        rootLayout=findViewById(R.id.root_layout);
        drawingView=new DrawingView(this,attributeSet);
        listViewFiles = (ListView) findViewById(R.id.listViewFiles);
        gestureDetector=new GestureDetector(this,new GestureListener());
        drawingView = findViewById(R.id.MyView);
        cardViewFileExplorer=findViewById(R.id.filemanager);
        ImageView clearCanvas = findViewById(R.id.board_clear);
        ImageView Undo= findViewById(R.id.undo);
        ImageView savePdf= findViewById(R.id.btn_save);
        ImageView Redo = findViewById(R.id.redo);
        ImageView browse=findViewById(R.id.webImageView);
        webView=findViewById(R.id.webView);
        snapClip=findViewById(R.id.screenshot);
        selectionOverlayView=new SelectionOverlayView(this);
        selectionOverlayView.setDrawingView(drawingView);
        ImageView green_color=findViewById(R.id.color_green);
        ImageView red_color=findViewById(R.id.color_red);
        ImageView blue_color=findViewById(R.id.color_blue);
        ImageView black_color=findViewById(R.id.color_black);
        ImageView yellow_color=findViewById(R.id.color_yellow);
        ImageView purple_color=findViewById(R.id.color_purple);
        ImageView orange_color=findViewById(R.id.color_orange);
        ImageView white_color=findViewById(R.id.color_White);
        colourWheel=findViewById(R.id.color_wheel);
        ImageView crop=findViewById(R.id.crop);
        ImageView shapeImage=findViewById(R.id.btn_shape);
        shapeLayout= findViewById(R.id.shape_layout_No);
        ImageView addPage=findViewById(R.id.new_add);
        ImageView previousPageImageView=findViewById(R.id.previousPage);
        ImageView nextPageImageView=findViewById(R.id.nextPage);
        ImageView fileExplorer=findViewById(R.id.open_file);
        btnCloseVideo=findViewById(R.id.btnCloseVideo);
        btnSwitchStudentCamera=findViewById(R.id.btnSwitchStudentCamera);
        videoContainerOverlay=findViewById(R.id.videoContainerOverlay);
        fullStudentVideoView=findViewById(R.id.fullStudentVideoView);
        front_bak=findViewById(R.id.front_back);
        cameraStramBtn=findViewById(R.id.cameraStream);
        chromaFilterRender=new ChromaFilterRender();
        cameraStreamView=findViewById(R.id.camera_stream_view_contener);
		sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        background=findViewById(R.id.backgraound);
        backWard=findViewById(R.id.webBack);
        forward=findViewById(R.id.webForw);
        pageAll=findViewById(R.id.page_all);
        pageCurrent=findViewById(R.id.page_current);
        videoCard=findViewById(R.id.VideoCardView);
        webCard=findViewById(R.id.webCardView);
        videoView=findViewById(R.id.videoPlayer);
        webClose=findViewById(R.id.webClose);
        webFullscreen=findViewById(R.id.webFullscreen);
        fileFullscreen=findViewById(R.id.maximum);
        videoClose=findViewById(R.id.videoClose);
        videoFullscreen=findViewById(R.id.videoFullscreen);
        fileclose=findViewById(R.id.close);
        pageDisplay=findViewById(R.id.page_display);
        pageLayout=findViewById(R.id.page_layout);
        rootEglBase=EglBase.create();
        recyclerView = findViewById(R.id.pag_view);
        studentLayout=findViewById(R.id.student_Layout);
        studentRecyclerView=findViewById(R.id.student_view);
        studentRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        studentAdapter = new StudentAdapter(studentList, rootEglBase.getEglBaseContext(), new StudentAdapter.OnStudentClickListener() {
            @Override
            public void onMuteToggle(StudentModel student, boolean shouldMute) {
                // Mute হলে 'mute_mic', Unmute করতে চাইলে 'unmute_mic' পাঠাবে
                String command = shouldMute ? "mute_mic" : "unmute_mic";
                sendCommandToService(student.getStudentId(), command);
            }

			@Override
			public void onVideoClick(StudentModel student) {
				selectedStudentId = student.getStudentId();
				videoContainerOverlay.setVisibility(View.VISIBLE);
				studentLayout.setVisibility(View.GONE);
				isStudent_layout_visible = false;

				fullStudentVideoView.init(rootEglBase.getEglBaseContext(), null);

				// TrackManager থেকে রিসিভড ট্র্যাক বের করে সিঙ্ক করা
				VideoTrack track = StudentTrackManager.getTrack(student.getStudentId());
				if (track != null) {
					track.addSink(fullStudentVideoView);
				} else if (student.getVideoTrack() != null) {
					student.getVideoTrack().addSink(fullStudentVideoView);
				}
			}
        });


        studentRecyclerView.setAdapter(studentAdapter);
		rtmp_url_card=findViewById(R.id.rtmp_URL_Lay);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<DrawingView.Page> pages = drawingView.getPages();
        pagesAdapter = new PagesAdapter(pages, drawingView, this);

        recyclerView.setAdapter(pagesAdapter);
        shape_line=findViewById(R.id.shape_line);
        shape_rec=findViewById(R.id.shape_rect);
        shape_cir=findViewById(R.id.shape_cir);
        shape_sphere=findViewById(R.id.shape_shpere);
        shape_cone=findViewById(R.id.shape_cone);
        shape_cube=findViewById(R.id.shape_cube);
        shape_ellipse=findViewById(R.id.shape_ellipse);
        shape_arrow_line=findViewById(R.id.shape_arrow_line);
        shape_cylinder=findViewById(R.id.shape_cylinder);
        shape_poly=findViewById(R.id.shape_poly);
        ImageView triangle=findViewById(R.id.shape_tri);
        ImageView dashLine=findViewById(R.id.shape_dashline);
        ImageView pencil=findViewById(R.id.brush);
        ImageView erase=findViewById(R.id.eraser);
        ImageView highlighter=findViewById(R.id.highliter);
        verticalSeekbar=findViewById(R.id.verticalSeekbar);
        ImageView strokeSize=findViewById(R.id.stroke_size);
        pauseAndResume=findViewById(R.id.record_pause);
        startAndStop=findViewById(R.id.record_stop);
        scrollView=findViewById(R.id.scrollbar);
        TextView strokeSizeText= findViewById(R.id.szz);
        buttonContainer=findViewById(R.id.buttonContainer);
        paymentCardView=findViewById(R.id.paymentCardView);
        SeekBar seekBar=findViewById(R.id.v_seekbar);
        paymentBtn=findViewById(R.id.paymentButton);
        paymentLayout=findViewById(R.id.paymentLayout);
        mobileEditText=findViewById(R.id.mobile);
        emailEditText=findViewById(R.id.Email);
        cameraToggle=findViewById(R.id.camera);
        cameraViewCardView=findViewById(R.id.camera_view_contener);
        chronometer=findViewById(R.id.chronometer);
        selectRecordCard=findViewById(R.id.selectRecordCardview);
        goLiveBtn=findViewById(R.id.goliveBtn);
        screenRecordBtn=findViewById(R.id.screenRecBtn);
        bothBtn=findViewById(R.id.bothBtn);
        selectionOverlayView=new SelectionOverlayView(this);
        selectionOverlayView.setDrawingView(drawingView);
        selectionOverlayView.setVisibility(View.GONE);
        webCard.addView(selectionOverlayView);
        url_text=findViewById(R.id.url_brows);
        search_imageView=findViewById(R.id.search);
        openGlView=findViewById(R.id.streamCameraView);
        rtmpCamera2=new RtmpCamera2(openGlView,this);
        save_project=findViewById(R.id.project_btn_save);
        setteing_layout_view=findViewById(R.id.setting_layout);
        back_cancel=findViewById(R.id.back_cancel);
        back_black=findViewById(R.id.back_black);
        back_white=findViewById(R.id.back_white);
        back_border=findViewById(R.id.back_border);
        back_lines=findViewById(R.id.back_lines);
        back_green=findViewById(R.id.back_green);
        back_tabble=findViewById(R.id.back_table);
        back_image=findViewById(R.id.back_image);
		golive =findViewById(R.id.rtmp_btn_submit);
        cancel = findViewById(R.id.rtmp_btn_cancel);
        rtmp_editText=findViewById(R.id.url);
        checkBox=findViewById(R.id.checkBox);
        editImage=findViewById(R.id.editActive);
        webRtc_stream_btn=findViewById(R.id.rtcBtn);
        chatBtn=findViewById(R.id.chatBtn);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        seekBar.setMax(40);
        seekBar.setProgress(2);
        Intent serviceIntent = new Intent(this, ScreenCaptureAndLiveService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        addDragFunctionality();
        loadPageNo();
        AlluserRef = FirebaseDatabase.getInstance().getReference("DigitalBoard").child("All Users").child(deviceId);
        AlluserRef.setValue("");
        databaseReference= FirebaseDatabase.getInstance().getReference("DigitalBoard").child("Premium");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String premium=snapshot.getValue(String.class);
                    if (premium.equals("premium")){
                        loadFiirebase();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        // Top Camera Switch বাটন
        btnSwitchStudentCamera.setOnClickListener(v -> {
            if (!selectedStudentId.isEmpty()) {
                sendCommandToService(selectedStudentId, "switch_camera");
            }
        });
        btnCloseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoContainerOverlay.setVisibility(View.GONE);

            }
        });

        chatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showChatDialog();
                studentLayout.setVisibility(View.GONE);
                isStudent_layout_visible=false;

            }
        });
        background.setOnClickListener(v->{
            if (setteing_layout_view.getVisibility()==View.VISIBLE){
                setteing_layout_view.setVisibility(View.GONE);
            }else {
                setteing_layout_view.setVisibility(View.VISIBLE);
            }
        });
        back_cancel.setOnClickListener(v->{
            setteing_layout_view.setVisibility(View.GONE);
        });
        back_border.setOnClickListener(v -> {
            drawingView.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.backgrnd_white));
        });

        back_green.setOnClickListener(v -> {
            drawingView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.color_green_board));
        });
        back_white.setOnClickListener(v -> {
            drawingView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.color_white));
        });
        back_black.setOnClickListener(v -> {
            drawingView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.color_black));
        });
        back_lines.setOnClickListener(v -> {
          Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.line_background);
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
				drawingView.setBackground(bitmapDrawable);
			}

        });
        back_tabble.setOnClickListener(v -> {
			Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.table_background);
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
				drawingView.setBackground(bitmapDrawable);
			}

        });

        front_bak.setOnClickListener(v -> {

            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            } else {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            }
            if (rtmpCamera2 !=null && rtmpCamera2.isStreaming()){
                rtmpCamera2.switchCamera();
                if (rtmpCamera2.getCameraFacing()== CameraHelper.Facing.FRONT){
                    rtmpCamera2.getGlInterface().setIsStreamHorizontalFlip(true);
                    rtmpCamera2.getGlInterface().setIsPreviewHorizontalFlip(true);

                }else{
                    rtmpCamera2.getGlInterface().setIsStreamHorizontalFlip(false);
                    rtmpCamera2.getGlInterface().setIsPreviewHorizontalFlip(false);
                }

            }
            bindCamera(); // Re-bind camera with new lensFacing
        });
        openGlView.setOnClickListener(v->{
            if (rtmpCamera2 !=null && rtmpCamera2.isStreaming()){
                rtmpCamera2.switchCamera();
                if (rtmpCamera2.getCameraFacing()== CameraHelper.Facing.FRONT){
                    rtmpCamera2.getGlInterface().setIsStreamHorizontalFlip(true);
                    rtmpCamera2.getGlInterface().setIsPreviewHorizontalFlip(true);

                }else{
                    rtmpCamera2.getGlInterface().setIsStreamHorizontalFlip(false);
                    rtmpCamera2.getGlInterface().setIsPreviewHorizontalFlip(false);
                }

            }
            hideSystemUI();
        });
        chronometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isWebRtc){
                    if (isStudent_layout_visible){
                        studentLayout.setVisibility(View.GONE);
                        isStudent_layout_visible=false;
                    }else{
                        studentLayout.setVisibility(View.VISIBLE);
                        isStudent_layout_visible=true;
                    }

                }else{
                    studentLayout.setVisibility(View.GONE);
                    isStudent_layout_visible=false;
                }

            }
        });
        // Button action to proceed with payment  paymentBtn.setText( "Pay - ₹" + price);
        paymentBtn.setOnClickListener(v -> {
            if (isValidMobile() && isValidEmail()) {
                startPayment();

            } else {
                Toast.makeText(MainActivity.this, "Invalid Mobile or Email", Toast.LENGTH_SHORT).show();
            }

        });




       // drawingView.loadAllPagesNo(pageCurrent,pageAll);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                strokeSizeText.setText(""+progress);
                drawingView.setStrokeSize(progress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                verticalSeekbar.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);

            }
        });
        strokeSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(verticalSeekbar.getVisibility()==View.GONE){
                    scrollView.setVisibility(View.GONE);
                    verticalSeekbar.setVisibility(View.VISIBLE);
                }
            }
        });
        pauseAndResume.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if (isRecording){
                        pauseRecording();
                        isRecording=false;
                    } else if (rtmpCamera2.isStreaming() && isRunning) {
                        rtmpCamera2.stopStream();
                        chronometer.stop();
                        pauseOffset= SystemClock.elapsedRealtime()-chronometer.getBase();
                        isRunning=false;


                    }
                }else{
                    if (!rtmpCamera2.isStreaming() && !isRunning){
                       startStreaming(rtmp_url);
                        chronometer.setBase(SystemClock.elapsedRealtime()-pauseOffset);
                        chronometer.start();
                        isRunning=true;
                    }
                    resumeRecording();
                    isRecording=true;
                }
            }
        });


        startAndStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (checkPermissions()){
                    if (isChecked){
                       selectRecordCard.setVisibility(View.VISIBLE);
                    }else{
                        if (rtmpCamera2.isStreaming()){
                            rtmpCamera2.stopStream();
                            rtmpCamera2.stopPreview();
                            chronometer.setBase(SystemClock.elapsedRealtime());
                            pauseOffset=0;
                            isRunning=false;
                        }
                        stopRecording();
                        isRecording=false;
                        pauseAndResume.setVisibility(View.GONE);
                    }

                }else{
                    askPermissions();
                    startAndStop.setChecked(false);
                }
            }
        });
        screenRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRecordCard.setVisibility(View.GONE);
                mediaProjectionManager=(MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent screenCaptureIntent=mediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(screenCaptureIntent,REQUEST_CODE_SCREEN_CAPTURE);
                isRecording=true;
                pauseAndResume.setVisibility(View.VISIBLE);

            }
        });
        goLiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRecordCard.setVisibility(View.GONE);
                isGolive=true;
              showDialogAlert();
            }
        });
        bothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRecordCard.setVisibility(View.GONE);
                showDialogAlert();
            }
        });
        cameraStramBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRecordCard.setVisibility(View.GONE);
                cameraStreamView.setVisibility(View.VISIBLE);

                isCameraStreaming=true;
                showDialogAlert();
               // startCamera();
            }
        });
          webRtc_stream_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                selectRecordCard.setVisibility(View.GONE);
                showRoomIdAlertDialog();

               
            }
        });
        cameraToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (checkPermissions()){
                    if (isChecked){
                        startCamera();
                        cameraViewCardView.setVisibility(View.VISIBLE);
                        front_bak.setVisibility(View.VISIBLE);
                    }else{
                        cameraViewCardView.setVisibility(View.GONE);
                        front_bak.setVisibility(View.GONE);
                    }
                }else{
                    askPermissions();
                }
            }
        });
        // Set click listener for the streaming button
        save_project.setOnClickListener(view -> {
            if (checkPermissions()){
                drawingView.saveProject();
            }else{
                askPermissions();
            }

        });
        shape_arrow_line.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("arrow");
                shapeLayout.setVisibility(View.GONE);
            }
        });
        erase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //canvasView.enableMode("erase");
             //   drawingView.setGeometryMode("erase");
                drawingView.enableMode("erase");
                shapeLayout.setVisibility(View.GONE);
            }
        });

        pencil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // canvasView.enableMode("paint");
                drawingView.enableMode("paint");
                shapeLayout.setVisibility(View.GONE);
            }
        });

        dashLine.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                drawingView.enableMode("dline");
                shapeLayout.setVisibility(View.GONE);
            }
        });
        highlighter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("highlight");
            }
        });
        shape_ellipse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("ellips");
                shapeLayout.setVisibility(View.GONE);

            }
        });
        shape_cube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               drawingView.enableMode("cube");
                shapeLayout.setVisibility(View.GONE);

            }
        });
        shape_cone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("cone");
                shapeLayout.setVisibility(View.GONE);

            }
        });
        shape_sphere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("sphere");
                shapeLayout.setVisibility(View.GONE);

            }
        });
        shape_cylinder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("cylinder");
                shapeLayout.setVisibility(View.GONE);
            }
        });
        savePdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()){
                    drawingView.saveAsPdf();
                }else{
                    askPermissions();
                }

            }
        });
        shape_cir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //  canvasView.enableMode("circle");
                drawingView.enableMode("circle");
                shapeLayout.setVisibility(View.GONE);
            }
        });
        shape_rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("rectangle");
                shapeLayout.setVisibility(View.GONE);
            }
        });
        shape_line.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               drawingView.enableMode("line");
                shapeLayout.setVisibility(View.GONE);
            }
        });
        triangle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("triangle");
                shapeLayout.setVisibility(View.GONE);
            }
        });
        colourWheel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.enableMode("singlePage");
                shapeLayout.setVisibility(View.GONE);
            }
        });
        pageDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pageLayout.getVisibility()==View.VISIBLE){
                    pageLayout.setVisibility(View.GONE);
                }else {
                    pageLayout.setVisibility(View.VISIBLE);
                    pagesAdapter.notifyDataSetChanged();
                }
                hideSystemUI(); // Reapply the UI flags to hide the system UI
            }

        });


        videoFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup.LayoutParams params = videoCard.getLayoutParams();

                if (isFullscreen) {
                    // Restore original size
                    params.width = originalWidth;
                    params.height = originalHeight;
                    videoCard.setLayoutParams(params);
                    isFullscreen = false;
                } else {
                    // Save original size before changing to fullscreen
                    originalWidth = params.width;
                    originalHeight = params.height;

                    // Set fullscreen dimensions
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    videoCard.setLayoutParams(params);
                    isFullscreen = true;
                }
            }
        });

        webFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup.LayoutParams params = webCard.getLayoutParams();

                if (isFullscreen) {
                    // Restore original size
                    params.width = originalWidth;
                    params.height = originalHeight;
                    webCard.setLayoutParams(params);
                    isFullscreen = false;
                } else {
                    // Save original size before changing to fullscreen
                    originalWidth = params.width;
                    originalHeight = params.height;

                    // Set fullscreen dimensions
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    webCard.setLayoutParams(params);
                    isFullscreen = true;
                }
            }
        });
        fileFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup.LayoutParams params = cardViewFileExplorer.getLayoutParams();

                if (isFullscreen) {
                    // Restore original size
                    params.width = originalWidth;
                    params.height = originalHeight;
                    cardViewFileExplorer.setLayoutParams(params);
                    isFullscreen = false;
                } else {
                    // Save original size before changing to fullscreen
                    originalWidth = params.width;
                    originalHeight = params.height;

                    // Set fullscreen dimensions
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    cardViewFileExplorer.setLayoutParams(params);
                    isFullscreen = true;
                }
            }
        });

        fileclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardViewFileExplorer.setVisibility(View.GONE);
            }
        });
        videoClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.stopPlayback();
                videoCard.setVisibility(View.GONE);

            }
        });
        webClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webCard.setVisibility(View.GONE);

            }
        });

        // Start browsing from the root directory
        currentDirectory = new File("/storage/emulated/0/Digital Board");

fileExplorer.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        if (checkPermissions()){
            if (cardViewFileExplorer.getVisibility()==View.VISIBLE){
                cardViewFileExplorer.setVisibility(View.GONE);
            }else{
                cardViewFileExplorer.setVisibility(View.VISIBLE);
                listFiles(currentDirectory);
            }
        }else{
            askPermissions();
        }

    }
});
        listViewFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = fileList.get(position);

                if (selectedFile.getName().equals("..")) {
                    // Navigate to parent directory
                    currentDirectory = currentDirectory.getParentFile();
                    listFiles(currentDirectory);
                } else if (selectedFile.isDirectory()) {
                    // Navigate into the directory
                    currentDirectory = selectedFile;
                    listFiles(currentDirectory);
                } else {
                    // Check file type
                    if (selectedFile.getName().endsWith(".jpg") ||
                            selectedFile.getName().endsWith(".jpeg") ||
                            selectedFile.getName().endsWith(".png") ||
                            selectedFile.getName().endsWith(".gif") ||
                            selectedFile.getName().endsWith(".bmp")) {

                        // Handle Image File
                        addImageToCanvas(selectedFile);

                    } else if (selectedFile.getName().endsWith(".ias")){
                       String iasUri= selectedFile.getAbsolutePath();

                        drawingView.loadProject(iasUri);

                    }else if (selectedFile.getName().endsWith(".pdf")) {
                        // Handle PDF File
                        addPdfToCanvas(selectedFile);

                    } else if (selectedFile.getName().endsWith(".mp4") ||
                            selectedFile.getName().endsWith(".mkv") ||
                            selectedFile.getName().endsWith(".avi") ||
                            selectedFile.getName().endsWith(".mov") ||
                            selectedFile.getName().endsWith(".flv") ||
                            selectedFile.getName().endsWith(".wmv")) {

                        // Handle Video File

                        videoCard.setVisibility(View.VISIBLE);
                        videoView.setVideoURI(Uri.fromFile(selectedFile));

                        MediaController mediaController = new MediaController(MainActivity.this);
                        mediaController.setAnchorView(videoView);
                        videoView.setMediaController(mediaController);

            // Force layout size to match parent
                        videoView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                        videoView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                        videoView.requestLayout();

// Optional: set a listener to ensure scaling works well
                        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                // Set scaling mode (helps fill the VideoView properly)
                                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

                                // Start playback
                                videoView.start();
                            }
                        });


                    } else {
                        Toast.makeText(MainActivity.this, "Unsupported file type", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });




        previousPageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.previousPage();
                loadPageNo();
            }
        });



        nextPageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.nextPage();
                loadPageNo();
            }
        });

        addPage.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View p1)
            {
                drawingView.addNewPage();
                pagesAdapter.notifyDataSetChanged();
                 loadPageNo();
            }


        });
        shapeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shapeLayout.getVisibility()==View.GONE){

                    shapeLayout.setVisibility(View.VISIBLE);
                    if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
                        shapeLayout.setOrientation(LinearLayout.VERTICAL);
                    }
                }else {
                    shapeLayout.setVisibility(View.GONE);
                }

            }
        });


        crop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               drawingView.enableMode("crop");
            }
        });
        white_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setPaintColor(Color.WHITE);
            }
        });
        orange_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int orange = ContextCompat.getColor(v.getContext(), R.color.orange_color);
                drawingView.setPaintColor(orange);
            }
        });
        purple_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int purple = ContextCompat.getColor(v.getContext(), R.color.color_purple);
                drawingView.setPaintColor(purple);
            }
        });

       yellow_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setPaintColor(Color.YELLOW);
            }
        });
        black_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setPaintColor(Color.BLACK);
            }
        });
        blue_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setPaintColor(Color.BLUE);
            }
        });
        red_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setPaintColor(Color.RED);
            }
        });
        green_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setPaintColor(Color.GREEN);
            }
        });
        search_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputUrl = url_text.getText().toString().trim();

                // Add "http://" if missing
                if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                    inputUrl = "http://" + inputUrl;
                }

                browseUrl = inputUrl;
                viewWebPage();
            }
        });

        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webCard.getVisibility()==View.VISIBLE){
                    webCard.setVisibility(View.GONE);
                   // Toast.makeText(getApplicationContext(),"web browser hide",Toast.LENGTH_SHORT).show();

                }else {
                    viewWebPage();
                   // Toast.makeText(getApplicationContext(),"web browser visible",Toast.LENGTH_SHORT).show();

                }
            }
        });
        backWard.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });

        forward.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });
        snapClip.setOnClickListener(v -> {

            // Disable scroll
            webView.setOnTouchListener((view, motionEvent) -> true);

            // Show overlay
            selectionOverlayView.setVisibility(View.VISIBLE);
        });


        Redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               drawingView.redoImage();
            }
        });
        Undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.undoImage();
            }
        });



        clearCanvas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, "Are you sure to clear the screen?", Snackbar.LENGTH_LONG)
                        .setAction("Clear", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // This is where we call clearCanvas
                                drawingView.clearCanvas();
                            }
                        })
                        .show();
            }
        });


    }

    // ২. শুধুমাত্র Java দিয়ে AlertDialog তৈরি
    private void showRoomIdAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Start Live Stream");
        builder.setMessage("Enter Room ID and choose what to share:");

        // Programmatically EditText তৈরি
        final EditText input = new EditText(this);
        input.setHint("Room ID (e.g., class-101)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // Layout Parameters দিয়ে মার্জিন/প্যাডিং সেট করা
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(40, 0, 40, 20);

        final RadioGroup shareModeGroup = new RadioGroup(this);
        shareModeGroup.setOrientation(RadioGroup.VERTICAL);

        final RadioButton rbDrawingViewOnly = new RadioButton(this);
        rbDrawingViewOnly.setId(View.generateViewId());
        rbDrawingViewOnly.setText("A single app (DrawingView Only)");
        rbDrawingViewOnly.setChecked(true);

        final RadioButton rbEntireScreen = new RadioButton(this);
        rbEntireScreen.setId(View.generateViewId());
        rbEntireScreen.setText("Entire screen");

        shareModeGroup.addView(rbDrawingViewOnly);
        shareModeGroup.addView(rbEntireScreen);

        LinearLayout.LayoutParams rgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rgLp.setMargins(40, 10, 40, 10);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input, lp);
        container.addView(shareModeGroup, rgLp);
        builder.setView(container);

        // "Go Live" বাটন
        builder.setPositiveButton("Webrtc Live", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String roomId = input.getText().toString().trim();
                if (!roomId.isEmpty()) {
                    currentRoomId = roomId;
                    if (rbDrawingViewOnly.isChecked()) {
                        // Stream ONLY the DrawingView without full-screen MediaProjection
                        DrawingViewWebRtcService.setDrawingView(drawingView);
                        startService(DrawingViewWebRtcService.getStartIntent(MainActivity.this, currentRoomId));
                        isWebRtc = true;
                        isStreaming = true;
                        chronometer.setVisibility(View.VISIBLE);
                        chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
                        chronometer.start();
                        isRunning = true;
                    } else {
                        // Stream Entire screen via MediaProjection and ScreenWebRtcService
                        Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
                        startActivityForResult(screenCaptureIntent, REQUEST_CODE_WEBRTC_STREAM);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a Room ID", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // "Cancel" বাটন
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    // Dialog প্রদর্শনের মেথড
    private void showChatDialog() {
        if (chatDialog != null && chatDialog.isShowing()) {
            return;
        }

        chatDialog = new Dialog(this);
        chatDialog.setContentView(R.layout.dialog_chat);
        if (chatDialog.getWindow() != null) {
            chatDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        RecyclerView rvChat = chatDialog.findViewById(R.id.rvChatMessages);
        EditText etMessage = chatDialog.findViewById(R.id.etChatMessage);
        ImageButton btnSend = chatDialog.findViewById(R.id.btnSendMessage);
        ImageButton btnClose = chatDialog.findViewById(R.id.btnCloseChat);

        chatAdapter = new ChatAdapter(chatMessageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
        if (!chatMessageList.isEmpty()) {
            rvChat.scrollToPosition(chatMessageList.size() - 1);
        }

        btnSend.setOnClickListener(v -> {
            String msgText = etMessage.getText().toString().trim();
            if (!msgText.isEmpty()) {
                // সার্ভিস দিয়ে চ্যাট সকেটে পাঠানো (এখানে দ্বিতীয় ফিল্ডে মেসেজ পাস করা হচ্ছে)
                sendCommandToService(msgText, "send_chat");

                // টিচারের নিজের ডায়ালগে মেসেজ যোগ করা
                chatMessageList.add(new ChatMessage("Teacher (You)", msgText));
                chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
                rvChat.scrollToPosition(chatMessageList.size() - 1);

                etMessage.setText("");
            }
        });

        btnClose.setOnClickListener(v -> chatDialog.dismiss());

        chatDialog.show();
    }

private void sendCommandToService(String targetId, String command) {
    Intent intent = new Intent(this, DrawingViewWebRtcService.class);
    intent.setAction(DrawingViewWebRtcService.ACTION_SEND_COMMAND);
    intent.putExtra(DrawingViewWebRtcService.EXTRA_TARGET_ID, targetId);
    intent.putExtra(DrawingViewWebRtcService.EXTRA_COMMAND, command);
    startService(intent);

    Intent screenIntent = new Intent(this, ScreenWebRtcService.class);
    screenIntent.setAction(ScreenWebRtcService.ACTION_SEND_COMMAND);
    screenIntent.putExtra(ScreenWebRtcService.EXTRA_TARGET_ID, targetId);
    screenIntent.putExtra(ScreenWebRtcService.EXTRA_COMMAND, command);
    startService(screenIntent);
}

    private void viewWebPage() {
        webCard.setVisibility(View.VISIBLE);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url); // Allow in-app link loading
                return true;
            }
        });

        webView.loadUrl(browseUrl);
    }

    private void showDialogAlert() {
       rtmp_url_card.setVisibility(View.VISIBLE);
        String link=sharedPreferences.getString("Link",null);
       
        
        if (link !=null){
            rtmp_editText.setText(link);
            rtmp_editText.setEnabled(false);
			checkBox.setEnabled(false);
            editImage.setVisibility(View.VISIBLE);
        }
        editImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rtmp_editText.setEnabled(true);
				editImage.setVisibility(View.GONE);
            }
        });

        golive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rtmp_url=rtmp_editText.getText().toString().trim();
                if (rtmp_url.isEmpty()){
                    Toast.makeText(MainActivity.this, "Enter valid url", Toast.LENGTH_SHORT).show();
                }else{
                    mediaProjectionManager=(MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    if (isCameraStreaming){
                        startStreaming(rtmp_url);
                        front_bak.setVisibility(View.VISIBLE);
                    }else if(isGolive){
                        Intent screenCaptureIntent=mediaProjectionManager.createScreenCaptureIntent();
                        startActivityForResult(screenCaptureIntent,REQUEST_CODE_LIVE);
                    }else{
                        Intent screenCaptureIntent=mediaProjectionManager.createScreenCaptureIntent();
                        startActivityForResult(screenCaptureIntent,REQUEST_CODE_BOTH);

                    }
                }

                if (checkBox.isChecked() && !rtmp_url.isEmpty()){
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString("Link", rtmp_url);
				editor.apply(); // You missed this line to save the data
			}


                isRecording=true;
                pauseAndResume.setVisibility(View.VISIBLE);
               rtmp_url_card.setVisibility(View.GONE);
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               rtmp_url_card.setVisibility(View.GONE);
               cameraStreamView.setVisibility(View.GONE);

            }
        });

       
    }

    private void startStreaming(String rtmpUrl) {
        if (!rtmpCamera2.isStreaming()){
            if (rtmpCamera2.prepareAudio(128*1024,44100,true,false,false) && rtmpCamera2.prepareVideo(1920,1080,30,3000*1024,0,0)){
                openGlView.setAspectRatioMode(AspectRatioMode.Adjust);
                rtmpCamera2.startPreview();
                rtmpCamera2.getGlInterface().setFilter(chromaFilterRender);
                rtmpCamera2.startStream(rtmpUrl);
            }else {
                Toast.makeText(MainActivity.this, "Cannot prepare stream", Toast.LENGTH_SHORT).show();
            }
        }
		 chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime()-pauseOffset);
            chronometer.start();
            isRunning=true;
		
    }

    private void captureClippedArea(Rect clipRect) {
        // Create a bitmap with WebView's dimensions
        Bitmap fullBitmap = Bitmap.createBitmap(
                webView.getWidth(),
                webView.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        // Create a Canvas to draw into the bitmap
        Canvas canvas = new Canvas(fullBitmap);

        // Method 1: Preferred for most cases
        webView.draw(canvas);

        // Method 2: Alternative if draw() doesn't work
        // webView.setDrawingCacheEnabled(true);
        // fullBitmap = Bitmap.createBitmap(webView.getDrawingCache());
        // webView.setDrawingCacheEnabled(false);

        // Clip the area inside the rectangle
        Rect safeRect = new Rect(
                Math.max(0, clipRect.left),
                Math.max(0, clipRect.top),
                Math.min(webView.getWidth(), clipRect.right),
                Math.min(webView.getHeight(), clipRect.bottom)
        );

        Bitmap cropped = Bitmap.createBitmap(
                fullBitmap,
                safeRect.left,
                safeRect.top,
                safeRect.width(),
                safeRect.height()
        );

        drawingView.addImageToCanvas(cropped);

    }
    private void clippedArea(Rect clipRect) {
        // Create a Picture to capture the WebView content
        Picture picture = webView.capturePicture();

        // Create a Bitmap from the Picture
        Bitmap fullBitmap = Bitmap.createBitmap(
                picture.getWidth(),
                picture.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(fullBitmap);
        picture.draw(canvas);

        // Clip the area inside the rectangle
        Rect safeRect = new Rect(
                Math.max(0, clipRect.left),
                Math.max(0, clipRect.top),
                Math.min(picture.getWidth(), clipRect.right),
                Math.min(picture.getHeight(), clipRect.bottom)
        );

        Bitmap cropped = Bitmap.createBitmap(
                fullBitmap,
                safeRect.left,
                safeRect.top,
                safeRect.width(),
                safeRect.height()
        );

        drawingView.addImageToCanvas(cropped);
    }


    private void resizeCardView(float rawX, float rawY) {
       ViewGroup.LayoutParams params=cameraViewCardView.getLayoutParams();
       int newWidth=(int)(params.width+(rawX-lastX));
       int newHeight=(int) (params.height+(rawY-lastY));
       if (newWidth>80 && newHeight>80){
           params.width=newWidth;
           params.height=newHeight;
           cameraViewCardView.setLayoutParams(params);
       }
       lastX=rawX;
       lastY=rawY;
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderListenableFuture.get();
                bindCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll(); // Unbind any previously bound use cases

        preview = new Preview.Builder().build();
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        if (isCameraStreaming){
            preview.setSurfaceProvider(((androidx.camera.view.PreviewView) findViewById(R.id.streamCameraView)).getSurfaceProvider());
            Intent intent = CameraStreamService.getIntent(this, rtmp_url);
            startService(intent);

        }else{
            preview.setSurfaceProvider(((androidx.camera.view.PreviewView) findViewById(R.id.cameraView)).getSurfaceProvider());
        }

        cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }



    public void loadFiirebase() {
        userRef = FirebaseDatabase.getInstance().getReference("DigitalBoard").child("users").child(deviceId);

        paymentRef = FirebaseDatabase.getInstance().getReference("DigitalBoard").child("paymentPlans");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    checkSubscriptionStatus(snapshot);
                } else {
                    initializeFreeTrial(userRef);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle the error
            }
        });
    }

    public void startPayment() {
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_live_GVP563KyCPJCpK");
        checkout.setImage(R.drawable.apk_logo);
        int payable_price= Integer.parseInt(price);
        /**
         * Reference to current activity
         */
        final Activity activity = this;

        /**
         * Pass your payment options to the Razorpay Checkout as a JSONObject
         */
        try {
            JSONObject options = new JSONObject();

            options.put("name", "Lachit Global Studies");
            options.put("description", "Digital Board Premium");
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg");
            //options.put("order_id", "order_DBJOWzybf0sJbb");//from response of step 3.
            options.put("theme.color", "#3399cc");
            options.put("currency", "INR");
            options.put("amount", payable_price*100);//pass amount in currency subunits
            options.put("prefill.email", email);
            options.put("prefill.contact",mobile);
            JSONObject retryObj = new JSONObject();
            retryObj.put("enabled", true);
            retryObj.put("max_count", 4);
            options.put("retry", retryObj);

            checkout.open(activity, options);

        } catch(Exception e) {
            Log.e("TAG", "Error in starting Razorpay Checkout", e);
        }
    }



    private boolean isValidMobile() {
         mobile = mobileEditText.getText().toString().trim();
        return mobile.matches("[6-9][0-9]{9}"); // Validates 10-digit mobile numbers starting from 6-9
    }

    private boolean isValidEmail() {
         email = emailEditText.getText().toString().trim();
        return Patterns.EMAIL_ADDRESS.matcher(email).matches(); // Uses built-in pattern for email validation
    }

    private void checkSubscriptionStatus(DataSnapshot snapshot) {
        String freeTrialStart = snapshot.child("freeTrialStart").getValue(String.class);
        String planExpiryDate = snapshot.child("planExpiryDate").getValue(String.class);

        LocalDate currentDate = LocalDate.now();
        LocalDate freeTrialEnd = LocalDate.parse(freeTrialStart).plusDays(3);
        LocalDate planExpiry = planExpiryDate != null ? LocalDate.parse(planExpiryDate) : freeTrialEnd;

        if (currentDate.isAfter(planExpiry)) {
            showPlanExpiredDialog();
        } else {
            // Allow access
            paymentCardView.setVisibility(View.GONE);

        }
    }

    private void initializeFreeTrial(DatabaseReference userRef) {
        LocalDate startDate = LocalDate.now();
        Map<String, Object> userData = new HashMap<>();
        userData.put("freeTrialStart", LocalDate.now().toString());
        userData.put("planType", "none");
        userData.put("planStartDate", LocalDate.now().toString());
        userData.put("planExpiryDate",startDate.plusDays(2).toString());

        userRef.setValue(userData).addOnSuccessListener(aVoid ->
                Toast.makeText(MainActivity.this, "Free trial started", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Log.e("Firebase", "Failed to initialize free trial", e)

        );
    }


    private void showPlanExpiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Subscription Expired")
                .setMessage("Your free trial or plan has expired. Please purchase a plan to continue.")
                .setPositiveButton("Buy Plan", (dialog, which) -> {
                    // Navigate to purchase screen
                    paymentCardView.setVisibility(View.VISIBLE);
                    loadPaymentOptions();
                })
                .setCancelable(false)
                .show();
    }

    private void loadPaymentOptions() {
        paymentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                buttonContainer.removeAllViews();
                if (snapshot.exists()){
                    for (DataSnapshot planSnapshot : snapshot.getChildren()) {
                        String key=planSnapshot.getKey();
                        String value=planSnapshot.getValue(String.class);
                        Button button= new Button(MainActivity.this);
                        button.setText(key + " / ₹" + value);
                        button.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                price=value;
                                planName=key;
                                paymentLayout.setVisibility(View.VISIBLE);
                                paymentBtn.setAllCaps(false);
                                buttonContainer.setVisibility(View.GONE);
                                paymentBtn.setText("Pay "+value+" for "+key);
                            }
                        });
                        buttonContainer.addView(button);

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load plans", Toast.LENGTH_SHORT).show();
            }
        });
    }



    @SuppressLint("ClickableViewAccessibility")
 private  void addDragFunctionality(){
        cardViewFileExplorer.setOnTouchListener(new View.OnTouchListener() {
            float dX ,dY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                if (event.getPointerCount()==1){
                    switch (event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            dX=v.getX()-event.getRawX();
                            dY=v.getY()-event.getRawY();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            v.animate()
                                    .x(event.getRawX()+dX)
                                    .y(event.getRawY()+dY)
                                    .setDuration(0)
                                    .start();
                            return true;
                        default:
                            return false;
                    }
                }
                return false;
            }
        });
     webCard.setOnTouchListener(new View.OnTouchListener() {
         float dX ,dY;
         @Override
         public boolean onTouch(View v, MotionEvent event) {
             gestureDetector.onTouchEvent(event);
             if (event.getPointerCount()==1){
                 switch (event.getAction()){
                     case MotionEvent.ACTION_DOWN:
                         dX=v.getX()-event.getRawX();
                         dY=v.getY()-event.getRawY();
                         return true;

                     case MotionEvent.ACTION_MOVE:
                         v.animate()
                                 .x(event.getRawX()+dX)
                                 .y(event.getRawY()+dY)
                                 .setDuration(0)
                                 .start();
                         return true;
                     default:
                         return false;
                 }
             }
             return false;
         }
     });
        cameraViewCardView.setOnTouchListener(new View.OnTouchListener() {
            float dX ,dY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                if (event.getPointerCount()==1){
                    switch (event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            dX=v.getX()-event.getRawX();
                            dY=v.getY()-event.getRawY();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            v.animate()
                                    .x(event.getRawX()+dX)
                                    .y(event.getRawY()+dY)
                                    .setDuration(0)
                                    .start();
                            return true;
                        default:
                            return false;
                    }
                } else if (event.getPointerCount()==2) {

                    if (isResizing && event.getAction()==MotionEvent.ACTION_MOVE){
                        resizeCardView(event.getRawX(),event.getRawY());
                    }
                    if (event.getAction()==MotionEvent.ACTION_UP){
                        isResizing=false;
                    }
                }
                return false;
            }
        });

     videoCard.setOnTouchListener(new View.OnTouchListener() {
         float dX ,dY;
         @Override
         public boolean onTouch(View v, MotionEvent event) {
             gestureDetector.onTouchEvent(event);
             if (event.getPointerCount()==1){
                 switch (event.getAction()){
                     case MotionEvent.ACTION_DOWN:
                         dX=v.getX()-event.getRawX();
                         dY=v.getY()-event.getRawY();
                         return true;

                     case MotionEvent.ACTION_MOVE:
                         v.animate()
                                 .x(event.getRawX()+dX)
                                 .y(event.getRawY()+dY)
                                 .setDuration(0)
                                 .start();
                         return true;
                     default:
                         return false;
                 }
             }
             return false;
         }
     });
    }



    private void addPdfToCanvas(File pdfFile) {
        Uri pdfUri = Uri.fromFile(pdfFile);

        // Pass the PDF URI to the DrawingView
        drawingView.addPdf(pdfUri);
        loadPageNo();

    }



    private void addImageToCanvas(File imageFile) {
        ArrayList<Uri> imageUris = new ArrayList<>();

        // Get the Uri from the File
       Uri imageUri = Uri.fromFile(imageFile);
        try{
            Bitmap bitmap= MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageUri);
            drawingView.addImageToCanvas(bitmap);
        }catch (Exception e){
            e.printStackTrace();
        }
//       imageUris.add(imageUri);
//
//        // Pass the image URIs to the DrawingView
//         drawingView.addImages(imageUris);
    }





    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File file = fileList.get(position);

        if (file.isDirectory()) {
            currentDirectory = file;
            listFiles(currentDirectory);
        } else {
            openFile(file);
        }
    }
    // Open a file using an appropriate app
    private void openFile(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri fileUri = Uri.fromFile(file);
        intent.setDataAndType(fileUri, "*/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadPageNo() {
//        drawingView.updateUI();
//        pagesAdapter.updateUI();
        int currentPage=drawingView.getCurrentPage()+1;
        int totalPage=drawingView.getTotalPage();
        pageCurrent.setText(""+currentPage);
        pageAll.setText("/"+totalPage);
    }




    private class GestureListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            super.onLongPress(e);
            if (isResizeZone(e.getRawX(),e.getRawY())){
            isResizing=true;
            lastX=e.getRawX();
            lastY=e.getRawY();
            }
        }

        @Override
    public boolean onDoubleTap(MotionEvent event){

            return false;
        }
}

    private boolean isResizeZone(float rawX, float rawY) {
        int width=cameraViewCardView.getWidth();
        int height=cameraViewCardView.getHeight();
        int[] location=new int[2];
        cameraViewCardView.getLocationOnScreen(location);
        int left=location[0];
        int top=location[1];
        int right=left+width;
        int bottom=top+height;
        return (Math.abs(rawX-right)<RESIZE_THRESHOLD && Math.abs(rawY-bottom)<RESIZE_THRESHOLD);
    }

    private void listFiles(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            fileList.clear();

            // Add "Parent Directory" option
            if (!directory.getAbsolutePath().equals("/storage/emulated/0/")) {
                fileList.add(new File(directory.getParent()));
            }

            for (File file : files) {
                fileList.add(file);
            }

            FileListAdapter adapter = new FileListAdapter(this, fileList);
            listViewFiles.setAdapter(adapter);
        } else {
            Toast.makeText(this, "Cannot access directory", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI(); // Reapply the UI flags to hide the system UI
        LocalBroadcastManager.getInstance(this).registerReceiver(studentReceiver, new IntentFilter("ACTION_STUDENT_JOINED"));
        // VideoTrack আসার সাথে সাথে লিস্ট আপডেট হবে

    }




    public void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

    }


    private void pauseRecording() {
        startService(ScreenCaptureAndLiveService.getPauseIntent(this));
        pausedTime= SystemClock.elapsedRealtime()-chronometer.getBase();
        chronometer.stop();
    }

    private void resumeRecording() {
        startService(ScreenCaptureAndLiveService.getResumeIntent(this));
        chronometer.setBase(SystemClock.elapsedRealtime()-pausedTime);
        chronometer.start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_LIVE) {
                startService(RtmpServce.getInternt(this, resultCode, data, rtmp_url));
                isLive = true;
                isStreaming = true;
            } else if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
                startService(ScreenCaptureService.getStartIntent(this, resultCode, data));
                isScreenCapture = true;
                isStreaming = true;
            } else if(requestCode==REQUEST_CODE_WEBRTC_STREAM){
				startService(ScreenWebRtcService.getStartIntent(this,resultCode,data,currentRoomId));
				isWebRtc=true;
				isStreaming=true;
				}else if (requestCode == REQUEST_CODE_BOTH) {
                startService(ScreenCaptureAndLiveService.getStartIntent(this, resultCode, data, rtmp_url));
                isBothLiveAndScreen = true;
                isStreaming = true;
            }

            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isRunning = true;
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Screen capture permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        startService(ScreenCaptureService.getStopIntent(this));
        chronometer.setVisibility(View.GONE);
    }

    private boolean checkPermissions() {
        for (String perm : getAppPermissions()) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public boolean askPermissions() {
        List<String> listPermissions = new ArrayList<>();
        for (String perm : getAppPermissions()) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissions.add(perm);
            }
        }
        if (!listPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissions.toArray(new String[0]), REQUEST_CODE);
            return false;
        }
        return true;
    }
    @Override
    public void onPaymentSuccess(String s) {
        Toast.makeText(this, "Payment successful "+s, Toast.LENGTH_SHORT).show();
        updatePlanDetails(planName);  // Call method to update user details
        paymentCardView.setVisibility(View.GONE);
    }

    private void updatePlanDetails(String planType) {
        String planeName=planType.replace("days","");
        int planDays=Integer.parseInt(planeName)-1;

        Map<String, Object> planData = new HashMap<>();
        LocalDate startDate = LocalDate.now();
        planData.put("planType", planType);
        planData.put("planStartDate", startDate.toString());
        planData.put("planExpiryDate", startDate.plusDays(planDays).toString());

        userRef.updateChildren(planData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Subscription updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update subscription!", Toast.LENGTH_SHORT).show();
            }
        });
    }



    @Override
    public void onPaymentError(int i, String s) {
        Toast.makeText(this, "Payment failed "+s, Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }
            if (deniedCount == 0) {
                Toast.makeText(MainActivity.this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                boolean anyRationale = false;
                for (Map.Entry<String, Integer> entry : permissionResults.entrySet()) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, entry.getKey())) {
                        anyRationale = true;
                        break;
                    }
                }
                if (anyRationale) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permissions")
                            .setCancelable(false)
                            .setMessage("Please provide permissions (storage, camera, recording) to use all features of the board.")
                            .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    askPermissions();
                                }
                            }).setNegativeButton("Continue Anyway", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Permissions")
                            .setCancelable(false)
                            .setMessage("Some permissions were not granted. You can enable them in Settings > Permissions, or continue using the whiteboard.")
                            .setPositiveButton("Go to settings", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).setNegativeButton("Continue to Board", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
            }
        }
    }

  @Override
public void onBackPressed() {
    if (isStreaming) {
        // Show only the streaming exit dialog
        streamExit();
        return;
    }
    if (isCameraStreaming){
        streamExit();
        return;
    }
    if (cameraStreamView.getVisibility()==View.VISIBLE){
        cameraStreamView.setVisibility(View.GONE);
        return;
    }

    if (paymentLayout.getVisibility() == View.VISIBLE) {
        paymentLayout.setVisibility(View.GONE);
        buttonContainer.setVisibility(View.VISIBLE);
        return;
    }

    if (paymentCardView.getVisibility() == View.VISIBLE) {
        showDialogSelect();
        return;
    }

    showDialogSelect();
}


    private void streamExit() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.stream_exit);

        // Set the dialog window background to be transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button exit = dialog.findViewById(R.id.btn_exit);
        Button cancel = dialog.findViewById(R.id.btn_cancel);

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               stopAllService();
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void stopAllService() {
		if(isWebRtc){
			stopService(new Intent(this, DrawingViewWebRtcService.class));
			stopService(new Intent(this, ScreenWebRtcService.class));
            isWebRtc=false;
            isStreaming=false;
	}else if (isLive){
            Intent serviceIntent = new Intent(this, RtmpServce.class);
            stopService(serviceIntent);
            isLive=false;
            isStreaming=false;
        } else if (isScreenCapture) {
            Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
            stopService(serviceIntent);
            isScreenCapture=false;
            isStreaming=false;
            
        } else if (isBothLiveAndScreen) {
            Intent serviceIntent = new Intent(this, ScreenCaptureAndLiveService.class);
            stopService(serviceIntent);
            isBothLiveAndScreen=false;
            isStreaming=false;
        }else if (isCameraStreaming){
            Intent serviceIntent=new Intent(this,CameraStreamService.class);
            stopService(serviceIntent);
            isCameraStreaming=false;
        }
        chronometer.stop();
    }

    private void showDialogSelect() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.exit_massage);

        // Set the dialog window background to be transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button exit = dialog.findViewById(R.id.btn_exit);
        Button cancel = dialog.findViewById(R.id.btn_cancel);

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter studentFilter = new IntentFilter("ACTION_STUDENT_JOINED");
        LocalBroadcastManager.getInstance(this).registerReceiver(studentReceiver, studentFilter);

        // ২. চ্যাট মেসেজ ব্রডকাস্ট ফিল্টার
        IntentFilter chatFilter = new IntentFilter("ACTION_RECEIVE_CHAT_MESSAGE");
        LocalBroadcastManager.getInstance(this).registerReceiver(chatMessageReceiver, chatFilter);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            registerReceiver(studentReceiver,studentFilter,Context.RECEIVER_NOT_EXPORTED);
        }else{
            registerReceiver(studentReceiver,studentFilter);
        }
        Intent serviceIntent = new Intent(this, ScreenCaptureAndLiveService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        registerReceiver(networkChangeReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
           
			
		



    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        unregisterReceiver(networkChangeReceiver);
        unregisterReceiver(studentReceiver);
        // রিসিভারগুলো আনরেজিস্টার করা (মেমোরি লিক রোধ করতে)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(studentReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(chatMessageReceiver);
    }


 

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(studentReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
       stopAllService();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(studentReceiver);

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
