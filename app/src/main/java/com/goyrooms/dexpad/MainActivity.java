package com.goyrooms.dexpad;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int SHIZUKU_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure Shizuku is bound
        Shizuku.pingBinder();

        // Overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Enable overlay permission and reopen app", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Shizuku running?
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Start Shizuku first!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            startService(new Intent(this, MouseService.class));
            finish();
        } else {
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    startService(new Intent(this, MouseService.class));
                } else {
                    Toast.makeText(this, "Shizuku permission denied", Toast.LENGTH_SHORT).show();
                }
                finish();
            });

            Shizuku.requestPermission(SHIZUKU_CODE);
        }
    }
}