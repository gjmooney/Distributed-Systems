import java.net.DatagramSocket;
import java.net.InetAddress;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Client {

    public static void main(String[] args) {
        DatagramSocket socket;
        socket = null;
        int port = 8080;

        if (args.length != 2) {
            System.out.println("Expected two arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Port number must be an integer");
            System.exit(2);
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            socket = new DatagramSocket(port, address);
            do {
                JSONObject request = null;

                if (request != null) {

                }
            } while (true);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not establish connection over port: " + port);
            System.exit(2);
        }
    }
}
