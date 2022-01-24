package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
        for (int i = 0; i < args.length; i++) {
            System.out.println("Args: " + args[i]);
        }
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
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not establish connection over port: " + port);
            System.exit(2);
        }

        while (serverSock.isBound() && !serverSock.isClosed()) {
            try {
                clientSocket = serverSock.accept();
                inputStream = new ObjectInputStream(clientSocket.getInputStream());
                outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                System.out.println(inputStream.readObject());
                System.out.println("SERVER: END OF TRY");

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Client could not establish connection");
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (clientSocket != null) {
                    clientSocket.close();
                }
            }
        }
    }
}
