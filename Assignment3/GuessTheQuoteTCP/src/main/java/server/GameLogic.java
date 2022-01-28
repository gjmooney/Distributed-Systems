package server;

import org.json.JSONObject;

import java.util.Locale;

public class GameLogic {
    private int score;
    private int numberOfGuesses;
    private int quoteNumber;
    private int correctGuesses;
    private String quoteCharacter;
    private boolean guessWasCorrect;
    private boolean gameOver;

    public GameLogic() {
        this.score = 0;
        this.numberOfGuesses = 0;
        this.quoteNumber = 0;
        this.quoteCharacter = "";
        this.guessWasCorrect = false;
        this.correctGuesses = 0;
    }

    public void checkAnswer(String answerFromClient) {
        if (answerFromClient.equals(getQuoteCharacter())) {
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

    public JSONObject buildResponse(String answerFromClient) {
        JSONObject payloadForServer = new JSONObject();
        System.out.println("[CHECK ANSWER]");
        System.out.println("[ANSWER] " + getQuoteCharacter());
        System.out.println("[GUESS] " + answerFromClient);

        if (!isGameOver()) {
            if (isGuessWasCorrect()) {
                //correct answer
                payloadForServer.put("text", "You got it right!");
                payloadForServer.put("score", getScore());
            } else {
                //wrong answer
                payloadForServer.put("text", "NOPE! you got it wrong!\nGuess again!");
                payloadForServer.put("score", getScore());
            }
        } else {
            payloadForServer.put("text", "You won!!!!");
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
