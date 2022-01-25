package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        int count = 0;
        ServerSocket serverSock = null;
        ObjectInputStream inputStream = null;
        ObjectOutputStream outputStream = null;
        Socket clientSocket = null;
        int port = 8080;
        int sleepDelay = 10000;

        if (args.length > 1) {
            System.out.println("Expected one argument: <port(int)>");
            System.exit(1);
        }
        System.out.println("Establishing connection on port: "  + args[0]);

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port number must be an integer");
            System.exit(2);
        }

        try {
            serverSock = new ServerSocket(port);
            while (true) {
                System.out.println("COUNT THIS");
                clientSocket = null;
                try {
                    System.out.println("Waiting for client to connect");
                    clientSocket = serverSock.accept();
                    System.out.println("Server accepted socket");
                    inputStream = new ObjectInputStream(clientSocket.getInputStream());
                    outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                    while (true) {
                        Object fromClient = inputStream.readObject();
                        System.out.println(fromClient);
                        String reply = "ACKNOWLEDGE";
                        outputStream.writeObject(reply);
                        System.out.println("SERVER: END OF WHILE");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Client disconnected");
                } finally {
                    if (clientSocket == null) {
                        System.out.println("Closing client socket");
                        clientSocket.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not establish connection over port: " + port);
            System.exit(2);
        } finally {
            if (serverSock != null) {
                System.out.println("Closing server socket");
                serverSock.close();
            }
        }


    }
}
