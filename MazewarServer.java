import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.io.*;

public class MazewarServer {
	
	//Global list of events received at the server
	private static final List<MazewarServerHandlerThread> clients=Collections.synchronizedList(new ArrayList<MazewarServerHandlerThread>());
    //private static List<Socket> clientSockets = new LinkedList<Socket>();
    private final int seeds = 42;

	//keeps tracks of number of clients currently joined the game
	private int currClient=0;
	
    public static void main(String[] args) throws IOException {
    	new MazewarServer(args);
    }
    
    public MazewarServer(String[] args) throws IOException 
    {
        ServerSocket serverSocket = null;
        boolean listening = true;
        int waitForNumClients=4;

        try {
        	if(args.length == 2 || args.length == 1) {
				//Create server socket to listen for client connection requests
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        		//Second optional argument is number of clients to wait for until starting the game (default 4)
        		if(args.length==2)
        			waitForNumClients=Integer.parseInt(args[1]);
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        
        ObjectOutputStream player=null;
        while (currClient!=waitForNumClients) {
        	synchronized(clients)
        	{
				//Continuously listen for and accept client connection requests
	        	clients.add(currClient++,new MazewarServerHandlerThread(serverSocket.accept()));
        		MazewarPacket clientpacket = new MazewarPacket();
        		clientpacket.action = MazewarPacket.ACTION_JOIN;
	        	clientpacket.seed = seeds;
	        	//initialize and get ready for game to begin soon
	        	player = new ObjectOutputStream(clients.get(currClient-1).getClientSocket().getOutputStream());
	        	player.writeObject(clientpacket);
	        	clients.get(currClient-1).start();
	            //Send START packets with clientID (aka seed number) to generate map
        	}
        }//Exit accepting connections
        
        /*
         * At this point of time all the clients have received the seed number and are ready to START the game
         */

        List<MazewarPacket> Queue = MazewarServerHandlerThread.serverQueue;
        while (listening) {
        		/*
        		 *  At this point the game has started and any change made by any client should be recorded
        		 *  in the arraylist <serverQueue> and the packet recording that information should be send to 
        		 *  all the clients to make the respective necessary moves
        		 */

        }
        serverSocket.close();
        
        //Send JOIN packet to all clients with their initial position (they know their own ID from START)
        
    }
}