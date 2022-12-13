package flutter.overlay.window.flutter_overlay_window;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.app.PendingIntent;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;

import com.example.flutter_overlay_window.R;
import com.robertlevonyan.views.customfloatingactionbutton.FloatingActionButton;
import com.robertlevonyan.views.customfloatingactionbutton.FloatingLayout;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import io.flutter.embedding.android.FlutterTextureView;
import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.JSONMessageCodec;
import io.flutter.plugin.common.MethodChannel;

public class OverlayService extends Service implements View.OnTouchListener {

    public static final String INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow";
    public static boolean isRunning = false;
    private WindowManager windowManager = null;
    private FlutterView flutterView;
    private MethodChannel flutterChannel = new MethodChannel(FlutterEngineCache.getInstance().get(OverlayConstants.CACHED_TAG).getDartExecutor(), OverlayConstants.OVERLAY_TAG);
    private BasicMessageChannel<Object> overlayMessageChannel = new BasicMessageChannel(FlutterEngineCache.getInstance().get(OverlayConstants.CACHED_TAG).getDartExecutor(), OverlayConstants.MESSENGER_TAG, JSONMessageCodec.INSTANCE);
    private int clickableFlag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

    private Handler mAnimationHandler = new Handler();
    private float lastX, lastY;
    private int lastYPosition;
    private boolean dragging;
    private static final float MAXIMUM_OPACITY_ALLOWED_FOR_S_AND_HIGHER = 0.8f;
    private Point szWindow = new Point();
    private Timer mTrayAnimationTimer;
    private TrayAnimationTimerTask mTrayTimerTask;

    private View myView;
    private Timer timer;
    private long timerSec = 0;
    private FloatingActionButton mainFab;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onDestroy() {
        Log.d("OverLay", "Destroying the overlay window service");
        isRunning = false;
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(OverlayConstants.NOTIFICATION_ID);
    }

    private void stopTimer(){
        if(timer == null){
            return;
        }
        timer.cancel();
        timerSec = 0;
    }

    private void startTimer(){
        timer = new Timer();
        timerSec = 0;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timerSec += 1;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        mainFab.setText(secToMinSec());

                    }
                });

            }
        },0,1000);
    }
    private String secToMinSec(){
        if(timerSec == 0) { return "00:00"; }
        Date date = new Date(timerSec * 1000);
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss", Locale.ENGLISH);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    private Drawable resize(Drawable image) {
        Bitmap b = ((BitmapDrawable)image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 12, 12, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }


    private Drawable getAppIcon(){
        // return AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_launcher);

       try
       {

           Drawable icon = getApplicationContext().getPackageManager().getApplicationIcon(OverlayConstants.APP_PACKAGE);
           return icon;
       }
       catch (PackageManager.NameNotFoundException e) {
           e.printStackTrace();
           return AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_launcher);
       }
    }

    boolean isExpanded = false;
    ViewGroup.LayoutParams prms = null;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isCloseWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false);
        if (isCloseWindow) {
            if (windowManager != null) {
                stopTimer();
                windowManager.removeView(myView);
                windowManager = null;
                stopSelf();
            }
            isRunning = false;
            return START_STICKY;
        }
        if (windowManager != null) {
            stopTimer();
            windowManager.removeView(myView);
            windowManager = null;
            stopSelf();
        }
        isRunning = true;
        Log.d("onStartCommand", "Service started");
        FlutterEngine engine = FlutterEngineCache.getInstance().get(OverlayConstants.CACHED_TAG);
        EventsChannel.instant.configureChannel(engine.getDartExecutor().getBinaryMessenger());
        engine.getLifecycleChannel().appIsResumed();
        flutterView = new FlutterView(getApplicationContext(), new FlutterTextureView(getApplicationContext()));
        flutterView.attachToFlutterEngine(FlutterEngineCache.getInstance().get(OverlayConstants.CACHED_TAG));
        flutterView.setFitsSystemWindows(true);
        flutterView.setFocusable(true);
        flutterView.setFocusableInTouchMode(true);
        flutterView.setBackgroundColor(Color.TRANSPARENT);
        flutterChannel.setMethodCallHandler((call, result) -> {
            if (call.method.equals("updateFlag")) {
                String flag = call.argument("flag").toString();
                updateOverlayFlag(result, flag);
            } else if (call.method.equals("resizeOverlay")) {
                int width = call.argument("width");
                int height = call.argument("height");
                resizeOverlay(width, height, result);
            }
        });

        overlayMessageChannel.setMessageHandler((message, reply) -> {
            WindowSetup.messenger.send(message);
        });
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        int LAYOUT_TYPE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_PHONE;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_TYPE,
                WindowSetup.flag | WindowManager.LayoutParams.FLAG_SECURE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && WindowSetup.flag == clickableFlag) {
            params.alpha = MAXIMUM_OPACITY_ALLOWED_FOR_S_AND_HIGHER;
        }
        params.gravity = WindowSetup.gravity;
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());

        myView = inflater.inflate(R.layout.fab, null);
        mainFab = myView.findViewById(R.id.main_fab);
        FloatingLayout fabLayout = myView.findViewById(R.id.floating_layout);
        FloatingActionButton appFab = myView.findViewById(R.id.app_fab);
        FloatingActionButton stopFab = myView.findViewById(R.id.stop_fab);

