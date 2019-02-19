package com.example.myapplication.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

public class PermissionsHelper {

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;  // The request code
    private static final int ENABLE_LOCATION_REQUEST = 2;  // The request code

    public static boolean hasPermissions(final Activity activity)
    {
        if (!BluetoothHelper.getInstance(activity.getApplicationContext()).isBluetoothEnabled())
        {
            requestBluetoothEnable(activity);
            return false;
        } else if (!hasLocationPermissions(activity))
        {
            requestLocationPermission(activity);
            return false;
        } else if (!hasNotificationPermissions())
        {
            requestNotificationPermission(activity);
            return false;
        }

        return true;
    }

    private static void requestBluetoothEnable(final Activity activity)
    {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableIntent, ENABLE_BLUETOOTH_REQUEST);
        Log.d("requestBluetoothEnable", "Request user enables bluetooth.  Try starting scan again");
    }

    private static boolean hasLocationPermissions(final Activity activity)
    {
        // Request permission from user to access location data so we can use BLE
        return ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static void requestLocationPermission(final Activity activity)
    {
        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ENABLE_LOCATION_REQUEST);
    }

    private static boolean hasNotificationPermissions()
    {
        // Request permission for notifications
//        return ContextCompat.checkSelfPermission(mAppContext, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
//                == PackageManager.PERMISSION_GRANTED;
        return false;
    }

    private static void requestNotificationPermission(final Activity activity)
    {
//        mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, ENABLE_NOTIFICATION_REQUEST);
        activity.startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

}
