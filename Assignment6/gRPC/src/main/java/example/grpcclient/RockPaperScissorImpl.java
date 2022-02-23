package example.grpcclient;

import io.grpc.stub.StreamObserver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import service.*;

import java.io.*;
import java.util.Iterator;
import java.util.Random;

public class RockPaperScissorImpl extends RockPaperScissorsGrpc.RockPaperScissorsImplBase {

    private JSONObject leaderboard;

    public RockPaperScissorImpl() {
        this.leaderboard = readLeaderboard();
    }

    public void play(PlayReq req, StreamObserver<PlayRes> responseObserver) {
        System.out.println("Received from " + req.getName());

        PlayReq.Played clientChoice = req.getPlay();
        PlayReq.Played computerChoice = getComputerPlay();

        String message;
        boolean win;
        if (clientChoice.equals(computerChoice)) {
            message = "\nIt's a tie!\n";
            updateLeaderboard(req.getName(), false);
            win = false;
        } else if ((clientChoice.equals(PlayReq.Played.ROCK) && computerChoice.equals(PlayReq.Played.SCISSORS)) ||
                (clientChoice.equals(PlayReq.Played.PAPER) && computerChoice.equals(PlayReq.Played.ROCK)) ||
                (clientChoice.equals(PlayReq.Played.SCISSORS) && computerChoice.equals(PlayReq.Played.PAPER))) {
            message = "\nYou win! " + clientChoice + " beats " + computerChoice + "\n";
            updateLeaderboard(req.getName(), true);
            win = true;
        } else {
            message = "\nYou lose! " + computerChoice + " beats " + clientChoice + "\n";
            updateLeaderboard(req.getName(), false);
            win = false;
        }

        PlayRes response;
        try {
            saveLeaderboard();
            response = PlayRes.newBuilder()
                    .setIsSuccess(true)
                    .setMessage(message)
                    .setWin(win)
                    .build();
        } catch (Exception e) {
            response = PlayRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Oh no, something bad happened")
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    public void updateLeaderboard(String name, boolean won) {
        JSONObject stats;
        if (leaderboard.has(name)) {
            if (won) {
                stats = leaderboard.getJSONObject(name);
                int oldWins = stats.getInt("wins");
                stats.put("wins", ++oldWins);
            } else {
                stats = leaderboard.getJSONObject(name);
                int oldLosses = stats.getInt("losses");
                stats.put("losses", ++oldLosses);

            }

        } else {
            stats = new JSONObject();
            if (won) {
                stats.put("wins", 1);
                stats.put("losses", 0);
            } else {
                stats.put("wins", 0);
                stats.put("losses", 1);
            }
        }
        leaderboard.put(name, stats);

    }

    public JSONObject readLeaderboard() {
        BufferedReader leaderboardReader = null;
        JSONTokener tokener;
        try {
            File file = new File("src/main/resources/leaderboard.txt");

            leaderboardReader = new BufferedReader(new FileReader(file));
            tokener = new JSONTokener(leaderboardReader);
            return new JSONObject(tokener);
        } catch (FileNotFoundException e) {
            System.out.println("No ledger yet");
            return new JSONObject();
        } catch (JSONException e) {
            System.out.println("Ledger file is blank");
            return new JSONObject();
        }
        finally {
            if (leaderboardReader != null) {
                try {
                    leaderboardReader.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveLeaderboard() {
        File file = new File("src/main/resources/leaderboard.txt");
        FileWriter fileWriter = null;
        try {
            if (file.createNewFile()) {
                System.out.println("New ledger created");
            }
            fileWriter = new FileWriter("src/main/resources/leaderboard.txt");
            fileWriter.write(leaderboard.toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Problem saving client ledger");
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("problem closing ledger file");
            }
        }
    }

    public PlayReq.Played getComputerPlay() {
        Random rand = new Random();
        int randomNumber = rand.nextInt(3);
        PlayReq.Played computerChoice = null;

        switch(randomNumber) {
            case 0:
                computerChoice = PlayReq.Played.ROCK;
                break;
            case 1:
                computerChoice = PlayReq.Played.PAPER;
                break;
            case 2:
                computerChoice = PlayReq.Played.SCISSORS;
                break;
        }

        return computerChoice;

    }

    public void leaderboard(Empty req, StreamObserver<LeaderboardRes> responseObserver) {
        LeaderboardRes response = null;
        try {
            LeaderboardRes.Builder responseBuild = LeaderboardRes.newBuilder();
            Iterator<String> players = leaderboard.keys();

            while (players.hasNext()) {
                String key = players.next();
                JSONObject stats = leaderboard.getJSONObject(key);
                int wins = stats.getInt("wins");
                int losses = stats.getInt("losses");
                LeaderboardEntry entry = LeaderboardEntry.newBuilder()
                        .setName(key)
                        .setWins(wins)
                        .setLost(losses)
                        .build();
                responseBuild.addLeaderboard(entry);
                responseBuild.setIsSuccess(true);

                response = responseBuild.build();
           }
        } catch (Exception e) {
            response = LeaderboardRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Something bad happened")
                    .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
