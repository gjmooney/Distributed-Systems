package client;

import java.net.*;
import java.io.*;

import org.json.*;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

import java.util.*;
import java.util.stream.Collectors;

class SockBaseClient {

    public static void main (String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int i1=0, i2=0;
        int port = 9099; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server. ( ͡❛ ͜ʖ ͡❛)");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response response;

        try {
            // connect to the server
            serverSock = new Socket(host, port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            // send name to server
            op.writeDelimitedTo(out);

            // read from the server
            response = Response.parseDelimitedFrom(in);
            // display greeting fomr the server

            // print the server response.
            System.out.println(response.getMessage());

            // display menu after sending name
            int choice;
            Scanner input = new Scanner(System.in);
            do {
                System.out.println();
                System.out.println("Client Menu");
                System.out.println("Please select a valid option (1, 2, or 3).");
                System.out.println("1. See the leaderboard");
                System.out.println("2. Start the game");
                System.out.println("3. quit");
                System.out.println();
                try {
                    choice = input.nextInt();
                    Request clientRequest = null;
                    switch (choice) {
                        case (1):
                            // build leader board request
                            break;
                        case (2):
                            // build start game request
                            break;
                        case (3):
                            // build quit respons
                            break;
                        default:
                            System.out.println("Please select a valid option (1, 2, or 3");
                            break;
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    input = new Scanner(System.in);
                    System.out.println("Please Enter a valid number");
                }

            } while (true);





        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null)   in.close();
            if (out != null)  out.close();
            if (serverSock != null) serverSock.close();
        }
    }
}


