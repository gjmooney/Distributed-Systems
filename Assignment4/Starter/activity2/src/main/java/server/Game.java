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
    JSONObject playerInfo;

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
        playerInfo = readLeaderboard();
    }

    synchronized public JSONObject readLeaderboard() {
         BufferedReader leaderboardReader = null;
         JSONTokener tokener;
         try {
             File file = new File("src/main/resources/leaderboard.txt");
             leaderboardReader = new BufferedReader(new FileReader(file));
             tokener = new JSONTokener(leaderboardReader);
             return new JSONObject(tokener);
         } catch (FileNotFoundException e) {
             System.out.println("No leaderboard yet");
             return new JSONObject();
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
        if(!playerInfo.has(name)) {
            JSONObject stats = new JSONObject();
            stats.put("wins", 0);
            stats.put("logins", 1);
            stats.put("taskAnswer", "");
            stats.put("tilesToFlip" , 1);
            playerInfo.put(name, stats);
        } else {
            JSONObject temp = (JSONObject) playerInfo.get(name);
            temp.put("logins", (int) temp.get("logins") + 1);
        }
        System.out.println(playerInfo);
    }

    synchronized public void updatePlayerInfo(String name) {
         if (playerInfo.has(name)) {
             JSONObject temp = (JSONObject) playerInfo.get(name);
             temp.put("wins", (int) temp.get("wins") + 1);
         } else {
             System.out.println("update leaderboard issue");
         }
    }

    synchronized public void saveLeaderboard() {
        FileWriter file = null;
         try {
             file = new FileWriter("src/main/resources/leaderboard.txt");
             file.write(playerInfo.toString());
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

    public JSONObject getPlayerInfo() {
        return playerInfo;
    }

    public String getPlayersTaskAnswer(String name) {
         JSONObject temp = (JSONObject) playerInfo.get(name);
         return (String) temp.get("taskAnswer");
    }

    /**
     * Sets the won flag to true
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

    /**
     * Replaces num characters in the image. I used it to turn more than one x when the task is fulfilled
     * @param num -- number of x to be turned
     * @return String of the new hidden image
     */
    public String replaceNumCharacters(int num){
        for (int i = 0; i < num; i++){
            if (getIdx()< getIdxMax())
                replaceOneCharacter();
        }
        return getImage();
    }

    public void setNumberOfTilesToFlip(String name) {
        if (playerInfo.has(name)) {
            JSONObject temp = (JSONObject) playerInfo.get(name);
            int minimum = idxMax / 8;
            int numToFlip;
            // makes the game easier for new players
            if ((int) temp.get("tilesToFlip") == 1) {
                numToFlip = minimum;
            } else {
                numToFlip = minimum + ((int) temp.get("logins") - (int) temp.get("wins"));
            }
            temp.put("tilesToFlip", numToFlip);
        }
    }

    public int getNumberOfTilesToFlip(String name) {
        if (playerInfo.has(name)) {
            JSONObject temp = (JSONObject) playerInfo.get(name);
            return (int) temp.get("tilesToFlip");
        } else {
            return idxMax / 8;
        }
    }

    synchronized public String chooseTask(String name) {
        Random rand = new Random();
        int task = rand.nextInt(4);
        String taskText;

        switch (task) {
            case (0):
                taskText = "Enter e";
                setCorrectAnswer(name, "e");
                break;
            case (1):
                taskText = "What's 2 * 3 ?";
                setCorrectAnswer(name, String.valueOf(6));
                break;
            case (2):
                taskText = "What's the missing vowel? \nB*n*n*";
                setCorrectAnswer(name, "a");
                break;
            case(3):
                taskText = "What is the capital of Germany?";
                setCorrectAnswer(name, "berlin");
                break;
            default:
                taskText = "we broke";
                setCorrectAnswer(name, "yerp");
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

    synchronized public void setCorrectAnswer(String name, String correctAnswer) {
        if (playerInfo.has(name)) {
            JSONObject temp = (JSONObject) playerInfo.get(name);
            temp.put("taskAnswer", correctAnswer);
        } else  {
            System.out.println("Player " + name + " not on list");
        }
        //this.correctAnswer = correctAnswer;
    }
}
