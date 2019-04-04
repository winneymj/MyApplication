package com.example.myapplication.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.myapplication.utils.BluetoothHelper;
import com.example.myapplication.utils.DataFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Vector;

public class MyService extends Service {
    public MyService() {
    }

//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        Log.d("MyService.onBind", "ENTER");
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d("MyService.onStartCommand", "ENTER");
//        return START_STICKY;
//    }

    private BluetoothAdapter mBluetoothAdapter;
    public static final String BT_DEVICE = "btdevice";
    public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming
    // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
    // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
    // _device
    private ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;
    // public mInHangler mHandler = new mInHangler(this);
    private static Handler mHandler = null;
    public static int mState = STATE_NONE;
    public static String deviceName;
    public Vector<Byte> packdata = new Vector<Byte>(2048);

    @Override
    public void onCreate() {
        Log.d("MyService", ".onCreate:ENTER");

        // Setup broadcast receiver to see GATT status changes.
        final IntentFilter requestFilter = new IntentFilter();
        requestFilter.addAction(BluetoothHelper.GATT_CONNECTED_INTENT);
        requestFilter.addAction(BluetoothHelper.GATT_DISCONNECTED_INTENT);
        requestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mGattBroadcastReceiver, requestFilter);

        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("MyService", ".onBind:ENTER");
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MyService", ".onStartCommand:ENTER");

        BluetoothHelper btHelperInstance = BluetoothHelper.getInstance(getApplicationContext());

        String sender = (intent.hasExtra("sender")) ? intent.getStringExtra("sender") : "";
        String from = (intent.hasExtra("from")) ? intent.getStringExtra("from") : "";
        String subject = (intent.hasExtra("subject")) ? intent.getStringExtra("subject") : null;
        String body = (intent.hasExtra("body")) ? intent.getStringExtra("body") : null;

        Log.d("MyService", ".onStartCommand: " + DataFormat.TrimText("1" + sender));

        btHelperInstance.sendBLEData("testing a very long string to see if works");
        btHelperInstance.sendBLEData("smaller test");

//        btHelperInstance.writeDataToBtCharacteristic(DataFormat.TrimText("1" + sender));
//        btHelperInstance.writeDataToBtCharacteristic(DataFormat.TrimText("2" + from));
//        if (null != subject) {
//            try {
//                Thread.sleep(100);
//                btHelperInstance.writeDataToBtCharacteristic(DataFormat.TrimText("3" + subject));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        if (null != body) {
//            try {
//                Thread.sleep(100);
//                btHelperInstance.writeDataToBtCharacteristic(DataFormat.TrimText("4"+ body));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        // Send finished by sending a 9
//        try {
//            Thread.sleep(100);
//            btHelperInstance.writeDataToBtCharacteristic("9Done");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        // Start connecting to device.  Wait for GATT connected event
//        btHelperInstance.connectToDevice();


//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter != null) {
//            try {
//                _device = mBluetoothAdapter.getRemoteDevice(BT_DEVICE);
//                if (null != _device) {
//                    deviceName = _device.getName();
//                    String macAddress = _device.getAddress();
//                    if (macAddress != null && macAddress.length() > 0) {
//                        connectToDevice(macAddress);
//                    } else {
//                        stopSelf();
//                        return Service.START_NOT_STICKY;
//                    }
//                }
//            }
//            catch (IllegalArgumentException e)
//            {
//                Log.d("MyService: exception:", e.getMessage());
//                stopSelf();
//                return Service.START_NOT_STICKY;
//            }
//        }
//        String stopservice = intent.getStringExtra("stopservice");
//        if (stopservice != null && stopservice.length() > 0) {
//            stop();
//        }
        Log.d("MyService", ".onStartCommand:EXIT");
        return START_STICKY;
    }

    private final BroadcastReceiver mGattBroadcastReceiver = new BroadcastReceiver()
    {
        private static final String TAG = "MyService";
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.i(TAG,"mGattBroadcastReceiver.onReceive:" + intent.getAction());
            if (BluetoothHelper.GATT_CONNECTED_INTENT.equals(intent.getAction())) {
                // Temporary code
//                BluetoothHelper.getInstance(getApplicationContext()).discoverServices();
                // Only connect send out data when we have a connection
//                BluetoothHelper.getInstance(getApplicationContext()).writeDataToBtCharacteristic();
            } else if (BluetoothHelper.GATT_DISCONNECTED_INTENT.equals(intent.getAction())) {
                Log.i(TAG,"mGattBroadcastReceiver.onReceive:" + intent.getAction());
            }
        }
    };

