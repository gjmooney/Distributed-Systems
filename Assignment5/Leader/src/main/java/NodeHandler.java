import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NodeHandler implements Runnable{
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private int _port;
    private ArrayList<InnerNode> connectedNodes;
    ServerSocket mainSocket;

    @Override
    public void run() {
        System.out.println("InnerNode handler started");
        _port = 8001;
        connectedNodes = new ArrayList<>();
        mainSocket = null;
        try {
            while (keepRunning.get()) {
                mainSocket = new ServerSocket(_port);
                Socket nodeSocket = null;
                nodeSocket = mainSocket.accept();
                System.out.println("node " + _port + " connected");
                InnerNode node =  new InnerNode(nodeSocket, _port);
                connectedNodes.add(node);
                _port++;
            }
        } catch (IOException e) {
            System.out.println("Error in node handler");
            //e.printStackTrace();
        }
    }

    public ArrayList<InnerNode> getConnectedNodes() {
        return connectedNodes;
    }

    class InnerNode {
        Socket socket;
        ObjectInputStream in;
        ObjectOutputStream out;
        int port;
        double amountOwed;
        public InnerNode (Socket sock, int port) {
            this.socket = sock;
            this.port = port;
            try {
                this.in = new ObjectInputStream(sock.getInputStream());
                this.out = new ObjectOutputStream(sock.getOutputStream());
            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("Error creating node in node handler");
            }
        }

        public void setAmountOwed(double amount) {
            this.amountOwed = amount;
        }

        public double getAmountOwed() {
            return amountOwed;
        }

        public int getPort() {
            return port;
        }

        public void shutDownNode() {
            for (int i = 0; i < connectedNodes.size(); i++) {
                if (connectedNodes.get(i).port == this.port) {
                    connectedNodes.remove(i);
                    i--;
                    System.out.println("HANDLER: removing node on port " + port);
                }
            }

            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("DO i need this?");
                //e.printStackTrace();
            }
        }
    }
}
