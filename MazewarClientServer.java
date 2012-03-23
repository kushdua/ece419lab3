import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.io.*;

public class MazewarClientServer extends Thread {
    private final int topindex = 0;
    
    private int gamePort=0;
    
	//keeps tracks of number of clients currently joined the game
	private int currClient=0;
    
	public MazewarClientServer(int port)
	{
		gamePort=port;
	}
	
    public void run()
    {
        ServerSocket serverSocket = null;
		try {
			System.out.println("Client listening for other players on port "+gamePort);
			serverSocket = new ServerSocket(gamePort);

	        while(true)
	        {
	        	MazewarClientHandlerThread temp = new MazewarClientHandlerThread(serverSocket.accept());
	        	synchronized(Mazewar.clientSockets)
				{
					Mazewar.clientSockets.add(temp);
					temp.fromplayer=new ObjectInputStream(temp.getClientSocket().getInputStream());
					temp.toPlayer=new ObjectOutputStream(temp.getClientSocket().getOutputStream());
				}
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if(serverSocket!=null)
			{
				try {
					serverSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}
    }
}