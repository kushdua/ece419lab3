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
	private int myNum=-1;
	public ObjectOutputStream toPlayer=null;
	public ObjectInputStream fromplayer=null;
	
	public MazewarClientHandlerThread(Socket accept) {
		super("MazewarServerHandlerThread");
		this.socket = accept;
		//for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
		//    Mazewar.println(ste + "\n");
		//}
		System.out.println("Created new Thread to handle remote server client");
	}

	public void run() {
		//Handle incoming packet and add it to queue of events to process
		//Mazewar server would send that packet
        try {
			MazewarPacket fromclientpacket = null;
			while((fromclientpacket = (MazewarPacket) fromplayer.readObject())!=null){
				synchronized(Mazewar.queue)
	        	{
					//Add this player to list of players if we don't have them before
					if(fromclientpacket.getType()==MazewarPacket.TYPE_SPAWN && myNum==-1)
					{
						synchronized(Mazewar.clientSockets)
						{
							if(Mazewar.clientSockets.containsKey(socket.getInetAddress().getHostAddress())==false)
							{
Mazewar.printLn("Putting from MCHT client "+socket.getInetAddress().getHostAddress());
								Mazewar.clientSockets.put(fromclientpacket.getPlayerID(), this);
								myNum=fromclientpacket.getPlayerID();
							}
						}
					}
//Mazewar.printLn("Put message of type "+fromclientpacket.getAction()+", type "+fromclientpacket.getType()+" sequence number "+fromclientpacket.getSeqNo()+" in queue when expecting "+Mazewar.prevSeq);
					Mazewar.queue.put(fromclientpacket.getSeqNo(),fromclientpacket);
	        	}
			}	
		} catch (SocketException e) {
			System.err.println("SocketException generated. Game client most likely disconnected.");
			synchronized(Mazewar.clientSockets)
			{
				Mazewar.clientSockets.remove(myNum);
			}
		} catch (EOFException e) {
			System.err.println("EOFException generated. Game client most likely disconnected.");
			synchronized(Mazewar.clientSockets)
			{
				Mazewar.clientSockets.remove(myNum);
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
