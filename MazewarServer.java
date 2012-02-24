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
    private int seeds=42;
    private final int topindex = 0;

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
        seeds= (int) Math.random();
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

        
        ObjectOutputStream[] toplayer  = new ObjectOutputStream[waitForNumClients];
        while (currClient!=waitForNumClients) {
        	synchronized(clients)
        	{
				//Continuously listen for and accept client connection requests
	        	clients.add(currClient++,new MazewarServerHandlerThread(serverSocket.accept()));
        		MazewarPacket toclientpacket = new MazewarPacket();
        		toclientpacket.setAction(MazewarPacket.ACTION_JOIN);
        		toclientpacket.setSeed(seeds);
        		toclientpacket.setPlayerID(currClient-1);
        		toclientpacket.setMaxplayer(waitForNumClients);
	        	//initialize and get ready for game to begin soon
/*      		if(toplayer==null)
        			System.out.println("topplayer is null");
        		if(clients.get(currClient-1)==null)
        			System.out.println("clients.get(currClient-1) is null");
        		if(clients.get(currClient-1).getClientSocket()==null)
        			System.out.println("clients.get(currClient-1).getClientSocket() is null");
*/	        	toplayer[currClient-1] = new ObjectOutputStream(clients.get(currClient-1).getClientSocket().getOutputStream());
	        	toplayer[currClient-1].writeObject(toclientpacket);
	        	//clients.get(currClient-1).start();
	            //Send START packets with clientID (aka seed number) to generate map
        	}
        }//Exit accepting connections
        
        /*
         * At this point of time all the clients have received the seed number and are ready to START the game
         * the for loop command all the clients to START
         */
        for(int i =0;i<waitForNumClients;i++) {
        	MazewarPacket toclientpacket = new MazewarPacket();
    		toclientpacket.setAction(MazewarPacket.ACTION_START);
    		toclientpacket.setPlayerID(i);
        	toplayer[i].writeObject(toclientpacket);
        }

        
		/*
		 *  At this point the game has started and any change made by any client has started recording in
		 *  Queue i.e. in the arraylist <serverQueue> and the topmost packet needs to be broadcasted to 
		 *  all the clients
		 */
        List<MazewarPacket> Queue = MazewarServerHandlerThread.serverQueue;
        while (listening) {
        	synchronized(Queue) {
        		if(Queue.isEmpty()){
        			// relaxing time ... nothing left to do
                    //System.out.println("Queue EMPTY");
        		}
        		else{
        			MazewarPacket toclientpacket =  (MazewarPacket) Queue.remove(topindex);
        			toclientpacket.setAction(MazewarPacket.ACTION_MOVE);
            		//need to broadcast this packet, so send it to all the clients
            		for(int i =0;i<waitForNumClients;i++) {
                		toplayer[i].writeObject(toclientpacket);
            		}
        		}	
        	}
        }
        serverSocket.close();
        
        //Send JOIN packet to all clients with their initial position (they know their own ID from START)
        
    }
}