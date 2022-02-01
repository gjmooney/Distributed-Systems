import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class Server {
    private String payloadFromClient;
    private String status;
    private String charChoice;
    private int imgChoice;
    private int state;
    private int characterIndex;
    private GameLogic gameLogic;
    private String prevImage;
    private LocalTime timeReceived;
    private String playerName;
    private boolean firstTime;
    private boolean clientPlaying;

    public Server() {
        this.setState(0);
        this.setPayloadFromClient("");
        this.status = "";
        this.gameLogic = new GameLogic();
        this.firstTime = true;
    }

    public void run(String[] args) {
        System.out.println("[RUN]" + Arrays.toString(args));
        DatagramSocket serverSock = null;
        int port = 9000;

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
            serverSock = new DatagramSocket(port);
            setState(1);
            boolean keepGoing = true;
            while (keepGoing) {
                try {

                    System.out.println("Waiting for client");
                    clientPlaying = true;

                    while (clientPlaying) {
                        NetworkUtils.Tuple messageTuple = NetworkUtils.Receive(serverSock);
                        JSONObject message = JsonUtils.fromByteArray(messageTuple.Payload);
                        JSONObject sendToClient = null;
                        receiveFromClient(message);
                        sendToClient = createResponse(getState(), getPayloadFromClient());
                        byte[] output = JsonUtils.toByteArray(sendToClient);
                        NetworkUtils.Send(serverSock, messageTuple.Address, messageTuple.Port, output);
                    }
                    keepGoing = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Client disconnected");
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

    public void receiveFromClient(JSONObject jsonData) {
        try {

            JSONObject headerJSON = (JSONObject) jsonData.get("header");
            JSONObject payloadJSON = (JSONObject) jsonData.get("payload");
            Map header = headerJSON.toMap();
            Map payload = payloadJSON.toMap();
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
        String imageToSend;
        String userEntry = textFromClient.toLowerCase(Locale.ROOT);
        switch (state) {
            case 1:
                // Ask clients name
                imageToSend = encodeImage("hi");

                // NOTE: Not text from client here but whatever
                objectToSend = createJSONObject(true, state, imageToSend, "HEY!! What's your name!?");
                setState(getState() + 1);
                break;
            case 2:
                // Greet client by name
                //add player name to list for leaderboard if they're not already added
                if (firstTime) {
                    playerName = userEntry;
                }
                String reply = "Hello " + playerName + ". Check out this cute lil' piggie!" +
                        "\nThat's it, enter quit to exit.";
                imageToSend = encodeImage("cute");
                objectToSend = createJSONObject(true, state, imageToSend, reply);
                setState(getState() + 1);
                firstTime = false;

                break;
            case 3:
                if (userEntry.equals("quit")) {
                    String response = "OKAY BYE BYE!";
                    imageToSend = encodeImage("hi");
                    objectToSend = createJSONObject(true, 5, imageToSend, response);
                    clientPlaying = false;
                    resetGame();
                } else {
                    String response = "Sorry I didn't understand that. Please enter quit to quit";
                    imageToSend = encodeImage("question");
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

    public void resetGame() {
        firstTime = true;
        setState(1);
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

    public String encodeImage(String imageType) throws IOException {
        String encodedImage;
        File file = null;
        if (imageType.equals("hi")) {
            file = new File("src/main/hi.png");

        } else if (imageType.equals("cute")) {
            file = new File("src/main/cutePiggie.jpg");

        }
        else if (imageType.equals("question")) {
            file = new File("src/main/questions.jpg");
        }
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return "noPic";
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
        return "noPic";
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getState() {
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
