
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Leader implements Runnable{
    private Socket clientSocket = null;
    private ServerSocket serv = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private int clientPort = 8000;
    private int id;
    private static ArrayList<Node> connectedNodes;
    private static JSONObject connectedClients;
    private static int index;

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

                } else if (request.get("type").equals("payback")) {

                } else if (request.get("type").equals("exit")) {

                }
                out.writeObject(response.toString());
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        // ask the client their id
        // add it to a list
        // use protocol
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



    public static void main (String[] args) throws Exception {
        int nodeSocket = 8001;
        NodeHandler nodeHandler = new NodeHandler();
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
               // connectedClients.add(leader);
                leader.run();
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
