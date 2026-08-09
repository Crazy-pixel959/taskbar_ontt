package com.farmerbb.taskbar.service;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class PowerMenuService extends AccessibilityService {

    private final BroadcastReceiver powerMenuReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!performGlobalAction(intent.getIntExtra(EXTRA_ACTION, -1)))
                U.showToast(PowerMenuService.this, R.string.tb_lock_device_not_supported);
        }
    };

    private static final long LONG_PRESS_DURATION = 600;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean volumeDownPressed = false;
    private final Runnable volumeLongPressRunnable = () -> {
        if(volumeDownPressed)
            U.sendBroadcast(PowerMenuService.this, ACTION_SHOW_HIDE_TASKBAR);
    };

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                volumeDownPressed = true;
                handler.postDelayed(volumeLongPressRunnable, LONG_PRESS_DURATION);
            } else if(event.getAction() == KeyEvent.ACTION_UP) {
                handler.removeCallbacks(volumeLongPressRunnable);
                volumeDownPressed = false;
                return false;
            }
        }
        return false;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}

    @Override
    public void onCreate() {
        super.onCreate();
        U.registerReceiver(this, powerMenuReceiver, ACTION_ACCESSIBILITY_ACTION);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(volumeLongPressRunnable);
        U.unregisterReceiver(this, powerMenuReceiver);
    }
}
