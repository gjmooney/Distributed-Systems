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

    ServerSocket serv = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;
    static LinkedList<Server> connectedClients;



    public Server(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;
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

    public Response startGameResponse() {
        game.newGame();
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.TASK)
                .setImage(game.getImage())
                .setTask(game.chooseTask())
                .build();
        return response;
    }

    public Response evalResponse(String answer, String name) {
        boolean eval = game.getCorrectAnswer().equals(answer);
        String message;
        Response response;

        if (eval) {
            message = "GOOD JOB";
            replace(50);
        } else {
            message = "OOOOO NAWP";
        }

        if (game.getImage().equals(game.getOriginalImage())) {

            response = Response.newBuilder()
                    .setResponseType(Response.ResponseType.WON)
                    .setImage(game.getImage())
                    .setMessage("YOU WON BRO\nLETS PLAY AGAIN!!!!!!!")
                    .build();
            game.setWon();

            game.updatePlayerInfo(name);
            game.saveLeaderboard();

        } else {
            response = Response.newBuilder()
                    .setResponseType(Response.ResponseType.TASK)
                    .setImage(game.getImage())
                    .setTask(game.chooseTask())
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
        String name = "";
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

                //leaderboard stuff
                game.addClientToPlayerInfo(name);

                Response response = Response.newBuilder()
                        .setResponseType(Response.ResponseType.GREETING)
                        .setMessage("Hello " + name + " and welcome.")
                        .build();
                response.writeDelimitedTo(out);
            } else {
                //TODO something bad appen make an error
            }

            while (!quit) {
                // right now the client has their menu and were waiting on their
                // new request
                Request request = Request.parseDelimitedFrom(in);
                Response response = null;

                if (request.getOperationType() == Request.OperationType.LEADER) {
                    response = leaderBoardResponse();
                }

                if (request.getOperationType() == Request.OperationType.NEW) {
                    response = startGameResponse();
                }

                if (request.getOperationType() == Request.OperationType.ANSWER) {
                    response = evalResponse(request.getAnswer(), name);
                }

                if (request.getOperationType() == Request.OperationType.QUIT) {
                    response = quitResponse();
                    quit = true;
                }

                response.writeDelimitedTo(out);


            }
            if (out != null)  out.close();
            if (in != null)   in.close();
            if (clientSocket != null) clientSocket.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

        }
    }

    /**
     * Replaces num characters in the image. I used it to turn more than one x when the task is fulfilled
     * @param num -- number of x to be turned
     * @return String of the new hidden image
     */
    public String replace(int num){
        for (int i = 0; i < num; i++){
            if (game.getIdx()< game.getIdxMax())
                game.replaceOneCharacter();
        }
        return game.getImage();
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
        connectedClients = new LinkedList<>();
        //players = new HashSet<>();
        int id = 0;
        Socket clientSocket = null;


        try {
            if (args.length != 2) {
                System.out.println("Expected arguments: <port(int)> <delay(int)>");
                System.exit(1);
            }
            int port = 9099; // default port
            int sleepDelay = 10000; // default delay
            //ServerSocket serv = null;

            try {
                port = Integer.parseInt(args[0]);
                sleepDelay = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                System.out.println("[Port|sleepDelay] must be an integer");
                System.exit(2);
            }
            ServerSocket serv = new ServerSocket(port);
            while (true) {
                System.out.println("Waiting for client to connect");
                clientSocket = serv.accept();
                System.out.println("Client " + id++ + " connected");
                Server server = new Server(clientSocket, game);
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

