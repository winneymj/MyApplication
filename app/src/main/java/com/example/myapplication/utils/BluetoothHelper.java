package com.example.myapplication.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

public class BluetoothHelper {

    enum eState {
        GATT_CONNECTING,
        GATT_CONNECTED,
        GATT_DISCONNECTED
    };

    public static final  String GATT_CONNECTED_INTENT = "com.example.myapplication.GATT_CONNECTED";
    public static final  String GATT_DISCONNECTED_INTENT = "com.example.myapplication.GATT_DISCONNECTED";

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;  // The request code
    private static final int ENABLE_LOCATION_REQUEST = 2;  // The request code
    private static final int ENABLE_NOTIFICATION_REQUEST = 3;  // The request code
    private static final int NOTIFICATION_PERMISSION_CODE = 123;

    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    private final static String MACAddress = "E9:0C:48:22:6A:AC";

    private static final BluetoothHelper instance = new BluetoothHelper();

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothLeScanner mBleScanner;
    private Context mAppContext;
    private Activity mActivity;
    private ScanSettings settings;
    private List<ScanFilter> filtersList;
    private BluetoothGatt mGatt;
    private eState mState;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private final static int REQUEST_ENABLE_BT = 1;

    //private constructor to avoid client applications to use constructor
    private BluetoothHelper(){}

    public static BluetoothHelper getInstance()
    {
        return instance;

    }

    public void setArguments(final Activity activity)
    {
        mActivity = activity;
        mAppContext = activity.getApplicationContext();
    }

    public boolean startScan()
    {
        Log.i("BluetoothHelper", ".startScan():ENTER");

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!hasPermissions())
        {
//            return false;
        }

        // We only bond to single device so if the list returned is empty then
        // we need to scan and pair and bond again.
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        Log.i("BluetoothHelper", ".bondedDevices.size=" + bondedDevices.size());
        if (bondedDevices.isEmpty()) {


//
//        // Ensures Bluetooth is available on the _device and it is enabled. If not,
//        // displays a dialog requesting user permission to enable Bluetooth.
//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
//        {
////            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
////            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            Log.i("BluetoothHelper", ".init() : no ble _device or not enabled");
//            return false;
//        }
//
            if (Build.VERSION.SDK_INT >= 21) {
                mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();

                // Scan only for fixed MACAddress
                filtersList = new ArrayList<>();
                ScanFilter.Builder builder = new ScanFilter.Builder();
                builder.setDeviceAddress(MACAddress);
                filtersList.add(builder.build());

                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
            }

            // Actually set it in response to ACTION_PAIRING_REQUEST.
            final IntentFilter pairingRequestFilter = new IntentFilter();
            pairingRequestFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
            pairingRequestFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            pairingRequestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
            mAppContext.registerReceiver(mPairingRequestReceiver, pairingRequestFilter);

            mHandler = new Handler();
            scanLeDevice(true);
        }
        Log.i("BluetoothHelper", ".startScan():EXIT");

