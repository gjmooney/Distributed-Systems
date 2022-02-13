import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NodeHandler implements Runnable{
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private int _port;
    private ArrayList<Integer> connectedNodes;

    @Override
    public void run() {
        System.out.println("Node handler started");
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
                    connectedNodes.add(_port);
                    _port++;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
