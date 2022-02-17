
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

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
    private static String clientName;
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
            File file = new File("src/main/resources/ledger.txt");

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
        File file = new File("src/main/resources/ledger.txt");
        FileWriter fileWriter = null;
        try {
            if (file.createNewFile()) {
                System.out.println("New ledger created");
            }


            fileWriter = new FileWriter("src/main/resources/ledger.txt");
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
        /*try {
            String jsonData = (String) in.readObject();
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            return new JSONObject(jsonTokener);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }*/

        //return error();
    }

    public void run() {
        clientLedger = readLedger();
        System.out.println("PROGRESS");
        JSONObject response = buildNameResponse();
        try {
            // send name
            clientOut.writeObject(response.toString());

            while (true) {
                //get request from client
                String jsonData = (String) clientIn.readObject();
                JSONTokener jsonTokener = new JSONTokener(jsonData);
                JSONObject request = new JSONObject(jsonTokener);

                if (request.get("type").equals("name")) {
                    response = buildGreetingResponse(request);
                } else if (request.get("type").equals("credit")) {
                    if (creditConsensus(request)) {
                        System.out.println("UPDATING LEDGER");
                        updateLedger(Double.parseDouble((String) request.get("amount")), true);
                        System.out.println("SENDING RESPONSE");
                        response = buildCreditResponse(request, true);
                        nodesSplitCredit((String) request.get("amount"));
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


                } else if (request.get("type").equals("exit")) {

                }
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
            message = "Welcome back " + name + " !";
            message += "\nYou current have $" + clientLedger.get(name) + " owed";
        } else {
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
        workingList = new ArrayList<>();
        System.out.println("nodes in leader" + nodeHandler.getConnectedNodes());
        JSONObject creditRequestToNodes = new JSONObject();
        creditRequestToNodes.put("type", "credit");
        creditRequestToNodes.put("name", clientName);
        creditRequestToNodes.put("amount", request.get("amount"));
        for (NodeHandler.InnerNode node : nodeHandler.getConnectedNodes()) {
            try {
                System.out.println("CONSENSUS SENDING");
                node.out.writeObject(creditRequestToNodes.toString());
                System.out.println("CONSENSUS RECEIVING");
                //TODO break out into vote counting method
                JSONObject voteResponseFromNode = receive(node.in);
                System.out.println("COUNTING " + count++ + " VOTE");
                System.out.println("156: node response: " + voteResponseFromNode);
                if (voteResponseFromNode.get("vote").equals("no")) {
                    noCount++;
                } else {
                    yesCount++;
                    workingList.add(node);
                }
            } catch (IOException e ) {
                e.printStackTrace();
                System.out.println("Error in credit consensus");
                try {
                    int port = node.shutDownNode();
                    System.out.println("Shutting down node on port " + port);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        //return yesCount > noCount;
        return yesCount != 0;
    }

    public void updateLedger(double amount, boolean credit) {
        if (clientLedger.has(clientName)) {
            double oldAmount = (double) clientLedger.get(clientName);
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
        System.out.println("LEDGER" + clientLedger.toString());
        saveLedger();
    }

    public void nodesSplitCredit(String amount) {
        //TODO this algorithm will need updating to handle numbers that don't split
        // evenly
        double nodeAmount = Double.parseDouble(amount);
        nodeAmount = nodeAmount / workingList.size();
        String strAmount = Double.toString(nodeAmount);
        JSONObject json = new JSONObject();
        json.put("type", "creditGrant");
        json.put("amount", strAmount);
        json.put("name", clientName);

        for (NodeHandler.InnerNode node: workingList) {
            try {
                node.out.writeObject(json.toString());
                JSONObject responseFromNode = receive(node.in);
            } catch (IOException | ClassNotFoundException e ) {
                e.printStackTrace();
                System.out.println("Error in credit consensus");
            }

        }
    }

    public boolean countVotes(ArrayList<JSONObject> list) {
        int yesCount = 0;
        int noCount = 0;
        for (JSONObject response : list) {
            if (response.get("type").equals("error")) {
                System.out.println("BEEP BOOP BROKE");
                break;
            } else {
                if (response.get("vote").equals("no")) {
                    noCount++;
                } else {
                    yesCount++;
                }
            }
        }
        return yesCount > noCount;
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
        for (NodeHandler.InnerNode node : nodeHandler.getConnectedNodes()) {
            try {
                node.out.writeObject(paybackQuery.toString());
                JSONObject responseFromNode = receive(node.in);
                System.out.println("254: owed: " + responseFromNode);
                if (responseFromNode.getBoolean("owed")) {
                    workingList.add(node);
                    node.setAmountOwed(responseFromNode.getDouble("amount"));
                } else {
                    //node not owed dont do nuffin
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void calcPaybackAmount(JSONObject request) throws IOException, ClassNotFoundException {
        double owed = Double.parseDouble((String) request.get("amount"));
        double difference = 0.0;
        JSONObject nodePayback = new JSONObject();

        do {
            int numNodes = workingList.size();
            double perNode = owed / numNodes;


            for (NodeHandler.InnerNode node : workingList) {
                nodePayback.put("name", clientName);
                nodePayback.put("type", "nodePayback");

                if (perNode < node.getAmountOwed()) {
                    nodePayback.put("paybackAmount", perNode);
                } else {
                    difference += perNode - node.getAmountOwed();
                    nodePayback.put("paybackAmount", node.getAmountOwed());
                }
                node.out.writeObject(nodePayback.toString());
                JSONObject responseFromNode = receive(node.in);
            }
            if (difference > 0) {
                getOwedNodes();
                owed = difference;
                difference = 0.0;
            }
        } while (difference != 0.0);
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
            e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                System.out.println("Server closing socket");
                clientSocket.close();
            }
        }
    }


}
