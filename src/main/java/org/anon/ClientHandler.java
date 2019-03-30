package org.anon;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ClientHandler extends Thread {
    private Socket socket;
    private String clientID;
    private Scanner iStream;
    private PrintWriter oStream;
    private TCPServer server;
    private HashMap<String, ClientHandler> clients;

    public ClientHandler(Socket client, String clientID, HashMap<String, ClientHandler> clients) throws IOException {
        socket = client;
        server = TCPServer.getInstance();
        this.clientID = clientID;
        this.clients = clients;

        iStream = new Scanner(socket.getInputStream());
        oStream = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try (DBUtil db = new DBUtil()) {
            if (db.isRegisteredUser(clientID)) {
                if (!auth(clientID)) {
                    try {
                        disconnect();
                    } catch (IOException ioEx) {
                        System.out.println("Unable to disconnect!");
                    }
                    return;
                }
            } else oStream.println("/noauth");
        } catch (DBUtilException | IOException e) {
            System.out.println(e.getMessage());
        }

        try {
            server.serverBroadcast("-- '" + clientID + "' joined the server --");
            oStream.println("Welcome to Stupid Server!\n" +
                    "Let's chat with anyone on this server.\n" +
                    "@<nickname> to PM, /part to left the server");
            server.updateUserList();

            while (interpretMessage());
        }
        catch (NoSuchElementException | IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        finally {
            try {
                disconnect();
            }
            catch (IOException ioEx) {
                System.out.println("Unable to disconnect!");
            }
        }
    }

    public void disconnect() throws IOException {
        server.serverBroadcast("-- '" + clientID + "' left the server --");
        System.out.println("\n* Closing connection for " + clientID + "... *");
        clients.remove(clientID);
        server.updateUserList();
        oStream.close();
        iStream.close();
        socket.close();
    }

    public PrintWriter getOStream() {
        return oStream;
    }

    private boolean interpretMessage() {
        String message = iStream.nextLine();
        String[] token = message.trim().split(" ");
        switch (token[0]) {
            case "/part":
                return false;
            case "/rename":
                if (token.length == 1) oStream.println("-- Invalid rename syntax --");
                else renameUser(token[1]);
                break;
            case "/kick":
                if (token.length == 1) oStream.println("-- Invalid kick syntax --");
                else kickUser(token[1]);
                break;
            case "/register":
                registerUser();
                break;
            case "/chpass":
                changePassword();
                break;
            default:
                boolean isBroadcastMessage = true;

                for (String s : token) {
                    if (s.matches("@\\w+")) {
                        sendMessageTo(message, s.substring(1));
                        isBroadcastMessage = false;
                    }
                }

                if (isBroadcastMessage) broadcastMessage(message);
                else oStream.println("[" + clientID + "]: " + message);
        }
        return true;
    }

    private void registerUser() {
        try (DBUtil db = new DBUtil()) {
            if (db.isRegisteredUser(clientID)) {
                oStream.println("-- You're already registered --");
            }
            else {
                String password, rePassword;
                oStream.println("-- Enter your new password --");
                oStream.println("/auth");
                password = iStream.nextLine();
                oStream.println("-- Re-enter your new password --");
                oStream.println("/auth");
                rePassword = iStream.nextLine();
                if (password.matches(".{8,}")) {
                    if (password.equals(rePassword)) {
                        try {
                            db.registerUser(clientID, password);
                            oStream.println("-- Your name registered successfully --");
                        } catch (DBUtilException e) {
                            oStream.println("-- An error has occurred while registering your name --");
                        }
                    } else oStream.println("-- Password mismatch --");
                } else oStream.println("-- Password should be 8 characters or more --");
            }
        }
        catch (DBUtilException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void changePassword() {
        try (DBUtil db = new DBUtil()) {
            if (!db.isRegisteredUser(clientID)) {
                oStream.println("-- Your name haven't registered yet --");
            }
            else {
                oStream.println("-- Enter the old password --");
                if (auth(clientID)) {
                    String newPassword, reNewPassword;
                    oStream.println("-- Enter your new password --");
                    oStream.println("/auth");
                    newPassword = iStream.nextLine();
                    oStream.println("-- Re-enter your new password --");
                    oStream.println("/auth");
                    reNewPassword = iStream.nextLine();
                    if (newPassword.matches(".{8,}")) {
                        if (newPassword.equals(reNewPassword)) {
                            try {
                                db.changePassword(clientID, newPassword);
                                oStream.println("-- Your password changed successfully --");
                            } catch (DBUtilException e) {
                                oStream.println("-- An error has occurred while changing your password --");
                            }
                        } else oStream.println("-- Password mismatch --");
                    } else oStream.println("-- Password should be 8 characters or more --");
                }
            }
        }
        catch (DBUtilException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void kickUser(String username) {
        try (DBUtil db = new DBUtil()) {
            if (db.isRegisteredUser(clientID)) {
                if (username.equals(clientID)) oStream.println("-- Cannot kick yourself. Use /part instead! --");
                else {
                    ClientHandler target = clients.get(username);
                    if (target != null) {
                        server.serverBroadcast("-- " + clientID + " kicked " + username + " from server --");
                        target.disconnect();
                    } else oStream.println("-- Nickname not found --");
                }
            } else oStream.println("-- You don't have privileges to kick user! --\n" +
                        "-- Only registered users can kick other user --");
        }
        catch (IOException | DBUtilException e) {
            System.out.println("An error has occurred\n" + e.getMessage());
        }
    }

    private void sendMessageTo(String message, String recipient) {
        if (recipient.equals(clientID)) return;
        ClientHandler temp = clients.get(recipient);
        if (temp != null) {
            temp.getOStream().println("[" + clientID + "]: " + message);
        }
        else oStream.println("-- Can't find user '" + recipient + "' --");
    }

    private void broadcastMessage(String message) {
        clients.forEach((k, v) -> v.getOStream().println("[" + clientID + "]: " + message));
    }

    private boolean auth(String name) {
        oStream.println("/auth");
        try (DBUtil db = new DBUtil()) {
            String password = iStream.nextLine();

            if (!db.authenticateUser(name, password)) {
                oStream.println("-- Wrong password! --");
                return false;
            }
            return true;
        }
        catch (NoSuchElementException | IOException | DBUtilException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private void renameUser(String newID) {
        if (!clients.containsKey(newID)) {
            try (DBUtil db = new DBUtil()) {
                if (db.isRegisteredUser(newID)) {
                    if (!auth(newID)) {
                        oStream.println("-- Unable to rename: Wrong password! --");
                        return;
                    }
                }
            } catch (DBUtilException | IOException e) {
                System.out.println(e.getMessage());
            }
            clients.put(newID, clients.remove(clientID));
            server.serverBroadcast("-- '" + clientID + "' changed the name to '" + newID + "' --");
            server.updateUserList();
            clientID = newID;
        }
        else oStream.println("-- User '" + newID + "' already exist --");
    }
}
