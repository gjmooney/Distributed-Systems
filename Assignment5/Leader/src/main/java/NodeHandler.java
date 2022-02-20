import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class NodeHandler implements Runnable{
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private int _port;
    private ArrayList<InnerNode> connectedNodes;

    @Override
    public void run() {
        System.out.println("InnerNode handler started");
        _port = 8001;
        connectedNodes = new ArrayList<>();
        ServerSocket socket = null;
        try {
            while (keepRunning.get()) {
                socket = new ServerSocket(_port);
                Socket nodeSocket = null;
                try {
                    nodeSocket = socket.accept();
                    System.out.println("node " + _port + " connected");
                    InnerNode node =  new InnerNode(nodeSocket, _port);
                    //System.out.println("in handler" + connectedNodes.toString());
                    connectedNodes.add(node);
                    _port++;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                e.printStackTrace();
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

        public int shutDownNode() throws IOException {
            System.out.println("PRE NODE LIST: " + connectedNodes);
            in.close();
            out.close();
            socket.close();
            for (int i = 0; i < connectedNodes.size(); i++) {
                if (connectedNodes.get(i).port == this.port) {
                    connectedNodes.remove(this);
                    i--;
                    System.out.println("removing node on port " + port);
                }
            }
            /*Iterator<InnerNode> it = connectedNodes.iterator();
            while (it.hasNext()) {
                InnerNode node = it.next();
                if (node.port == this.port) {
                    connectedNodes.remove(this);
                    System.out.println("removing node on port " + port);
                }
            }*/
            System.out.println("POST NODE LIST: " + connectedNodes);

            return port;


        }



    }
}
