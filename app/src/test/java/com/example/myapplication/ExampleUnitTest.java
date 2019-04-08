package com.example.myapplication;

import com.example.myapplication.utils.DataFormat;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testDataFormat() {
        ArrayList<byte[]> res = DataFormat.ToUTF8ByteArray("Let's do it very long Let's end it");
        assertEquals(res.size(), 2);
        assertEquals(res.get(0).length, 17);
        assertEquals(res.get(1).length, 17);
        byte[] data = res.get(0);
        for (byte chr: data) {
            System.out.print(String.format("0x%02X ", chr));
        }
    }
}