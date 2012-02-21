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
		this.socket = socket;
		System.out.println("Created new Thread to handle remote server client");
	}

	public void start() {
		//Handle incoming packet, change type and whatever, and add it to queue of events to process
		//Mazewar server would send that packet
	}
	
	public boolean sendPacket(int queueIndex)
	{
		//Check if index exists in queue and send that packet (don't remove it... MazewarServer does that)
		return false;
	}

}
