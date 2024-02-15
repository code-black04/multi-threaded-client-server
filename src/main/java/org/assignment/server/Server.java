package org.assignment.server;

import org.assignment.dto.ReceivedMessage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class Server {

    private String serverPort;
    private Map<String, Queue<ReceivedMessage>> messageQueue;

    public Server(String serverPort){
        this.messageQueue = new HashMap<>();
        this.serverPort = serverPort;
        System.out.println("Server bean created: " + this);
    }

    public static void main(String[] args) {
        String port = args[0];
        System.out.println(port);
        try {
            new Server(port).startServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public void startServer() throws Exception{
        System.out.println("Port : " + serverPort);
        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(serverPort))) {
            System.out.println("Server started on port " + serverPort);
            System.out.println("Waiting incoming connection requests : ");
            createClientHandlerForEachClient(serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createClientHandlerForEachClient(ServerSocket serverSocket) throws IOException {
        while (true) {
            Socket socket = serverSocket.accept(); // Accept incoming connections
            System.out.println("Client connected: " + socket.getInputStream());
            // Create a new ClientHandler instance for each client connection
            Thread thread = new Thread(new ClientHandler(socket, messageQueue));
            thread.start();
        }
    }
}
