import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class Server {

    public static void main(String[] args) {
        DatagramSocket serverSocket = null;

        int port = 9000;
        int state = 1;

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
            serverSocket = new DatagramSocket(9000);
            // NOTE: SINGLE-THREADED, only one connection at a time
            while (true) {
                try {
                    while (true) {
                        NetworkUtils.Tuple messageTuple = NetworkUtils.Receive(serverSocket);
                        JSONObject message = JsonUtils.fromByteArray(messageTuple.Payload);
                        JSONObject returnMessage;
                        if (message.get("type").equals("acknowledge")) {
                            returnMessage = namePrompt();
                        } else if (message.get("type").equals("nameResponse")) {
                            returnMessage = sendImage();
                        } else {
                            returnMessage = error("Invalid message received");
                        }
                       /* if (message.has("selected")) {
                            if (message.get("selected") instanceof Long || message.get("selected") instanceof Integer) {
                                int choice = message.getInt("selected");
                                switch (choice) {
                                    case (1):
                                        returnMessage = namePrompt();
                                        break;
                                    case (2):
                                        returnMessage = image();
                                        break;
                                    default:
                                        returnMessage = error("Invalid selection: " + choice + " is not an option");
                                }
                            } else {
                                returnMessage = error("Selection must be an integer");
                            }
                        } else {
                            returnMessage = error("Invalid message received");
                        }*/

                        byte[] output = JsonUtils.toByteArray(returnMessage);
                        NetworkUtils.Send(serverSocket, messageTuple.Address, messageTuple.Port, output);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                System.out.println("Closing socket");
                serverSocket.close();
            }
        }
    }

    public static JSONObject namePrompt() {
        JSONObject json = new JSONObject();
        json.put("datatype", 1);
        json.put("type", "namePrompt");
        json.put("data", "HEY! What's your name!?.");

        return json;
    }

    public static JSONObject sendImage() throws IOException {
        JSONObject json = new JSONObject();
        json.put("datatype", 2);

        json.put("type", "image");

        File file = new File("img/To-Funny-For-Words1.png");
        if (!file.exists()) {
            System.err.println("Cannot find file: " + file.getAbsolutePath());
            System.exit(-1);
        }
        // Read in image
        BufferedImage img = ImageIO.read(file);
        byte[] bytes = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            bytes = out.toByteArray();
        }
        if (bytes != null) {
            Base64.Encoder encoder = Base64.getEncoder();
            json.put("data", encoder.encodeToString(bytes));
            return json;
        }
        return error("Unable to save image to byte array");
    }

    public static JSONObject error(String err) {
        JSONObject json = new JSONObject();
        json.put("error", err);
        return json;
    }

    static JSONObject createMessage(String type) {
        JSONObject message = new JSONObject();
        if (type.equals("name")) {
            message.put("text", "HEY! What's your name!?");
        } else if (type.equals("broke")) {
            message.put("text", "we ded");
        }
        return message;
    }
}
