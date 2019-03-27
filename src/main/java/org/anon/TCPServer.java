package org.anon;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class TCPServer {
    private ServerSocket serverSocket;
    private static int port = 10000;
    private HashMap<String, ClientHandler> clients;

    private static TCPServer instance = new TCPServer();

    private TCPServer() { }

    public static TCPServer getInstance() {
        return instance;
    }

    private void start() {
        String id;
        clients = new HashMap<>();
        System.out.println("Opening port...\n");
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Port opened. Listening to port " + port);
        }
        catch (IOException e) {
            System.out.println("Unable to attach to port!");
            System.exit(1);
        }

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                id = new Scanner(socket.getInputStream()).nextLine();
                if (clients.get(id) == null) {
                    ClientHandler handler = new ClientHandler(socket, id, clients);
                    handler.start();
                    clients.put(id, handler);
                    System.out.println("New client connected with ID: " + id);
                }
                else {
                    new PrintWriter(socket.getOutputStream(), true).println("User " + id + " already registered");
                    socket.close();
                }
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void updateUserList() {
        StringBuilder tmp = new StringBuilder("/userlist ");
        clients.forEach((k, v) -> tmp.append(k).append(' '));
        serverBroadcast(tmp.toString());
    }

    public void serverBroadcast(String message) {
        clients.forEach((k, v) -> v.getOStream().println(message));
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server closed!");
        }));

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e) {
                System.out.println("Invalid port number! Using default value");
                port = 10000;
            }
        }

        try (DBUtil test = new DBUtil()) {
            System.out.println("Connection test to database success.");
        } catch (DBUtilException | IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        instance.start();
    }
}
