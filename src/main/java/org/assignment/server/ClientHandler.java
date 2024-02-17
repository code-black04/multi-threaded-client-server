package org.assignment.server;

import org.assignment.dto.ReceivedMessage;
import org.assignment.rsa.RSAUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assignment.utils.CommonUtils.byteStreamToHandleString;

class ClientHandler implements Runnable {

    private final Socket socket;
    private Map<String, Queue<ReceivedMessage>> messageQueue = null;

    public ClientHandler(Socket socket,  Map<String, Queue<ReceivedMessage>> messageQueue) {
        this.messageQueue = messageQueue ;
        this.socket = socket;

    }


    @Override
    public void run() {
        handleClient();
    }

    public void handleClient() throws RuntimeException{
        try {
            System.out.println("Messages is sever " + messageQueue.size());
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
        System.out.println("RECIPIENT USER ID : " + recipientUserId);

        byte[] allData = byteStreamToHandleString(dataInputStream);

        try {
            while (allData !=  null) {
                System.out.println("MESSAGE BEFORE DECRYPTING BY SERVER: " + allData);
                message = RSAUtils.decryptMessageWithPrivate(allData, "server");
                System.out.println("MESSAGE AFTER DECRYPTING BY SERVER: " + message);
                ReceivedMessage receivedMessage = new ReceivedMessage(senderUserId, LocalDateTime.now(), RSAUtils.encryptMessageWithPublicKey(message, recipientUserId));
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
            displayMessageSummaryToClient(receivedMessageQueue, stringBuilder, senderUserId);
            System.out.println("String builder before encryption : "+ stringBuilder);
            dataOutputStream.write(RSAUtils.encryptMessageWithPublicKey(stringBuilder.toString(), senderUserId));
            System.out.println("String builder after encryption : "+ stringBuilder);
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } finally {
            socket.close();
            dataInputStream.close();
            dataOutputStream.close();
        }
    }

    private static void displayMessageSummaryToClient(Queue<ReceivedMessage> receivedMessageQueue, StringBuilder stringBuilder, String recipientUserId) {
        if (receivedMessageQueue != null && !receivedMessageQueue.isEmpty()) {

            stringBuilder.append("There are " + receivedMessageQueue.size() + " message(s) for you.\n");
            receivedMessageQueue.forEach(messageReceived -> {
                stringBuilder.append("From User: " +messageReceived.getSenderUserId() + "\n");
                stringBuilder.append("Date: " + messageReceived.getDateTime() + "\n");
                try {
                    stringBuilder.append("Message: " + RSAUtils.decryptMessageWithPrivate(messageReceived.getMessageBody(), recipientUserId)+ "\n\n");
                } catch (IOException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                         NoSuchPaddingException | InvalidKeySpecException | NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            stringBuilder.append("There are 0 message(s) for you.\n");
        }
    }

    public synchronized Queue<ReceivedMessage> getMessageFromServersMessageQueue(String senderUserId) {
        System.out.println("GET : " + messageQueue.size());
        Queue<ReceivedMessage> receivedMessages = messageQueue.getOrDefault(senderUserId, new ConcurrentLinkedQueue<>());
        System.out.println("Received message queue size " + receivedMessages.size());
        System.out.println(receivedMessages.size());
        messageQueue.remove(senderUserId);
        return receivedMessages;
    }
}
