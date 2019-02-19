package com.example.myapplication.services;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.myapplication.utils.BluetoothHelper;

public class NotificationService extends NotificationListenerService {

    Context context;
    private int mInterval = 10000; // 1 seconds by default, can be changed later
    private Handler mTimerHandler;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("NotificationService.onBind", ":Enter");
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        Log.i("NotificationService.onCreate", ":Enter");
        context = getApplicationContext();

        // Make sure we have initialized the bluetooth helper as this service
        // has prob started without the app running.
        BluetoothHelper ble = BluetoothHelper.getInstance(context);
        mTimerHandler = new Handler();
        startRepeatingTask();

        super.onCreate();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i("NotificationService.onNotificationPosted", ":Enter");


        String pack = sbn.getPackageName();
        CharSequence cs = sbn.getNotification().tickerText;
        String ticker = (null != cs) ? cs.toString() : "";
        Bundle extras = sbn.getNotification().extras;

        Log.i("NotificationService.onNotificationPosted:extras.toString()", extras.toString());

        cs = extras.getCharSequence(Notification.EXTRA_TEXT);
        String title = (null != cs) ? cs.toString() : "";
        cs = extras.getCharSequence(Notification.EXTRA_TEXT);
        String text = (null != cs) ? cs.toString() : "";

        Log.i("Package",pack);
        Log.i("Ticker",ticker);
        Log.i("Title",title);
        Log.i("Text",text);

        Intent msgrcv = new Intent("Msg");
        msgrcv.putExtra("package", pack);
        msgrcv.putExtra("ticker", ticker);
        msgrcv.putExtra("title", title);
        msgrcv.putExtra("text", text);

//        LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);

        // Send notification to the BLE service to send the message to remote _device.
        Intent intent = new Intent(this, MyService.class);
        ComponentName service = startService(intent);
        Log.i("MainActivity.onCreateView:service=", (service != null) ? service.toString() : "null");
    }

    @Override

    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i("Msg","Notification Removed");
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                BluetoothHelper.getInstance(getApplicationContext()).printConnectionStatus();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mTimerHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    private void startRepeatingTask() {
        mStatusChecker.run();
    }

    private void stopRepeatingTask() {
        mTimerHandler.removeCallbacks(mStatusChecker);
    }

}
