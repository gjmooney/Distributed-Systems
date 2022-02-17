import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {
    private static double credit;


    public static JSONObject receiveFromServer(ObjectInputStream in) {
        try {
            String jsonData = (String) in.readObject();
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            JSONObject fromServer = new JSONObject(jsonTokener);
            return fromServer;

        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            System.out.println("Can't communicate with leader\nShutting down...");
            System.exit(0);
        }
        return error();
    }

    public static JSONObject error() {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        return error;
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
        String amount = input.nextLine();
        JSONObject creditRequest = new JSONObject();
        creditRequest.put("type", "credit");
        creditRequest.put("amount", amount);

        return creditRequest;
    }

    public static JSONObject buildPaybackRequest() {
        Scanner input = new Scanner(System.in);
        System.out.println("You currently owe $" + credit
                            + "\nHow much would you like to pay back?");
        String amount = input.nextLine();
        JSONObject paybackRequest = new JSONObject();
        paybackRequest.put("type", "payback");
        paybackRequest.put("amount", amount);

        return paybackRequest;
    }

    public static void handleResponse(JSONObject response) {
        String type = (String) response.get("type");
        String message = "";
        if (type.equals("name")) {
            message = (String) response.get("message");

        } else if (type.equals("greeting")) {
            message = (String) response.get("message");
            credit = response.getDouble("credit");

        } else if (type.equals("creditResponse")) {
            if ((boolean) response.get("approved")) {
                credit = response.getDouble("credit");
                message = "Congratulations! You're credit request for $"
                        + response.get("amount") + " has been approved!"
                        + "\nYou now have a total of $" + response.get("credit")
                        + " in credit";
            } else {
                message = "We're sorry, your request has been denied."
                        + "\nYou currently have a total of $" + response.get("credit")
                        + " in credit";
            }
        } else if (type.equals("paybackResponse")) {
            if (response.getBoolean("approved")) {
                credit = response.getDouble("credit");
                message = "Congratulations! You have paid back $"
                        + response.get("amount") + "!"
                        + "\nYou now have a total of $" + response.get("credit")
                        + " in credit";
            } else  {
                message = "Sorry, you can't pay back more than you owe"
                        + "\nThat would be ridiculous!";
            }
        }
        System.out.println(response);
        System.out.println(message);

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
            JSONObject response = receiveFromServer(in);
            handleResponse(response);
            String name = input.nextLine();
            JSONObject sendToServer = buildNameRequest(name);
            out.writeObject(sendToServer.toString());
            // get greeting from server
            response = receiveFromServer(in);
            handleResponse(response);


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
                            sendToServer = buildPaybackRequest();
                            break;
                        case 3:
                            System.out.println("Bye bye!");
                            in.close();
                            out.close();
                            leaderSocket.close();
                            System.exit(0);
                            break;
                        default:
                            System.out.println("How did this happen?");
                            break;
                    }
                    out.writeObject(sendToServer.toString());
                    response = receiveFromServer(in);

                    System.out.println("checkpoint");
                    handleResponse(response);

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
