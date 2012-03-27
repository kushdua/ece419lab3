import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
	static HashMap<Integer,NetworkAddress> Player= new HashMap<Integer, NetworkAddress>();

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

        if(recover)
        {
    		File fin = new File("server.dat"); 
    		FileReader fis = new FileReader(fin);  
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
        
        if(!recover)
        {
	        while (currClient!=waitForNumClients) {
	        	synchronized(clients)
	        	{
					//Continuously listen for and accept client connection requests
		        	clients.put(currClient++,new MazewarServerHandlerThread(serverSocket.accept()));
		        	clients.get(currClient-1).myID=(currClient-1);
		        	clients.get(currClient-1).start();
	        		MazewarPacket toclientpacket = new MazewarPacket();
	        		toclientpacket.setAction(MazewarPacket.ACTION_JOIN);
	        		toclientpacket.setSeed(seeds);
	        		toclientpacket.setPlayerID(currClient-1);
	        		toclientpacket.setMaxplayer(waitForNumClients);
	        		// grap the ip address
	        		String ip;
	        		ip = clients.get(currClient-1).getClientIP();
	        		NetworkAddress net1 = new NetworkAddress(ip,0);
	        		Player.put(currClient-1, net1);
	        		//toclientpacket.setPlayers(currClient-1,net1);
		        	//initialize and get ready for game to begin soon
		        	toplayer[currClient-1] = new ObjectOutputStream(clients.get(currClient-1).getClientSocket().getOutputStream());
		        	//toplayer[currClient-1] = new ObjectOutputStream(clients.get(currClient-1).getClientIP();
		        	toplayer[currClient-1].writeObject(toclientpacket);
	        	}
	        }
        }
	    else
        {
        	MazewarServerRecoverThread temp = new MazewarServerRecoverThread(serverSocket);
        	temp.start();
        }

        
        /*
         * At this point of time all the clients have received the seed number and are ready to START the game
         * the for loop command all the clients to START
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
        //List<MazewarPacket> Queue = MazewarServerHandlerThread.serverQueue;
        while (listening) {
        	synchronized(MazewarServerHandlerThread.serverQueue) {
        		if(MazewarServerHandlerThread.serverQueue.isEmpty()){
        			// relaxing time ... nothing left to do
                    //System.out.println("Queue EMPTY");
        		}
        		else{
        			//System.out.println("QUEUE IS NONEMPTY");
        			MazewarPacket toclientpacket =  MazewarServerHandlerThread.serverQueue.remove(topindex);
            		//broadcasting this packet, so send it to all the clients
            		/*for(int i =0;i<waitForNumClients;i++) {
            			try
            			{
            				toplayer[i].writeObject(toclientpacket);
            			}
            			catch(IOException ioe)
            			{
            				//Client must have disconnected.. Hide the error though.
            			}
            		}*/
        			seqno++;
        			
        			toclientpacket.setSeqNo(seqno);
        			toplayer[toclientpacket.getPlayerID()].writeObject(toclientpacket);
        			
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
        
        //Send JOIN packet to all clients with their initial position (they know their own ID from START)
        
    }

}