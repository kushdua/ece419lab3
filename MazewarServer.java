import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.*;

public class MazewarServer {
	
	//Global list of events received at the server
	private static final List<MazewarServerHandlerThread> clients=Collections.synchronizedList(new ArrayList<MazewarServerHandlerThread>());
	
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

        while (currClient!=waitForNumClients) {
        	synchronized(clients)
        	{
				//Continuously listen for and accept client connection requests
	        	clients.add(currClient++,new MazewarServerHandlerThread(serverSocket.accept()));
	        	clients.get(currClient-1).start();
	            //Send START packets with clientID (aka seed number) to generate map
        	}
        }
        //Exit accepting connections
        serverSocket.close();
        
        //Send JOIN packet to all clients with their initial position (they know their own ID from START)
        
    }
}