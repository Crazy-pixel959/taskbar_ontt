/* Copyright 2024 - Blade overlay service for Xiaomi-style sidebar toggle */

package com.farmerbb.taskbar.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class BladeService extends Service {

    private WindowManager windowManager;
    private View bladeView;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showBlade();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(bladeView != null) {
            try { windowManager.removeView(bladeView); } catch(Exception ignored) {}
            bladeView = null;
        }
    }

    private void showBlade() {
        float density = getResources().getDisplayMetrics().density;
        int bladeWidth = (int)(20 * density);
        int bladeHeight = (int)(72 * density);

        // Pill drawable
        GradientDrawable pill = new GradientDrawable();
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setCornerRadius(16 * density);
        pill.setColor(0xCCFFFFFF);

        // Inner pill view
        View pillView = new View(this);
        pillView.setBackground(pill);
        FrameLayout.LayoutParams pillParams = new FrameLayout.LayoutParams(
                (int)(6 * density), (int)(48 * density));
        pillParams.gravity = Gravity.CENTER;

        // Container
        FrameLayout container = new FrameLayout(this);
        container.addView(pillView, pillParams);

        // Nearly-transparent background so touch area works
        container.setBackgroundColor(0x01000000);

        // Determine which edge
        String position = TaskbarPosition.getTaskbarPosition(this);
        boolean isRight = position.contains("right");
        int gravity = (isRight ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                bladeWidth,
                bladeHeight,
                U.getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = gravity;

        final float[] downX = {0};        container.setOnTouchListener((v, event) -> {
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX[0] = event.getRawX();
                    return true;
                case MotionEvent.ACTION_UP:
                    // Use local broadcasts that TaskbarController listens to directly
                    android.content.SharedPreferences pref = U.getSharedPreferences(BladeService.this);
                    if(pref.getBoolean(PREF_COLLAPSED, false)) {
                        U.sendBroadcast(BladeService.this, ACTION_HIDE_TASKBAR);
                    } else {
                        U.sendBroadcast(BladeService.this, ACTION_SHOW_TASKBAR);
                    }
                    return true;
            }
            return true;
        });

        bladeView = container;
        windowManager.addView(bladeView, params);
    }
}
