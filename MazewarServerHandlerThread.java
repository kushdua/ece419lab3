import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MazewarServerHandlerThread extends Thread {
	private Socket socket = null;
	private String IP = null;
	
	//Global list of events received at the server
	public static final List<MazewarPacket> serverQueue=Collections.synchronizedList(new ArrayList<MazewarPacket>());

	public MazewarServerHandlerThread(Socket accept) {
		super("MazewarServerHandlerThread");
		this.socket = accept;
		this.IP = accept.getInetAddress().getHostAddress();
		System.out.println("the IP address is "+this.IP);
		//this.IP= accept.getRemoteSocketAddress().toString();
		System.out.println("Created new Thread to handle remote server client");
	}

	public void run() {
		//Handle incoming packet and add it to queue of events to process
		//Mazewar server would send that packet
        try {
			ObjectInputStream fromplayer = new ObjectInputStream(socket.getInputStream());
			MazewarPacket fromclientpacket = null;
			while((fromclientpacket = (MazewarPacket) fromplayer.readObject())!=null){
				System.out.println("Received packet of type "+fromclientpacket.getAction());
				synchronized(serverQueue)
	        	{
					serverQueue.add(fromclientpacket);
	        	}
			}	
		} catch (SocketException e) {
			System.err.println("SocketException generated. Client most likely disconnected.");
		} catch (EOFException e) {
			System.err.println("EOFException generated. Client most likely disconnected.");
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
