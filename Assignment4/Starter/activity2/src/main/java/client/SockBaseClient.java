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

    public static Request leaderboardRequest() {
        Request request = Request.newBuilder()
                .setOperationType(Request.OperationType.LEADER)
                .build();
        return request;
    }

    public static Request startGameRequest() {
        Request request = Request.newBuilder()
                .setOperationType(Request.OperationType.NEW)
                .build();
        return request;
    }

    public static Request gameLoopRequest(String answer) {
        Request request = Request.newBuilder()
                .setOperationType(Request.OperationType.ANSWER)
                .setAnswer(answer)
                .build();
        return request;
    }

    public static Request quitRequest() {
        Request request = Request.newBuilder()
                .setOperationType(Request.OperationType.QUIT)
                .build();
        return request;
    }

    //TODO can actually replace with setting the while loops to false and it should
    // drop though to the finally
    public static void exit(Socket serverSock, OutputStream out, InputStream in) {
        try {
            serverSock.close();
            out.close();
            in.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Problem shutting down client");
        }

    }

    public static void gameLoop(Socket serverSock, OutputStream out, InputStream in) {
        boolean gameOn = true;

        while (gameOn) {
            System.out.println("Enter your answer: ");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            Request request;
            Response response = null;
            try {
                String answer = stdin.readLine();
                if (answer.toLowerCase(Locale.ROOT).equals("exit")) {
                    request = quitRequest();
                } else {
                    request = gameLoopRequest(answer);
                }

                request.writeDelimitedTo(out);
                response = Response.parseDelimitedFrom(in);

                if (response.getResponseType() == Response.ResponseType.TASK) {
                    System.out.println(response.getMessage());
                    System.out.println();
                    System.out.println(response.getImage());
                    System.out.println();
                    System.out.println("Your task: " + response.getTask());
                } else if(response.getResponseType() == Response.ResponseType.WON) {
                    System.out.println();
                    System.out.println(response.getImage());
                    System.out.println(response.getMessage());
                    gameOn = false;
                } else if (response.getResponseType() == Response.ResponseType.BYE) {
                    System.out.println(response.getImage());
                    System.out.println(response.getMessage());
                    exit(serverSock, out, in);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("YOU DID A BAD");
            }
        }
    }

    public static void main (String args[]) throws Exception {
        Socket socket = null;
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
        Request nameRequest = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response nameResponse;

        try {
            // connect to the server
            socket = new Socket(host, port);

            // write to the server
            out = socket.getOutputStream();
            in = socket.getInputStream();

            // send name to server
            nameRequest.writeDelimitedTo(out);

            // read from the server
            nameResponse = Response.parseDelimitedFrom(in);
            // display greeting fomr the server

            // print the server response.
            System.out.println(nameResponse.getMessage());

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
                    Request request = null;
                    Response response;
                    switch (choice) {
                        case (1):
                            request = leaderboardRequest();
                            break;
                        case (2):
                            request = startGameRequest();
                            break;
                        case (3):
                            request = quitRequest();
                            break;
                        default:
                            System.out.println("Please select a valid option (1, 2, or 3");
                            break;
                    }

                    if (request != null) {
                        //send request to server
                        request.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);

                        if (response.getResponseType() == Response.ResponseType.LEADER) {
                            for (Entry player : response.getLeaderList()) {
                                System.out.println(player.getName() + ": " + player.getWins());
                            }
                        }

                        if (response.getResponseType() == Response.ResponseType.TASK) {
                            System.out.println(response.getImage());
                            System.out.println();
                            System.out.println(response.getTask());
                            gameLoop(socket, out, in);

                        }

                        if (response.getResponseType() == Response.ResponseType.BYE) {
                            System.out.println(response.getMessage());
                            exit(socket, out, in);
                        }
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
            if (socket != null) socket.close();
        }
    }
}