//        Log.e("MINE", String.valueOf(appFab.getHeight()));
//        Log.e("MINE", String.valueOf(stopFab.getHeight()));
//        appFab.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
////                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
////                    this.appFab.getViewTreeObserver().removeGlobalOnLayoutListener(this);
////                } else {
////                    this.appFab.getViewTreeObserver().removeOnGlobalLayoutListener(this);
////                }
//                int width  = appFab.getMeasuredWidth();
//                int height = appFab.getMeasuredHeight();
//                Log.e("MINE", String.valueOf(height));
//                if(prms == null){
//                    prms = fabLayout.getLayoutParams();
//                }
//                if(height > 0){
//                    if(!isExpanded){
//                        isExpanded  = true;
//                        int newHeight = prms.height +  height * 2;
//                        prms.height = newHeight;
//                        ConstraintLayout.LayoutParams newParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,newHeight);
//                        fabLayout.setLayoutParams(newParams);
//                        //
//                        WindowManager.LayoutParams params = (WindowManager.LayoutParams) flutterView.getLayoutParams();
//                        params.width = width;
//                        params.height = height;
//                        windowManager.updateViewLayout(myView, params);
//                        //
////                        myView.updateViewLayout(fabLayout,prms);
////                        windowManager.updateViewLayout(myView, myView.getLayoutParams());
//                        Log.e("MINE","UPDATED PARAMS" + newHeight);
//                    }
//                } else {
//                    if(isExpanded){
//                        isExpanded  = false;
//                        int newHeight = prms.height +  height * 2;
//                        prms.height = newHeight;
//                        ConstraintLayout.LayoutParams newParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,newHeight);
//
//                        fabLayout.setLayoutParams(newParams);
//                        //
//                        WindowManager.LayoutParams params = (WindowManager.LayoutParams) flutterView.getLayoutParams();
//                        params.width = width;
//                        params.height = height;
//                        windowManager.updateViewLayout(myView, params);
//                        //
//
////                        myView.updateViewLayout(fabLayout,prms);
////                        windowManager.updateViewLayout(myView, myView.getLayoutParams());
//                        Log.e("MINE","UPDATED PARAMS" + newHeight);
//
//                    }
//                }
//
//
//            }
//        });

//        fabLayout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                return false;
//            }
//        });
//        fabLayout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                return true;
//            }
//        });
//
//        mainFab.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
//                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                        public void run() {
//                            if(isExpanded){
//                                fabLayout.setLayoutParams(new ConstraintLayout.LayoutParams(
//                                        ConstraintLayout.LayoutParams.WRAP_CONTENT,ConstraintLayout.LayoutParams.WRAP_CONTENT
//                                ));
//                            } else {
//                                fabLayout.setLayoutParams(new ConstraintLayout.LayoutParams(
//                                        ConstraintLayout.LayoutParams.WRAP_CONTENT,220
//                                ));
//                            }
//                            isExpanded = !isExpanded;
//
//                        }
//                    },500);
//                }
//                return true;
//            }
//        });


        appFab.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                   OverlayConstants.launchApp(getApplicationContext());
                }
                return true;
            }
        });
        stopFab.setOnTouchListener(OverlayConstants.stopRecordingTouchListener);

