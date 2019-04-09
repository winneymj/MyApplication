package com.example.myapplication.utils;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static java.util.Locale.US;

public class BluetoothHelper {

    enum eState {
        GATT_CONNECTING(1),
        GATT_CONNECTED(2),
        GATT_DISCONNECTED(4),
        BLE_SCANNING(8),
        BLE_NOT_SCANNING(16),
        BOND_BONDED(128),
        BOND_BONDING(256),
        BOND_UNBONDED(512);

        private int value;

        private eState(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    };

//    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
//    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";

    public final static int BTS_MTU_SIZE_DEFAULT = 20;
    public final static int BLOCK_HEADER_SIZE = 3;
    public final static int BT_TYPE_WRITE_SETUP = 0x00;
    public final static int BT_TYPE_WRITE_DIRECT = 0x04;

    public static final UUID UUID_WRITE_CHARACTERISTIC = UUID.fromString("0000a001-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE = UUID.fromString("0000a000-0000-1000-8000-00805f9b34fb");

    public static final  String GATT_CONNECTED_INTENT = "com.example.myapplication.GATT_CONNECTED";
    public static final  String GATT_DISCONNECTED_INTENT = "com.example.myapplication.GATT_DISCONNECTED";

//    public final static String ACTION_GATT_SERVICES_DISCOVERED =
//            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    private final static String MACAddress = "E9:0C:48:22:6A:AC";
    private static final long SCAN_PERIOD = 20000;
    private static final long CHARACTERISTIC_WRITE_WAIT = 2000;

    private static final BluetoothHelper instance = new BluetoothHelper();

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    private int mInterval = 8000; // 1 seconds by default, can be changed later
    private Handler mTimerHandler;

    private BluetoothLeScanner mBleScanner;
    private Context mAppContext;
    private BluetoothGatt mGatt = null;
    private int mState = eState.BOND_UNBONDED.value() | eState.GATT_DISCONNECTED.value();
    /**
     * Lock used in synchronization purposes
     */
    final Lock mRlock = new ReentrantLock();
    final Condition writeCondition  = mRlock.newCondition();

    private int mOutgoingTotalFragments = 0;
    private int mMaxBlockPayloadSize = BTS_MTU_SIZE_DEFAULT - BLOCK_HEADER_SIZE;


    // Stops scanning after 10 seconds.
//    private final static int REQUEST_ENABLE_BT = 1;

    //private constructor to avoid client applications to use constructor
    private BluetoothHelper(){}

    public static BluetoothHelper getInstance(final Context context)
    {
        Log.i("BluetoothHelper", ".getInstance():ENTER");
        instance.mAppContext = context;

        // Initializes Bluetooth adapter.
        if (null == instance.mBluetoothAdapter) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) instance.mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
            instance.mBluetoothAdapter = bluetoothManager.getAdapter();

            // Setup a broadcast receiver.
            final IntentFilter requestFilter = new IntentFilter();
            requestFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
            requestFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            requestFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            requestFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            requestFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            requestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
            instance.mAppContext.registerReceiver(instance.mPairingRequestReceiver, requestFilter);

            instance.mHandler = new Handler();

            // Start a timer to check BLE connection periodically.
            instance.mTimerHandler = new Handler();
            instance.startRepeatingTask();
        }

