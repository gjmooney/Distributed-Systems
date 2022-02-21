
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.image.Kernel;
import java.io.*;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class Leader extends Thread{
    private Socket clientSocket = null;
    private ServerSocket serv = null;
    private ObjectInputStream clientIn = null;
    private ObjectOutputStream clientOut = null;
    private int clientPort = 8000;
    private int id;
    private static ArrayList<Node> connectedNodes;
    private static JSONObject clientLedger;
    private static int index;
    private String clientName;
    private ArrayList<NodeHandler.InnerNode> workingList;
    static NodeHandler nodeHandler;

    public Leader(Socket sock, int index) {
        this.clientSocket = sock;
        this.id = index;
        try {
            clientIn = new ObjectInputStream(clientSocket.getInputStream());
            clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (Exception e){
            System.out.println("Error in Server constructor: " + e);
        }
    }

    public JSONObject error() {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        return error;
    }

    public JSONObject readLedger() {
        BufferedReader leaderboardReader = null;
        JSONTokener tokener;
        try {
            File file = new File("src/main/resources/ledgerLeader.txt");

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

    public void saveLedger() {
        File file = new File("src/main/resources/ledgerLeader.txt");
        FileWriter fileWriter = null;
        try {
            if (file.createNewFile()) {
                System.out.println("New ledger created");
            }


            fileWriter = new FileWriter("src/main/resources/ledgerLeader.txt");
            fileWriter.write(clientLedger.toString());
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

    public JSONObject receive(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String jsonData = (String) in.readObject();
        JSONTokener jsonTokener = new JSONTokener(jsonData);
        return new JSONObject(jsonTokener);
    }

    public void syncWithNodes() {
        double nodeOwed = 0.0;
        JSONObject syncLedger = new JSONObject();

        // This gets the ledger from each node and iterates through it,
        // adding the clients and their amount to the syncLedger
        if (nodeHandler.getConnectedNodes().size() > 0) {
            for (NodeHandler.InnerNode node : nodeHandler.getConnectedNodes()) {
                JSONObject syncRequest = new JSONObject();
                syncRequest.put("type", "sync");
                try {
                    node.out.writeObject(syncRequest.toString());
                    JSONObject response = receive(node.in);
                    Iterator<String> keys = response.keys();
                    while (keys.hasNext()) {
                        String client = keys.next();
                        if (!Objects.equals(client, String.valueOf(node.getPort()))) {
                            if (syncLedger.has(client)) {
                                double newTotal = syncLedger.getDouble(client) + response.getDouble(client);
                                syncLedger.put(client, newTotal);
                            } else {
                                syncLedger.put(client, response.getDouble(client));
                            }
                        }

                    }
                } catch (IOException | ClassNotFoundException e) {
                    //e.printStackTrace();
                    System.out.println("BRUUK");
                }
            }

            // This compares the sync ledger with the leaders ledger
            Iterator<String> syncKeys = clientLedger.keys();
            while (syncKeys.hasNext()) {
                String syncClient = syncKeys.next();
                if (syncLedger.has(syncClient)) {
                    if (syncLedger.getDouble(syncClient) != clientLedger.getDouble(syncClient)) {
                        reSync();
                    }
                } else {
                    reSync();
                }
            }
        }
    }

    public void reSync() {
        System.out.println("\nSTARTING RESYNC\n");
        JSONObject resync = new JSONObject();
        resync.put("type", "resync");
        List<NodeHandler.InnerNode> nodes = new CopyOnWriteArrayList<>(nodeHandler.getConnectedNodes());
        System.out.println("CONNECTED NODES: "+ nodeHandler.getConnectedNodes());

        Iterator<String> syncKeys = clientLedger.keys();
        while (syncKeys.hasNext()) {
            String syncClient = syncKeys.next();
            //double amountPerNode = clientLedger.getDouble(syncClient) / nodes.size();
            double amountPerNode = formatDouble(clientLedger.getDouble(syncClient), nodes.size());
            resync.put(syncClient, amountPerNode);
            System.out.println("ADDING TO RESYNC: " + syncClient + " : " + amountPerNode);
        }

        for (NodeHandler.InnerNode node: nodes) {
            try {
                node.out.writeObject(resync.toString());
                System.out.println("SYNCING: " + resync);
                System.out.println("\nResyncing to node "
                        + node.getPort() + " : " + resync);
                JSONObject responseFromNode = receive(node.in);
            } catch (IOException | ClassNotFoundException e ) {
                //e.printStackTrace();
                System.out.println("Error in resync");
                node.shutDownNode();
            }

        }

    }

    public double formatDouble(double amount, int nodes) {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.DOWN);
        double amountD = amount / nodes;
        return Double.parseDouble(df.format(amountD));
    }

    public void run() {
        workingList = new ArrayList<>();
        clientLedger = readLedger();
        JSONObject response = buildNameResponse();
        try {
            // send name
            clientOut.writeObject(response.toString());

            while (true) {
                //get request from client
                String jsonData = (String) clientIn.readObject();
                JSONTokener jsonTokener = new JSONTokener(jsonData);
                JSONObject request = new JSONObject(jsonTokener);

                syncWithNodes();

                //System.out.println("From client " + clientName + " : " + request);
                if (request.get("type").equals("name")) {
                    response = buildGreetingResponse(request);

                } else if (request.get("type").equals("credit")) {
                    if (creditConsensus(request)) {
                        double amount = Double.parseDouble((String) request.get("amount"));
                        updateLedger(amount, true);
                        response = buildCreditResponse(request, true);
                        //nodesSplitCredit((String) request.get("amount"));
                        nodesSplitCredit(amount);

                        workingList.clear();
                    } else {
                        response = buildCreditResponse(request, false);
                        workingList.clear();
                    }

                } else if (request.get("type").equals("payback")) {
                    //check how much client owes
                    double amount = request.getDouble("amount");
                    if (amount > clientLedger.getDouble(clientName)) {
                        response = buildPaybackResponse(request, false);
                    } else {
                        getOwedNodes();
                        calcPaybackAmount(request);
                        updateLedger(Double.parseDouble((String) request.get("amount")), false);
                        response = buildPaybackResponse(request, true);
                        workingList.clear();

                    }
                }
                response.put("client", clientName);
                System.out.println("\nSending to client " + clientName + " : " + response);
                clientOut.writeObject(response.toString());
            }

        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            System.out.println("Client " + clientName + " disconnected");
        } finally {
            try {
                clientIn.close();
                clientOut.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public JSONObject buildNameResponse() {
        JSONObject json = new JSONObject();
        json.put("type", "name");
        json.put("message", "Please enter your ID");
        return json;
    }

    public JSONObject buildGreetingResponse(JSONObject request) {
        JSONObject response = new JSONObject();
        String message;
        String name = (String) request.get("name");
        clientName = name;
        if (clientLedger.has(name)) {
            System.out.println("1Adding " + name + " as " + clientName);
            message = "Welcome back " + name + " !";
            message += "\nYou current have $" + clientLedger.get(name) + " owed";
        } else {
            System.out.println("2Adding " + name + " as " + clientName);

            double credit = 0.0; // initial amount of credit owed
            clientLedger.put(name, credit);
            message = "Thank you for joining us, " + name + "!";
        }
        message += "\nYour current credit amount is: $" + clientLedger.get(name);

        response.put("type", "greeting");
        response.put("message", message);
        response.put("credit", clientLedger.getDouble(name));
        return response;
    }

    public boolean creditConsensus(JSONObject request) {
        int yesCount = 0;
        int noCount = 0;
        int count = 1;
        JSONObject creditRequestToNodes = new JSONObject();
        creditRequestToNodes.put("type", "credit");
        creditRequestToNodes.put("name", clientName);
        creditRequestToNodes.put("amount", request.get("amount"));

        List<NodeHandler.InnerNode> list = new CopyOnWriteArrayList<>(nodeHandler.getConnectedNodes());
        for (NodeHandler.InnerNode node : list) {
            try {
                System.out.println("\nConsensus -- Sending to "
                        + node.getPort() + " : " + creditRequestToNodes);
                node.out.writeObject(creditRequestToNodes.toString());
                JSONObject voteResponseFromNode = receive(node.in);
                System.out.println("Counting " + count++ + " votes");
                System.out.println("\nResponse from node "
                        + node.getPort() + " : " + voteResponseFromNode);
                if (voteResponseFromNode.get("vote").equals("no")) {
                    noCount++;
                } else {
                    yesCount++;
                    workingList.add(node);
                }
            } catch (IOException e ) {
                //e.printStackTrace();
                System.out.println("Error in credit consensus");
                node.shutDownNode();
                creditConsensus(request);
            } catch (ClassNotFoundException e) {
                //e.printStackTrace();
                System.out.println("Another Error in credit consensus");
            }
        }
        return yesCount > noCount;
    }

    synchronized public void updateLedger(double amount, boolean credit) {
        if (clientLedger.has(clientName)) {
            double oldAmount = clientLedger.getDouble(clientName);
            double newAmount;
            if (credit) {
                newAmount = oldAmount + amount;
            } else {
                newAmount = oldAmount - amount;
            }
            clientLedger.put(clientName, newAmount);
        } else {
            System.out.println("This shouldn't happen");
        }
        System.out.println("Current ledger: " + clientLedger.toString());
        saveLedger();
    }

    public void nodesSplitCredit(double amount) {
        double nodeAmount = formatDouble(amount, workingList.size());
        JSONObject json = new JSONObject();
        json.put("type", "creditGrant");
        json.put("amount", nodeAmount);
        json.put("name", clientName);

        for (NodeHandler.InnerNode node: workingList) {
            try {
                node.out.writeObject(json.toString());
                System.out.println("\nSending to node "
                        + node.getPort() + " : " + json);
                JSONObject responseFromNode = receive(node.in);
            } catch (IOException | ClassNotFoundException e ) {
                //e.printStackTrace();
                System.out.println("Error in nodeSplitCredit");
            }

        }
    }

    public JSONObject buildCreditResponse(JSONObject request, boolean approved) {
        JSONObject json = new JSONObject();
        json.put("type", "creditResponse");
        json.put("amount", request.get("amount"));
        json.put("credit", clientLedger.get(clientName));
        if (approved) {
            json.put("approved", true);
        } else {
            json.put("approved", false);
        }

        return json;
    }

    public void getOwedNodes() {
        JSONObject paybackQuery = new JSONObject();
        paybackQuery.put("type", "payback");
        paybackQuery.put("name", clientName);
        List<NodeHandler.InnerNode> list = new CopyOnWriteArrayList<>(nodeHandler.getConnectedNodes());
        for (NodeHandler.InnerNode node : list) {
            try {
                System.out.println("\nSending to node "
                    + node.getPort() + " : " + paybackQuery);
                node.out.writeObject(paybackQuery.toString());
                JSONObject responseFromNode = receive(node.in);
                System.out.println("\nResponse from node "
                        + node.getPort() + " : " + responseFromNode);
                if (responseFromNode.getBoolean("owed")) {
                    workingList.add(node);
                    node.setAmountOwed(responseFromNode.getDouble("amount"));
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error caught in getOwedNodes. Cleaning up crashed node");
                workingList.clear();
                //e.printStackTrace();
                node.shutDownNode();
                getOwedNodes();

            }
        }
    }

    public void calcPaybackAmount(JSONObject request) throws IOException, ClassNotFoundException {
        double payingBack = Double.parseDouble((String) request.get("amount"));
        JSONObject nodePayback = new JSONObject();
        double amountUsed = 0.0;
        //DecimalFormat df = new DecimalFormat("#.##");
        //df.setRoundingMode(RoundingMode.DOWN);

        do {
            amountUsed = 0.0;
            int numNodes = workingList.size();
            double perNode = 0.0;
            //double perNodeD = payingBack / numNodes;
            if (workingList.size() != 0) {
                perNode = formatDouble(payingBack, numNodes);
            }

            for (NodeHandler.InnerNode node : workingList) {
                nodePayback.put("name", clientName);
                nodePayback.put("type", "nodePayback");

                if (perNode < node.getAmountOwed()) {
                    nodePayback.put("paybackAmount", perNode);
                    amountUsed += perNode;
                    payingBack -= perNode;
                } else {
                    nodePayback.put("paybackAmount", node.getAmountOwed());
                    amountUsed += node.getAmountOwed();
                    payingBack -= node.getAmountOwed();
                }
                System.out.println("\nSending to node "
                        + node.getPort() + " : " + nodePayback);
                node.out.writeObject(nodePayback.toString());
                JSONObject responseFromNode = receive(node.in);
            }
            if (payingBack > 0) {
                workingList.clear();
                getOwedNodes();
            }
        } while (amountUsed != 0.0);
    }

    public JSONObject buildPaybackResponse(JSONObject request, boolean approved) {
        JSONObject json = new JSONObject();
        json.put("type", "paybackResponse");
        json.put("amount", request.get("amount"));
        json.put("credit", clientLedger.get(clientName));
        if (approved) {
            json.put("approved", true);
        } else {
            json.put("approved", false);
        }

        return json;
    }

    public static void main (String[] args) throws Exception {
        nodeHandler = new NodeHandler();
        Thread thread = new Thread(nodeHandler);
        thread.start();

        Socket clientSocket = null;
        connectedNodes = new ArrayList<>();
        clientLedger = new JSONObject();
        index = 0;

        try {
            if (args.length != 3) {
                System.out.println("Expected arguments: <port(int)>");
                System.exit(1);
            }
            int port = 8000; // default client port

            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.out.println("[Port] must be an integer");
                System.exit(2);
            }

            ServerSocket serv = new ServerSocket(port);
            while (true) {
                System.out.println("Waiting for client to connect");
                clientSocket = serv.accept(); //listening on 8000
                System.out.println("Client " + index + " connected");
                Leader leader = new Leader(clientSocket, index++);
                leader.start();
            }

        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                System.out.println("Server closing socket");
                clientSocket.close();
            }
        }
    }


}