        return true;
    }

    private boolean hasPermissions()
    {
        if (null == mBluetoothAdapter || !mBluetoothAdapter.isEnabled())
        {
            requestBluetoothEnable();
            return false;
        } else if (!hasLocationPermissions())
        {
            requestLocationPermission();
            return false;
        } else if (!hasNotificationPermissions())
        {
            requestNotificationPermission();
            return false;
        }

        return true;
    }

    private void requestBluetoothEnable()
    {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        mActivity.startActivityForResult(enableIntent, ENABLE_BLUETOOTH_REQUEST);
        Log.d("requestBluetoothEnable", "Request user enables bluetooth.  Try starting scan again");
    }

    private boolean hasLocationPermissions()
    {
        // Request permission from user to access location data so we can use BLE
        return ContextCompat.checkSelfPermission(mAppContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission()
    {
        mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ENABLE_LOCATION_REQUEST);
    }

    private boolean hasNotificationPermissions()
    {
        // Request permission for notifications
//        return ContextCompat.checkSelfPermission(mAppContext, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
//                == PackageManager.PERMISSION_GRANTED;
        return false;
    }

    private void requestNotificationPermission()
    {
//        mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, ENABLE_NOTIFICATION_REQUEST);
        mActivity.startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    public boolean verifyBleSupported()
    {
        // Use this check to determine whether BLE is supported on the _device. Then
        // you can selectively disable BLE-related features.
        if (!this.mAppContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            Toast.makeText(this.appContext, "BLE is not supported", Toast.LENGTH_SHORT).show();
//            Log.i("BluetoothHelper.init() : return false", "");
            return false;
        }

        return true;
    }

    //    // Returns true if permissions needed else false if permission
//    // has already been given.
//    public boolean checkAndRequestPermissions(final AppCompatActivity activity) {
//        // Request permission from user to access location data so we can use BLE
//        String[] allPermissionNeeded = {
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION};
//        List<String> permissionNeeded = new ArrayList<>();
//
//        for (String permission : allPermissionNeeded) {
//            if (ContextCompat.checkSelfPermission(mAppContext, permission) != PackageManager.PERMISSION_GRANTED)
//                permissionNeeded.add(permission);
//        }
//
//        // We need to ask for permission
//        if (permissionNeeded.size() > 0) {
//            activity.requestPermissions(permissionNeeded.toArray(new String[0]), 1);
//            return true;
//        }
//
//        return false;
//    }
//
    private void scanLeDevice(final boolean enable) {
        Log.i("BluetoothHelper", ".scanLeDevice() : ENTER,enable="+String.valueOf(enable));
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        Log.i("BluetoothHelper", ".scanLeDevice():mBleScanner.stopScan");
                        mBleScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                Log.i("BluetoothHelper", ".scanLeDevice():mBleScanner.startScan");
                mBleScanner.startScan(filtersList, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                Log.i("BluetoothHelper", ".scanLeDevice():mBleScanner.stopScan");
                mBleScanner.stopScan(mScanCallback);
            }
        }
        Log.i("BluetoothHelper", ".scanLeDevice() : EXIT");
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // Callback type 1 = CALLBACK_TYPE_ALL_MATCHES
            Log.i("BluetoothHelper", ".ScanCallback().onScanResult():callbackType"+String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            String devName = btDevice.getName();
            String devAddress = btDevice.getAddress();
            Log.i("BluetoothHelper", ".ScanCallback().onScanResult():devAddress="+devAddress);
            Log.i("BluetoothHelper", ".ScanCallback().onScanResult():devName=" + devName);
            // See if we are already bonded and do not create bond again
            if (BOND_BONDED != btDevice.getBondState()) {
                Log.i("BluetoothHelper", ".ScanCallback().onScanResult():NOT BOND_BONDED");
                if (btDevice.createBond()) {
                    Log.i("BluetoothHelper", ".ScanCallback().onScanResult():createBond Failed");
                }
            }
            else {
                Log.i("BluetoothHelper", ".ScanCallback().onScanResult():ALREADY BONDED");
                connectToDevice();
                }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("BluetoothHelper", ".ScanCallback().onBatchScanResults - Results"+sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BluetoothHelper", ".ScanCallback().onScanFailed: Scan Failed:Error Code:" + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
//                    AsyncTask.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            Log.i("onLeScan", _device.toString());
////                            connectToDevice(_device);
//                        }
//                    });
//
////                    runOnUiThread(new Runnable() {
////                        @Override
////                        public void run() {
////                            Log.i("onLeScan", _device.toString());
////                            connectToDevice(_device);
////                        }
////                    });
                }
            };

    public boolean connectToDevice() {
        Log.i("BluetoothHelper", ".connectToDevice:ENTER");

        if (null != mBluetoothAdapter && mBluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
            Log.i("BluetoothHelper", ".bondedDevices.size=" + bondedDevices.size());
            if (!bondedDevices.isEmpty()) {
                Object device[] = bondedDevices.toArray();
                    // Start Async connection to GATT.  See gattCallback for callback
                    mGatt = ((BluetoothDevice)device[0]).connectGatt(mAppContext, false, gattCallback);
                    mState = eState.GATT_CONNECTING;
                    return true;
            }
        }
        return false;
    }

    /**
     * This abstract class is used to implement BluetoothGatt callbacks.
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote GATT server.
         * @param gatt
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("BluetoothHelper", "onConnectionStateChange-Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED: {
                    Log.i("BluetoothHelper", ".gattCallback:STATE_CONNECTED");
                    mState = eState.GATT_CONNECTED;
                    // Emit a GATT connected
                    Intent intent = new Intent();
                    intent.setAction(GATT_CONNECTED_INTENT);
                    LocalBroadcastManager.getInstance(mAppContext).sendBroadcast(intent);
                    break;
                }
                case BluetoothProfile.STATE_DISCONNECTED: {
                    Log.e("BluetoothHelper", ".gattCallback:STATE_DISCONNECTED");
                    mState = eState.GATT_DISCONNECTED;
                    // Emit a GATT disconnected
                    Intent intent = new Intent();
                    intent.setAction(GATT_DISCONNECTED_INTENT);
                    LocalBroadcastManager.getInstance(mAppContext).sendBroadcast(intent);
                    break;
                }
                default:
                    Log.e("BluetoothHelper", "gattCallback:STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i("BluetoothHelper", ".onServicesDiscovered:ENTER");
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService gattService : gatt.getServices()) {
                    Log.i("onServicesDiscovered", "onServicesDiscovered: ---------------------");
                    Log.i("onServicesDiscovered", "onServicesDiscovered: service=" + gattService.getUuid());
                    for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                        Log.i("onServicesDiscovered", "onServicesDiscovered: characteristic=" + characteristic.getUuid());

                        if (characteristic.getUuid().toString().equals("0000a001-0000-1000-8000-00805f9b34fb")) {

                            Log.w("onServicesDiscovered", "onServicesDiscovered: found LED");

                            String originalString = "00";

                            byte[] b = hexStringToByteArray(originalString);

                            characteristic.setValue(b); // call this BEFORE(!) you 'write' any stuff to the server
                            gatt.writeCharacteristic(characteristic);

                            Log.i("onServicesDiscovered", "onServicesDiscovered: , write bytes?! " + bytesToHexStr(b));
                        }
                    }
                }

//                broadcastUpdate(BluetoothHelper.ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w("onServicesDiscovered", "onServicesDiscovered received: " + status);
            }

//            List<BluetoothGattService> services = gatt.getServices();
//            Log.i("onServicesDiscovered", services.toString());
//            gatt.readCharacteristic(services.get(1).getCharacteristics().get
//                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };

    private final BroadcastReceiver mPairingRequestReceiver = new BroadcastReceiver()
    {
        private static final String TAG = "mPairingRequestReceiver";
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.i(TAG,"onReceive:" + intent.getAction());
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction()))
            {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                Log.i(TAG,"onReceive:ACTION_PAIRING_REQUEST");
            }
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction()))
            {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                Log.i(TAG,"onReceive:ACTION_BOND_STATE_CHANGED");

                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                switch(state){
                    case BluetoothDevice.BOND_BONDING:
                        // Bonding...
                        Log.i(TAG,"onReceive:Bonding...");
                        break;

                    case BOND_BONDED:
                        // Bonded...
                        Log.i(TAG,"onReceive:Bonded...");
                        mAppContext.unregisterReceiver(mPairingRequestReceiver);

                        // Now try connecting to device
                        connectToDevice();
                        break;

                    case BluetoothDevice.BOND_NONE:
                        Log.i(TAG,"onReceive:Not bonded...");
                        // Not bonded...
                        break;
                }
            }
        }
    };

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHexStr(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}

