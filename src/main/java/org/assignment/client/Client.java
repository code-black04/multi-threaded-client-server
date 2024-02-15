package org.assignment.client;

import org.assignment.dto.Message;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

import static org.assignment.utils.Utils.callCloseSockets;

public class Client {

    private String hostName;
    private String severPort;
    private String senderUserId;

    public Client(String hostName, String serverPort, String senderUserId) {
        this.hostName = hostName;
        this.severPort = serverPort;
        this.senderUserId = senderUserId;
        System.out.println("Client bean created : " + this);
    }



    public static void main(String[] args) {
        String host = args[0];
        String port = args[1];
        String senderUserId = args[2];
        System.out.println("Host: " + host + " Port: " + port + " senderUserId: " + senderUserId);
        Client client = new Client(host, port, senderUserId);
        client.init();
    }

    private void init() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                getMessage(new Message(senderUserId, null, null, "get-message"));
                while (true) {
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Do you want to send a message? [y/n]: ");
                    String actionSelection = scanner.next();
                    try {
                        createMessageInputsAndSend(actionSelection, scanner);
                    } catch (IOException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                             NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        thread.start();
    }

    private void createMessageInputsAndSend(String actionSelection, Scanner scanner) throws IOException, InvalidKeySpecException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (actionSelection.equalsIgnoreCase("y")) {
            System.out.println("Enter the recipient userid: ");
            String recipientUserId = scanner.next();

            System.out.println("Enter your message: ");
            String message = scanner.next();

            System.out.println("Message sent: " + message);
            sendMessageWithRecipientUserId(message, recipientUserId);
        } else if (actionSelection.equalsIgnoreCase("n")){
            System.exit(0);
        }
    }

    private void sendMessageWithRecipientUserId(String message, String recipientUserId) {
        if (message != null) {
            try {
                sendMessage(new Message(senderUserId, recipientUserId, message, "send-message"));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        } else
            System.out.println("No message found to be sent");
    }

    public void sendMessage(Message message) throws UnknownHostException {
        System.out.println("Server host: " + hostName);
        System.out.println("Server Port: " + severPort);

        System.out.println("User Id: " + senderUserId);

        try {
            Socket s = new Socket(hostName, Integer.parseInt(severPort));

            DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            dos.writeUTF(message.getSenderUserId());
            dos.writeUTF(message.getMessageType());
            dos.writeUTF(message.getRecipientUserId());
            dos.writeUTF(message.getMessageBody());

            System.out.println("Connected server");
            String serverResponse = dataInputStream.readUTF();
            System.out.println("Server response: " + serverResponse);

            callCloseSockets(s, dataInputStream, dos);
        } catch (Exception e) {
            System.err.println("Cannot connect to server.");
            e.printStackTrace();
        }
    }

    public void getMessage(Message message) {
        try {
            Socket s = new Socket(hostName, Integer.parseInt(severPort));

            DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());

            dataOutputStream.writeUTF(message.getSenderUserId());
            dataOutputStream.writeUTF(message.getMessageType());

            System.out.println("Connected server");

            String serverResponse = dataInputStream.readUTF();
            System.out.println("Server response: " + serverResponse);

            callCloseSockets(s, dataInputStream, dataOutputStream);

        } catch (Exception e) {
            System.err.println("Cannot connect to server.");
            e.printStackTrace();
        }
    }
}
