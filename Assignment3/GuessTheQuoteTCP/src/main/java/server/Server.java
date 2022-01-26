package server;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;


public class Server {
    private String dataFromClient;
    private int state;

    public Server() {
        this.setState(0);
        this.setDataFromClient(null);
    }

    public String getDataFromClient() {
        return dataFromClient;
    }

    public void setDataFromClient(String dataFromClient) {
        this.dataFromClient = dataFromClient;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void run(String[] args) throws IOException {
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
            //TODO set state to 1
            while (true) {
                System.out.println("COUNT THIS");
                clientSocket = null;
                try {
                    JSONObject sendToClient = null;
                    System.out.println("Waiting for client to connect");
                    clientSocket = serverSock.accept();
                    System.out.println("Server accepted socket");
                    inputStream = new ObjectInputStream(clientSocket.getInputStream());
                    outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                    // ask client for name after first connection
                    sendToClient = createResponse(1, "Hey! What's your name!?");
                    outputStream.writeObject(sendToClient.toString());

                    while (true) {
                        //TODO increment state for every call
                        Object fromClient = inputStream.readObject();
                        receiveFromClient((String) fromClient);
                        //TODO add extra logic in create response for state 3, maybe increment in createResponse
                        sendToClient = createResponse(2, getDataFromClient());
                        outputStream.writeObject(sendToClient.toString());

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

    public void receiveFromClient(String jsonData) {
        System.out.println("JSON server receive: " + jsonData);

        try {
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            JSONObject namePrompt = new JSONObject(jsonTokener);
            JSONObject headerJSON = (JSONObject) namePrompt.get("header");
            JSONObject payloadJSON = (JSONObject) namePrompt.get("payload");
            Map header = headerJSON.toMap();
            Map payload = payloadJSON.toMap();
//            int state = (int) header.get("state");
//
//            switch (state) {
//                case 1:
//                    String reply = "Hello " + payload.get("text") + ". Do you want to see the " +
//                            "leaderboard or start the game?";
//                    createResponse(2, reply);
//                    break;
//                default:
//                    createResponse(5, "ERROR");
//            }
            setState((int) header.get("state"));
            setDataFromClient((String) payload.get("text"));


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error receiving from server");
        }


    }

    //send a response to the client
    public static JSONObject createResponse(int state, String message) {
        JSONObject objectToSend = new JSONObject();
        JSONObject objHeader = new JSONObject();
        JSONObject objPayload = new JSONObject();
        switch (state) {
            case 1:
                objHeader.put("state", state);
                objHeader.put("type", "text");
                objHeader.put("ok", true);
                objPayload.put("text", message);
                break;
            case 2:
                String reply = "Hello " + message + ". Do you want to see the " +
                           "leaderboard or start the game?";
                objHeader.put("state", state);
                objHeader.put("type", "text");
                objHeader.put("ok", true);
                objPayload.put("text", reply);
                break;
            default:
                objHeader.put("state", state);
                objHeader.put("type", "text");
                objHeader.put("ok", false);
                objPayload.put("text", "ERROR ERROR ERROR");
                break;
        }
        objectToSend.put("header", objHeader);
        objectToSend.put("payload", objPayload);
        System.out.println("JSON on server" + objectToSend + "\n");
        return objectToSend;
    }

    public static void main(String[] args) throws IOException {
        Server mainServer = new Server();
        try {
            mainServer.run(args);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Yo your server broke bruh");
            System.exit(1);
        }
    }
}
