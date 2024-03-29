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
    	//Listen and accept incoming connections from other players in the game
        ServerSocket serverSocket = null;
		try {
			Mazewar.printLn("Client listening for other players on port "+gamePort);
			serverSocket = new ServerSocket(gamePort);

			while(true)
			{
				Mazewar.printLn("!!! Client listening for other players on port "+gamePort);
				MazewarClientHandlerThread temp = new MazewarClientHandlerThread(serverSocket.accept());
				//System.out.println("MWCS before getting IS");
					temp.fromplayer=new ObjectInputStream(temp.getClientSocket().getInputStream());
				//System.out.println("MWCS after getting OS");
					temp.toPlayer=new ObjectOutputStream(temp.getClientSocket().getOutputStream());	
					//System.out.println("MWCS after getting OS");
					temp.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if(serverSocket!=null)
			{
				Mazewar.printLn("Closing server socket at client side");
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
