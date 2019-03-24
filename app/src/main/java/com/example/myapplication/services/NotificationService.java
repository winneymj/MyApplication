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
        cs = extras.getCharSequence("android.bigText");
        String body = (null != cs) ? cs.toString() : "";

        Log.i("Package",pack);
        Log.i("Ticker",ticker);
        Log.i("Title",title);
        Log.i("Text",text);
        Log.i("Body",body);

        Intent msgrcv = new Intent("Msg");
        msgrcv.putExtra("package", pack);
        msgrcv.putExtra("ticker", ticker);
        msgrcv.putExtra("title", title);
        msgrcv.putExtra("text", text);
        msgrcv.putExtra("body", body);

//        LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);

        // Send notification to the BLE service to send the message to remote _device.
        Intent intent = new Intent(this, MyService.class);
        intent.putExtra("package", pack);
        intent.putExtra("ticker", ticker);
        intent.putExtra("title", title);
        intent.putExtra("text", text);
        intent.putExtra("body", body);

        ComponentName service = startService(intent);
        Log.i("MainActivity.onCreateView:service=", (service != null) ? service.toString() : "null");
    }

    @Override

    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i("Msg","Notification Removed");
    }
}
