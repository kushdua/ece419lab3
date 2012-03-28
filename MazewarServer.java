import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.*;

public class MazewarServer {
	
	//Global list of events received at the server
	//private static final List<MazewarServerHandlerThread> clients=Collections.synchronizedList(new ArrayList<MazewarServerHandlerThread>());
	public static final Map<Integer,MazewarServerHandlerThread> clients=Collections.synchronizedMap(new HashMap<Integer, MazewarServerHandlerThread>());
    //private static List<Socket> clientSockets = new LinkedList<Socket>();
    static int seeds=42;
    private final int topindex = 0;
    static int seqno;
    static int waitForNumClients=4;
	//keeps tracks of number of clients currently joined the game
	static int currClient=0;
	static Hashtable<Integer,NetworkAddress> Player= new Hashtable<Integer, NetworkAddress>();

    static ObjectOutputStream[] toplayer  = new ObjectOutputStream[waitForNumClients];
	
    public static void main(String[] args) throws IOException {
    	new MazewarServer(args);
    }
    
    public MazewarServer(String[] args) throws IOException 
    {
        ServerSocket serverSocket = null;
        boolean listening = true;
        seeds= (int) Math.random();
        boolean recover=false;
        try {
        	if(args.length <= 3) {
				//Create server socket to listen for client connection requests
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        		//Second optional argument is number of clients to wait for until starting the game (default 4)
        		//Third optional argument is for recovery from failure
        		if(args.length==2)
        		{
        			if(args[1].compareToIgnoreCase("-recover")!=0)
        				waitForNumClients=Integer.parseInt(args[1]);
        			else
        				recover=true;
        		}
        		else
        		{
        			recover=true;
        			waitForNumClients=Integer.parseInt(args[1]);
        		}
        			
        	} else {
        		System.err.println("ERROR: Invalid arguments! Usage java MazewarServer <port> [number of clients] [-recover]");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port "+args[0]+"!");
            System.exit(-1);
        }

        //Load values from recovery file if server started in recovery mode
        if(recover)
        {
    		File fin = new File("server.dat"); 
    		FileReader fis = null;
		try
		{
			fis = new FileReader(fin);
		}
		catch(FileNotFoundException fnfe)
		{
			System.err.println("Could not find server.dat required for recovery. Please start the server in normal mode (i.e. omit -recover switch).");
			System.exit(-1);
		}

    		BufferedReader bis = new BufferedReader(fis);
    		String line = bis.readLine();
    		try
    		{
	    		if(line!=null && line !="")
	    		{
	    			seeds=Integer.parseInt(line.trim());
	    		}
	    		
	    		line=bis.readLine();
	    		if(line!=null && line !="")
	    		{
	    			waitForNumClients=Integer.parseInt(line.trim());
	    		}
	    		
	    		line=bis.readLine();
	    		if(line!=null && line !="")
	    		{
	    			seqno=Integer.parseInt(line.trim());
	    		}
    		}
    		catch(NumberFormatException nfe)
    		{
    			System.err.println("Invalid recovery file contents! Server cannot recover operation.");
        		
    			if(bis!=null)
    			{
    			bis.close();
    			}
        		
    			System.exit(-1);
    		}
    		
    		bis.close();
        }
        
        //Start listening server for recovery from clients already in game if in recovery mode,
        //else accept and assign player IDs up to maximum number of clients specified
        if(!recover)
        {
	        while (currClient!=waitForNumClients) {
	        	synchronized(clients)
	        	{
					//Accept new player connection and start listening for packets from it
		        	clients.put(currClient++,new MazewarServerHandlerThread(serverSocket.accept()));
		        	clients.get(currClient-1).myID=(currClient-1);
		        	clients.get(currClient-1).start();
		        	
		        	//Send JOIN message with seed, playerID and max players information included
	        		MazewarPacket toclientpacket = new MazewarPacket();
	        		toclientpacket.setAction(MazewarPacket.ACTION_JOIN);
	        		toclientpacket.setSeed(seeds);
	        		toclientpacket.setPlayerID(currClient-1);
	        		toclientpacket.setMaxplayer(waitForNumClients);
	        		
	        		//Grab the ip address
	        		String ip;
	        		ip = clients.get(currClient-1).getClientIP();
	        		NetworkAddress net1 = new NetworkAddress(ip,0);
	        		Player.put(currClient-1, net1);

		        	//Initialize array of output streams and get ready for game to begin soon
		        	toplayer[currClient-1] = new ObjectOutputStream(clients.get(currClient-1).getClientSocket().getOutputStream());

		        	//Send JOIN packet to new player
		        	toplayer[currClient-1].writeObject(toclientpacket);
	        	}
	        }
        }
	    else
        {
	    	//Start listening for recovery SEQ_REQ requests from players in past game
        	MazewarServerRecoverThread temp = new MazewarServerRecoverThread(serverSocket);
        	temp.start();
        }

        
        /*
         * At this point of time all the clients have received the seed number and are ready to START the game
         * The for loop commands all the clients to START
         */
        if(!recover)
        {
	        for(int i =0;i<waitForNumClients;i++) {
	        	MazewarPacket toclientpacket = new MazewarPacket();
	    		toclientpacket.setAction(MazewarPacket.ACTION_START);
	    		toclientpacket.setPlayerID(i);
	    		toclientpacket.setPlayers(Player);
	    		/*
	    		 * Need to send network ip address queue to all the clients
	    		 * that contains ip information of all the clients connected so far  
	    		 */
	        	toplayer[i].writeObject(toclientpacket);
	        }
        }
        
		/*
		 *  At this point the game has started and any change made by any client has started recording in
		 *  Queue i.e. in the arraylist <serverQueue> and the topmost packet needs to be broadcasted to 
		 *  all the clients
		 */
        while (listening) {
        	synchronized(MazewarServerHandlerThread.serverQueue) {
        		if(MazewarServerHandlerThread.serverQueue.isEmpty()){
        			// relaxing time ... nothing left to do
                    //System.out.println("Queue EMPTY");
        		}
        		else{
        			//Clients only send sequencer requests for sequence number, so we can assume without checking
        			MazewarPacket toclientpacket =  MazewarServerHandlerThread.serverQueue.remove(topindex);
            		
        			seqno++;
        			
        			toclientpacket.setSeqNo(seqno);
        			toplayer[toclientpacket.getPlayerID()].writeObject(toclientpacket);
        			
        			//Save sequence number to disk for fault tolerance
        			FileOutputStream fout =  new FileOutputStream("server.dat");
        			PrintStream pout = new PrintStream (fout);
        			pout.println(seeds);
        			pout.println(waitForNumClients);
        			pout.println(seqno);
        			pout.close();
        		}	
        	}
        }
        serverSocket.close();        
    }

}