//    private synchronized void connectToDevice(String macAddress) {
//        Log.d("MyService", ".connectToDevice:" + macAddress);
//
//        try {
//            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
//            if (mState == STATE_CONNECTING) {
//                if (mConnectThread != null) {
//                    mConnectThread.cancel();
//                    mConnectThread = null;
//                }
//            }
//
//            // Cancel any thread currently running a connection
//            if (mConnectedThread != null) {
//                mConnectedThread.cancel();
//                mConnectedThread = null;
//            }
//            mConnectThread = new ConnectThread(device);
//            mConnectThread.start();
//            setState(STATE_CONNECTING);
//        } catch (IllegalArgumentException e) {
//            Log.d("MyService: exception:", e.getMessage());
//            return;
//        }
//
//    }

    private void setState(int state) {
        Log.d("MyService", ".setState:ENTER");
        MyService.mState = state;
        if (mHandler != null) {
//            mHandler.obtainMessage(AbstractActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        }
    }

    public synchronized void stop() {
        Log.d("MyService", ".stop:ENTER");
        setState(STATE_NONE);
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        stopSelf();
    }

    @Override
    public boolean stopService(Intent name) {
        Log.d("MyService", ".stopService:ENTER");
        setState(STATE_NONE);
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mBluetoothAdapter.cancelDiscovery();
        return super.stopService(name);
    }

    private void connectionFailed() {
        Log.d("MyService", ".connectionFailed:ENTER");
//        BluetoothLEService2.this.stop();
//        Message msg = mHandler.obtainMessage(AbstractActivity.MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(AbstractActivity.TOAST, getString(R.string.error_connect_failed));
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);
    }

    private void connectionLost() {
        Log.d("MyService", ".connectionLost:ENTER");
//        BluetoothLEService2.this.stop();
//        Message msg = mHandler.obtainMessage(AbstractActivity.MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(AbstractActivity.TOAST, getString(R.string.error_connect_lost));
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);
    }

    private static Object obj = new Object();

    public static void write(byte[] out) {
        Log.d("MyService", ".write:ENTER");
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (obj) {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    private synchronized void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d("MyService", ".connected:ENTER");
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();

        // Message msg =
        // mHandler.obtainMessage(AbstractActivity.MESSAGE_DEVICE_NAME);
        // Bundle bundle = new Bundle();
        // bundle.putString(AbstractActivity.DEVICE_NAME, "p25");
        // msg.setData(bundle);
        // mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);

    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            Log.d("MyService", ".ConnectThread:ENTER");
            this.mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            Log.d("MyService", ".ConnectThread:Run:ENTER");
            setName("ConnectThread");
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                Log.d("MyService", ".ConnectThread:Calling connectionFailed");
                connectionFailed();
                return;

            }
            synchronized (MyService.this) {
                mConnectThread = null;
            }
            Log.d("MyService", ".ConnectThread:Calling connected");
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            Log.d("MyService", ".ConnectThread:Cancel:ENTER");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("MyService", "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d("MyService", ".ConnectedThread:ENTER");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("MyService", ".ConnectedThread:temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            Log.d("MyService", ".ConnectedThread:Run:ENTER");
//            while (true) {
//                try {
//                    if (!encodeData(mmInStream)) {
//                        mState = STATE_NONE;
//                        connectionLost();
//                        break;
//                    } else {
//                    }
//                    // mHandler.obtainMessage(AbstractActivity.MESSAGE_READ,
//                    // bytes, -1, buffer).sendToTarget();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    connectionLost();
//                    BluetoothLEService2.this.stop();
//                    break;
//                }
//
//            }
        }

        private byte[] btBuff;


        public void write(byte[] buffer) {
            Log.d("MyService", ".ConnectedThread:write:ENTER");
//            try {
//                mmOutStream.write(buffer);
//
//                // Share the sent message back to the UI Activity
//                mHandler.obtainMessage(AbstractActivity.MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
//            } catch (IOException e) {
//                Log.e("PrinterService", "Exception during write", e);
//            }
        }

        public void cancel() {
            Log.d("MyService", ".ConnectedThread:cancel:ENTER");
            try {
                mmSocket.close();

            } catch (IOException e) {
                Log.e("MyService", "close() of connect socket failed", e);
            }
        }

    }

    public void trace(String msg) {
        Log.d("AbstractActivity", msg);
        toast(msg);
    }

    public void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        Log.d("MyService", "onDestroy");
        stop();
        super.onDestroy();
    }

    private void sendMsg(int flag) {
        Message msg = new Message();
        msg.what = flag;
        handler.sendMessage(msg);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {//
            if (!Thread.currentThread().isInterrupted()) {
                switch (msg.what) {
                    case 3:

                        break;

                    case 4:

                        break;
                    case 5:
                        break;

                    case -1:
                        break;
                }
            }
            super.handleMessage(msg);
        }

    };
}
