package server;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;
import java.io.*;

/**
 * Class: Game 
 * Description: Game class that can load an ascii image
 * Class can be used to hold the persistent state for a game for different threads
 * synchronization is not taken care of .
 * You can change this Class in any way you like or decide to not use it at all
 * I used this class in my Server to create a new game and keep track of the current image evenon differnt threads.
 * My threads each get a reference to this Game
 */

public class Game {
    private int idx = 0; // current index where x could be replaced with original
    private int idxMax; // max index of image
    private char[][] original; // the original image
    private char[][] hidden; // the hidden image
    private int col; // columns in original, approx
    private int row; // rows in original and hidden
    private boolean won; // if the game is won or not
    private List<String> files = new ArrayList<String>(); // list of files, each file has one image
    //JSONObject leaderboard;
    HashMap<String, JSONObject> playerInfo;

    private String correctAnswer;


     public Game(){
        // you can of course add more or change this setup completely.
        // You are totally free to also use just Strings in your Server class
        // instead of this class
        won = true; // setting it to true, since then in newGame() a new image will be created
        files.add("pig.txt");
        files.add("snail.txt");
        files.add("duck.txt");
        files.add("crab.txt");
        files.add("cat.txt");
        files.add("joke1.txt");
        files.add("joke2.txt");
        files.add("joke3.txt");
        playerInfo = new HashMap<>();
        //leaderboard = tempMakeLeaderBoard();
    }

    public JSONObject tempMakeLeaderBoard() {
        JSONObject leaderboard = new JSONObject();


        leaderboard.put("bob", 100);
        leaderboard.put("chris", 50);


        return leaderboard;
    }

    synchronized public void readLeaderboard() {
         BufferedReader leaderboardReader = null;

         try {
             File file = new File("src/main/resources/leaderboard.txt");
             leaderboardReader = new BufferedReader(new FileReader(file));
             JSONTokener tokener = new JSONTokener(leaderboardReader);
         } catch (FileNotFoundException e) {
             System.out.println("No leaderboard yet");
         } finally {
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

    synchronized public void addClientToPlayerInfo(String name) {
        if(!playerInfo.containsKey(name)) {
            JSONObject stats = new JSONObject();
            stats.put("wins", 0);
            stats.put("logins", 1);
            playerInfo.put(name, stats);
        } else {
            JSONObject temp = playerInfo.get(name);
            temp.put("logins", (int) temp.get("logins") + 1);
        }
    }

    synchronized public void updatePlayerInfo(String name) {
         if (playerInfo.containsKey(name)) {
             JSONObject temp = playerInfo.get(name);
             temp.put("wins", (int) temp.get("wins") + 1);
         } else {
             System.out.println("update leaderboard issue");
         }
    }

    synchronized public void saveLeaderboard() {
        FileWriter file = null;
         try {
             file = new FileWriter("src/main/resources/leaderboard.txt");
             file.write(getJSONLeaderboard().toString());
         } catch (IOException e) {
             e.printStackTrace();
             System.out.println("Problem saving leaderboard");
         } finally {
             try {
                 if (file != null) {
                     file.flush();
                     file.close();
                 }
             } catch (IOException e) {
                 e.printStackTrace();
                 System.out.println("problem closing leaderboard file");
             }
         }

    }

    public JSONObject getJSONLeaderboard() {
        JSONObject jsonLeaderboard = new JSONObject();

        for (String key : playerInfo.keySet()) {
            jsonLeaderboard.put(key, playerInfo.get(key));
        }
        return jsonLeaderboard;
    }
    /**
     * Sets the won flag to true
     * @param args Unused.
     * @return Nothing.
     */
    synchronized public void setWon(){
        won = true;
    }

    /**
     * Method loads in a new image from the specified files and creates the hidden image for it. 
     * @return Nothing.
     */
    synchronized public void newGame(){
        if (won) {
            idx = 0;
            won = false; 
            List<String> rows = new ArrayList<String>();

            try{
                // loads one random image from list
                Random rand = new Random(); 
                col = 0;
                int randInt = rand.nextInt(files.size());
                File file = new File(
                        Game.class.getResource("/"+files.get(randInt)).getFile()
                        );
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    if (col < line.length()) {
                        col = line.length();
                    }
                    rows.add(line);
                }
            }
            catch (Exception e){
                System.out.println("File load error"); // extremely simple error handling, you can do better if you like. 
            }

            // this handles creating the orinal array and the hidden array in the correct size
            String[] rowsASCII = rows.toArray(new String[0]);

            row = rowsASCII.length;

            // Generate original array by splitting each row in the original array.
            original = new char[row][col];
            for(int i = 0; i < row; i++) {
                char[] splitRow = rowsASCII[i].toCharArray();
                for (int j = 0; j < splitRow.length; j++) {
                    original[i][j] = splitRow[j];
                }
            }

            // Generate Hidden array with X's (this is the minimal size for columns)
            hidden = new char[row][col];
            for(int i = 0; i < row; i++){
                for(int j = 0; j < col; j++){
                    hidden[i][j] = 'X';
                }
            }
            setIdxMax(col * row);
        }
        else {
        }
    }

    /**
     * Method returns the String of the current hidden image
     * @return String of the current hidden image
     */
    synchronized public String getImage(){
        StringBuilder sb = new StringBuilder();
        for (char[] subArray : hidden) {
            sb.append(subArray);
            sb.append("\n");
        }
        return sb.toString();
    }

    synchronized public String getOriginalImage(){
        StringBuilder sb = new StringBuilder();
        for (char[] subArray : original) {
            sb.append(subArray);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Method changes the next idx of the hidden image to the character in the original image
     * You can change this method if you want to turn more than one x to the original
     * @return String of the current hidden image
     */
    synchronized public String replaceOneCharacter() {
        int colNumber = idx%col;
        int rowNumber = idx/col;
        hidden[rowNumber][colNumber] = original[rowNumber][colNumber];
        idx++;
        return(getImage());
    }

    synchronized public String replaceHalfCharacter() {

        for (int i = 0; i < col/2; i++) {
            int colNumber = idx%col;
            int rowNumber = idx/col;
            hidden[rowNumber][colNumber] = original[rowNumber][colNumber];
            idx++;
        }

        return(getImage());
    }

    synchronized public String chooseTask() {
        Random rand = new Random();
        int task = rand.nextInt(2);
        String taskText;

        switch (task) {
            case (0):
                taskText = "Enter e";
                setCorrectAnswer("e");
                break;
            case (1):
                taskText = "Whats 2 * 3 ?";
                setCorrectAnswer(String.valueOf(6));
                break;
            default:
                taskText = "we broke";
                setCorrectAnswer("yerp");
                break;
        }
        return taskText;
    }

    synchronized public int getIdxMax() {
        return idxMax;
    }

    synchronized public void setIdxMax(int idxMax) {
        this.idxMax = idxMax;
    }

    synchronized public int getIdx() {
        return idx;
    }

    synchronized public void setIdx(int idx) {
        this.idx = idx;
    }

    synchronized public String getCorrectAnswer() {
        return correctAnswer;
    }

    synchronized public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}
