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
	}

	public void run() {
		//Handle incoming packet and add it to queue of events to process
		//Mazewar server would send that packet
        try {
			MazewarPacket fromclientpacket = null;
			while((fromclientpacket = (MazewarPacket) fromplayer.readObject())!=null){
				synchronized(Mazewar.queue)
	        	{
					//Add this player to list of players if we don't have them from before
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
Mazewar.printLn("Put message of type "+fromclientpacket.getAction()+", type "+fromclientpacket.getType()+" sequence number "+fromclientpacket.getSeqNo()+" from clientID "+myNum+" in queue when expecting "+(Mazewar.prevSeq+1));
					Mazewar.queue.put(fromclientpacket.getSeqNo(),fromclientpacket);
	        	}
			}	
		} catch (SocketException e) {
			//Remove socket and any game objects (client from maze, projectiles, etc)
			System.err.println("SocketException generated. Game client most likely disconnected.");
			synchronized(Mazewar.clientSockets)
			{
				Mazewar.clientSockets.remove(myNum);
			}
			
			if(myNum==Mazewar.clientID)
			{
				Mazewar.quit();
			}
			else
			{
				//Synchronize on maze since clients do operations on maze
				synchronized(Mazewar.maze)
				{
					if(Mazewar.maze!=null && Mazewar.clients!=null && Mazewar.clients.get(myNum)!=null)
					{
			        	Mazewar.maze.removePlayerProjectilesOnQuit(Mazewar.clients.get(myNum));
						Mazewar.maze.removeClient(Mazewar.clients.get(myNum));
					}
				}
			}
		} catch (EOFException e) {
			//Remove socket and any game objects (client from maze, projectiles, etc)
			System.err.println("EOFException generated. Game client most likely disconnected.");
			synchronized(Mazewar.clientSockets)
			{
				Mazewar.clientSockets.remove(myNum);
			}
			
			if(myNum==Mazewar.clientID)
			{
				Mazewar.quit();
			}
			else
			{
				//Synchronize on maze since clients do operations on maze
				synchronized(Mazewar.maze)
				{
					if(Mazewar.maze!=null && Mazewar.clients!=null && Mazewar.clients.get(myNum)!=null)
					{
						Mazewar.maze.removePlayerProjectilesOnQuit(Mazewar.clients.get(myNum));
						Mazewar.maze.removeClient(Mazewar.clients.get(myNum));
					}
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
