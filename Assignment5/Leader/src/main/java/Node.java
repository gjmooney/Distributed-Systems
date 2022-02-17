import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.Socket;

public class Node {
    private static JSONObject clientList;
    private static double money;
    private static int port;

    public static JSONObject receiveFromLeader(ObjectInputStream in) {
        try {
            String jsonData = (String) in.readObject();
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            JSONObject fromLeader = new JSONObject(jsonTokener);
            return fromLeader;
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            System.out.println("Lost connection to leader\nShutting down node...");
            System.exit(0);
        }
        return error();
    }

    public static JSONObject readLedger(String filename) {
        BufferedReader leaderboardReader = null;
        JSONTokener tokener;
        try {
            File file = new File(filename);

            leaderboardReader = new BufferedReader(new FileReader(file));
            tokener = new JSONTokener(leaderboardReader);
            return new JSONObject(tokener);
        } catch (FileNotFoundException e) {
            System.out.println("No ledger yet");
            return new JSONObject();
        } catch (JSONException e) {
            System.out.println("Ledger file is blank");
            return new JSONObject();
        }
        finally {
            if (leaderboardReader != null) {
                try {
                    leaderboardReader.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveLedger(String filename) {
        File file = new File(filename);
        FileWriter fileWriter = null;
        try {
            if (file.createNewFile()) {
                System.out.println("New ledger created");
            }
            clientList.put(String.valueOf(port), money);
            fileWriter = new FileWriter(filename);
            fileWriter.write(clientList.toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Problem saving client ledger");
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("problem closing ledger file");
            }
        }
    }

    public static JSONObject error() {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        return error;
    }


    public static JSONObject vote(JSONObject request) {
        String vote = "";
        System.out.println("VOTe " + request.get("name"));
        if (clientList.has((String) request.get("name"))) {
            System.out.println("returning client VOTING");

            vote = returningClient(request);
        } else {
            System.out.println("new client VOTING");

            vote = newClient(request);
        }

        System.out.println("VOTE: " + vote);
        JSONObject responseToLeader = new JSONObject();
        responseToLeader.put("type", "vote");
        responseToLeader.put("vote", vote);

        System.out.println(request);

        return responseToLeader;
    }

    public static String returningClient(JSONObject clientRequest) {
        double amountNeeded = Double.parseDouble((String) clientRequest.get("amount"));
        if (money >= amountNeeded) {
            return "yes";
        } else {
            return "no";
        }
    }

    public static String newClient(JSONObject clientRequest) {
        double amountNeeded = Double.parseDouble((String) clientRequest.get("amount")) * 1.5;
        System.out.println("NEEDED " + amountNeeded + " HAS " + money);
        if (money >= amountNeeded) {
            return "yes";
        } else {
            return "no";
        }
    }

    public static void updateClientCredit(JSONObject request) {
        String clientName = (String) request.get("name");
        double amount = Double.parseDouble((String) request.get("amount"));

        if (clientList.has(clientName)) {
            double oldAmount = clientList.getDouble(clientName);
            amount += oldAmount;
            clientList.put(clientName, amount);
        } else {
            clientList.put(clientName, amount);
        }
        //System.out.println("CREDIT: money " + money + " amount " + amount);
        money -= amount;
        //System.out.println("NODE CLIENT LIST: " + clientList.toString());
        //System.out.println("NODE HAS $" + money);
    }

    public static JSONObject payback(JSONObject request) {
        String client = (String) request.get("name");
        JSONObject json = new JSONObject();
        if (clientList.has(client)) {
            double amountOwed = clientList.getDouble(client);
            if (amountOwed != 0) {
                json.put("owed", true);
                json.put("amount", amountOwed);
            } else {
                json.put("owed", false);
            }
        } else {
            json.put("owed", false);
        }

        return json;
    }

    public static void updateClientPayback(JSONObject request) {
        String clientName = (String) request.get("name");
        double amount = request.getDouble("paybackAmount");

        if (clientList.has(clientName)) {
            double owedAmount = (double) clientList.get(clientName);
            owedAmount -= amount;
            clientList.put(clientName, owedAmount);
        } else {

        }
        //System.out.println("PAYBACK: money " + money + " amount " + amount);
        money += amount;
        //System.out.println("NODE CLIENT LIST: " + clientList.toString());
        //System.out.println("NODE HAS $" + money);
    }

    public static void main(String[] args) {
        Socket leaderSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        port = 8000; // default
        money = 1000; // default
        double argMoney = -1;
        String host = "localhost";
        clientList = new JSONObject();

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <money(int)>");
            System.exit(1);
        }
        try {
            port = Integer.parseInt(args[0]);
            argMoney = Double.parseDouble(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|Money] must be integer");
            System.exit(2);
        }

        //build file name for saving/loading ledger
        String filename = "src/main/resources/ledgerNode" + port + ".txt";
        clientList = readLedger(filename);

        // Node money is set to argument amount if there is one
        // Else sets money to saved amount, if there is one
        // else uses default amount of 1000
        if (argMoney != -1) {
            money = argMoney;
        } else if (clientList.has(String.valueOf(port))) {
            money = clientList.getDouble(String.valueOf(port));
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
                System.out.println("\nGot request from leader: " + request);

                if (request.get("type").equals("credit")) {
                    response = vote(request);
                } else if (request.get("type").equals("creditGrant")) {
                    updateClientCredit(request);
                    saveLedger(filename);
                } else if (request.get("type").equals("payback")) {
                    response = payback(request);
                } else if (request.get("type").equals("nodePayback")) {
                    updateClientPayback(request);
                    saveLedger(filename);

                }
                System.out.println("\nSending to leader: " + response);
                out.writeObject(response.toString());
                System.out.println("\nCurrent client list: " + clientList);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
