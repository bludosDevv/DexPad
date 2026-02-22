package com.goyrooms.dexpad;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MouseService extends Service implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private static final String TAG = "DexPad_Service";
    private static final int CURSOR_SIZE_DP = 24;
    private static final long LONG_PRESS_TIME = 500L;
    
    private WindowManager windowManager;
    private MouseOverlayView overlayView;
    private WindowManager.LayoutParams overlayParams;

    private float cursorX = 500;
    private float cursorY = 800;
    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private GestureDetector gestureDetector;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate called");

        try {
            // Check overlay permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    Log.w(TAG, "Overlay permission not granted");
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_LONG).show();
                    stopSelf();
                    return;
                }
            }

            Log.d(TAG, "Overlay permission granted, continuing...");
            
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            
            if (windowManager == null) {
                Log.e(TAG, "WindowManager is null!");
                Toast.makeText(this, "Error: WindowManager unavailable", Toast.LENGTH_SHORT).show();
                stopSelf();
                return;
            }

            // Get screen dimensions
            try {
                Display display = windowManager.getDefaultDisplay();
                Point size = new Point();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    display.getRealSize(size);
                } else {
                    display.getSize(size);
                }
                screenWidth = size.x;
                screenHeight = size.y;
                Log.d(TAG, "Screen dimensions: " + screenWidth + "x" + screenHeight);
            } catch (Exception e) {
                Log.e(TAG, "Error getting screen dimensions", e);
            }

            // Setup gesture detector
            gestureDetector = new GestureDetector(this, this);
            gestureDetector.setOnDoubleTapListener(this);

            // Setup overlay
            setupOverlay();
            
            Log.d(TAG, "Service initialized successfully");
            Toast.makeText(this, "Mouse Overlay Ready - Drag to Control", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
            Toast.makeText(this, "Service error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void setupOverlay() {
        try {
            Log.d(TAG, "Setting up overlay");
            
            overlayView = new MouseOverlayView(this);
            
            int cursorSizePx = dpToPx(CURSOR_SIZE_DP);
            overlayParams = new WindowManager.LayoutParams(
                    screenWidth,
                    screenHeight,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);

            overlayParams.gravity = Gravity.TOP | Gravity.LEFT;
            overlayParams.x = 0;
            overlayParams.y = 0;

            windowManager.addView(overlayView, overlayParams);
            Log.d(TAG, "Overlay view added successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up overlay", e);
            Toast.makeText(this, "Overlay error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // Gesture callbacks
    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(TAG, "Single tap (left click)");
        injectMouseClick(1, e.getX(), e.getY());
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(TAG, "Scroll - updating cursor position");
        cursorX = e2.getX();
        cursorY = e2.getY();
        
        // Clamp to screen bounds
        cursorX = Math.max(0, Math.min(cursorX, screenWidth));
        cursorY = Math.max(0, Math.min(cursorY, screenHeight));
        
        if (overlayView != null) {
            overlayView.setCursorPosition(cursorX, cursorY);
        }
        
        injectPointerMove(cursorX, cursorY);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "Long press (right click)");
        injectMouseClick(2, e.getX(), e.getY());
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d(TAG, "Single tap confirmed");
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(TAG, "Double tap (double click)");
        injectMouseClick(1, e.getX(), e.getY());
        handler.postDelayed(() -> injectMouseClick(1, e.getX(), e.getY()), 50);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return true;
    }

    // Mouse input injection
    private void injectMouseClick(int button, float x, float y) {
        try {
            Log.d(TAG, "Injecting mouse click button=" + button + " at x=" + x + " y=" + y);
            
            // Check if Shizuku permission is granted
            if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Shizuku permission not granted, attempting local input command");
            }

            String command;
            if (button == 1) {
                // Left click using input tap
                command = String.format("input tap %d %d", (int) x, (int) y);
            } else if (button == 2) {
                // Right click using input swipe (simulate right click with long press)
                command = String.format("input swipe %d %d %d %d 100", (int) x, (int) y, (int) (x + 1), (int) (y + 1));
            } else {
                command = String.format("input tap %d %d", (int) x, (int) y);
            }
            
            Log.d(TAG, "Executing: " + command);
            
            try {
                // Try via Shizuku first
                execShizukuCommand(command, button);
            } catch (Exception e) {
                Log.w(TAG, "Shizuku command failed, trying direct execution", e);
                execDirectCommand(command);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error injecting mouse click", e);
        }
    }

    private void injectPointerMove(float x, float y) {
        try {
            Log.d(TAG, "Pointer move to: " + x + ", " + y);
            
        } catch (Exception e) {
            Log.e(TAG, "Error moving pointer", e);
        }
    }

    private void execShizukuCommand(String command, int button) throws Exception {
        Log.d(TAG, "Executing via Shizuku: " + command);
        
        // Get user service from Shizuku and execute
        android.os.IBinder binder = Shizuku.getBinder();
        if (binder != null && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Using Shizuku transact interface");
            
            // Create parcel for transact call
            android.os.Parcel data = android.os.Parcel.obtain();
            android.os.Parcel reply = android.os.Parcel.obtain();
            
            try {
                data.writeInterfaceToken("com.android.shell.IShellCommand");
                data.writeString(command);
                
                binder.transact(android.os.IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
                Log.d(TAG, "Shizuku transact successful");
            } finally {
                data.recycle();
                reply.recycle();
            }
        } else {
            execDirectCommand(command);
        }
    }

    private void execDirectCommand(String command) {
        try {
            Log.d(TAG, "Executing direct shell command: " + command);
            java.lang.Process process = java.lang.Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            process.waitFor();
            Log.d(TAG, "Direct command executed");
        } catch (Exception e) {
            Log.e(TAG, "Direct command failed", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        
        try {
            if (windowManager != null && overlayView != null) {
                try {
                    windowManager.removeView(overlayView);
                    Log.d(TAG, "Overlay view removed");
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "View was not added or already removed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
        
        handler.removeCallbacksAndMessages(null);
        Toast.makeText(this, "Mouse Overlay Stopped", Toast.LENGTH_SHORT).show();
    }

    // Custom Overlay View with Touch Handling
    public class MouseOverlayView extends FrameLayout implements View.OnTouchListener {
        private ImageView cursorView;
        private GestureDetector detector;

        public MouseOverlayView(android.content.Context context) {
            super(context);
            setBackgroundColor(Color.TRANSPARENT);
            setOnTouchListener(this);
            
            detector = new GestureDetector(context, MouseService.this);

            // Create small cursor view
            cursorView = new ImageView(context);
            cursorView.setBackgroundColor(Color.argb(200, 255, 0, 0)); // Semi-transparent red
            int cursorSizePx = dpToPx(CURSOR_SIZE_DP);
            
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(cursorSizePx, cursorSizePx);
            params.gravity = Gravity.TOP | Gravity.LEFT;
            
            addView(cursorView, params);
            setCursorPosition(500, 800);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return detector.onTouchEvent(event);
        }

        public void setCursorPosition(float x, float y) {
            post(() -> {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cursorView.getLayoutParams();
                params.leftMargin = (int) x;
                params.topMargin = (int) y;
                cursorView.setLayoutParams(params);
            });
        }
    }
}

