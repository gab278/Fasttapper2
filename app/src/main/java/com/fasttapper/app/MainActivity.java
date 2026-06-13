package com.fasttapper.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusText = findViewById(R.id.statusText);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        updateStatus(statusText);

        btnStart.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ);
            } else {
                startOverlayService();
                updateStatus(statusText);
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            updateStatus(statusText);
            Toast.makeText(this, "FastTapper stopped", Toast.LENGTH_SHORT).show();
        });
    }

    private void startOverlayService() {
        Intent serviceIntent = new Intent(this, OverlayService.class);
        startForegroundService(serviceIntent);
        Toast.makeText(this, "FastTapper overlay started!", Toast.LENGTH_SHORT).show();
    }

    private void updateStatus(TextView tv) {
        boolean hasPermission = Settings.canDrawOverlays(this);
        tv.setText(hasPermission
                ? "✅ Overlay permission granted.\nUse the floating bubble to tap anywhere!"
                : "⚠️ Overlay permission required.\nTap START to grant it.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ) {
            TextView statusText = findViewById(R.id.statusText);
            if (Settings.canDrawOverlays(this)) {
                startOverlayService();
            } else {
                Toast.makeText(this, "Permission denied — cannot show overlay", Toast.LENGTH_LONG).show();
            }
            updateStatus(statusText);
        }
    }
}
