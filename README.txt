ECE419S 2012 Lab 2 - Mazewar Distributed game using Centralized server
Date: February 26, 2012			Due Date: February 27, 2012
-----------
Developers:
-----------
Bozhidar Lenchov 	: Student Number 995959431
Kush Dua			: Student Number 996081957

-----------------
Run instructions:
-----------------
Server - server.sh <port to listen on> <number of players to join before game starts>
		Note: <number of players...> is optional; if omitted, server waits for 4 players
		
Client - run.sh <address of server> <port of server>

-----------
Our Design:
-----------
- Order of events is enforced by order for receival at server side. Server broadcasts event 1 to all clients
  before broadcasting event 2, so along with TCPIP stream delivery, we're guaranteed the order of events on
  the client side as well.
  
- The server waits until all players connect. When a player joins server replies with a JOIN message containing
  clientID and random seed (same for all clients in the game) for constructing the maze. When all clients have
  joined, server sends a START message, so each client can construct the maze (but not show it yet). Server then
  broadcasts SPAWN messages so all clients can instantiate themselves (GUIClient) or other clients (RemoteClient)
  for consistency before starting the game.
  
- After initiating the game, the server basically forwards events from its queue. Threads listening to incoming
  events from game clients (in the form of network packets) insert events in this server queue, which are then
  broadcast to all clients.
     - Server broadcasts events in order (broadcast event 1 to all clients before broadcasting event 2)
        + (positive) : consistent event order across player screens
        - (negative) : visual lag to clients that are closer to the server, since they have to wait for message
        			   to be received by clients further away for that event, before next event can be sent
  
- On GUI events, the client sends a network message only.

- On death, the GUIClient on the client that died calculates a new position and sends a SPAWN message to server
  (i.e. deaths are processed locally and right away, even for RemotePlayers. All game players (GUIClient and
   RemoteClient alike) are respawned when a SPAWN message from a client is received.
   
- If an action (spawn, movement, fire) cannot be performed when the message is received, we discard the message
  (and we are guaranteed all clients will reject it too because events are sent in order from the server).
  - In the case of a SPAWN message, when the client that generated the SPAWN sees it's own player ID in the
  	packet (i.e. it sent the original SPAWN message), and it rejects it as it cannot apply it (something else
  	is in the cell for respawn), it generates a new SPAWN point and sends it to the server for broadcast.

- Actions are applied only when clients receive a network packet event message from the server (movement, fire,
  spawn are sample events).
  
- Some further decisions (such as implementing network messages for projectile movement, sent by owner of
  projectile) are described in corner cases considered during design and/or development and testing.

-------------  
Corner Cases:
-------------
As mentioned above, order of event processing at the client side is ultimately determined by event receiving (and
thereby processing from the queue) at the server side. Since server broadcasts events in order (broadcast event 1
to all clients before broadcasting event 2), screens and gameplay is consistent across all clients. The following
cornercases and associated decisions were identified to ensure consistent gameplay, while minimizing network
congestion with gameplay messages:

1. Movement or fire message sent before GUIClient detects its death by projectile (so PROJECTILE_MOVE message causing
   death is received shortly after sending MOVE message)
     - Movement cannot possibly be applied after client has respawned, as:
     	1. GUIClient will be killed on each client's screen
     	2. Movement message will be received (but since client is dead, it cannot be applied, so it is discarded)
     	   - Movement message is received after PROJECTILE_MOVE and before SPAWN on all clients, as events are
     	     broadcasted in order of receival to all clients before next event is broadcasted by the server,
     	     and TCPIP guarantees delivery
     	3. SPAWN message for killed client is received and processed by all nodes
     -> No visual bug of movement occurring after respawning also since the killed client sends SPAWN after
        MOVEMENT message due to TPCIP stream delivery
     - Network delay not a factor since server transmits events in order (event 1 broadcasted to all before
       event 2 begins broadcasting) --> might cause visual lag as described in design, but is consistent...
        
2. Similar to the discussion of corner case 1, if projectile movement still was handled by each client:
     i.e. client 1's projectile thread handles movement of projectiles for itself, as well as remote players
     (assume there are still FIRE messages transmitted to indicate client 1 or client 2 fired, but movement is
     handled locally by each client)
   , this might cause discrepancies as MOVE message could be received fast by client 1 (which detects and
   processes a kill in its maze display), whereas the MOVE message is delayed when received by client 2, causing
   the projectile to miss the target (no kill), as it has gone past the point where the other client would've
   stepped in the path of the projectile.
   
   By having each client in the game handle projectiles locally only for itself, and send network messages every
   200ms (its Projectile movement Thread), we're guaranteed movement for that projectile will be consistent
   accross game screens, as projectile movement events are processed in order of receiving by the server.
   