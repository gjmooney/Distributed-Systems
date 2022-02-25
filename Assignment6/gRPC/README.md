# Assignment 6

### Screencast
https://youtu.be/3xLQf2euVtg

### Description
This program utilizes GRPC to allow a client and node to communicate. The node hosts a variety of services that the client can use. The client provides a console based menu that the user can use to invoke the services. I implement the Rock, Paper, Scissors and Timers service, and created a service that is a simple Caesar Cipher, encrypting a message by shifting the letters by a certain amount.

#### Task 2 Details
For task 2 I created a service that implements a simple Caesar Cipher which encrypts a message by shifting the letters by a given amount. The service prompts the user to enter a phrase to encrypt and a positive number to be use as the key. For example if you want to encrypt "A" using a a key of 1 the encrypted message would be "B". The service also allows for the decryption of messages, prompting the user to enter an encrypted message the corresponding key and return the unencrypted phrase. The service also allows the user to display a list of all previously encrypted messages and their corresponding key. The list is saved to a file and is persistent through node restarts. 

### Running the programs

#### Tasks 1 and 2
gradle runNode  
gradle runClient -q --console=plain  

Run using default values  
Client provides a menu interface to interact with services and will prompt the user for needed input.  

For auto-mode: Run client with  
gradle runClient -Pauto=1 -q --console=plain  

Auto mode will run through all implemented services and exit upon completion

#### Task 3
gradle runRegistryServer  
gradle registerServiceNode  
gradle runClient2 -q --console=plain  

### Requirements
#### Task 1
- Client and node run via Gradle using default arguments
- Rock, Paper, Scissors and Timer services implemented using GRPC
- Client provides user with easy to use interface, showing available services and prompts to enter relevant data 
- Can use gradle argument to set port and host used
- Can use gradle argument to run in auto mode, calling all services with pre-defined input
- Server and Client are robust
#### Task 2
- Created a new service that implements a simple Caesar Cipher
- Created .proto file 
- Service allows 3 requests - encrypt, decrypt, and list encrypted messages
- Two of the requests require input - Phrase to be encrypted, and an offset to use for encryption
- Requests return different data - encrypted phrase, decrypted phrase, and a list of encrypted phrases with their offset
- Third request returns a repeated field
- Saved data is persistent
#### Task 3
- Created new version of client and node that use the registry
- New versions run through gradle
- Node registers services with registry
- Client contacts registry and print a list of available services
- User can select one using a number as input
- Client contacts registry and finds the server running the service
- Client uses that server to process request
    
