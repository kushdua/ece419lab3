import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MazewarServerRecoverThread extends Thread {
	private ServerSocket socket = null;
	private String IP = null;
	private int myID=-1;
	
	//Global list of events received at the server
	public static final List<MazewarPacket> serverQueue=Collections.synchronizedList(new ArrayList<MazewarPacket>());
	
	public ObjectInputStream fromplayer=null;

	public MazewarServerRecoverThread(ServerSocket accept) {
		this.socket = accept;
	}

	public void run() {
		MazewarServerHandlerThread temp;
		//Keep on listening for clients up to maximum number server was started for
		while(MazewarServer.currClient!=MazewarServer.waitForNumClients)
		{
			try {
				//Listen for incoming connections
				temp = new MazewarServerHandlerThread(socket.accept());
	
				MazewarPacket fromclientpacket=null;
				try {
					//System.out.println("Reconnected "+MazewarServer.currClient+" out of "+MazewarServer.waitForNumClients+" clients.");
					while((fromclientpacket = (MazewarPacket) temp.fromplayer.readObject())!=null){
						//System.out.println("Received packet from reconnected user of action type "+fromclientpacket.getAction()+" type "+fromclientpacket.getType());
						if(fromclientpacket.getAction()==MazewarPacket.ACTION_REQ_SEQ)
						{
							//Wait for SEQ_REQUEST and put HandlerThread in appropriate place based on received playerID
							//This is one of original players when failure occured.
							synchronized(MazewarServer.clients)
					    	{
								//Update server structures similarly to join procedure in MazewarServer
								MazewarServer.currClient++;
								int playerID=fromclientpacket.getPlayerID();
								MazewarServer.clients.put(playerID,temp);
								MazewarServer.clients.get(playerID).start();
					        	
					    		String ip;
					    		ip = MazewarServer.clients.get(playerID).getClientIP();
					    		NetworkAddress net1 = new NetworkAddress(ip,0);
					    		MazewarServer.Player.put(playerID, net1);
		
					        	//initialize and get ready for game to begin soon
					        	MazewarServer.toplayer[playerID] = new ObjectOutputStream(MazewarServer.clients.get(playerID).getClientSocket().getOutputStream());
					        	
					        	//Update and respond to sequence number request
					        	MazewarServer.seqno++;
					        	
					        	fromclientpacket.setSeqNo(MazewarServer.seqno);
								MazewarServer.toplayer[playerID].writeObject(fromclientpacket);
		
								//Save sequence number to disk for fault tolerance
					        	FileOutputStream fout =  new FileOutputStream("server.dat");
								PrintStream pout = new PrintStream (fout);
								pout.println(MazewarServer.seeds);
								pout.println(MazewarServer.waitForNumClients);
								pout.println(MazewarServer.seqno);
								pout.close();
								
								break;
					    	}
						}
						else
						{
							//Close connection and listen for more clients, as this was not one of the original clients before failure
							temp.getClientSocket().close();
							break;
						}
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
			}
		}
	}
}
