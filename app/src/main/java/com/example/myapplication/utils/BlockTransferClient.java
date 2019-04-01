package com.example.myapplication.utils;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;

public class BlockTransferClient {

    public final static int BTS_MTU_SIZE_DEFAULT = 20;
    public final static int BLOCK_HEADER_SIZE = 3;
    public final static int BT_TYPE_WRITE_SETUP = 0x00;
    public final static int BT_TYPE_WRITE_DIRECT = 0x04;

    private int mOutgoingTotalFragments = 0;
    private int mMaxBlockPayloadSize;
    private Context mContext;
    private BluetoothHelper mBt;


    public BlockTransferClient(Context context) {
        mMaxBlockPayloadSize = BTS_MTU_SIZE_DEFAULT - BLOCK_HEADER_SIZE;
        mContext = context;
        mBt = BluetoothHelper.getInstance(mContext);
    }

    public void init() {

    }

    public boolean sendData(final String data) {
        Log.d("BlockTransferClient", ".sendData: ENTER");

        //  Find the total number of fragments needed to transmit the block
        //  based on the MTU size minus payload header.
        mOutgoingTotalFragments = (data.length() + (mMaxBlockPayloadSize - 1)) / mMaxBlockPayloadSize;

        Log.d("BlockTransferClient", "data.length=" + data.length() + ",outgoingTotalFragments=" + mOutgoingTotalFragments + ",maxBlockPayloadSize=" +  mMaxBlockPayloadSize);

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
        mBt.writeDataToBtCharacteristic(writeBuffer);

        // Now write data blocks
        // Split the data bytes into smaller parts if needed.
        String[] dataStrArray = ToStringArray(data);
        int offset = 0;

        for (String dataStr: dataStrArray) {
            int strLen = BLOCK_HEADER_SIZE + dataStr.length();
            writeBuffer = new byte[strLen];

            writeBuffer[0] = BT_TYPE_WRITE_DIRECT << 4;
            writeBuffer[1] = (byte)offset;
            writeBuffer[2] = (byte)(offset >> 8);

            Log.d("BlockTransferClient", "dataStr.length=" + dataStr.length() + ",offset=" + offset);

            System.arraycopy(dataStr.getBytes(), 0, writeBuffer, 3, dataStr.length());

            try {
                Thread.sleep(200);
                // Now write data block.
                mBt.writeDataToBtCharacteristic(writeBuffer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            offset += dataStr.length();
        }

        Log.d("BlockTransferClient", ".sendData: EXIT");
        return true;
    }

    /**
     *
     * @param tooLong is a string that has too many characters to be sent via bluetooth Gatt.
     *                If the length is greater than {@code MAX_BYTES}, cut it and add it to an
     *                array so it can be sent in separate packets.
     * @return a new string array
     */
    private String[] ToStringArray(String tooLong) {
        ArrayList<String> strings = new ArrayList<String>();
        String temp = "";
        //20 is the max number of bytes that can be sent to ble device
        while(tooLong.length() > mMaxBlockPayloadSize){
            temp = tooLong.substring(0, Math.min(tooLong.length(), mMaxBlockPayloadSize));
            strings.add(temp);
            tooLong = tooLong.substring(temp.length(),tooLong.length());
        }
        strings.add(tooLong);

        return strings.toArray(new String[strings.size()]);
    }



}
