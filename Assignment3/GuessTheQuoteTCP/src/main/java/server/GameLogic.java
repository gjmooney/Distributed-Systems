package server;

import org.json.JSONObject;

import java.util.Locale;

public class GameLogic {
    private int score;
    private int numberOfGuesses;
    private int quoteNumber;
    private String quoteCharacter;

    public GameLogic() {
        this.score = 0;
        this.numberOfGuesses = 0;
        this.quoteNumber = 0;
        this.quoteCharacter = "";
    }

    public JSONObject checkAnswer(String answerFromClient) {
        JSONObject payloadForServer = new JSONObject();
        System.out.println("[CHECK ANSWER]");
        System.out.println("[ANSWER] " + getQuoteCharacter());
        System.out.println("[GUESS] " + answerFromClient);

        if (answerFromClient.equals(getQuoteCharacter())) {
            //correct answer
            payloadForServer.put("text", "You got it right!");
            // increase score based on number of guesses
            changeScore();
            payloadForServer.put("score", getScore());
        } else {
            //wrong answer
            payloadForServer.put("text", "NOPE! you got it wrong!");
            //score stays the same, number of guesses goes up
            setNumberOfGuesses(getNumberOfGuesses() + 1);
            payloadForServer.put("score", getScore());
        }

        return payloadForServer;
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
        //save the character in lower case with no underscores, should be what client sends
        setQuoteCharacter(character.replaceAll("_", " ").toLowerCase(Locale.ROOT));
        setQuoteNumber(number);
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
}
