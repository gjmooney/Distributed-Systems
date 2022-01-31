import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Server {

    public static void main(String[] args) {
        DatagramSocket serverSocket = null;

        int port = 8080;
        int state = 1;

        if (args.length > 1) {
            System.out.println("Expected one argument: <port(int)>");
            System.exit(1);
        }
        System.out.println("Establishing connection on port: "  + args[0]);

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port number must be an integer");
            System.exit(2);
        }


        try {
            serverSocket = new DatagramSocket(port);
            JSONObject messageToClient = null;

           // while (true) {
                switch (state) {
                    case 1:
                        messageToClient = createMessage("name");
                        break;
                    default:
                        messageToClient = createMessage("broke");
                        break;
                }
                byte[] output = messageToClient.toString().getBytes(StandardCharsets.UTF_8);
                send(serverSocket,output);

            //}

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not establish connection over port: " + port);
            System.exit(2);
        } finally {
            if (serverSocket != null) {
                System.out.println("Closing serverSocket");
                serverSocket.close();
            }
        }
    }

    // Based on code from AdvancedCustomProtocol sample
    static void send(DatagramSocket socket, byte[] toSend) throws IOException {
        int maxBufferLength = 1024 - 12;
        int packetsTotal = toSend.length / maxBufferLength + 1;
        int offset = 0;
        int packetNum = 0;
        boolean test = true;
        while (test) {
            int bytesLeftToSend = toSend.length - offset;
            int length = Math.min(maxBufferLength, bytesLeftToSend);

            System.out.println(packetsTotal);
            System.out.println(packetNum);
            System.out.println(length + "\n");

            byte[] totalBytes = ByteBuffer.allocate(4).putInt(packetsTotal).array();
            byte[] currentBytes = ByteBuffer.allocate(4).putInt(packetNum).array();
            byte[] lengthBytes = ByteBuffer.allocate(4).putInt(length).array();

            System.out.println(Arrays.toString(totalBytes));
            System.out.println(Arrays.toString(currentBytes));
            System.out.println(Arrays.toString(lengthBytes));

            byte[] buffer = new byte[12 + length];
            System.arraycopy(totalBytes, 0, buffer, 0, 4);
            System.arraycopy(currentBytes, 0, buffer, 4, 4);
            System.arraycopy(lengthBytes, 0, buffer, 8, 4);
            System.arraycopy(toSend, offset, buffer, 12, length);

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.send(packet);
            packetNum++;
            offset += length;

            test = false;
        }
    }


    static JSONObject createMessage(String type) {
        JSONObject message = new JSONObject();
        if (type.equals("name")) {
            message.put("text", "HEY! What's your name!?");
        } else if (type.equals("broke")) {
            message.put("text", "we ded");
        }
        return message;
    }

    static class Tuple {
        public final InetAddress Address;
        public final int Port;
        public final byte[] Payload;

        public Tuple(InetAddress address, int port, byte[] payload) {
            Address = address;
            Port = port;
            Payload = payload;
        }
    }
}
