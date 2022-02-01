# Assignment 3 - Guess The Quote

## Description
This program is a TCP version of a server/client quote guessing game. Both the server and clients are started via gradle with options to set the host name and port number. The games starts when a client connects to the server. The server sends a prompt asking for the clients name, upon receiving the name the client greets the client and presents them with an option to start the game or see the leaderboard.   
If the client chooses to see the leaderboard they are presented with a list of previous winners of the game and their score. The leaderboard is persistent through server restarts.   
If the client chooses to begin the game, the server sends them an image of a quote from a character. The client can they guess who said the quote. They can also enter 'more' to see more quotes from the same character, there are 4 quotes per character, and the last quote includes an image of the character. The client can also enter 'next' to receive a quote from another character. Quotes are only shown one time each  through the course of the game.   
When the client first chooses to start the game, a timer it set and they have 60 seconds to get 3 correct guess. If they guess three correctly within the limit they win. If this is their first win they are added to the leaderboard, if they're already on the leaderboard then their new score is added to their old total. If the timer runs out they lose and are not added to the leaderboard.   
Once the game is over the client is guven the option to enter their name to play again or to quit.   
If the client quits, the server closes the connection and begins listening for another client.  

## Checklist
- [x] Client connects to server  
- [x] The server asks the name of the player  
- [x] The client sends thier name to the server  
- [x] The server receives the name and greets the player  
- [x] The server gives the client the choice between seeing the leaderboard and starting the game  
- [x] If the client selects the leaderboard, they are shown all players that have been placed on the leaderboard along with their scores  
- [x] The leaderboard is persistent through server restarts  
- [x] If the client chooses to start the game, the server sends the first image quote  
- [x] The correct answer is printed in the servers terminal window  
- [x] The client can enter their guess, 'more', or 'next'  
- [x] If the client enters their guess, the server checks their answer  
- [x] If the answer is correct, the players score is updated and a new quote image is sent  
- [x] If the answer is incorrect, the player is informed and guess again, enter 'more', or enter 'next'  
- [x] If the client enters 'more', the server sends another quote from the same character  
- [x] If the client enters 'more' when the last image is already displayed, they are informed it's the last image for that character  
- [x] If the user enters 'next', the server sends a new quote from a different character  
- [x] If the user continues to enter 'next' the server will cycle through the characters   
- [x] If the server receivs three correct guesses without the timer running out it will send an image and message indicating the player has won  
- [x] If the server receivs any input and the timer has run out, it will send an image and message indicating the player has lost  
- [x] The server implements a tiered scoreing system, awarding 5 points if the player guesses correctly the first time, 4 points for the second time, 3 points for the third, and 1 point after that   
- [x] The player loses 2 points each time they enter 'next'  
- [x] The current score is always displayed in the users GUI  
- [x] At the end of the game, win or loser, the player receivs their final score  
- [x] If the player won the leaderboard is updated  
- [x] The first time a player wins, their name is added to the leaderboard  
- [x] For subsequent wins the players score will be updated by adding their new score to their old score  
- [x] If the player loses the leaderboard does not change  
- [x] All input evaluation happens on the server, the only things sent to the client are the image, text, and score to be displayed  
- [x] The custom protocol includes headers indicating game state and status  
- [x] The custom protocol is robust, the server is capable of responding to incorrect messages  
- [x] The programs are robust, error handling is included so neither the server or client crash on undesited input  
- [x] When the game is over, the player is able to play again by entering their name, or quit by entering quit  

## How to run the game
Run the server by entering   
gradle runServer  
or  
gradle runServer -Pport=8080  
with the desired port number  

Run the client by entering  
gradle runClient  
or  
gradle runClient -Pport=8080 -Phost='localhost'  
with the desired port and host IP  

Defaults are Port = 8080 and host = 'localhost'

## UML Diagram
![UML](https://github.com/gjmooney/ser321-spring2022-A-gjmooney/blob/0a7c3ea1ee721f580b86acf13ecb8dd138d59d91/Assignment3/GuessTheQuoteTCP/Sequence%20Diagram.png "UML Diagram")

## Protocol
The protocol I used is pretty simple. It is a JSONObject consisting of a header and a payload. The header contains state information, the type of data being sent, and an 'ok' flag. The payload consists of the image, score, and text to be displayed.  
The states are:  
- 1 - Server listening for client
- 2 - Greeting
- 3 - Start game or see leaderboard prompt
- 4 - Actual gameplay loop
- 5 - Disconnect client 

#### client -> server
```
{header : {  
    state : 1 | 2 | 3 | 4 | 5  
    type : json   
    ok : true | false  
    }  
payload : {  
    text : input from client  
    }  
}  
```
  
#### server -> client
```
{header : {  
    state : 1 | 2 | 3 | 4 | 5  
    type : json   
    ok : true | false  
    }   
payload : {  
    text : message to be printed on c lient GUI   
    image : String encoding of the image  
    score : the clients score   
    }  
}  
```

## Robustness
The program is designed to be robust by implementing try/catch blocks in any instance of network communication. If the server loses the client, it resets the state of the game and begins listeing for a new client to connect. If the client loses the server it termintes with an exit value of 1. If/else statements are used to handle undesired input and prompt the user to enter correct input, and to ensure stability if an image resource cannot be found. 



