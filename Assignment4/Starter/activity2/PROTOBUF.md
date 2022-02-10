### The procotol
You will see a response.proto and a request.proto file. You should implement these in your program. 
Protocol description
Request:
- NAME: a name is sent to the server, fields
	- name -- name of the player
	Response: GREETING, fields 
			- message -- greeting text from the server
- LEADER: client wants to get leader board
	- no further data
	Response: LEADER, fields 
			- leader -- repeated fields of Entry
- NEW: client wants to enter a game
	- no further data
	Response: TASK, fields
			- image -- current image as string
			- task -- current task for the cilent to solve
- ANSWER: client sent an answer to a server task
	- answer -- answer the client sent as string
	Response: TASK, fields 
			- image -- current image as string
			- task -- current task for the cilent to solve
			- eval -- true/false depending if the answer was correct
	OR
	Response: WON, fields
			- image -- competed image as string
- QUIT: clients wants to quit connection
	- no further data
	Response: BYE, fields 
		- message -- bye message from the server

Response ERROR: anytime there is an error you should send the ERROR response and give an appropriate message. Client should act appropriately
	- message