        // Set bonded state so we know if we need to rebind
        instance.setBondedState(instance.deviceBonded() ? eState.BOND_BONDED : eState.BOND_UNBONDED);
//
//        // If we are bonded then perhaps we can just GATT connect
//        if (instance.deviceBonded() && !instance.gattConnected() && !instance.gattConnecting()) {
//            Log.i("BluetoothHelper", ".getInstance.already Bonded");
//            BluetoothDevice device = instance.getBondedDevice();
//            if (null != device) {
//                // Start Async connection to GATT.  See gattCallback for callback
//                Log.i("BluetoothHelper", ".getInstance-device.connectGatt()");
//                instance.mGatt = device.connectGatt(instance.mAppContext, false, instance.gattCallback);
//                instance.setGATTState(eState.GATT_CONNECTING);
//                return instance;
//            }
//        }
//
//        // Make sure we are connected and bonded.  Don't do it if we are in the process of bonding.
//        Log.i("BluetoothHelper", ".gattConnecting=" + instance.gattConnecting());
//        if (!instance.gattConnecting()) {
//            // Only try to scan again if we are not in the middle of scan or connect
//            if (!instance.deviceBonded() || !instance.gattConnected()) {
//                Log.i("BluetoothHelper", ".deviceBonded=" + instance.deviceBonded() +
//                        ", gattConnected=" + instance.gattConnected());
//                // Stop the scan and try again
//                if (Build.VERSION.SDK_INT >= 21) {
//                    instance.mBleScanner = instance.mBluetoothAdapter.getBluetoothLeScanner();
//                    instance.mBleScanner.stopScan(instance.mScanCallback);
//                    instance.scanLeDevice(true);
//                }
//            }
//        }

        Log.i("BluetoothHelper", ".getInstance():EXIT");
        return instance;
    }

    public void connectToBLEDevice() {
        Log.i("BluetoothHelper", ".connectToBLEDevice():ENTER");
        // Make sure we are not connected before we do this or if we are connected but not bonded
        setBondedState(deviceBonded() ? eState.BOND_BONDED : eState.BOND_UNBONDED);

        if ((!gattConnected() && !gattConnecting() && !scanning()) || (gattConnected() && !deviceBonded())) {
            // Stop the scan and try again
            if (Build.VERSION.SDK_INT >= 21) {
                mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
                mBleScanner.stopScan(mScanCallback);
//                Log.i("BluetoothHelper", ".connectToBLEDevice():scanLeDevice");
                scanLeDevice(true);
            }
        }
        else {
            Log.i("BluetoothHelper", ".connectToBLEDevice():ALREADY SCANNING && GATT CONNECTED");
        }
        Log.i("BluetoothHelper", ".connectToBLEDevice():EXIT");
    }

    public BluetoothGatt gatt() {
        return mGatt;
    }

    private boolean deviceBonded() {
        Set<BluetoothDevice> bondedDevices = instance.mBluetoothAdapter.getBondedDevices();
//        Log.i("BluetoothHelper", ".deviceBonded.bondedDevices.size=" + bondedDevices.size());
        for (BluetoothDevice device : bondedDevices) {
            final String deviceAddr = device.getAddress();
//            Log.i("BluetoothHelper", ".deviceBonded.deviceAddr=" + deviceAddr);
            if (deviceAddr.equalsIgnoreCase(MACAddress)) {
                return true;
            }
        }
        return false;
    }

    private BluetoothDevice getBondedDevice() {
        Set<BluetoothDevice> bondedDevices = instance.mBluetoothAdapter.getBondedDevices();
//        Log.i("BluetoothHelper", ".deviceBonded.bondedDevices.size=" + bondedDevices.size());
        for (BluetoothDevice device : bondedDevices) {
            final String deviceAddr = device.getAddress();
//            Log.i("BluetoothHelper", ".deviceBonded.deviceAddr=" + deviceAddr);
            if (deviceAddr.equalsIgnoreCase(MACAddress)) {
                return device;
            }
        }
        return null;
    }

    private boolean deviceBonding() {
        return (instance.mState & eState.BOND_BONDING.value()) > 0;
    }

    private boolean gattConnected() {
        return (instance.mState & eState.GATT_CONNECTED.value()) > 0;
    }

    private boolean gattConnecting() {
        return (instance.mState & eState.GATT_CONNECTING.value()) > 0;
    }

    private boolean scanning() {
        return (instance.mState & eState.BLE_SCANNING.value()) > 0;
    }

