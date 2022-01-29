package server;

import org.json.JSONObject;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Stack;

public class GameLogic {
    private int score;
    private int numberOfGuesses;
    private int quoteNumber;
    private int correctGuesses;
    private String quoteCharacter;
    private boolean guessWasCorrect;
    private boolean gameOver;
    private HashMap<String, Integer> characterMap;

    public GameLogic() {
        this.score = 0;
        this.numberOfGuesses = 0;
        this.quoteNumber = 0;
        this.quoteCharacter = "";
        this.guessWasCorrect = false;
        this.correctGuesses = 0;
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
}
