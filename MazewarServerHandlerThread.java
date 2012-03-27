import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MazewarServerHandlerThread extends Thread {
	private Socket socket = null;
	private String IP = null;
	public int myID=-1;
	
	//Global list of events received at the server
	public static final List<MazewarPacket> serverQueue=Collections.synchronizedList(new ArrayList<MazewarPacket>());
	
	public ObjectInputStream fromplayer=null;

	public MazewarServerHandlerThread(Socket accept) {
		super("MazewarServerHandlerThread");
		this.socket = accept;

		try
		{
			this.IP = accept.getInetAddress().getHostAddress();//getHostName();
		
			if(IP.equals("localhost") || IP.equals("127.0.0.1"))
			{
				this.IP=InetAddress.getLocalHost().getHostAddress();//getHostName();
			}
		}
		catch(UnknownHostException uhe)
		{
			System.err.println("Could not find out IP of client. Terminating the connection.");
			try
			{
				socket.close();
			}
			catch(IOException ioef)
			{
				System.err.println("Could not close the client connection socket while terminating because server could not obtain IP address.");
			}
		}

		while(fromplayer==null)
		{
			try {
				fromplayer = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				Mazewar.printLn("Could not retrieve OIS for client connected to sequencer.");
			}
		}
		Mazewar.printLn("the IP address is "+this.IP);
		Mazewar.printLn("Created new Thread to handle remote server client");
	}

	public void run() {
		//Handle incoming packet and add it to queue of events to process
		//Mazewar server would send that packet
        try {
        	//fromplayer = new ObjectInputStream(socket.getInputStream());
			MazewarPacket fromclientpacket = null;
			while((fromclientpacket = (MazewarPacket) fromplayer.readObject())!=null){
				//System.out.println("Received packet of type "+fromclientpacket.getAction());
				synchronized(serverQueue)
	        	{
					serverQueue.add(fromclientpacket);
	        	}
			}	
		} catch (SocketException e) {
			System.err.println("SocketException generated. Client most likely disconnected.");
			MazewarServer.clients.remove(myID);
			MazewarServer.Player.remove(myID);
			MazewarServer.toplayer[myID]=null;
		} catch (EOFException e) {
			System.err.println("EOFException generated. Client most likely disconnected.");
			MazewarServer.clients.remove(myID);
			MazewarServer.Player.remove(myID);
			MazewarServer.toplayer[myID]=null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

    public Socket getClientSocket() {
        return socket;
    }
    public String getClientIP() {
        return IP;
    }

}
