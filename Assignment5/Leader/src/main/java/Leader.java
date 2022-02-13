
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Leader implements Runnable{
    Socket clientSocket = null;
    ServerSocket serv = null;
    ObjectInputStream in = null;
    ObjectOutputStream out = null;
    int clientPort = 8000;
    int id;
    static ArrayList<Node> connectedNodes;
    static ArrayList<Leader> connectedClients;
    static int index;

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
    }

    public static void main (String[] args) throws Exception {
        int nodeSocket = 8001;
        NodeHandler nodeHandler = new NodeHandler();
        Thread thread = new Thread(nodeHandler);
        thread.start();

        Socket clientSocket = null;
        connectedNodes = new ArrayList<>();
        connectedClients = new ArrayList<>();
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
                connectedClients.add(leader);
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
