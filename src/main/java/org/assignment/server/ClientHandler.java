package org.assignment.server;

import org.assignment.dto.ReceivedMessage;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class ClientHandler implements Runnable {

    private final Socket socket;
    private Map<String, Queue<ReceivedMessage>> messageQueue = null;

    public ClientHandler(Socket socket,  Map<String, Queue<ReceivedMessage>> messageQueue) {
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

    private void processSendMessage(DataInputStream dataInputStream, String senderUserId, DataOutputStream dataOutputStream) throws IOException {
        String message;
        String recipientUserId = dataInputStream.readUTF();
        System.out.println("Reciever : " + recipientUserId);

        try {
            while ((message = dataInputStream.readUTF()) != null) {
                System.out.println("Message received from " + senderUserId + " is " + message);
                ReceivedMessage receivedMessage = new ReceivedMessage(senderUserId, LocalDateTime.now(), message);
                dataOutputStream.writeUTF("Server Received " + message);
                addMessageToServersMessageQueue(recipientUserId, receivedMessage);
                System.out.println("Message : " + message);
            }
        } catch (IOException e) {
            System.err.println("Client closed its connection.");
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 InvalidKeySpecException | InvalidKeyException | BadPaddingException e) {
            throw new RuntimeException(e);
        } finally {
            socket.close();
            dataInputStream.close();
            dataOutputStream.close();
        }
    }

    private synchronized void addMessageToServersMessageQueue(String recipientUserId, ReceivedMessage receivedMessage) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        messageQueue.putIfAbsent(recipientUserId, new ConcurrentLinkedQueue<>());
        messageQueue.get(recipientUserId).add(receivedMessage);
    }

    private void processGetMessage(DataInputStream dataInputStream, String senderUserId, DataOutputStream dataOutputStream) throws IOException {
        try {
            System.out.println("process message sender user id: " + senderUserId);
            Queue<ReceivedMessage> receivedMessageQueue = getMessageFromServersMessageQueue(senderUserId);
            StringBuilder stringBuilder = new StringBuilder();
            displayMessageSummaryToClient(receivedMessageQueue, stringBuilder);
            dataOutputStream.writeUTF(stringBuilder.toString());
        } finally {
            socket.close();
            dataInputStream.close();
            dataOutputStream.close();
        }
    }

    private static void displayMessageSummaryToClient(Queue<ReceivedMessage> receivedMessageQueue, StringBuilder stringBuilder) {
        if (receivedMessageQueue != null && !receivedMessageQueue.isEmpty()) {
            stringBuilder.append("There are " + receivedMessageQueue.size() + " message(s) for you.\n");
            receivedMessageQueue.forEach(messageReceived -> {
                stringBuilder.append("From User: " +messageReceived.getSenderUserId() + "\n");
                stringBuilder.append("Date: " + messageReceived.getDateTime() + "\n");
                stringBuilder.append("Message: " + messageReceived.getMessageBody()+ "\n\n");
            });
        } else {
            stringBuilder.append("There are 0 message(s) for you.\n");
        }
    }

    public synchronized Queue<ReceivedMessage> getMessageFromServersMessageQueue(String senderUserId) {
        Queue<ReceivedMessage> receivedMessages = messageQueue.getOrDefault(senderUserId, new ConcurrentLinkedQueue<>());
        messageQueue.remove(senderUserId);
        return receivedMessages;
    }
}
