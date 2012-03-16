import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MazewarClientHandlerThread extends Thread {
	private Socket socket = null;
	
	public MazewarClientHandlerThread(Socket accept) {
		super("MazewarServerHandlerThread");
		this.socket = accept;
		System.out.println("Created new Thread to handle remote server client");
	}

	public void run() {
		//Handle incoming packet and add it to queue of events to process
		//Mazewar server would send that packet
        try {
			ObjectInputStream fromplayer = new ObjectInputStream(socket.getInputStream());
			MazewarPacket fromclientpacket = null;
			while((fromclientpacket = (MazewarPacket) fromplayer.readObject())!=null){
				synchronized(Mazewar.queue)
	        	{
					Mazewar.queue.put(fromclientpacket.getSeqNo(),fromclientpacket);
	        	}
			}	
		} catch (SocketException e) {
			System.err.println("SocketException generated. Game client most likely disconnected.");
		} catch (EOFException e) {
			System.err.println("EOFException generated. Game client most likely disconnected.");
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
