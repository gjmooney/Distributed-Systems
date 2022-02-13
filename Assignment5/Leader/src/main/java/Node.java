import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Node {

    public static void main(String[] args) {
        Socket leaderSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        int port = 8000; // default
        int money;
        String host = "localhost";

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <money(int)>");
            System.exit(1);
        }
        try {
            port = Integer.parseInt(args[0]);
            money = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|Money] must be integer");
            System.exit(2);
        }
        System.out.println("NODE PRT: " + port);

        try {
            leaderSocket = new Socket(host, port);
            System.out.println("Node connected");
            out = new ObjectOutputStream(leaderSocket.getOutputStream());
            in = new ObjectInputStream(leaderSocket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