//        fabLayout.setOnMenuExpandedListener(new FloatingLayout.OnMenuExpandedListener() {
//            @Override
//            public void onMenuExpanded() {
//                fabLayout.setLayoutParams(new ConstraintLayout.LayoutParams(
//                        ConstraintLayout.LayoutParams.WRAP_CONTENT,450
//                ));
//            }
//
//            @Override
//            public void onMenuCollapsed() {
//                fabLayout.setLayoutParams(new ConstraintLayout.LayoutParams(
//                        ConstraintLayout.LayoutParams.WRAP_CONTENT,ConstraintLayout.LayoutParams.WRAP_CONTENT
//                ));
//            }
//        });
//        stopFab.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
////                    MethodChannel customChannel = new MethodChannel(engine.getDartExecutor().getBinaryMessenger(), "x-slayer/customChannel");
////                    getParent().customChannel.invokeMethod("Frm Stop event",null);
//
////                    launchApp();
////                    EventsChannel.instant.stopScreenRecording();
////                    WindowSetup.messenger.send("This is my message from overlay");
////                    WindowSetup.messenger.send("Stop recording event");
//                }
//                return true;
//            }
//        });
        mainFab.setText("00:00");
        mainFab.setClickable(true);
        mainFab.setOnTouchListener(this);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    Drawable drawable = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.stop);
                    drawable.setBounds(35, 35, 75, 75);
                    stopFab.getOverlay().add(drawable);

                }
            });



            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    Drawable drawable = getAppIcon();
                    drawable.setBounds(0, 0, 110, 110);
                    appFab.getOverlay().add(drawable);

                }
            });





        windowManager.addView(myView, params);



        startTimer();

        return START_STICKY;
    }




    private void updateOverlayFlag(MethodChannel.Result result, String flag) {
        if (windowManager != null) {
            WindowSetup.setFlag(flag);
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) flutterView.getLayoutParams();
            params.flags = WindowSetup.flag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && WindowSetup.flag == clickableFlag) {
                params.alpha = MAXIMUM_OPACITY_ALLOWED_FOR_S_AND_HIGHER;
            }
            windowManager.updateViewLayout(myView, params);
            result.success(true);
        } else {
            result.success(false);
        }
    }

    private void resizeOverlay(int width, int height, MethodChannel.Result result) {
        if (windowManager != null) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) flutterView.getLayoutParams();
            params.width = width;
            params.height = height;
            windowManager.updateViewLayout(myView, params);
            result.success(true);
        } else {
            result.success(false);
        }
    }

    @Override
    public void onCreate() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, FlutterOverlayWindowPlugin.class);
        int pendingFlags;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingFlags = PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, pendingFlags);
        final int notifyIcon = getDrawableResourceId("mipmap", "launcher");
        Notification notification = new NotificationCompat.Builder(this, OverlayConstants.CHANNEL_ID)
                .setContentTitle(WindowSetup.overlayTitle)
                .setContentText(WindowSetup.overlayContent)
                .setSmallIcon(notifyIcon == 0 ? R.drawable.notification_icon : notifyIcon)
                .setContentIntent(pendingIntent)
                .setVisibility(WindowSetup.notificationVisibility)
                .build();
        startForeground(OverlayConstants.NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    OverlayConstants.CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private int getDrawableResourceId(String resType, String name) {
        return getApplicationContext().getResources().getIdentifier(String.format("ic_%s", name), resType, getApplicationContext().getPackageName());
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {

        if (windowManager != null && WindowSetup.enableDrag) {

            WindowManager.LayoutParams params = (WindowManager.LayoutParams) myView.getLayoutParams();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dragging = false;
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastX;
                    float dy = event.getRawY() - lastY;
                    if (!dragging && dx * dx + dy * dy < 25) {
                        return false;
                    }
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    int xx = params.x - (int) dx;
                    int yy = params.y + (int) dy;
                    params.x = xx;
                    params.y = yy;
                    windowManager.updateViewLayout(myView, params);
                    dragging = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    lastYPosition = params.y;
                    if (WindowSetup.positionGravity != "none") {
                        windowManager.updateViewLayout(myView, params);
                        mTrayTimerTask = new TrayAnimationTimerTask();
                        mTrayAnimationTimer = new Timer();
                        mTrayAnimationTimer.schedule(mTrayTimerTask, 0, 25);
                    }
                    return dragging;
                default:
                    return false;
            }
            return false;
        }
        return false;
    }

    private class TrayAnimationTimerTask extends TimerTask {
        int mDestX;
        int mDestY;
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) myView.getLayoutParams();

        public TrayAnimationTimerTask() {
            super();
            mDestY = lastYPosition;
            switch (WindowSetup.positionGravity) {
                case "auto":
                    mDestX = (params.x + (myView.getWidth() / 2)) <= szWindow.x / 2 ? 0 : szWindow.x - myView.getWidth();
                    return;
                case "left":
                    mDestX = 0;
                    return;
                case "right":
                    mDestX = szWindow.x - myView.getWidth();
                    return;
                default:
                    mDestX = params.x;
                    mDestY = params.y;
                    return;
            }
        }

        @Override
        public void run() {
            mAnimationHandler.post(() -> {
                params.x = (2 * (params.x - mDestX)) / 3 + mDestX;
                params.y = (2 * (params.y - mDestY)) / 3 + mDestY;
                windowManager.updateViewLayout(myView, params);
                if (Math.abs(params.x - mDestX) < 2 && Math.abs(params.y - mDestY) < 2) {
                    TrayAnimationTimerTask.this.cancel();
                    mTrayAnimationTimer.cancel();
                }
            });
        }
    }


}