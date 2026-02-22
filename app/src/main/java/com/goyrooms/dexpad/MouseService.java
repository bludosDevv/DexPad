package com.goyrooms.dexpad;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.lang.reflect.Method;

public class MouseService extends Service {

    private WindowManager windowManager;
    private ImageView cursorView;
    private View panelView;
    private WindowManager.LayoutParams cursorParams;

    private float cursorX = 500;
    private float cursorY = 800;
    private int screenWidth;
    private int screenHeight;

    private float lastX, lastY;

    private InputManager inputManager;
    private Method injectMethod;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            inputManager = (InputManager) getSystemService(INPUT_SERVICE);
            injectMethod = InputManager.class.getDeclaredMethod(
                    "injectInputEvent",
                    android.view.InputEvent.class,
                    int.class
            );
            injectMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        setupCursor();
        setupPanel();
    }

    private void setupCursor() {
        cursorView = new ImageView(this);
        cursorView.setImageResource(R.drawable.ic_mouse_pointer);

        cursorParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        cursorParams.gravity = Gravity.TOP | Gravity.LEFT;
        cursorParams.x = (int) cursorX;
        cursorParams.y = (int) cursorY;

        windowManager.addView(cursorView, cursorParams);
    }

    private void setupPanel() {
        panelView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.END;
        windowManager.addView(panelView, params);

        panelView.findViewById(R.id.touchpad).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                lastX = e.getRawX();
                lastY = e.getRawY();
            } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                float dx = e.getRawX() - lastX;
                float dy = e.getRawY() - lastY;

                moveMouse(dx, dy);

                lastX = e.getRawX();
                lastY = e.getRawY();
            }
            return true;
        });

        panelView.findViewById(R.id.btn_left_click)
                .setOnClickListener(v -> click(MotionEvent.BUTTON_PRIMARY));

        panelView.findViewById(R.id.btn_right_click)
                .setOnClickListener(v -> click(MotionEvent.BUTTON_SECONDARY));
    }

    private void moveMouse(float dx, float dy) {
        cursorX = Math.max(0, Math.min(screenWidth, cursorX + dx));
        cursorY = Math.max(0, Math.min(screenHeight, cursorY + dy));

        cursorParams.x = (int) cursorX;
        cursorParams.y = (int) cursorY;
        windowManager.updateViewLayout(cursorView, cursorParams);

        inject(MotionEvent.ACTION_HOVER_MOVE, 0);
    }

    private void click(int button) {
        inject(MotionEvent.ACTION_BUTTON_PRESS, button);
        inject(MotionEvent.ACTION_BUTTON_RELEASE, button);
    }

    private void inject(int action, int button) {
        try {
            long now = SystemClock.uptimeMillis();

            MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[1];
            props[0] = new MotionEvent.PointerProperties();
            props[0].id = 0;
            props[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;

            MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[1];
            coords[0] = new MotionEvent.PointerCoords();
            coords[0].x = cursorX;
            coords[0].y = cursorY;
            coords[0].pressure = 1;
            coords[0].size = 1;

            MotionEvent event = MotionEvent.obtain(
                    now, now,
                    action,
                    1,
                    props,
                    coords,
                    0,
                    button,
                    1f,
                    1f,
                    0,
                    0,
                    InputDevice.SOURCE_MOUSE,
                    0
            );

            injectMethod.invoke(inputManager, event, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}