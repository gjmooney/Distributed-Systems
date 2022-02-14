import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Node {
    private static JSONObject clientList;
    private static double money;

    public static JSONObject receiveFromLeader(ObjectInputStream in) {
        try {
            String jsonData = (String) in.readObject();
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            JSONObject fromLeader = new JSONObject(jsonTokener);
            return fromLeader;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return error();
    }

    public static JSONObject error() {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        return error;
    }


    public static JSONObject vote(JSONObject request) {
        String vote = "";
        System.out.println("VOTING");
        if (clientList.has((String) request.get("name"))) {
            returningClient();
        } else {
            vote = newClient(request);
        }

        JSONObject responseToLeader = new JSONObject();
        responseToLeader.put("type", "vote");
        responseToLeader.put("vote", vote);

        System.out.println("Receiving");
        System.out.println(request);

        return responseToLeader;
    }

    public static void returningClient() {

    }

    public static String newClient(JSONObject clientRequest) {
        int amountNeeded = ((int) clientRequest.get("amount"));
        if (money >= amountNeeded) {
            return "yes";
        } else {
            return "no";
        }
    }

    public static void main(String[] args) {
        Socket leaderSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        int port = 8000; // default
        money = 1000;
        String host = "localhost";
        clientList = new JSONObject();

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <money(int)>");
            System.exit(1);
        }
        try {
            port = Integer.parseInt(args[0]);
            money = Double.parseDouble(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|Money] must be integer");
            System.exit(2);
        }
        System.out.println("NODE PRT: " + port);
        System.out.println("NODE MONEY " + money);

        try {
            leaderSocket = new Socket(host, port);
            System.out.println("Node connected");
            out = new ObjectOutputStream(leaderSocket.getOutputStream());
            in = new ObjectInputStream(leaderSocket.getInputStream());
            JSONObject request = new JSONObject();

            while (true) {
                JSONObject response = new JSONObject();
                request = receiveFromLeader(in);

                if (request.get("type").equals("credit")) {
                    response = vote(request);
                }

                out.writeObject(response.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
