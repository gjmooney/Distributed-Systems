import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

    public static String receiveFromServer(ObjectInputStream in) {
        try {
            String jsonData = (String) in.readObject();
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            JSONObject fromServer = new JSONObject(jsonTokener);
            String type = (String) fromServer.get("type");
            String message = "";
            if (type.equals("name")) {
                message = (String) fromServer.get("message");
            } else if (type.equals("greeting")) {
                message = (String) fromServer.get("message");
            } else if (type.equals("creditResponse")) {
                message = "NOT BROK";
            }

            System.out.println("Receiving");
            System.out.println(fromServer);
            return message;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return "ERROR ERROR ERROR";
    }

    public static JSONObject buildNameRequest(String name) {
        JSONObject json = new JSONObject();
        json.put("type", "name");
        json.put("name", name);
        return json;
    }

    public static JSONObject buildCreditRequest() {
        Scanner input = new Scanner(System.in);
        System.out.println("How much credit would you like?");
        double amount = Double.parseDouble(input.nextLine());
        JSONObject creditRequest = new JSONObject();
        creditRequest.put("type", "credit");
        creditRequest.put("amount", amount);

        return creditRequest;
    }

    public static void main(String[] args) {
        Socket leaderSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        int port = 8000; // default

        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        try {
            leaderSocket = new Socket(host, port);
            out = new ObjectOutputStream(leaderSocket.getOutputStream());
            in = new ObjectInputStream(leaderSocket.getInputStream());

            //receive name prompt from server
            String message;
            int choice;
            Scanner input = new Scanner(System.in);
            message = receiveFromServer(in);
            System.out.println(message);
            String name = input.nextLine();
            JSONObject sendToServer = buildNameRequest(name);
            out.writeObject(sendToServer.toString());
            // get greeting from server
            message = receiveFromServer(in);
            System.out.println(message);


            do {
                System.out.println();
                System.out.println("Client Menu");
                System.out.println("Please select a valid option (1, 2, or 3).");
                System.out.println("1. Get Credit");
                System.out.println("2. Pay Back Credit");
                System.out.println("3. Quit");
                System.out.println();

                try {
                    choice = input.nextInt();
                    switch (choice) {
                        case 1:
                            sendToServer = buildCreditRequest();
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                        default:
                            break;
                    }
                    out.writeObject(sendToServer.toString());
                    System.out.println(receiveFromServer(in));

                } catch (InputMismatchException e) {
                    input = new Scanner(System.in);
                    System.out.println("Please Enter a valid selection (1, 2, or 3");
                }
            } while (true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
