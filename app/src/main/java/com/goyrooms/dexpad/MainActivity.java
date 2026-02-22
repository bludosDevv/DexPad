package com.goyrooms.dexpad;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int SHIZUKU_CODE = 1001;
    private static final String TAG = "DexPad";
    
    private MaterialButton btnStartService;
    private MaterialButton btnStopService;
    private MaterialButton btnRequestPermission;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "MainActivity onCreate called");

        try {
            btnStartService = findViewById(R.id.btn_start_service);
            btnStopService = findViewById(R.id.btn_stop_service);
            btnRequestPermission = findViewById(R.id.btn_request_permission);
            statusText = findViewById(R.id.status_text);

            if (btnStartService == null || btnStopService == null || btnRequestPermission == null || statusText == null) {
                Log.e(TAG, "Some UI elements are null!");
                Toast.makeText(this, "UI Error: Elements not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set up Shizuku callbacks
            setupShizukuListener();
            
            // Check permission status on startup
            checkPermissionStatus();

            // Button listeners
            btnStartService.setOnClickListener(v -> {
                Log.d(TAG, "Start service button clicked");
                startMouseService();
            });
            
            btnStopService.setOnClickListener(v -> {
                Log.d(TAG, "Stop service button clicked");
                stopMouseService();
            });
            
            btnRequestPermission.setOnClickListener(v -> {
                Log.d(TAG, "Request permission button clicked");
                requestShizukuPermission();
            });

            Log.d(TAG, "MainActivity setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Init error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupShizukuListener() {
        try {
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                Log.d(TAG, "Permission result: code=" + requestCode + " result=" + grantResult);
                if (requestCode == SHIZUKU_CODE) {
                    checkPermissionStatus();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Shizuku listener", e);
        }
    }

    private void checkPermissionStatus() {
        try {
            Log.d(TAG, "Checking Shizuku permission");
            
            // First check if Shizuku binder is available
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Shizuku binder not available");
                updateStatus("✗ Shizuku Not Running");
                btnStartService.setEnabled(false);
                btnRequestPermission.setEnabled(true);
                Toast.makeText(this, "Please start Shizuku app first", Toast.LENGTH_SHORT).show();
                return;
            }

            int permission = Shizuku.checkSelfPermission();
            Log.d(TAG, "Permission check result: " + permission);
            
            if (permission == PackageManager.PERMISSION_GRANTED) {
                updateStatus("✓ Shizuku Permission Granted");
                btnStartService.setEnabled(true);
                btnRequestPermission.setEnabled(false);
                Log.d(TAG, "Permission granted");
            } else {
                updateStatus("✗ Shizuku Permission Denied");
                btnStartService.setEnabled(false);
                btnRequestPermission.setEnabled(true);
                Log.d(TAG, "Permission denied");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permission", e);
            updateStatus("✗ Shizuku Error: " + e.getMessage());
            btnStartService.setEnabled(false);
        }
    }

    private void requestShizukuPermission() {
        try {
            Log.d(TAG, "Requesting Shizuku permission");
            Shizuku.requestPermission(SHIZUKU_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error requesting permission", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startMouseService() {
        try {
            Log.d(TAG, "Starting mouse service");
            
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permission not granted, requesting");
                Toast.makeText(this, "Please grant Shizuku permission first", Toast.LENGTH_SHORT).show();
                requestShizukuPermission();
                return;
            }

            Intent serviceIntent = new Intent(this, MouseService.class);
            startService(serviceIntent);
            updateStatus("✓ Mouse Service Started");
            Toast.makeText(this, "Mouse service started", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Mouse service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
            updateStatus("✗ Service Error: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMouseService() {
        try {
            Log.d(TAG, "Stopping mouse service");
            stopService(new Intent(this, MouseService.class));
            updateStatus("✓ Shizuku Permission Granted");
            Toast.makeText(this, "Mouse service stopped", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Mouse service stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatus(String message) {
        statusText.setText("Status: " + message);
        Log.d(TAG, "Status updated: " + message);
    }
}