    // Make sure one of the bonded devices is the one we are looking for
    public boolean writeDataToBtCharacteristic(final byte[] data) {
        Log.i("BluetoothHelper", ".writeDataToBtCharacteristic ENTER");

        if (null == mBluetoothAdapter) {
            Log.i("BluetoothHelper", ".writeDataToBtCharacteristic. null == mBluetoothAdapter");
            return false;
        }

        try {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MACAddress);
            Log.i("BluetoothHelper", ".writeDataToBtCharacteristic.getRemoteDevice()");

            if (null == mGatt) {
                Log.e("BluetoothHelper", ".writeDataToBtCharacteristic null == mGatt");
                return false;
            }
            BluetoothGattService mSVC = mGatt.getService(UUID_SERVICE);
            Log.i("BluetoothHelper", ".writeDataToBtCharacteristic.getService()");
            if (null == mSVC) {
                Log.e("BluetoothHelper", ".writeDataToBtCharacteristic mGatt.getService == null");
                return false;
            }
            Log.i("BluetoothHelper", ".writeDataToBtCharacteristic.getCharacteristic(()");
            BluetoothGattCharacteristic characteristic = mSVC.getCharacteristic(UUID_WRITE_CHARACTERISTIC);
            if (null == characteristic) {
                Log.e("BluetoothHelper", ".writeDataToBtCharacteristic mSVC.getCharacteristic == null");
                return false;
            }

            for (byte chr: data) {
//                Log.d("BluetoothHelper", String.valueOf(chr));
                Log.d("BluetoothHelper", String.format("%02X ", chr));
            }

            characteristic.setValue(data);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            // We have to wait for confirmation
            try {
                mRlock.lock();
                if (mGatt.writeCharacteristic(characteristic)) {
                    Log.i("BluetoothHelper", ".writeDataToBtCharacteristic success");

                    Log.d("BluetoothHelper", ".writeDataToBtCharacteristic:wait() for max 2 sec");
                    if (!writeCondition.await(CHARACTERISTIC_WRITE_WAIT, TimeUnit.MILLISECONDS)) {
                        // We timed out so clean up
                        Log.d("BluetoothHelper", ".writeDataToBtCharacteristic:TIMED OUT");
                        return false;
                    }
                    Log.d("BluetoothHelper", ".writeDataToBtCharacteristic:Woken");
                } else {
                    Log.e("BluetoothHelper", ".writeDataToBtCharacteristic failed");
                    return false;
                }
            } catch (InterruptedException e) {
                Log.d("BluetoothHelper", ".writeDataToBtCharacteristic:lock.InterruptedException!");
            } finally {
                mRlock.unlock();
                Log.d("BluetoothHelper", ".writeDataToBtCharacteristic:lock.unlock()");
            }
        }catch (IllegalArgumentException ia)
        {
            Log.e("BluetoothHelper", ".writeDataToBtCharacteristic:", ia);
            return false;
        }

        Log.i("BluetoothHelper", ".writeDataToBtCharacteristic return true");
        return true;
    }

    public boolean isBluetoothEnabled() {
        return (null != mBluetoothAdapter && mBluetoothAdapter.isEnabled());
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
                    Log.i("BluetoothHelper", ".scanLeDevice():mBleScanner.stopScan");
                    mBleScanner.stopScan(mScanCallback);
                    // Indicate we have stopped scanning
                    setScanningState(eState.BLE_NOT_SCANNING);
                }
            }, SCAN_PERIOD);
            Log.i("BluetoothHelper", ".scanLeDevice():mBleScanner.startScan");
            // Scan only for fixed MACAddress
            List<ScanFilter> filtersList = new ArrayList<>();
            ScanFilter.Builder builder = new ScanFilter.Builder();
            builder.setDeviceAddress(MACAddress);
            filtersList.add(builder.build());

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            mBleScanner.startScan(filtersList, settings, mScanCallback);
            setScanningState(eState.BLE_SCANNING);
        } else {
            Log.i("BluetoothHelper", ".scanLeDevice():mBleScanner.stopScan");
            mBleScanner.stopScan(mScanCallback);
            setScanningState(eState.BLE_NOT_SCANNING);
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
            if (!deviceBonded()) {
                setBondedState(eState.BOND_BONDING);
                Log.i("BluetoothHelper", ".ScanCallback().onScanResult():NOT BOND_BONDED");
                if (!btDevice.createBond()) {
                    setBondedState(eState.BOND_UNBONDED);
                    Log.i("BluetoothHelper", ".ScanCallback().onScanResult():createBond Failed");
                }
            }
            else {
                Log.i("BluetoothHelper", ".ScanCallback().onScanResult():ALREADY BONDED");
                setBondedState(eState.BOND_BONDED);
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
            setScanningState(eState.BLE_NOT_SCANNING);
        }
    };

