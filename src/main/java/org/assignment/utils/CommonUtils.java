package org.assignment.utils;

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
}
