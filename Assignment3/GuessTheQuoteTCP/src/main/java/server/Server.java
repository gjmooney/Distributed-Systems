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
import java.util.Random;


public class Server {
    private String payloadFromClient;
    private String status;
    private String charChoice;
    private int imgChoice;
    private int state;
    private int characterIndex;
    boolean gameOver = false;
    GameLogic gameLogic = null;
    private String prevImage;

    public Server() {
        this.setState(0);
        this.setPayloadFromClient("");
        this.status = "";
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
                        Object fromClient = inputStream.readObject();
                        receiveFromClient((String) fromClient);
                        sendToClient = createResponse(getState(), getPayloadFromClient());
                        outputStream.writeObject(sendToClient.toString());
                        outputStream.flush();
                        //TODO flush the out? idk

                        System.out.println("SERVER: END OF WHILE");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Client disconnected");
                } finally {
                    if (clientSocket != null) {
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
        try {
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            JSONObject namePrompt = new JSONObject(jsonTokener);
            JSONObject headerJSON = (JSONObject) namePrompt.get("header");
            JSONObject payloadJSON = (JSONObject) namePrompt.get("payload");
            Map header = headerJSON.toMap();
            Map payload = payloadJSON.toMap();
            //setState((int) header.get("state"));
            String reply = ((String) payload.get("text"));
            setPayloadFromClient(reply);
            System.out.println("[RECEIVE FROM CLIENT] " + getPayloadFromClient());


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error receiving from client");
        }
    }

    //create a response to send to the client
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
                objPayload.put("score", 0);
                setState(getState() + 1);
                break;
            case 2:
                // Greet client by name
                imageToSend = encodeImage("hi");
                String reply = "Hello " + message + ". Do you want to see the " +
                           "leaderboard or start the game?";
                objHeader.put("state", state);
                objHeader.put("type", "text");
                objHeader.put("ok", true);
                objPayload.put("text", reply);
                objPayload.put("image", imageToSend);
                objPayload.put("score", 0);
                setState(getState() + 1);
                break;
            case 3:
                // Ask if client wants to see the leaderboard or start the game
                if (getPayloadFromClient().equals("start")) {
                    if (gameLogic == null) {
                        System.out.println("Server: Creating game logic");
                        gameLogic = new GameLogic();
                    }
                    imageToSend = encodeImage("quote");
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", true);
                    objPayload.put("text", "Who said the quote?");
                    objPayload.put("image", imageToSend);
                    objPayload.put("score", 0);
                    setState(getState() + 1);
                    break;
                } else if (getPayloadFromClient().equals("leader")) {
                    imageToSend = encodeImage("question");
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", true);
                    //objPayload.put("text", getLeaderboard());
                    objPayload.put("image", imageToSend);
                    objPayload.put("score", gameLogic.getScore());
                    setState(6);
                    break;
                } else {
                    imageToSend = encodeImage("question");
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", false);
                    objPayload.put("text", "Sorry I didn't understand that. Please enter start or leader");
                    objPayload.put("image", imageToSend);
                    objPayload.put("score", gameLogic.getScore());
                    break;
                }
            case 4:
                //actual gameplay -- server expects a name, more, or next
                if (getPayloadFromClient().equals("more")) {
                    //get another quote form the same character
                    int quoteNum = gameLogic.getQuoteNumber();
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", true);
                    if (quoteNum < 4) {
                        objPayload.put("text", "Here's another quote from that character");
                        imageToSend = encodeImage("more");
                        gameLogic.setNumberOfGuesses(gameLogic.getNumberOfGuesses() + 1);
                    } else {
                        objPayload.put("text", "This is the last quote!\nThe character is in the picture," +
                                " you can do it!");
                        imageToSend = getPrevImage();
                    }
                    objPayload.put("score", gameLogic.getScore());
                    objPayload.put("image", imageToSend);
                } else if (getPayloadFromClient().equals("next")) {
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", true);
                    imageToSend = encodeImage("quote");
                    objPayload.put("score", gameLogic.getScore());
                    objPayload.put("text", "Okay! Here's another one!");
                    objPayload.put("image", imageToSend);
                } else {
                    // check if the answer is right and set the boolean in gameLogic
                    gameLogic.checkAnswer(message);

                    // start building JSON reply
                    objHeader.put("state", state);
                    objHeader.put("type", "text");
                    objHeader.put("ok", true);
                    objPayload = gameLogic.buildResponse(message);

                    // get a new quote if the guess was correct else use the previous quote
                    if (!gameLogic.isGameOver()) {
                        if (gameLogic.isGuessWasCorrect()) {
                            imageToSend = encodeImage("quote");

                        } else {
                            imageToSend = getPrevImage();
                        }
                    } else if (gameLogic.getCorrectGuesses() == 3) {
                        imageToSend = encodeImage("win");
                    } else {
                        imageToSend = encodeImage("lose");
                    }


                    // finish building JSON reply
                    objPayload.put("image", imageToSend);
                }
                break;

            default:
                imageToSend = encodeImage("question");
                objHeader.put("state", state);
                objHeader.put("type", "text");
                objHeader.put("ok", false);
                objPayload.put("text", "ERROR ERROR ERROR");
                objPayload.put("image", imageToSend);
                objPayload.put("score", gameLogic.getScore());
                setState(7);
                break;
        }
        objectToSend.put("header", objHeader);
        objectToSend.put("payload", objPayload);

