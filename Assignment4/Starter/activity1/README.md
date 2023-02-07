# Assignment 4 Activity 1

### Recording 
https://youtu.be/Fin5kc6wXQE  

### Description
This program consists of three server options and a client. The client can perform some simple string manipulation functions. It can add a string to the list, pop the top element of the list, display the current list, count the number of elements in the lists, and switch two elements in the list.  
The list is maintained on the server.   
The server is available in three varietes. The first (Task 1) is single thhreaded, the second (Task 2) is multi threaded and allows an unbounded number of clients to connect, the third (Task 3) utilizes a thread pool and limits how mnay clients can be connected at once. The amount of threads can be defined by passing a parameter when starting it. 

### Running the project
The servers can be started with default options by using the command  
gradle runTask1  
gradle runTask2  
gradle runTask3  

One server at a time please.   

and the client can be ran with  
gradle runClient  


#### Default 
The default port is 8000, the default host is localhost, and in Task3, the deafault number of threads is 5


#### With parameters:
You can change the port and host by using the following commands  
(Port, host, and threads are examples, use whatever you like)
gradle runClient -Phost=localhost -Pport=9099 -q --console=plain  
gradle runTask1 -Pport=9099 -q --console=plain  
gradle runTask2 -Pport=9099 -q --console=plain  
gradle runTask3 -Pport=9099 -Pthreads=5 -q --console=plain  

### Working with the program
The server must be started first.  
After the menu is displayed, enter the number of the selection you wish to choose  
If you select the switch option, it will be followed with a prompt to enter the first index, then the second index

### Requirements
- [x] Made the performer more interesting
- [x] Added pop, display, count, and switch functionality to performer
- [x] Allow unbounded connections in task 2 server
- [x] State of string list is shared between clents
- [x] Implement thread pool in task 3 server 
- [x] Allow numer of threads to be set via command line parameter
