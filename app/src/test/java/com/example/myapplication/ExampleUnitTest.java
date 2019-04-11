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
        {
            ArrayList<byte[]> res = DataFormat.ToUTF8ByteArray("Let's do it very long Let's end it");
            assertEquals(2, res.size());
            assertEquals(17, res.get(0).length);
            assertEquals(17, res.get(1).length);
            byte[] data = res.get(0);
            for (byte chr : data) {
                System.out.print(String.format("0x%02X ", chr));
            }
            System.out.println();
        }
        {
            ArrayList<byte[]> res = DataFormat.ToUTF8ByteArray("12345678901234567");
            assertEquals(1, res.size());
            assertEquals(17, res.get(0).length);
//            assertEquals(res.get(1).length, 17);
            byte[] data = res.get(0);
            for (byte chr : data) {
                System.out.print(String.format("0x%02X ", chr));
            }
            System.out.println();
        }
        {
            ArrayList<byte[]> res = DataFormat.ToUTF8ByteArray("Be great to let you know the weather is birthday \uD83C\uDF81");
            assertEquals(4, res.size());
            assertEquals(17, res.get(0).length);
            assertEquals(17, res.get(1).length);
            assertEquals(17, res.get(2).length);
            assertEquals(2, res.get(3).length);
            byte[] data = res.get(0);
            for (byte chr : data) {
                System.out.print(String.format("0x%02X ", chr));
            }
            System.out.println();
        }

    }
}