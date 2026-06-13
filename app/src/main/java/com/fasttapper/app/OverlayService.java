package com.fasttapper.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "fasttapper_channel";
    private static final int TAP_INTERVAL_MS = 50;

    private WindowManager windowManager;
    private View overlayView;
    private View targetMarker;

    private float targetX = -1, targetY = -1;
    private boolean isTapping = false;

    private final Handler tapHandler = new Handler(Looper.getMainLooper());
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private int state = 0;
    private Runnable tapRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showOverlayBubble();
    }

    private void showOverlayBubble() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 200;

        windowManager.addView(overlayView, params);

        ImageButton btnAction = overlayView.findViewById(R.id.btnAction);
        ImageButton btnClose = overlayView.findViewById(R.id.btnClose);
        TextView labelText = overlayView.findViewById(R.id.labelText);

        final int[] lastX = {0};
        final int[] lastY = {0};

        overlayView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX[0] = (int) event.getRawX();
                    lastY[0] = (int) event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) event.getRawX() - lastX[0];
                    int dy = (int) event.getRawY() - lastY[0];
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        params.x += dx;
                        params.y += dy;
                        windowManager.updateViewLayout(overlayView, params);
                        lastX[0] = (int) event.getRawX();
                        lastY[0] = (int) event.getRawY();
                    }
                    return true;
            }
            return false;
        });

        btnClose.setOnClickListener(v -> stopSelf());

        btnAction.setOnClickListener(v -> {
            switch (state) {
                case 0:
                    state = 1;
                    labelText.setText("Tap target");
                    showTargetPicker(btnAction, labelText, params);
                    break;
                case 1:
                    state = 0;
                    labelText.setText("Set Target");
                    if (targetMarker != null) {
                        try { windowManager.removeView(targetMarker); } catch (Exception ignored) {}
                        targetMarker = null;
                    }
                    break;
                case 2:
                    stopTapping(btnAction, labelText);
                    break;
            }
        });
    }

    private void showTargetPicker(ImageButton btn, TextView label, WindowManager.LayoutParams bubbleParams) {
        View picker = new View(this);
        picker.setBackgroundColor(0x33FF6600);

        WindowManager.LayoutParams pickerParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(picker, pickerParams);

        picker.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                targetX = event.getRawX();
                targetY = event.getRawY();
                windowManager.removeView(picker);
                showTargetMarker(targetX, targetY);
                state = 2;
                label.setText("STOP");
                btn.setImageResource(R.drawable.ic_stop);
                startTapping();
                return true;
            }
            return false;
        });

        Toast.makeText(this, "Tap the spot you want to auto-tap!", Toast.LENGTH_SHORT).show();
    }

    private void showTargetMarker(float x, float y) {
        if (targetMarker != null) {
            try { windowManager.removeView(targetMarker); } catch (Exception ignored) {}
        }

        targetMarker = LayoutInflater.from(this).inflate(R.layout.target_marker, null);

        WindowManager.LayoutParams markerParams = new WindowManager.LayoutParams(
                60, 60,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        markerParams.gravity = Gravity.TOP | Gravity.START;
        markerParams.x = (i
