package org.assignment.server;

import org.assignment.dto.ReceivedMessage;
import org.assignment.rsa.RSAUtils;

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

import static org.assignment.utils.CommonUtils.generateMD5Hash;

class ServerClientHandler implements Runnable {

    public static final String SEND_MESSAGE_TYPE = "send-message";
    public static final String GET_MESSAGE_TYPE = "get-message";
    private final Socket socket;
    private Map<String, Queue<ReceivedMessage>> messageQueue = null;

    public ServerClientHandler(Socket socket, Map<String, Queue<ReceivedMessage>> messageQueue) {
        this.messageQueue = messageQueue ;
        this.socket = socket;
    }


    @Override
    public void run() {
        handleClient();
    }

    public void handleClient() throws RuntimeException{
        try {
            System.out.println("Messages in server " + messageQueue.size());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            String clientUserId = dis.readUTF();
            String messageType = dis.readUTF();
            System.out.println("Message type: " + messageType);
            if (messageType.equals(SEND_MESSAGE_TYPE)) {
                try {
                    processMessageFromClient(dis, clientUserId, dos);
                } catch (NoSuchPaddingException | InvalidKeyException | BadPaddingException | InvalidKeySpecException |
                         NoSuchAlgorithmException | IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                }
            } else if (messageType.equals(GET_MESSAGE_TYPE)) {
                fetchClientMessage(dis, clientUserId, dos);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void processMessageFromClient(DataInputStream dis, String clientUserId, DataOutputStream dos) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        String message;
        int recipientUserIdLength = dis.readInt();

        byte[] recipientUserIdByte = new byte[recipientUserIdLength];
        dis.readFully(recipientUserIdByte);

        String recipientUserId = RSAUtils.decryptMessageWithPrivate(recipientUserIdByte, "server");

        int messageLength = dis.readInt();
        byte[] allEncryptedMessageData = new byte[messageLength];
        dis.readFully(allEncryptedMessageData);

        try {
            while (allEncryptedMessageData !=  null) {
                message = RSAUtils.decryptMessageWithPrivate(allEncryptedMessageData, "server");
                ReceivedMessage receivedMessage = new ReceivedMessage(clientUserId, LocalDateTime.now(), RSAUtils.encryptMessageWithPublicKey(message, recipientUserId));
                dos.writeUTF("Server Received " + message);
                addMessageToQueue(recipientUserId, receivedMessage);
            }
        } catch (IOException e) {
            System.err.println("Client closed its connection.");
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 InvalidKeySpecException | InvalidKeyException | BadPaddingException e) {
            throw new RuntimeException(e);
        } finally {
            socket.close();
            dis.close();
            dos.close();
        }
    }

    private synchronized void addMessageToQueue(String recipientUserId, ReceivedMessage receivedMessage) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        recipientUserId = generateMD5Hash(recipientUserId);
        messageQueue.putIfAbsent(recipientUserId, new ConcurrentLinkedQueue<>());
        messageQueue.get(recipientUserId).add(receivedMessage);
    }

    private void fetchClientMessage(DataInputStream dis, String clientUserId, DataOutputStream dos) throws IOException {
        try {
            Queue<ReceivedMessage> receivedMessageQueue = getClientMessagesQueue(clientUserId);
            StringBuilder clientMessageBuilder = new StringBuilder();
            displayMessageSummaryToClient(receivedMessageQueue, clientMessageBuilder, clientUserId);
            byte[] encryptedMessageByte = RSAUtils.encryptMessageWithPublicKey(clientMessageBuilder.toString(), clientUserId);
            dos.writeInt(encryptedMessageByte.length);
            dos.write(encryptedMessageByte);
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } finally {
            socket.close();
            dis.close();
            dos.close();
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

    public synchronized Queue<ReceivedMessage> getClientMessagesQueue(String clientUserId) {
        clientUserId = generateMD5Hash(clientUserId);
        Queue<ReceivedMessage> receivedMessages = messageQueue.getOrDefault(clientUserId, new ConcurrentLinkedQueue<>());
        messageQueue.remove(clientUserId);
        return receivedMessages;
    }
}
