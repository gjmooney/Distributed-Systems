package server;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import javax.swing.*;
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
    public JSONObject createResponse(int state, String textFromClient) throws IOException {
        JSONObject objectToSend = new JSONObject();
        JSONObject objHeader = new JSONObject();
        JSONObject objPayload = new JSONObject();
        String imageToSend;
        switch (state) {
            case 1:
                // Ask clients name
                imageToSend = encodeImage("hi");

                // NOTE: Not text from client here but whatever
                objectToSend = createJSONObject(true, state, imageToSend, textFromClient);
                setState(getState() + 1);
                break;
            case 2:
                // Greet client by name
                String reply = "Hello " + textFromClient + ". Do you want to see the " +
                           "leaderboard or start the game?";
                imageToSend = encodeImage("hi");
                objectToSend = createJSONObject(true, state, imageToSend, reply);
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
                    String response = "Who said the quote?";
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                    setState(getState() + 1);
                    break;
                } else if (getPayloadFromClient().equals("leader")) {
                    imageToSend = encodeImage("question");
                    String response = "Here's the leaderboard";
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                    setState(6);
                    break;
                } else {
                    String response = "Sorry I didn't understand that. Please enter start or leader";
                    imageToSend = encodeImage("question");
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                    break;
                }
            case 4:
                //actual gameplay -- server expects a name, more, or next
                if (getPayloadFromClient().equals("more")) {
                    //get another quote form the same character
                    String response;
                    int quoteNum = gameLogic.getQuoteNumber();
                    if (quoteNum < 4) {
                        response = "Here's another quote from that character";
                        imageToSend = encodeImage("more");
                        gameLogic.setNumberOfGuesses(gameLogic.getNumberOfGuesses() + 1);
                    } else {
                        response = "This is the last quote!\nThe character is in the picture," +
                                " you can do it!";
                        imageToSend = getPrevImage();
                    }
                    objPayload.put("image", imageToSend);
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                } else if (getPayloadFromClient().equals("next")) {
                    imageToSend = encodeImage("quote");
                    String response = "Okay! Here's another one!";
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                } else {
                    // check if the answer is right and set the boolean in gameLogic
                    gameLogic.checkAnswer(textFromClient);

                    //TODO can combine these ifs but thats maybe bad actually
                    String response;
                    if (!gameLogic.isGameOver()) {
                        if (gameLogic.isGuessWasCorrect()) {
                            //correct answer
                            response =  "You got it right!";
                        } else {
                            //wrong answer
                            response = "NOPE! you got it wrong!\nGuess again!";
                        }
                    } else if (gameLogic.getCorrectGuesses() == 3) {
                        response = "You won!!!!";
                    } else {
                        response = "Sorry, you lose";
                    }

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

                    objectToSend = createJSONObject(true, state, imageToSend, response);
                }
                break;

            default:
                imageToSend = encodeImage("question");
                String response = "ERROR ERROR EROOR";
                objectToSend = createJSONObject(false, state, imageToSend, response);
                setState(7);
                break;
        }
        return objectToSend;
    }

    public JSONObject createJSONObject(boolean ok, int state, String image, String text) {
        JSONObject header = new JSONObject();
        JSONObject payload = new JSONObject();
        JSONObject JSONToSend = new JSONObject();

        header.put("state", state);
        header.put("type", "JSON");
        header.put("ok", ok);

        payload.put("image", image);
        if (state == 1 || state == 2) {
            payload.put("score", 0);
        } else {
            payload.put("score", gameLogic.getScore());
        }
        payload.put("text", text);

        JSONToSend.put("header", header);
        JSONToSend.put("payload", payload);

        return JSONToSend;
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
