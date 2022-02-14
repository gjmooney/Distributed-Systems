
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Leader extends Thread{
    private Socket clientSocket = null;
    private ServerSocket serv = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private int clientPort = 8000;
    private int id;
    private static ArrayList<Node> connectedNodes;
    private static JSONObject connectedClients;
    private static int index;
    private static String clientName;
    private ArrayList<NodeHandler.InnerNode> yesList;
    static NodeHandler nodeHandler;

    public Leader(Socket sock, int index) {
        this.clientSocket = sock;
        this.id = index;
        try {
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (Exception e){
            System.out.println("Error in Server constructor: " + e);
        }
    }

    public JSONObject error() {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        return error;
    }

    public JSONObject receive(ObjectInputStream in) {
        try {
            String jsonData = (String) in.readObject();
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            return new JSONObject(jsonTokener);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return error();
    }

    public void run() {
        System.out.println("PROGRESS");
        JSONObject response = buildNameResponse();
        try {
            // send name
            out.writeObject(response.toString());

            while (true) {
                //get request from client
                String jsonData = (String) in.readObject();
                JSONTokener jsonTokener = new JSONTokener(jsonData);
                JSONObject request = new JSONObject(jsonTokener);

                if (request.get("type").equals("name")) {
                    response = buildGreetingResponse(request);
                } else if (request.get("type").equals("credit")) {
                //need to send client and amount to all nodes
                    if (creditConsensus(request)) {
                        System.out.println("UPDATING LEDGER");
                        updateLedger((int) request.get("amount"));
                        System.out.println("SENDING RESPONSE");
                        response = buildCreditResponse(request, true);
                        //nodesSplitCredit((String) request.get("amount"));
                    }

                    // divide amount amongst clients that said yes
                    // they need to update thier lists

                } else if (request.get("type").equals("payback")) {

                } else if (request.get("type").equals("exit")) {

                }
                out.writeObject(response.toString());
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void updateLedger(int amount) {
        if (connectedClients.has(clientName)) {
            int oldAmount = (int) connectedClients.get(clientName);
            int newAmount = oldAmount + amount;
            connectedClients.put(clientName, newAmount);
        } else {
            System.out.println("This shouldn't happen");
        }
        System.out.println("LEDGER" + connectedClients.toString());
    }

    public void nodesSplitCredit(String amount) {

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
        if (connectedClients.has(name)) {
            message = "Welcome back " + name + " !";
            message += "\nYou current have $" +connectedClients.get(name) + " owed";
        } else {
            int credit = 0; // initial amount of credit owed
            connectedClients.put(name, credit);
            message = "Thank you for joining us, " + name + "!";
        }
        message += "\nYour current credit amount is: $" + connectedClients.get(name);

        response.put("type", "greeting");
        response.put("message", message);
        return response;
    }

    public boolean creditConsensus(JSONObject request) {
        int yesCount = 0;
        int noCount = 0;
        int count = 1;
        yesList = new ArrayList<NodeHandler.InnerNode>();
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
                JSONObject voteResponseFromNode = receive(node.in);
                System.out.println("COUNTING " + count++ + " VOTE");
                if (voteResponseFromNode.get("vote").equals("no")) {
                    noCount++;
                } else {
                    yesCount++;
                }
                yesList.add(node);
            } catch (IOException e ) {
                e.printStackTrace();
                System.out.println("Error in credit consensus");
            }
        }
        return yesCount > noCount;
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
        json.put("credit", connectedClients.get(clientName));
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
        connectedClients = new JSONObject();
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
