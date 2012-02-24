import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MazewarServerHandlerThread extends Thread {
	private Socket socket = null;
	
	//Global list of events received at the server
	public static final List<MazewarPacket> serverQueue=Collections.synchronizedList(new ArrayList<MazewarPacket>());

	public MazewarServerHandlerThread(Socket accept) {
		super("MazewarServerHandlerThread");
		this.socket = accept;
		System.out.println("Created new Thread to handle remote server client");
	}

	public void run() {
		//Handle incoming packet and add it to queue of events to process
		//Mazewar server would send that packet
        try {
			ObjectInputStream fromplayer = new ObjectInputStream(socket.getInputStream());
			MazewarPacket fromclientpacket = (MazewarPacket) fromplayer.readObject();
			while(fromclientpacket!=null){
				synchronized(serverQueue)
	        	{
					serverQueue.add(fromclientpacket);
	        	}
			}	
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

}