//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//                @Override
//                public void onLeScan(final BluetoothDevice device, int rssi,
//                                     byte[] scanRecord) {
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
//                }
//            };

    private void setGATTState(eState state) {
        // Clear down bits
        mState &= ~(eState.GATT_CONNECTING.value());
        mState &= ~(eState.GATT_DISCONNECTED.value());
        mState &= ~(eState.GATT_CONNECTED.value());
        mState |= state.value();
    }

    private void setBondedState(eState state) {
        // Clear down bits
        mState &= ~(eState.BOND_BONDED.value());
        mState &= ~(eState.BOND_BONDING.value());
        mState &= ~(eState.BOND_UNBONDED.value());
        mState |= state.value();
    }

    private void setScanningState(eState state) {
        // Clear down bits
        mState &= ~(eState.BLE_SCANNING.value());
        mState &= ~(eState.BLE_NOT_SCANNING.value());
        mState |= state.value();
    }

    private void clearDeviceState(eState state) {
        // Clear down bits
        mState &= ~(state.value());
    }

    public boolean connectToDevice() {
        Log.i("BluetoothHelper", ".connectToDevice:ENTER");

        if (null != mBluetoothAdapter && mBluetoothAdapter.isEnabled()) {
            BluetoothDevice device = getBondedDevice();
            if (null != device) {
                // Start Async connection to GATT.  See gattCallback for callback
                Log.i("BluetoothHelper", "connectToDevice.connectGatt()");
                mGatt = device.connectGatt(mAppContext, false, gattCallback);
                setGATTState(eState.GATT_CONNECTING);
                return true;
            } else {
//                // TODO Perhaps we should try to connect and bind to device
//                Log.i("BluetoothHelper", "NO BONDED DEVICES");
//                setGATTState(eState.GATT_DISCONNECTED);
//                mGatt = null;
            }
        } else {
//            Log.i("BluetoothHelper", ".connectToDevice:mBluetoothAdapter=" + mBluetoothAdapter);
//            setGATTState(eState.GATT_DISCONNECTED);
//            mGatt = null;
        }
        Log.i("BluetoothHelper", ".connectToDevice:EXIT");
        return false;
    }

    public boolean sendBLEData(final String data) {
        Log.d("BluetoothHelper", ".sendBLEData: ENTER");

        // Now write data blocks
        // Split the data bytes into smaller parts if needed.
        mOutgoingTotalFragments = 0;
        ArrayList<byte[]> dataArray = DataFormat.ToUTF8ByteArray(data);

        // Total the number of bytes to send.
        for (byte[] dataStr: dataArray) {
            mOutgoingTotalFragments += dataStr.length;
        }

        //  Find the total number of fragments needed to transmit the block
        //  based on the MTU size minus payload header.
        mOutgoingTotalFragments = (mOutgoingTotalFragments + (mMaxBlockPayloadSize - 1)) / mMaxBlockPayloadSize;

        Log.d("BluetoothHelper", "data.length=" + data.length() + ",outgoingTotalFragments=" + mOutgoingTotalFragments + ",maxBlockPayloadSize=" +  mMaxBlockPayloadSize);

        /*  Send setup message.
            When the receiver is ready for data it will send a request for fragments.
        */
        byte length = 10;
        byte[] writeBuffer = new byte[length];

        writeBuffer[0] = BT_TYPE_WRITE_SETUP << 4;

        writeBuffer[1] = (byte) data.length();
        writeBuffer[2] = (byte) (data.length() >> 8);
        writeBuffer[3] = (byte) (data.length() >> 16);

//        writeBuffer[4] = block->getOffset();
//        writeBuffer[5] = block->getOffset() >> 8;
//        writeBuffer[6] = block->getOffset() >> 16;

        writeBuffer[7] = (byte)mOutgoingTotalFragments;
        writeBuffer[8] = (byte)(mOutgoingTotalFragments >> 8);
        writeBuffer[9] = (byte)(mOutgoingTotalFragments >> 16);

        // Now write header block.
        if (!writeDataToBtCharacteristic(writeBuffer)) {
            Log.w("BluetoothHelper", ".writeDataToBtCharacteristic: FAILED");
        }

        int offset = 0;

        for (byte[] dataStr: dataArray) {
            int strLen = BLOCK_HEADER_SIZE + dataStr.length;
            writeBuffer = new byte[strLen];

            writeBuffer[0] = BT_TYPE_WRITE_DIRECT << 4;
            writeBuffer[1] = (byte)offset;
            writeBuffer[2] = (byte)(offset >> 8);

            Log.d("BluetoothHelper", "dataStr=" + dataStr + ",dataStr.length=" + dataStr.length + ",offset=" + offset);

            System.arraycopy(dataStr, 0, writeBuffer, 3, dataStr.length);

            if (!writeDataToBtCharacteristic(writeBuffer)) {
                Log.w("BluetoothHelper", ".writeDataToBtCharacteristic: FAILED");
            }

            offset += dataStr.length;
        }

        Log.d("BluetoothHelper", ".sendBLEData: EXIT");
        return true;
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    };

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
                    Log.i("BluetoothHelper", ".gattCallback:GATT_STATE_CONNECTED");
                    setGATTState(eState.GATT_CONNECTED);
                    // Now try to discover services
                    mGatt.discoverServices();

                    // Emit a GATT connected
                    Intent intent = new Intent();
                    intent.setAction(GATT_CONNECTED_INTENT);
                    LocalBroadcastManager.getInstance(mAppContext).sendBroadcast(intent);
                    break;
                }
                case BluetoothProfile.STATE_DISCONNECTED: {
                    Log.i("BluetoothHelper", ".gattCallback:GATT_STATE_DISCONNECTED");
                    setGATTState(eState.GATT_DISCONNECTED);
                    mGatt = null;

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

//                        if (characteristic.getUuid().toString().equals("0000a001-0000-1000-8000-00805f9b34fb")) {
//
//                            Log.w("onServicesDiscovered", "onServicesDiscovered: found LED");
//
//                            String originalString = "00";
//
//                            byte[] b = hexStringToByteArray(originalString);
//
//                            characteristic.setValue(b); // call this BEFORE(!) you 'write' any stuff to the server
//                            gatt.writeCharacteristic(characteristic);
//
//                            Log.i("onServicesDiscovered", "onServicesDiscovered: , write bytes?! " + bytesToHexStr(b));
//                        }
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

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status != BluetoothGatt.GATT_SUCCESS){
                Log.d("onCharacteristicWrite", "Failed write, retrying");
                gatt.writeCharacteristic(characteristic);
                return;
            }
            Log.d("onCharacteristicWrite", "GATT_SUCCESS");

            // notify waiting thread
            mRlock.lock();
            try {
                writeCondition.signal();
            } catch (Exception e) {
                Log.d("onCharacteristicWrite", "writeCondition.signal(): Exception!");
            } finally {
                mRlock.unlock();
            }
        }
    };

    private final BroadcastReceiver mPairingRequestReceiver = new BroadcastReceiver()
    {
        private static final String TAG = "mPairingRequestReceiver";
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.i(TAG,"onReceive:" + intent.getAction());
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action))
            {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                Log.i(TAG,"onReceive:ACTION_PAIRING_REQUEST");
            }
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
            {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                Log.i(TAG,"onReceive:ACTION_BOND_STATE_CHANGED");

                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                switch(state){
                    case BluetoothDevice.BOND_BONDING:
                        // Bonding...
                        Log.i(TAG,"onReceive:Bonding...");
                        setBondedState(eState.BOND_BONDING);
                        break;

                    case BOND_BONDED:
                        // Bonded...
                        Log.i(TAG,"onReceive:Bonded...");
                        mAppContext.unregisterReceiver(mPairingRequestReceiver);
                        setBondedState(eState.BOND_BONDED);

                        // Now try connecting to device
                        connectToDevice();
                        break;

                    case BluetoothDevice.BOND_NONE:
                        Log.i(TAG,"onReceive:Not bonded...");
                        setBondedState(eState.BOND_UNBONDED);
                        // Not bonded...
                        break;
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Device found
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG,"onReceive:ACTION_FOUND");
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                // Device is now connected
                Log.i(TAG,"onReceive:ACTION_ACL_CONNECTED");
            }
