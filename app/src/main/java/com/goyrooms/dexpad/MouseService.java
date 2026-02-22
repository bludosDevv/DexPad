package com.goyrooms.dexpad;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class MouseService extends Service {

    private static final String TAG = "DexPad_Service";
    
    private WindowManager windowManager;
    private ImageView cursorView;
    private WindowManager.LayoutParams cursorParams;

    private float cursorX = 500;
    private float cursorY = 800;
    private int screenWidth;
    private int screenHeight;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        Toast.makeText(this, "Initializing Mouse Service...", Toast.LENGTH_SHORT).show();
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
                screenWidth = 1080;
                screenHeight = 1920;
            }

            // Setup cursor view
            setupCursor();
            
            Log.d(TAG, "Service initialized successfully");
            Toast.makeText(this, "Mouse Service Ready", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
            Toast.makeText(this, "Service error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void setupCursor() {
        try {
            Log.d(TAG, "Setting up cursor view");
            
            cursorView = new ImageView(this);
            
            try {
                int drawableId = getResources().getIdentifier("ic_mouse_pointer", "drawable", getPackageName());
                if (drawableId == 0) {
                    Log.w(TAG, "Drawable ic_mouse_pointer not found, using default");
                    cursorView.setImageResource(android.R.drawable.ic_dialog_info);
                } else {
                    cursorView.setImageResource(drawableId);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error setting cursor image", e);
                cursorView.setImageResource(android.R.drawable.ic_dialog_info);
            }

            cursorParams = new WindowManager.LayoutParams(
                    100, // width
                    100, // height
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);

            cursorParams.gravity = Gravity.TOP | Gravity.LEFT;
            cursorParams.x = (int) cursorX;
            cursorParams.y = (int) cursorY;

            windowManager.addView(cursorView, cursorParams);
            Log.d(TAG, "Cursor view added successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up cursor", e);
            Toast.makeText(this, "Cursor error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        
        try {
            if (windowManager != null && cursorView != null) {
                try {
                    windowManager.removeView(cursorView);
                    Log.d(TAG, "Cursor view removed");
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "View was not added or already removed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
        
        Toast.makeText(this, "Mouse Service Stopped", Toast.LENGTH_SHORT).show();
    }
}

