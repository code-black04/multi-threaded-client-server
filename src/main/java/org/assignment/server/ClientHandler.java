package org.assignment.server;

import org.assignment.dto.RecievedMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class ClientHandler implements Runnable {

    private final Socket socket;
    private Map<String, Queue<RecievedMessage>> messageQueue = null;

    public ClientHandler(Socket socket,  Map<String, Queue<RecievedMessage>> messageQueue) {
        this.messageQueue = messageQueue ;
        this.socket = socket;
        System.out.println("Client Handler created: " + this);
    }


    @Override
    public void run() {
        handleClient();
    }

    public void handleClient() throws RuntimeException{
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

            String senderUserId = dataInputStream.readUTF();
            String messageType = dataInputStream.readUTF();
            System.out.println("Message type: " + messageType);
            if (messageType.equals("send-message")) {
                processSendMessage(dataInputStream, senderUserId, dataOutputStream);
            } else if (messageType.equals("get-message")) {
                processGetMessage(dataInputStream, senderUserId, dataOutputStream);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void processGetMessage(DataInputStream dataInputStream, String senderUserId, DataOutputStream dataOutputStream) throws IOException {
        try {
            System.out.println("process message sender user id: " + senderUserId);
            Queue<RecievedMessage> recievedMessageQueue = getMessages(senderUserId);

            StringBuilder stringBuilder = new StringBuilder();

            if (recievedMessageQueue != null && !recievedMessageQueue.isEmpty()) {
                stringBuilder.append("There are " + recievedMessageQueue.size() + " message(s) for you.\n");
                recievedMessageQueue.forEach(messageReceived -> {
                    stringBuilder.append("From User: " +messageReceived.getSenderUserId() + "\n");
                    stringBuilder.append("Date: " + messageReceived.getDateTime() + "\n");
                    stringBuilder.append("Message: " + messageReceived.getMessageBody()+ "\n\n");
                });
            } else {
                stringBuilder.append("There are 0 message(s) for you.\n");
            }
            dataOutputStream.writeUTF(stringBuilder.toString());
        } finally {
            socket.close();
            dataInputStream.close();
            dataOutputStream.close();
        }
    }

    private void processSendMessage(DataInputStream dataInputStream, String senderUserId, DataOutputStream dataOutputStream) throws IOException {
        String message;
        String recipientUserId = dataInputStream.readUTF();
        System.out.println("Reciever : " + recipientUserId);

        try {
            while ((message = dataInputStream.readUTF()) != null) {
                System.out.println("Message received from " + senderUserId + " is " + message);
                RecievedMessage recievedMessage = new RecievedMessage(senderUserId, LocalDateTime.now(), message);
                dataOutputStream.writeUTF("Server Received " + message);
                addMessage(recipientUserId, recievedMessage);
                System.out.println("Message : " + message);
            }
        } catch (IOException e) {
            System.err.println("Client closed its connection.");
        } finally {
            socket.close();
            dataInputStream.close();
            dataOutputStream.close();
        }
    }

    private synchronized void addMessage(String recipientUserId, RecievedMessage recievedMessage) {
        messageQueue.putIfAbsent(recipientUserId, new ConcurrentLinkedQueue<>());
        messageQueue.get(recipientUserId).add(recievedMessage);
    }

    public synchronized Queue<RecievedMessage> getMessages(String senderUserId) {
        Queue<RecievedMessage> recievedMessages = messageQueue.getOrDefault(senderUserId, new ConcurrentLinkedQueue<>());
        messageQueue.remove(senderUserId);
        return recievedMessages;
    }
}
