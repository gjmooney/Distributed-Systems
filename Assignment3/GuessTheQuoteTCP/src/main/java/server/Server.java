package server;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.*;


public class Server {
    private String payloadFromClient;
    private int state;
    private int characterIndex;
    private GameLogic gameLogic;
    private String prevImage;
    private LocalTime timeLimit;
    private LocalTime timeReceived;
    private String playerName;
    private boolean firstTime;
    private boolean clientPlaying;

    public Server() {
        this.setState(0);
        this.setPayloadFromClient("");
        this.gameLogic = new GameLogic();
        this.firstTime = true;
    }

    public void run(String[] args) throws IOException {
        ServerSocket serverSock = null;
        ObjectInputStream inputStream = null;
        ObjectOutputStream outputStream = null;
        Socket clientSocket;
        int port = 8080;

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
                    JSONObject sendToClient;
                    System.out.println("Waiting for client to connect");
                    clientSocket = serverSock.accept();
                    clientPlaying = true;
                    System.out.println("Server accepted socket");
                    inputStream = new ObjectInputStream(clientSocket.getInputStream());
                    outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                    // ask client for name after first connection
                    sendToClient = createResponse(getState(), "Hey! What's your name!?");
                    outputStream.writeObject(sendToClient.toString());

                    while (clientPlaying) {
                        Object fromClient = inputStream.readObject();
                        receiveFromClient((String) fromClient);
                        sendToClient = createResponse(getState(), getPayloadFromClient());
                        outputStream.writeObject(sendToClient.toString());
                        outputStream.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (clientSocket != null) {
                        System.out.println("Closing client socket");
                        setState(1);
                        inputStream.close();
                        outputStream.close();
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
            Map<String, Object> header = headerJSON.toMap();
            Map<String, Object> payload = payloadJSON.toMap();
            setPayloadFromClient(((String) payload.get("text")));
            timeReceived = LocalTime.now();
            System.out.println("[RECEIVE FROM CLIENT] " + getPayloadFromClient());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error receiving from client");
        }
    }

    //create a response to send to the client
    public JSONObject createResponse(int state, String textFromClient) throws IOException {
        JSONObject objectToSend;
        JSONObject objHeader = new JSONObject();
        JSONObject objPayload = new JSONObject();
        String imageToSend;
        String userEntry = textFromClient.toLowerCase(Locale.ROOT).trim();
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
                // add player name to list for leaderboard if they're not already added
                if (userEntry.equals("quit")) {
                    String response = "OKAY BYE BYE!";
                    imageToSend = encodeImage("hi");
                    objectToSend = createJSONObject(true, 5, imageToSend, response);
                    clientPlaying = false;
                } else {
                    if (firstTime) {
                        playerName = userEntry;
                    }
                    String reply = "Hello " + playerName + ". If you want to see the leaderboard " +
                            "enter leader or enter start to start the game!";
                    imageToSend = encodeImage("hi");
                    objectToSend = createJSONObject(true, state, imageToSend, reply);
                    setState(getState() + 1);
                    firstTime = false;
                }
                break;
            case 3:
                if (userEntry.equals("start")) {
                    imageToSend = encodeImage("quote");
                    String response = "Who said the quote? " +
                            "\n[Enter more for another quote from this character]" +
                            "\n[Enter next to see quotes from a different character]";
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                    timeLimit = LocalTime.now().plusMinutes(1);
                    setState(getState() + 1);
                    break;
                } else if (userEntry.equals("leader")) {
                    imageToSend = encodeImage("question");
                    String response = gameLogic.displayLeaderboard();
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                    setState(2);
                    break;
                } else {
                    String response = "Sorry I didn't understand that. Please enter start or leader";
                    imageToSend = encodeImage("question");
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                    break;
                }
            case 4:
                //actual gameplay -- server expects a name, more, or next
                gameLogic.checkTimer(timeLimit, timeReceived);

                if (!gameLogic.isGameOver()) {
                    if (userEntry.equals("more")) {
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

                    } else if (userEntry.equals("next")) {
                        imageToSend = encodeImage("quote");
                        String response = "Okay! Here's another one! Also, you just lost 2 points";
                        gameLogic.setScore(gameLogic.getScore() - 2);
                        objectToSend = createJSONObject(true, state, imageToSend, response);

                    } else {
                        // check if the answer is right and set the boolean in gameLogic
                        gameLogic.checkAnswer(userEntry);

                        String response;
                        String responseTail = "\nYou finished with " + gameLogic.getScore() + " points." +
                                "\nEnter your name to play again, or quit to stop playing.";

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
                            response += responseTail;
                            gameLogic.updateLeaderboard(playerName);
                        } else {
                            response = "Sorry, you lose";
                            response += responseTail;
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
                            resetGame();
                        } else {
                            imageToSend = encodeImage("lose");
                            resetGame();
                        }

                        objectToSend = createJSONObject(true, state, imageToSend, response);
                    }
                } else {
                    //ran out of time
                    imageToSend = encodeImage("lose");
                    String response = "Sorry, you ran out of time!\nYou finished with "
                            + gameLogic.getScore() + " points." +
                            "\nEnter your name to play again, or quit to stop playing.";
                    objectToSend = createJSONObject(true, state, imageToSend, response);
                    resetGame();
                }
                break;
            default:
                imageToSend = encodeImage("question");
                String response = "ERROR ERROR ERROR";
                objectToSend = createJSONObject(false, state, imageToSend, response);
                setState(7);
                break;
        }
        return objectToSend;
    }

    public void resetGame() {
        firstTime = true;
        setState(2);
        gameLogic.resetGame();
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

    // Adapted from AdvancedCustomProtocol example
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
            return "unableToEncode";
        }
        BufferedImage image = ImageIO.read(file);
        byte[] bytes;
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
        return "unableToEncode";
    }

    public static void main(String[] args) {
        Server mainServer = new Server();
        try {
            mainServer.run(args);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("The server broke");
            System.exit(1);
        }
    }

    public String getPayloadFromClient() {
        return payloadFromClient;
    }

    public void setPayloadFromClient(String payloadFromClient) {
        this.payloadFromClient = payloadFromClient;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getPrevImage() {
        return prevImage;
    }

    public void setPrevImage(String prevImage) {
        this.prevImage = prevImage;
    }
}
