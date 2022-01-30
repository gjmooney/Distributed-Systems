package server;

import org.json.JSONObject;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class GameLogic {
    private int score;
    private int numberOfGuesses;
    private int quoteNumber;
    private int correctGuesses;
    private String quoteCharacter;
    private boolean guessWasCorrect;
    private boolean gameOver;
    private HashMap<String, Integer> characterMap;
    private TreeMap<String, Integer> leaderboard;

    public GameLogic() {
        this.score = 0;
        this.numberOfGuesses = 0;
        this.quoteNumber = 0;
        this.quoteCharacter = "";
        this.guessWasCorrect = false;
        this.correctGuesses = 0;
        this.leaderboard = new TreeMap<>();
        initMap();
    }

    public void initMap() {
        // just for convenience
        characterMap = new HashMap<>();
        String[] characters = {"Captain_America", "Darth_Vader", "Homer_Simpson", "Jack_Sparrow",
                "Joker", "Tony_Stark", "Wolverine"};
        // add names to map
        for (String character: characters ) {
            characterMap.put(character, 1);
        }
    }

    public int getQuoteNumber(String character) {
        System.out.println("[getQuoteNumber] " + characterMap);
        int quoteNum = characterMap.get(character);
        System.out.println("[etuoteNum] " + characterMap.get(character) + "saved num " + quoteNum);
        if (quoteNum < 4) {
            characterMap.put(character, quoteNum + 1);
        }
        return quoteNum;
    }

    public void checkAnswer(String answerFromClient) {
        String formatClientAnswer = answerFromClient.toLowerCase(Locale.ROOT);
        String formatCorrectAnswer = getQuoteCharacter().replaceAll("_", " ")
                .toLowerCase(Locale.ROOT);
        System.out.println("client: " + formatClientAnswer + " correct: " + formatCorrectAnswer);
        if (formatClientAnswer.equals(formatCorrectAnswer)) {
            setGuessWasCorrect(true);
            changeScore();
            System.out.println("checkAnswer: correct");
            setNumberOfGuesses(0);
            setCorrectGuesses(getCorrectGuesses() + 1);
            if (getCorrectGuesses() == 3) {
                setGameOver(true);
            }
        } else {
            setGuessWasCorrect(false);
            setNumberOfGuesses(getNumberOfGuesses() + 1);
            System.out.println("checkAnswer: wrong");
        }
    }

    public void updateLeaderboard(String name) {
        if (!leaderboard.containsKey(name)) {
            // player not on board yet
            leaderboard.put(name, getScore());
        } else {
            //player already on board
            int oldScore = leaderboard.get(name);
            leaderboard.put(name, oldScore + getScore());
        }
    }

    public String displayLeaderboard() {
        String message;

        if (!leaderboard.isEmpty()) {
            message = "Here's the leaderboard\n" + leaderboard;
        } else {
            message = "There's nobody on the leaderboard yet! You're the first player!";
        }

        message += "\nEnter anything to continue...";
        return message;
    }


    public void checkTimer(LocalTime timeLimit, LocalTime timeReceived) {
        if (timeReceived.isAfter(timeLimit)) {
            setGameOver(true);
        }

    }

    public void changeScore() {
        //5 points for first guess, 4 for second, 3 for third, 1 for rest
        switch (getNumberOfGuesses()) {
            case 0:
                //first guess
                setScore(getScore() + 5);
                break;
            case 1:
                setScore(getScore() + 4);
                break;
            case 2:
                setScore(getScore() + 3);
                break;
            default:
                setScore(getScore() + 1);
                break;
        }
    }

    public void saveChoices(String character, int number) {
        setQuoteCharacter(character);
        setQuoteNumber(number);
        System.out.println("Game Logic: Saved char: " + getQuoteCharacter());
    }

    public void resetGame() {
        setScore(0);
        setNumberOfGuesses(0);
        setGameOver(false);
        setCorrectGuesses(0);
        initMap();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getNumberOfGuesses() {
        return numberOfGuesses;
    }

    public void setNumberOfGuesses(int numberOfGuesses) {
        this.numberOfGuesses = numberOfGuesses;
    }

    public int getQuoteNumber() {
        return quoteNumber;
    }

    public void setQuoteNumber(int quoteNumber) {
        this.quoteNumber = quoteNumber;
    }

    public String getQuoteCharacter() {
        return quoteCharacter;
    }

    public void setQuoteCharacter(String quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
    }

    public boolean isGuessWasCorrect() {
        return guessWasCorrect;
    }

    public void setGuessWasCorrect(boolean guessWasCorrect) {
        this.guessWasCorrect = guessWasCorrect;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public int getCorrectGuesses() {
        return correctGuesses;
    }

    public void setCorrectGuesses(int correctGuesses) {
        this.correctGuesses = correctGuesses;
    }

    public TreeMap<String, Integer> getLeaderboard() {
        return leaderboard;
    }


}
