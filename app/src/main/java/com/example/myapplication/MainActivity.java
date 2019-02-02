package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.myapplication.services.MyService;
import com.example.myapplication.services.NotificationService;
import com.example.myapplication.utils.BluetoothHelper;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

public class MainActivity extends AppCompatActivity {


    private static final int ENABLE_BLUETOOTH_REQUEST = 1;  // The request code
    private static final int ENABLE_LOCATION_REQUEST = 2;  // The request code
    private static final int NOTIFICATION_PERMISSION_CODE = 123;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the _device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, ENABLE_BLUETOOTH_REQUEST);
        }

        List<String> permissions = isLocationPermissionsEnabled();
        if (permissions.size() > 0)
        {
            requestPermissions(permissions.toArray(new String[0]), ENABLE_LOCATION_REQUEST);
        }

        permissions = isBLEPermissionsEnabled();
        if (permissions.size() > 0)
        {
            requestPermissions(permissions.toArray(new String[0]), ENABLE_BLUETOOTH_REQUEST);
        }

//        enableNotificationListenerService(getApplicationContext());

//        permissions = isNotificationPermissionsEnabled();
//        if (permissions.size() > 0) {
            startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));

//            requestPermissions(permissions.toArray(new String[0]), NOTIFICATION_PERMISSION_CODE);
//        }

        BluetoothHelper ble = BluetoothHelper.getInstance();
        ble.setArguments(getApplication().getApplicationContext());
        ble.init();
    }

    /**
     * Returns true if location permissions needed else false if permission
     * has already been given.
     * @return True if enabled, false otherwise.
     */
    private List<String> isLocationPermissionsEnabled() {
        // Request permission from user to access location data so we can use BLE
        String[] allPermissionNeeded = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        List<String> permissionNeeded = new ArrayList<>();

        for (String permission : allPermissionNeeded) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED)
                permissionNeeded.add(permission);
        }

        return permissionNeeded;
    }

    /**
     * Returns true if location permissions needed else false if permission
     * has already been given.
     * @return True if enabled, false otherwise.
     */
    private List<String> isBLEPermissionsEnabled() {
        // Request permission from user to access location data so we can use BLE
        String[] allPermissionNeeded = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN};
        List<String> permissionNeeded = new ArrayList<>();

        for (String permission : allPermissionNeeded) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED)
                permissionNeeded.add(permission);
        }

        return permissionNeeded;
    }

    /**
     * Returns true if notification permissions needed else false if permission
     * has already been given.
     * @return True if enabled, false otherwise.
     */
    private List<String> isNotificationPermissionsEnabled() {
        // Request permission from user to access location data so we can use BLE
        String[] allPermissionNeeded = {
                Manifest.permission.ACCESS_NOTIFICATION_POLICY};
        List<String> permissionNeeded = new ArrayList<>();

        for (String permission : allPermissionNeeded) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED)
                permissionNeeded.add(permission);
        }

        return permissionNeeded;
    }

    /**
     * enable notification service.
     *
     * @param context context
     */
    public static void enableNotificationListenerService(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationListenerService.requestRebind(
                    ComponentName.createRelative(context.getPackageName(),
                            NotificationService.class.getCanonicalName()));
        } else {
            PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(context.getPackageName(),
                            "NotificationService"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