        return objectToSend;
    }

    public String chooseCharacterAndQuote(boolean first) {
        String[] characters = {"Captain_America", "Darth_Vader", "Homer_Simpson", "Jack_Sparrow",
                                "Joker", "Tony_Stark", "Wolverine"};

        if (first) {
            Random rand = new Random();
            characterIndex = rand.nextInt(7);
        } else {
            characterIndex = (characterIndex + 1) % 7;
        }

        int quoteNumber = gameLogic.getQuoteNumber(characters[characterIndex]);

        gameLogic.saveChoices(characters[characterIndex], quoteNumber);
        String filename = "src/main/resources/img/" + characters[characterIndex]
                + "/quote" + quoteNumber + ".png";

        return filename;
    }

    public String chooseNewQuote() {
        String character = gameLogic.getQuoteCharacter();
        int quoteNumber = gameLogic.getQuoteNumber(character);
        gameLogic.saveChoices(character, quoteNumber);
        String filename = "src/main/resources/img/" + character
                + "/quote" + quoteNumber + ".png";

        return filename;
    }

    public String encodeImage(String imageType) throws IOException {
        String encodedImage;
        File file;
        if (imageType.equals("hi")) {
            file = new File("src/main/resources/img/hi.png");

        } else if(imageType.equals("question")) {
            file = new File("src/main/resources/img/questions.jpg");

        } else if(imageType.equals("quote")) {
            String filename;
            if (getState() == 3) {
                filename = chooseCharacterAndQuote(true);
            } else {
                filename = chooseCharacterAndQuote(false);
            }
            file = new File(filename);

        } else if (imageType.equals("more")) {
            String filename = chooseNewQuote();
            file = new File(filename);

        } else if (imageType.equals("win")) {
            file = new File("src/main/resources/img/win.jpg");

        } else if (imageType.equals("lose")) {
            file = new File("src/main/resources/img/lose.jpg");

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
            setPrevImage(encodedImage); // save image we just encoded
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

    public String getCharChoice() {
        return charChoice;
    }

    public void setCharChoice(String charChoice) {
        this.charChoice = charChoice;
    }

    public int getImgChoice() {
        return imgChoice;
    }

    public void setImgChoice(int imgChoice) {
        this.imgChoice = imgChoice;
    }

    public String getPrevImage() {
        return prevImage;
    }

    public void setPrevImage(String prevImage) {
        this.prevImage = prevImage;
    }
}
