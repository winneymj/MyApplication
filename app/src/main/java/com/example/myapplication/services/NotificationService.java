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

        if (null != sbn) {
            // Send notification to the BLE service to send the message to remote _device.
            Intent intent = new Intent(this, MyService.class);

            String pack = sbn.getPackageName();
            intent.putExtra("sender", pack);
            Log.i("package", pack);

            CharSequence cs = sbn.getNotification().tickerText;
            if (null != cs) {
                intent.putExtra("ticker", cs.toString());
                Log.i("Ticker", cs.toString());
            }

            Bundle extras = sbn.getNotification().extras;

            Log.i("NotificationService.onNotificationPosted:extras.toString()", extras.toString());

            cs = extras.getCharSequence(Notification.EXTRA_TITLE);
            if (null != cs) {
                intent.putExtra("from", cs.toString());
                Log.i("Title", cs.toString());
            }

            cs = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (null != cs) {
                intent.putExtra("subject", cs.toString());
                Log.i("Text", cs.toString());
            }

            cs = extras.getCharSequence("android.bigText");
            if (null != cs) {
                intent.putExtra("body", cs.toString());
                Log.i("Body", cs.toString());
            }


            //        Intent msgrcv = new Intent("Msg");
            //        msgrcv.putExtra("package", pack);
            //        msgrcv.putExtra("ticker", ticker);
            //        msgrcv.putExtra("title", title);
            //        msgrcv.putExtra("text", text);
            //        msgrcv.putExtra("body", body);

            //        LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);

            ComponentName service = startService(intent);
            Log.i("MainActivity.onCreateView:service=", (service != null) ? service.toString() : "null");
        }
    }

    @Override

    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i("Msg","Notification Removed");
    }
}
