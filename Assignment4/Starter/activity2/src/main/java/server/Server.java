package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

import buffers.RequestProtos.Request;
import buffers.RequestProtos.Logs;
import buffers.RequestProtos.Message;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;
import org.json.JSONObject;

class Server extends Thread{
    static String logFilename = "logs.txt";
    static Integer index;
    int id;
    ServerSocket serv = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 8000; // default port
    Game game;
    static ArrayList<Server> connectedClients;
    boolean isEval;
    int numCorrect;
    String name;





    public Server(Socket sock, Game game, int index){
        this.clientSocket = sock;
        this.game = game;
        this.id = index;
        this.isEval = false;
        this.numCorrect = 0;
        this.name = "";
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in Server constructor: " + e);
        }
    }

    public Response leaderBoardResponse() {
        Response.Builder res = Response.newBuilder()
                .setResponseType(Response.ResponseType.LEADER);

        JSONObject lb = game.getPlayerInfo();
        Iterator<String> players = lb.keys();

        while (players.hasNext()) {
            String key = players.next();
            JSONObject stat = (JSONObject) lb.get(key);
            int wins = (int) stat.get("wins");
            int logins = (int) stat.get("logins");
            Entry entry = Entry.newBuilder()
                    .setName(key)
                    .setWins(wins)
                    .build();
            res.addLeader(entry);
        }
        return res.build();
    }

    public Response startGameResponse(String name) {
        String task;
        if (!game.isWon()) {
            task = game.getCurrentTask();
        } else {
            task = game.chooseTask(name);
        }
        game.newGame();
        game.setNumberOfTilesToFlip(name);

        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.TASK)
                .setImage(game.getImage())
                .setTask(task)
                .build();
        return response;
    }

    public Response evalResponse(String answer, String name) {
        boolean eval = game.getCorrectAnswer().equals(answer);
        //System.out.println("RECEIVED " + answer + " FROM " + id);
        String message;
        Response response;


        if (eval) {
            message = "CORRECT";
            game.replaceNumCharacters(game.getNumberOfTilesToFlip(name));
            //game.replaceNumCharacters(100);

            numCorrect++;
        } else {
            message = "INCORRECT";
        }

        if (game.getImage().equals(game.getOriginalImage())) {
            response = Response.newBuilder()
                    .setResponseType(Response.ResponseType.WON)
                    .setImage(game.getImage())
                    .setMessage(" ---LETS PLAY AGAIN!!!!!!!----")
                    .build();
            game.setWon();


            for (Server client : connectedClients) {
                if (client.numCorrect > 0) {
                    game.updatePlayerInfo(client.name);
                    client.numCorrect = 0;
                }

            }
            game.saveLeaderboard();


        } else {
            response = Response.newBuilder()
                    .setResponseType(Response.ResponseType.TASK)
                    .setImage(game.getImage())
                    .setTask(game.chooseTask(name))
                    .setEval(eval)
                    .setMessage(message)
                    .build();
        }

        return response;
    }

    public Response quitResponse() {
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.BYE)
                .setMessage("BUH BYEEEE!! Thanks for playing!")
                .build();

        return response;
    }

    // Handles the communication right now it just accepts one input and then is
    // done you should make sure the server stays open
    // can handle multiple requests and does not crash when the server crashes
    // you can use this server as based or start a new one if you prefer.
    public void run() {
        boolean quit = false;

        System.out.println("Ready...");
        try {
            // read the proto object and put into new object
            Request nameRequest = Request.parseDelimitedFrom(in);
            //String result = null;

            // if the operation is NAME (so the beginning then say there is a
            // connection and greet the client)
            if (nameRequest.getOperationType() == Request.OperationType.NAME) {
                // get name from proto object
                name = nameRequest.getName();

                // writing a connect message to the log with name and CONNENCT
                writeToLog(name, Message.CONNECT);
                System.out.println("Got a connection and a name: " + name);

                Response response = Response.newBuilder()
                        .setResponseType(Response.ResponseType.GREETING)
                        .setMessage("Hello " + name + " and welcome.")
                        .build();
                response.writeDelimitedTo(out);

                game.addClientToPlayerInfo(name.toLowerCase(Locale.ROOT));
            } else {
                System.out.println("Message type not recognized");
            }

            while (!quit) {
                // right now the client has their menu and were waiting on their
                // new request
                Request request = Request.parseDelimitedFrom(in);
                Response response = null;
                isEval = false;

                if (request.getOperationType() == Request.OperationType.LEADER) {
                    response = leaderBoardResponse();
                }

                if (request.getOperationType() == Request.OperationType.NEW) {
                    response = startGameResponse(name);
                    isEval = true;
                }

                if (request.getOperationType() == Request.OperationType.ANSWER) {
                    response = evalResponse(request.getAnswer().toLowerCase(Locale.ROOT), name);
                    isEval = true;
                }

                if (request.getOperationType() == Request.OperationType.QUIT) {
                    response = quitResponse();
                    quit = true;
                }

                if (isEval) {
                    for (Server client: connectedClients) {
                        if (client.isEval) {
                            response.writeDelimitedTo(client.out);
                            //System.out.println("SENDING EVAL to " + client.id);
                        }
                    }
                } else {
                    response.writeDelimitedTo(out);
                }
            }
        } catch (Exception ex) {
            System.out.println("Client " + this.id + " disconnected");

        } finally {
            connectedClients.remove(this);
            System.out.println("Client " + this.id + " disconnected");
            //reset the game if there are no more connected clients
            if (connectedClients.size() == 0) {
                game.setWon();
            }
                try {
                    if (out != null) out.close();
                    if (in != null)   in.close();
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

        }
    }

    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect) 
     * @return String of the new hidden image
     */
    public static void writeToLog(String name, Message message){
        try {
            // read old log file 
            Logs.Builder logs = readLogFile();

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " +  name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a protobuf object
            // which has a repeated field "log"

            for (String log: logsObj.getLogList()){

                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        }catch(Exception e){
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static Logs.Builder readLogFile() throws Exception{
        Logs.Builder logs = Logs.newBuilder();

        try {
            // just read the file and put what is in it into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }


    public static void main (String args[]) throws Exception {
        Game game = new Game();
        Socket clientSocket = null;
        connectedClients = new ArrayList<>();
        index = 0;

        try {
            if (args.length != 1) {
                System.out.println("Expected arguments: <port(int)>");
                System.exit(1);
            }
            int port = 8000; // default port

            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.out.println("[Port] must be an integer");
                System.exit(2);
            }
            ServerSocket serv = new ServerSocket(port);
            while (true) {
                System.out.println("Waiting for client to connect");
                clientSocket = serv.accept();
                System.out.println("Client " + index + " connected");
                Server server = new Server(clientSocket, game, index++);
                connectedClients.add(server);
                server.start();
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

