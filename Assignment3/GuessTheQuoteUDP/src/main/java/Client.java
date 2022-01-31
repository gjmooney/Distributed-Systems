import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Scanner;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import javax.swing.*;

public class Client {
    static int state = 1;


    public static void main(String[] args) {
        DatagramSocket socket;
        int port = 9000;

        if (args.length != 2) {
            System.out.println("Expected two arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Port number must be an integer");
            System.exit(2);
        }

        try {
            InetAddress address = InetAddress.getByName("localhost");
            System.out.println("HSOT " + host);
            socket = new DatagramSocket();

            Scanner input = new Scanner(System.in);
            String choice;
            System.out.println("Enter anything to start");
            do {
                choice = input.next(); // what if not int? .. should error handle this
                JSONObject request = null;
                switch (state) {
                    case (1):
                        request = acknowledge();
                        break;
                    case (2):
                        request = sendName();
                        break;
                    case (5):
                        System.out.println("Jokes on you, I decided I do not like num 5: https://gph.is/g/a99OP09");
                        break;
                    default:
                        System.out.println("Please select a valid option (1-5).");
                        break;
                }

                if (request != null) {
                    NetworkUtils.Send(socket, address, port, JsonUtils.toByteArray(request));
                    NetworkUtils.Tuple responseTuple = NetworkUtils.Receive(socket);
                    JSONObject response = JsonUtils.fromByteArray(responseTuple.Payload);
                    if (response.has("error")) {
                        System.out.println(response.getString("error"));
                    } else /*if (response.get("type").equals("namePrompt"))*/ {
                        //System.out.println(response.get("data"));

                        switch (response.getInt("datatype")) {
                            case (1):
                                System.out.println("Your " + response.getString("type"));
                                System.out.println(response.getString("data"));
                                break;
                            case (2): {
                                System.out.println("Your image");
                                Base64.Decoder decoder = Base64.getDecoder();
                                byte[] bytes = decoder.decode(response.getString("data"));
                                ImageIcon icon = null;
                                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                                    BufferedImage image = ImageIO.read(bais);
                                    icon = new ImageIcon(image);
                                }
                                if (icon != null) {
                                    JFrame frame = new JFrame();
                                    JLabel label = new JLabel();
                                    label.setIcon(icon);
                                    frame.add(label);
                                    frame.setSize(icon.getIconWidth(), icon.getIconHeight());
                                    frame.show();
                                }
                            }
                            break;
                        }
                    }
                }
            } while (true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Tells server there is a client waiting
    public static JSONObject acknowledge() {
        JSONObject request = new JSONObject();
        request.put("type", "acknowledge");
        state = 2;
        return request;
    }

    public static JSONObject sendName() {
        JSONObject request = new JSONObject();
        request.put("type", "nameResponse");
        return request;
    }

    public static JSONObject image() {
        JSONObject request = new JSONObject();
        request.put("selected", 3);
        return request;
    }
}
