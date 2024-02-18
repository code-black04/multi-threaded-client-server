package org.assignment.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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


    public static String toHexString(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String generateMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(input.getBytes());
            return toHexString(hashInBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 hashing algorithm not found");
        }
    }

    public static void main(String[] args) {
        String input = "alice";
        String md5Hash = generateMD5Hash(input);
        System.out.println("The MD5 hash for '" + input + "' is: " + md5Hash);
        String md5Hash1 = generateMD5Hash(input);
        System.out.println("The MD5 hash for '" + input + "' is: " + md5Hash1);
    }
}
