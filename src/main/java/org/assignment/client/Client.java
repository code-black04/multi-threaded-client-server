package org.assignment.client;

import org.assignment.dto.SendMessage;
import org.assignment.rsa.RSAUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
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

    private void init() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                getMessageFromServer(new SendMessage(senderUserId, null, null, "get-message"));
                while (true) {
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Do you want to send a message? [y/n]: ");
                    String actionSelection = scanner.nextLine();
                    try {
                        writeMessageDetailsAndSend(actionSelection, scanner);
                    } catch (IOException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                             NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        thread.start();
    }

    private void writeMessageDetailsAndSend(String actionSelection, Scanner scanner) throws IOException, InvalidKeySpecException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (actionSelection.equalsIgnoreCase("y")) {
            System.out.println("Enter the recipient userid: ");
            String recipientUserId = scanner.nextLine();
            System.out.println("Enter your message: ");
            String message = scanner.nextLine();
            System.out.println("Message sent: " + message);
            sendMessage(message, recipientUserId);
        } else if (actionSelection.equalsIgnoreCase("n")) {
            System.exit(0);
        }
    }

    private void sendMessage(String message, String recipientUserId) {
        if (message != null) {
            try {
                byte[] encryptedRecipientUserIdBytes = RSAUtils.encryptMessageWithPublicKey(recipientUserId, "server");
                byte[] encryptedMessageBytes = RSAUtils.encryptMessageWithPublicKey(message, "server");
                sendMessageToServer(new SendMessage(senderUserId, encryptedRecipientUserIdBytes, encryptedMessageBytes, "send-message"));
            } catch (NoSuchPaddingException | IllegalBlockSizeException | IOException | NoSuchAlgorithmException |
                     InvalidKeySpecException | BadPaddingException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        } else
            System.out.println("No message found to be sent");
    }

    public void sendMessageToServer(SendMessage sendMessage) throws UnknownHostException {
        try {
            Socket s = new Socket(hostName, Integer.parseInt(severPort));

            DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());

            dataOutputStream.writeUTF(sendMessage.getSenderUserId());
            dataOutputStream.writeUTF(sendMessage.getMessageType());
            dataOutputStream.writeInt(sendMessage.getRecipientUserId().length);
            dataOutputStream.write(sendMessage.getRecipientUserId());
            dataOutputStream.writeInt(sendMessage.getMessageBody().length);
            dataOutputStream.write(sendMessage.getMessageBody());

            callCloseSocketAndStreams(dataInputStream, dataOutputStream, s);
        } catch (Exception e) {
            System.err.println("Cannot connect to server.");
            e.printStackTrace();
        }
    }

    public void getMessageFromServer(SendMessage sendMessage) {
        try {
            Socket s = new Socket(hostName, Integer.parseInt(severPort));

            DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(s.getOutputStream());

            dataOutputStream.writeUTF(sendMessage.getSenderUserId());
            dataOutputStream.writeUTF(sendMessage.getMessageType());
            int messageLength = dataInputStream.readInt();
            byte[] allData = byteStreamToHandleString(dataInputStream, messageLength);

            String serverResponse = RSAUtils.decryptMessageWithPrivate(allData, senderUserId);
            System.out.println("Server response: " + serverResponse);

            callCloseSocketAndStreams(dataInputStream, dataOutputStream, s);

        } catch (Exception e) {
            System.err.println("Cannot connect to server.");
            e.printStackTrace();
        }
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
}
