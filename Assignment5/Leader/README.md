# Activity 2: Simplified Consensus Algorithm

### Screencast
https://youtu.be/oN0uGhnaSlI

### Project Explanation
This project is a simplified consensus algorithm that mimics some basic financial transactions. The project consists of a leader, several nodes, and several client.   
Th basic functionality consists of a client connecting to the leader node and requesting an amount of credit, upon receiving this request the leader checks with the nodes if they have enough available funds to to grant the credit. If more nodes respond with "yes" than "no", the leader splits the amount specified by the client and each node holds a piece of the credit.  
The client can also request to payback owed credit. Upon receiving this request the leader queries the connected nodes, the nodes respond telling the leader if this client owes the node. The leader then splits the amount the client wishes to payback among the nodes that are owed.  
The leader and each nodes maintains their own ledgers tracking how much a client owes.  
The leader regularly syncs with the nodes, and in the case that leaders ledger does not match the sum of the nodes ledgers the leader redistributes the debt among the connected nodes. This situation can arise if a node crashes for example.   
The leaders ledger and each nodes ledger is stored persistently and will be loaded at start up.  
The leader is the primary node and in the case that the leader goes down, all nodes will shutdown and the project must be restarted, 

### Instructions
Please start the leader first using "gradle leader -q --console=plain"  
Then start the nodes in order with   
"gradle node1 -q --console=plain"  
"gradle node2 -q --console=plain"  
"gradle node3 -q --console=plain"  
The default amount of money for the nodes is 1000, you can use the argument "-Pmoney='amount of money'" to set it to a specific amount.  
Example: "gradle node1 -Pmoney=2000 -q --console=plain"  
Note: I only included 3 nodes in the build file, but you could add more if you wanted as long as the port numbers are sequential.  
Finally start the clients with "gradle client -q --console=plain"  
Note: nodes can be started after clients, the important part is that the leader needs to be started first and the nodes started sequentially.  
Multiple clients can be started. 

### Requirements Met
- Project is well structured and easy to understand
- Leader can be started with "gradle leader" using Port 8000 as the default
- Nodes can be started with "gradle node1", "gradle node2", or "gradle node3"
- Nodes start with a default amount of money. Amount of money can also be set through gradle arguments
- Client starts with "gradle client" and connects to leader 
- Leader asks client for ID/name. Client responds with name
- Client is presented with a menu allowing the to request credit, paying back credit that is owed, or quitting
- The Client can specify the amount of credit/payback
- Leader receives request
- If the request is for a credit
    - Leader sends all nodes the request
    - Nodes check if they have enough money 
        - If the client is new, the node need 150% of the requested amount
        - If the client already has credit with that node, the node needs at least 100% of the requested amount
    - If the majority of nodes vote yes, the credit is granted to the client
    - If the credit is granted, the leader splits the amount between the connect nodes
    - The nodes and the leader persistently track the owed amount
    - The leader informs the client the credit has been granted 
    - If the majority of nodes vote no, the client is informed that their request has been denied
- If the request is a payback
    - The leader queries the nodes and generates a list of nodes that are owed by the client
    - Nodes tell the leader if they are owed and how much
    - Leader splits the amount between nodes
        - Split is as even as possible
        - Nodes will not take money they are not owed
    - Nodes and leader update ledgers 
    - Client is informed and told their remaining balance 
    - Clients can not pay back more than they owe 
- A node crashing will not affect the system stability
- If the leader goes down, all nodes are shut down 
- Leader syncs its own ledger with the ledgers of connected nodes. If a node goes down, debt can be reallocated so failure is transparent to the client 
- Multiple clients can interact with the leader 
- New nodes can join the network seamlessly 

### Protocol
Leader <---> Client
    - Requests
        - type : name - Client sends name to leader 
        - Fields
            - name - Clients name 
        - Response
            - type : greeting - Leader sends greeting to client 
            - credit - Amount owed by client 
            - message - Welcome message for client
                
        - type : credit - Client requests credit
        - Fields
            - amount - Amount of credit client wants 
        - Response
            - type : creditResponse
            - amount - Amount requested by client
            - credit - Total credit owed by client 
            - approved - Boolean representing if credit was approved 
            
        - type : payback - Client wants to payback owed credit
        - Fields
            - amount - Amount client wants to payback
        - Response 
            - type : paybackResponse
            - amount - Amount client wants to payback
            - credit - Total credit owed by client 
            - approved - Boolean representing if payback was approved (Can't payback more than is owed)
                
Leader <---> Node
    - Requests
        - type : credit - Ask nodes if they have enough money to authorize the credit
        - Fields
            - name - Name of the client making request
            - amount - Amount requested by client
        - Response
            - type : vote
            - vote - Node says Yes or No to client request 
            
        - type : creditGrant - Majority of nodes approved request, leader sends split amount to nodes
        - Fields
            - amount - Amount of credit to be held on node 
            - name - Name of client the credit belongs to 
            
        - type : payback
        - Fields
            - name - Name of the client paying back 
        - Response
            - owed - Boolean representing if client owes the node
            - amount - Amount owed by client 
            
        - type : nodePayback - Distributes payback amount amongst owed nodes
        - Fields
            - name - Name of client paying back
            - paybackAmount - Amount of payback going to node 
            
        - type : sync - Checks if leaders ledger is in sync with nodes ledger
        - Fields
            - No fields
        - Response 
            - name - Name of client on ledger
            - amount - Amount owed to node by client
            * Note: Node stores ledger as JSON object and simply sends the entire ledger, name : amount are key : value pairs
            
        - type : resync - Resyncs the nodes ledger to the leaders ledger 
        - Fields
            - name - Name of client on ledger
            - amount - Amount owed to node by client
            * Note: Leader generates ledger as JSON object and simply sends the entire ledger, name : amount are key : value pairs
                
           
