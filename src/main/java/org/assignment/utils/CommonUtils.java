package org.assignment.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class CommonUtils {
    public static void callCloseSocketAndStreams(DataInputStream dataInputStream, DataOutputStream dataOutputStream, Socket s) throws IOException {
        dataInputStream.close();
        dataOutputStream.close();
        s.close();
    }

    public static byte[] byteStreamToHandleString(DataInputStream dataInputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384]; // Temporary buffer
        while ((nRead = dataInputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        byte[] allData = buffer.toByteArray();
        return allData;
    }
}