//            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                // Done searching
//                Log.i(TAG,"onReceive:ACTION_DISCOVERY_FINISHED");
//            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                // Device is about to disconnect
                Log.i(TAG,"onReceive:ACTION_ACL_DISCONNECT_REQUESTED");
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // Device has disconnected
                Log.i(TAG,"onReceive:ACTION_ACL_DISCONNECT_REQUESTED");
            }
        }
    };

//    public static byte[] hexStringToByteArray(String s) {
//        byte[] b = new byte[s.length() / 2];
//        for (int i = 0; i < b.length; i++) {
//            int index = i * 2;
//            int v = Integer.parseInt(s.substring(index, index + 2), 16);
//            b[i] = (byte) v;
//        }
//        return b;
//    }

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

    public void printConnectionStatus()
    {
        final String TAG = "printConnectionStatus";
        String bondedState = "";
        bondedState += ((mState & eState.BOND_UNBONDED.value()) == eState.BOND_UNBONDED.value()) ? "UNBONDED" : "";
        bondedState += ((mState & eState.BOND_BONDED.value()) == eState.BOND_BONDED.value()) ? "BONDED" : "";
        bondedState += ((mState & eState.BOND_BONDING.value()) == eState.BOND_BONDING.value()) ? "BONDING" : "";
        Log.i(TAG,"mState:0b" + Integer.toBinaryString(mState));
        Log.i(TAG,"BOND:" + bondedState);

        String gattState = "";
        gattState += ((mState & eState.GATT_DISCONNECTED.value()) == eState.GATT_DISCONNECTED.value()) ? "DISCONNECTED" : "";
        gattState += ((mState & eState.GATT_CONNECTED.value()) == eState.GATT_CONNECTED.value()) ? "CONNECTED" : "";
        gattState += ((mState & eState.GATT_CONNECTING.value()) == eState.GATT_CONNECTING.value()) ? "CONNECTING" : "";
        Log.i(TAG,"GATT:" + gattState);

        String scanningState = "";
        scanningState += ((mState & eState.BLE_SCANNING.value()) == eState.BLE_SCANNING.value()) ? "SCANNING" : "";
        scanningState += ((mState & eState.BLE_NOT_SCANNING.value()) == eState.BLE_NOT_SCANNING.value()) ? "NOT SCANNING" : "";
        Log.i(TAG,"SCAN:" + scanningState);
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                printConnectionStatus();
                connectToBLEDevice();
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

    private void broadcastUpdate(final String action) {

        final Intent intent = new Intent(action);
        mAppContext.sendBroadcast(intent);
    }
}

