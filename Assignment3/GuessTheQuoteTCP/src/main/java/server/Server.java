package server;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;


public class Server {
    private String payloadFromClient;
    private String status;
    private int state;
    boolean gameOver = false;

    public Server() {
        this.setState(0);
        this.setPayloadFromClient("");
        this.status = "";
    }

    public String getPayloadFromClient() {
        return payloadFromClient;
    }

    public void setPayloadFromClient(String payloadFromClient) {
        this.payloadFromClient = payloadFromClient;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getState() {
        System.out.println("STATE: " + state);
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
            setState(1);
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
                    sendToClient = createResponse(getState(), "Hey! What's your name!?");
                    outputStream.writeObject(sendToClient.toString());

                    while (true) {
                        //TODO increment state for every call
                        Object fromClient = inputStream.readObject();
                        receiveFromClient((String) fromClient);
                        //TODO add extra logic in create response for state 3, maybe increment in createResponse
                        sendToClient = createResponse(getState(), getPayloadFromClient());
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
            //setState((int) header.get("state"));
            String reply = ((String) payload.get("text"));
            setPayloadFromClient(reply.toLowerCase(Locale.ROOT));


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error receiving from server");
        }


    }

    //send a response to the client
    public JSONObject createResponse(int state, String message) throws IOException {
        JSONObject objectToSend = new JSONObject();
        JSONObject objHeader = new JSONObject();
        JSONObject objPayload = new JSONObject();
        String imageToSend;
        switch (state) {
            case 1:
                // Ask clients name
                imageToSend = encodeImage("hi");
                objHeader.put("state", state);
                objHeader.put("type", "text");
                objHeader.put("ok", true);
                objPayload.put("text", message);
                objPayload.put("image", imageToSend);
                setState(getState() + 1);
                break;
            case 2:
                // Greet client by name
                String reply = "Hello " + message + ". Do you want to see the " +
                           "leaderboard or start the game?";
                objHeader.put("state", state);
                objHeader.put("type", "text");
                objHeader.put("ok", true);
                objPayload.put("text", reply);
                setState(getState() + 1);
                break;
            case 3:
                // Ask if client wants to see the leaderboard or start the game
                if (getPayloadFromClient().equals("start")) {
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", true);
                    objPayload.put("text", "Who said the quote?");
                    setState(getState() + 1);
                    break;
                } else if (getPayloadFromClient().equals("leader")) {
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", true);
                    //objPayload.put("text", getLeaderboard());
                    setState(6);
                    break;
                } else {
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", false);
                    objPayload.put("text", "Sorry I didn't understand that. Please enter start or leader");
                    break;
                }
            case 4:
                //actual gameplay -- server expects a name, more, or next
                //gameLogic();
                break;

            default:
                objHeader.put("state", state);
                objHeader.put("type", "text");
                objHeader.put("ok", false);
                objPayload.put("text", "ERROR ERROR ERROR");
                setState(7);
                break;
        }
        objectToSend.put("header", objHeader);
        objectToSend.put("payload", objPayload);
        System.out.println("JSON on server" + objectToSend + "\n");


        return objectToSend;
    }

    public void gameLogic() {

    }

    public String encodeImage(String imageType) throws IOException {
        String encodedImage;
        File file;
        if (imageType.equals("hi")) {
            file = new File("src/main/resources/img/hi.png");

        } else {
            file = new File("/nope");
        }
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return "File not found";
        }
        BufferedImage image = ImageIO.read(file);
        byte[] bytes = null;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            bytes = output.toByteArray();
        }
        if (bytes != null) {
            Base64.Encoder encoder = Base64.getEncoder();
            encodedImage = encoder.encodeToString(bytes);
            return encodedImage;
        }
        return "Unable to encode image";
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
