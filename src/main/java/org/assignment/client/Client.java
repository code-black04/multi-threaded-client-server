package org.assignment.client;

import org.assignment.dto.Message;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

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
                    if (actionSelection.equalsIgnoreCase("y")) {
                        System.out.println("Enter the recipient userid: ");
                        String recipientUserId = scanner.next();
                        System.out.println("Enter your message: ");
                        String message = scanner.next();
                        System.out.println("Message sent: " + message);
                        if (message != null) {
                            try {
                                sendMessage(new Message(senderUserId, recipientUserId, message, "send-message"));
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        } else
                            System.out.println("No message found to be sent");
                    } else if (actionSelection.equalsIgnoreCase("n")){
                        System.exit(0);
                    }
                }
            }
        });

        thread.start();
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

            s.close();
            dataInputStream.close();
            dos.close();
        } catch (Exception e) {
            System.err.println("Cannot connect to server.");
            e.printStackTrace();
        }
    }

    public void getMessage(Message message) {
        try {
            Socket s = new Socket(hostName, Integer.parseInt(severPort));

            DataInputStream dataInputStream = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            dos.writeUTF(message.getSenderUserId());
            dos.writeUTF(message.getMessageType());

            System.out.println("Connected server");

            String serverResponse = dataInputStream.readUTF();
            System.out.println("Server response: " + serverResponse);

            s.close();
            dataInputStream.close();
            dos.close();

        } catch (Exception e) {
            System.err.println("Cannot connect to server.");
            e.printStackTrace();
        }
    }
}
