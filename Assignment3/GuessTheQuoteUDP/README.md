# Assignment 3 - Guess The Quote

## Description
This program is a UDP version of a server/client quote guessing game. Both the server and clients are started via gradle with options to set the host name and port number. The games starts when a client connects to the server. The server sends a prompt asking for the clients name, upon receiving the name the client greets the client and presents them with a picture of a very cute pig.  

That's it. The client can keep looking at the cute pig or quit.   
The server shuts down when the client quits.   

## Checklist
- [x] Client connects to server  
- [x] The server asks the name of the player  
- [x] The client sends thier name to the server  
- [x] The server receives the name and greets the player  
- [x] The server sends the client a picture of a cute pig

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
![UML](https://github.com/gjmooney/ser321-spring2022-A-gjmooney/blob/a27f7086f971c55cabdda6f2e99f911cb9abae97/Assignment3/GuessTheQuoteUDP/SequenceDiagram.png "UML Diagram")

## Protocol
The protocol I used is pretty simple. It is a JSONObject consisting of a header and a payload. The header contains state information, the type of data being sent, and an 'ok' flag. The payload consists of the image, score, and text to be displayed.  
The states are:  
- 1 - Server listening for client
- 2 - Greeting and cute pig
- 3 - Prompt the user to quit
- 5 - Disconnect client   
  
The JSON object is converted to bytes and sent in 1024 byte long packets that include the total number of packets and the packets place in the sequence.

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
The program is designed to be robust by implementing try/catch blocks in any instance of network communication. If the server loses the client, it does not crash. If the client loses the server it also does not crash. If/else statements are used to handle undesired input and prompt the user to enter correct input, and to ensure stability if an image resource cannot be found. 


 
