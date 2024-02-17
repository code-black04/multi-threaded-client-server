package org.assignment.client;

import org.assignment.dto.Message;
import org.assignment.rsa.RSAUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

import static org.assignment.utils.CommonUtils.byteStreamToHandleString;
import static org.assignment.utils.CommonUtils.callCloseSocketAndStreams;

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

        if (args.length != 3) {
            System.err.println("Client UserId has not been passed");
            System.exit(-1);
        }

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
                getMessageFromServer(new Message(senderUserId, null, null, "get-message"));
                while (true) {
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Do you want to send a message? [y/n]: ");
                    String actionSelection = scanner.nextLine();
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
            String recipientUserId = scanner.nextLine();

            System.out.println("Enter your message: ");
            String message = scanner.nextLine();

            System.out.println("Message sent: " + message);
            sendMessageWithRecipientUserId(message, recipientUserId);
        } else if (actionSelection.equalsIgnoreCase("n")){
            System.exit(0);
        }
    }

    private void sendMessageWithRecipientUserId(String message, String recipientUserId) {
        if (message != null) {
            try {
                System.out.println("CLIENT ENCRYPTION BEFORE SENDING MESSAGE TO SERVER: " + message);
                byte[] encryptedMessageBytes = RSAUtils.encryptMessageWithPublicKey(message, "server");
                System.out.println("CLIENT ENCRYPTION BEFORE SENDING MESSAGE TO SERVER: " + new String(encryptedMessageBytes));
                sendMessageToServer(new Message(senderUserId, recipientUserId, encryptedMessageBytes, "send-message"));
            } catch (NoSuchPaddingException | IllegalBlockSizeException | IOException | NoSuchAlgorithmException |
                     InvalidKeySpecException | BadPaddingException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        } else
            System.out.println("No message found to be sent");
    }

    public void sendMessageToServer(Message message) throws UnknownHostException {
        System.out.println("Server host: " + hostName);
        System.out.println("Server Port: " + severPort);
        System.out.println("User Id: " + senderUserId);

        try {
            Socket s = new Socket(hostName, Integer.parseInt(severPort));

            DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());

            dataOutputStream.writeUTF(message.getSenderUserId());
            dataOutputStream.writeUTF(message.getMessageType());
            dataOutputStream.writeUTF(message.getRecipientUserId());
            dataOutputStream.write(message.getMessageBody());

            System.out.println("Connected server");

            callCloseSocketAndStreams(dataInputStream, dataOutputStream, s);
        } catch (Exception e) {
            System.err.println("Cannot connect to server.");
            e.printStackTrace();
        }
    }

    public void getMessageFromServer(Message message) {
        try {
            Socket s = new Socket(hostName, Integer.parseInt(severPort));

            DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());

            dataOutputStream.writeUTF(message.getSenderUserId());
            dataOutputStream.writeUTF(message.getMessageType());
            System.out.println("Connected server");

            byte[] allData = byteStreamToHandleString(dataInputStream);

            String serverResponse = RSAUtils.decryptMessageWithPrivate(allData, senderUserId);
            System.out.println("Server response: " + serverResponse);

            callCloseSocketAndStreams(dataInputStream, dataOutputStream, s);

        } catch (Exception e) {
            System.err.println("Cannot connect to server.");
            e.printStackTrace();
        }
    }
}
