/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/
  
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

		/**
		 * Boolean mode for whether or not to print debug messages through printLn function in Mazewar.
		 */
		private static boolean debugMode=false;
	
        /**
         * The default width of the {@link Maze}.
         */
        private final int mazeWidth = 20;

        /**
         * The default height of the {@link Maze}.
         */
        private final int mazeHeight = 10;

        /**
         * The default random seed for the {@link Maze}.
         * All implementations of the same protocol must use 
         * the same seed value, or your mazes will be different.
         */
        private int mazeSeed = 42;
        
        public final static int GAME_PORT=7000;

        /**
         * The {@link Maze} that the game uses.
         */
        static Maze maze = null;

        /**
         * The {@link GUIClient} for the game.
         */
        private Client gameClient = null;
        
        /**
         * ID for this client (as returned from server)
         */
        public static int clientID = -1;

        /**
         * Number of players to wait for (to display) before starting the game (as passed in from server).
         */
        private int numplayers=0;
        
        /**
         * The panel that displays the {@link Maze}.
         */
        private OverheadMazePanel overheadPanel = null;

        /**
         * The table the displays the scores.
         */
        private JTable scoreTable = null;
        
        /** 
         * Create the textpane statically so that we can 
         * write to it globally using
         * the static consolePrint methods  
         */
        private static final JTextPane console = new JTextPane();
        
        /**
         * Server address command line parameter
         */
        private static String serverAddress="";
        
        /**
         * Server port command line parameter
         */
        private static int serverPort=0;
        
        /**
         * Game port (same on all players)
         */
        private int gamePort=0;
        
        /**
         * Client socket for communication with server
         */
        private static Socket clientSocket=null;
        
        /**
         * Output stream for sending to server
         */
        public static ObjectOutputStream pout = null;
        
        /**
         * Input stream for receiving from server
         */
        public static ObjectInputStream pin = null;
        
        /**
         * HashMap storing client objects instantiated locally (key = player ID)
         */
        public static HashMap<Integer, Client> clients = new HashMap<Integer, Client>();
        
        /**
         * Linked blocking queue of received packets buffered until no gaps exist.
         */
        public static Map<Integer,MazewarPacket> queue=Collections.synchronizedMap(new HashMap<Integer, MazewarPacket>());
        
        /**
         * Global list of events received at the server
         */
    	public static Map<Integer, MazewarClientHandlerThread> clientSockets=Collections.synchronizedMap(new HashMap<Integer, MazewarClientHandlerThread>());
    	
        /**
         * Variable for tracking previous send event highest sequence seen at this node.
         */
        public static int prevSeq=0;
        
        /**
         * GUI (local) client name sa input by user
         */
        private static String name = "";
      
        /** 
         * Write a message to the console followed by a newline.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrintLn(String msg) {
                console.setText(console.getText()+msg+"\n");
        }
        
        /** 
         * Write a message to the console.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrint(String msg) {
                console.setText(console.getText()+msg);
        }
        
        /** 
         * Clear the console. 
         */
        public static synchronized void clearConsole() {
           console.setText("");
        }
        
        /**
         * Static method for performing cleanup before exiting the game.
         */
        public static void quit() {
                // Put any network clean-up code you might have here.
                // (inform other implementations on the network that you have 
                //  left, etc.)
                

                System.exit(0);
        }
       
        /** 
         * The place where all the pieces are put together. 
         */
        public Mazewar(String args[]) {

        		super("ECE419 Mazewar");
        		boolean isGUI=false;
        		//Populate from CL args
	    		if(args.length!=3)
	    		{
	    			System.out.println("Usage: java Mazewar <server address> <server port> <0 for GUI client, 1 for RobotClient instantiation>");
	    			System.exit(-1);
	    		}
	    		else
	    		{
	    			serverAddress=args[0];
	    			serverPort=Integer.parseInt(args[1]);
	    			if(Integer.parseInt(args[2])==0)
	    			{
	    				isGUI=true;
	    			}
	    		}
                
                // Throw up a dialog to get the GUIClient name.
                name = JOptionPane.showInputDialog("Enter your name");
                if((name == null) || (name.length() == 0)) {
                  Mazewar.quit();
                }
                
                ScoreTableModel scoreModel = null;

                // You may want to put your network initialization code somewhere in here.
    			try
    			{
    				ObjectOutputStream out = null;
    				ObjectInputStream in = null;
    				if(clientSocket!=null)
    				{
    					clientSocket.close();
    				}
    	
    				//Open connection and save In/Out stream objects for future communication
    				clientSocket = new Socket(serverAddress, serverPort);
    	
    				out = new ObjectOutputStream(clientSocket.getOutputStream());
    				in = new ObjectInputStream(clientSocket.getInputStream());
    				
    				pout=out;
    				pin=in;
    				
    				MazewarPacket packet=null;
    				MazewarPacket pts=null;
    				boolean ackJoined=false;
    				int playersJoined=0;
    				
    				//Listen for game JOIN, START and SPAWN messages so we can start the gameplay
    				while((packet=(MazewarPacket)(in.readObject()))!=null)
    				{
    					Mazewar.printLn("Received packet of action="+packet.getAction()+" and type="+packet.getType()+
    							" and I have spawned only "+(playersJoined+1)+" out of "+numplayers+" players.");
    					if(packet.getAction()==MazewarPacket.ACTION_JOIN)
    					{
    						//Save seed, client ID assigned to local player and total number of players in game
    						mazeSeed=packet.getSeed();
    						clientID=packet.getPlayerID();
    						numplayers=packet.getMaxplayer();
    						ackJoined=true;
    						
    			    		//Start listening on game port... different for each client ID
    			    		MazewarClientServer tempServer = new MazewarClientServer(Mazewar.GAME_PORT+clientID);
    			    		tempServer.start();
    						
    						Mazewar.printLn("Client joined the game. Waiting for other players to join before starting...");
    					}
    					else if(packet.getAction()==MazewarPacket.ACTION_START)
    					{
    						//Wait for all JOIN messages to reach other players
							try
							{
								Thread.sleep(2000);
							}
							catch(InterruptedException ioee)
							{
	
							}
    						//Mazewar.printLn("Got action_start message");
    						//TODO: Create connections to other clients based on packet contents from START message
    						synchronized(clientSockets)
    						{
    							HashMap<Integer,NetworkAddress> mp = packet.getPlayers();
    							
    						    Iterator it = mp.entrySet().iterator();
    						    while (it.hasNext()) {
    						        Map.Entry pairs = (Map.Entry)it.next();
    						        int id=(Integer) pairs.getKey();
    						        NetworkAddress value=(NetworkAddress) pairs.getValue();
    						        //Add even localhost (GUI) client, so event application code doesn't have to be repeated
    						        if(clientSockets.containsKey(value.address)==false)
    						        {
    						        Mazewar.printLn("MW Trying to connect to client "+id+" at "+value.address+":"+(Mazewar.GAME_PORT+id));
						        	MazewarClientHandlerThread temp = new MazewarClientHandlerThread(new Socket(value.address, Mazewar.GAME_PORT+id));
    						        	clientSockets.put(id,temp);
    						        	temp.toPlayer=new ObjectOutputStream(temp.getClientSocket().getOutputStream());
    						        	temp.fromplayer=new ObjectInputStream(temp.getClientSocket().getInputStream());
    						        	temp.start();
    						        }
    						    }
    						}
    						
    						//Set up maze structures on START message, but wait for SPAWN
    						//of other players before starting gameplay
    						if(ackJoined==true && clientID!=-1)
    						{
    							Mazewar.printLn("Trying to init maze");
    							//Init maze
    							consolePrintLn("ECE419 Mazewar started!");
    							//Mazewar.printLn("Printed to console");
    							
				                // Create the maze
				                maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
    							//Mazewar.printLn("Created maze");
				                assert(maze != null);
				                
				                // Have the ScoreTableModel listen to the maze to find
				                // out how to adjust scores.
				                scoreModel = new ScoreTableModel();
				                assert(scoreModel != null);
				                maze.addMazeListener(scoreModel);
				                
				                while(clientSockets.size()!=numplayers)
				                {
				                	try {
										Thread.sleep(0,1000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
				                }
				                
				                //Send spawn for local client to all other players
			                	Mazewar.sendSpawn(Mazewar.getSequenceNumber());
			                	
				                break;
    						}
    						else
    						{
    							//For some unknown reason START sent before JOIN.. error in comm or corruption => exit
    							System.err.println("Unknown error in starting the game. Join message not received most probably.");
    							System.exit(-1);
    						}
    					}
    				}

    				//Process SPAWN for all other game clients (before displaying and starting the actual game)
    				while(true)
    				{
    					if(queue.isEmpty())
    					{
							//Mazewar.printLn("WAITING FOR SPAWN");
    						try {
								Thread.sleep(0,1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
    					}
    					else
    					{
							//Wait until expected sequence number message is present then dequeue it
							while(queue.containsKey(prevSeq+1)==false)
							{
								Mazewar.printLn("WAITING FOR SPAWN");
	    						try {
									Thread.sleep(0,1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							
							synchronized(queue)
							{
								packet=queue.remove(++prevSeq);
							}
    						
							if(packet.getAction()==MazewarPacket.ACTION_MOVE && 
									packet.getType()==MazewarPacket.TYPE_SPAWN)
							{
								Mazewar.printLn("Received spawn for client "+packet.getPlayerID());
								if(clientID==packet.getPlayerID())
								{
									if(isGUI)
									{
								        //Create the GUIClient and save it in our local clients list
								        gameClient = new GUIClient(packet.getPlayerName());
								        clients.put(clientID, gameClient);
								        
								        if(maze.addClient(gameClient, 
								        			new Point(packet.getXpos(), packet.getYpos()), packet.getDir())==false)
								        {
								        	//Respawn because we couldn't add ourselves at this point...
								        	//someone/something else is here now
								        	sendSpawn(getSequenceNumber());
								            continue;
								        }
								        //Mazewar.printLn("Player "+packet.getPlayerID()+" added at ("+packet.getXpos()+","+packet.getYpos()+") facing "+packet.getDir());
								        this.addKeyListener((GUIClient)gameClient);
								        playersJoined++;
									}
									else
									{
								        //Create the GUIClient and save it in our local clients list
								        gameClient = new RobotClient(packet.getPlayerName());
								        clients.put(clientID, gameClient);
								        
								        if(maze.addClient(gameClient, 
								        			new Point(packet.getXpos(), packet.getYpos()), packet.getDir())==false)
								        {
								        	//Respawn because we couldn't add ourselves at this point...
								        	//someone/something else is here now
								        	sendSpawn(getSequenceNumber());
								            continue;
								        }
								        //Mazewar.printLn("Player "+packet.getPlayerID()+" added at ("+packet.getXpos()+","+packet.getYpos()+") facing "+packet.getDir());
								        playersJoined++;
									}
								}
								else
								{
									//Create the RemoteClient and save it in our local clients list
									RemoteClient rc = new RemoteClient(packet.getPlayerName());
									if(maze.addClient(rc,
												new Point(packet.getXpos(), packet.getYpos()), packet.getDir())==true)
									{
										clients.put(packet.getPlayerID(), rc);
										//Mazewar.printLn("Player "+packet.getPlayerID()+" added at ("+packet.getXpos()+","+packet.getYpos()+") facing "+packet.getDir());
										playersJoined++;
									}
								}
								
								if(playersJoined==numplayers)
								{
									//Debug messages for weird, randomly occurring bug (which might have been fixed) where visual maze doesn't appear
									Mazewar.printLn("1");
									                    // Create the panel that will display the maze.
									                    overheadPanel = new OverheadMazePanel(maze, gameClient);
									                    assert(overheadPanel != null);
									                    maze.addMazeListener(overheadPanel);
							
									Mazewar.printLn("2");
									                    // Don't allow editing the console from the GUI
									                    console.setEditable(false);
									                    console.setFocusable(false);
									                    console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
							
									Mazewar.printLn("3");
									                    // Allow the console to scroll by putting it in a scrollpane
									                    JScrollPane consoleScrollPane = new JScrollPane(console);
									                    assert(consoleScrollPane != null);
									                    consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));
							
									Mazewar.printLn("4");
									                    // Create the score table
									                    scoreTable = new JTable(scoreModel);
									                    assert(scoreTable != null);
									                    scoreTable.setFocusable(false);
									                    scoreTable.setRowSelectionAllowed(false);
							
									Mazewar.printLn("5");
									                    // Allow the score table to scroll too.
									                    JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
									                    assert(scoreScrollPane != null);
									                    scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));
							
									Mazewar.printLn("6");
									                    // Create the layout manager
									                    GridBagLayout layout = new GridBagLayout();
									                    GridBagConstraints c = new GridBagConstraints();
									                    getContentPane().setLayout(layout);
							
									Mazewar.printLn("7");
									                    // Define the constraints on the components.
									                    c.fill = GridBagConstraints.BOTH;
									                    c.weightx = 1.0;
									                    c.weighty = 3.0;
									                    c.gridwidth = GridBagConstraints.REMAINDER;
									                    layout.setConstraints(overheadPanel, c);
									                    c.gridwidth = GridBagConstraints.RELATIVE;
									                    c.weightx = 2.0;
									                    c.weighty = 1.0;
									                    layout.setConstraints(consoleScrollPane, c);
									                    c.gridwidth = GridBagConstraints.REMAINDER;
									                    c.weightx = 1.0;
									                    layout.setConstraints(scoreScrollPane, c);
							
									Mazewar.printLn("8");
									                    // Add the components
									                    getContentPane().add(overheadPanel);
									                    getContentPane().add(consoleScrollPane);
									                    getContentPane().add(scoreScrollPane);
							
									Mazewar.printLn("9");
									                    // Pack everything neatly.
									                    pack();
							
									Mazewar.printLn("10");
									                    // Let the magic begin.
									                    setVisible(true);
									                    overheadPanel.repaint();
									Mazewar.printLn("11");
									                    this.requestFocusInWindow();
									Mazewar.printLn("12");
									Mazewar.printLn("Created and displayed maze");
									break;
								}
							}
							else
							{
								Mazewar.printLn("Received packet type "+packet.getType()+" action "+packet.getAction()+" instead of SPAWN. Dropping it.");
							}
    					}
    				}


					//Listen for events and perform appropriate GUI update action... self-explanatory
    				while(true)
    				{
    					while(queue.isEmpty())
    					{
    						try {
								Thread.sleep(0,1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
    					}
    					
    					if(queue.isEmpty() || queue.containsKey(prevSeq+1)==false)
    					{
    						//Mazewar.printLn("Thought queue was nonempty but couldn't find "+(prevSeq+1)+"key.");
    						continue;
    					}
    					else
    					{
    						//Process events from queue
        					synchronized(queue)
        					{
        						//Mazewar.printLn("Removed key "+(prevSeq)+" from queue");
        						packet=queue.remove(++prevSeq);
        					}
    					}
					
						//If packet is not next expected, queue it until you receive this next one.
						if(packet.getSeqNo()!=prevSeq)
						{
							synchronized(queue)
							{
								queue.put(packet.getSeqNo(), packet);
							}
						}
						else
						{
	    					if(packet.getAction()==MazewarPacket.ACTION_MOVE)
	    					{
	    						if(packet.getType()==MazewarPacket.TYPE_MOVE_DOWN_BACKWARD)
	    						{
	    							if(clients.containsKey(packet.getPlayerID()))
	    							{
	    								clients.get(packet.getPlayerID()).backup();
	    							}
	    						}
	    						else if(packet.getType()==MazewarPacket.TYPE_MOVE_UP_FORWARD)
	    						{
	    							if(clients.containsKey(packet.getPlayerID()))
	    							{
	    								clients.get(packet.getPlayerID()).forward();
	    							}
	    						}
	    						else if(packet.getType()==MazewarPacket.TYPE_MOVE_LEFT)
	    						{
	    							if(clients.containsKey(packet.getPlayerID()))
	    							{
	    								clients.get(packet.getPlayerID()).turnLeft();
	    							}
	    						}
	    						else if(packet.getType()==MazewarPacket.TYPE_MOVE_RIGHT)
	    						{
	    							if(clients.containsKey(packet.getPlayerID()))
	    							{
	    								clients.get(packet.getPlayerID()).turnRight();
	    							}
	    						}
	    						else if(packet.getType()==MazewarPacket.TYPE_FIRE)
	    						{
	    							//Fire projectiles regardless of whether it is for GUI or Remote client
	    							//Thread which sends MOVE_PROJECTILE messages only does so for projectiles
	    							//fired by the local GUI client, and leaves the remote ones intact.
	    							//Only MOVE_PROJECTILE messages can move projectiles (whether local or remote).
		    						clients.get(packet.getPlayerID()).fire();
	    						}
	    						else if(packet.getType()==MazewarPacket.TYPE_MOVE_PROJECTILE)
	    						{
	    							Mazewar.printLn("Received MOVE_PROJECTILE for client "+packet.getPlayerID());
	    							//Run the timer loop code only once for the client's (from packet) projectile
	    							// => move/remove projectile + kill, w.e.
	    							Collection deadPrj = new HashSet();
	    							Map projectileMap =((MazeImpl)maze).getProjectileMap();
	    	                        if(!projectileMap.isEmpty()) {
	                                    Iterator it = projectileMap.keySet().iterator();
	                                    synchronized(projectileMap) {
	                                            while(it.hasNext()) {
	                                                    Object o = it.next();
	                                                    //Mazewar.printLn("Projectile owner = "+((Projectile)o).getOwner()+" and packet client = "+clients.get(packet.getPlayerID()));
	                                                    assert(o instanceof Projectile);
	                                                    if(	deadPrj.contains(o)==false &&
	                                                    	((Projectile)o).getOwner()==clients.get(packet.getPlayerID()))
	                                                    {
	                                                    	//Send MOVE_PROJECTILE message
	                                                    	deadPrj.addAll(((MazeImpl)maze).moveProjectile((Projectile)o));
	                                                    }
	                                            }
	                                            it = deadPrj.iterator();
	                                            while(it.hasNext()) {
                                                    Object o = it.next();
                                                    assert(o instanceof Projectile);
                                                    Projectile prj = (Projectile)o;
                                                    projectileMap.remove(prj);
                                                    ((MazeImpl)maze).getClientFired().remove(prj.getOwner());
	                                            }
	                                            deadPrj.clear();
	                                    }
	    	                        }
	    						}
	        					else if(packet.getType()==MazewarPacket.TYPE_SPAWN)
	        					{
	        						//Mazewar.printLn("Received spawn for client "+packet.getPlayerID());
	        						if(clientID==packet.getPlayerID())
	        						{
	        							//Update GUIClient with new spawn position
	        							if(maze.respawnClient(clients.get(clientID), 
	        									new Point(packet.getXpos(), packet.getYpos()),
	        									new Direction(packet.getDir()))==false)
	        							{
	        								//Try to respawn again as we couldn't place ourselves in the maze
	        								sendSpawn(getSequenceNumber());
	        							}
	        						}
	        						else
	        						{
	        							//Update RemoteClient with new spawn position
	        							maze.respawnClient(clients.get(packet.getPlayerID()), 
	        									new Point(packet.getXpos(), packet.getYpos()),
	        									new Direction(packet.getDir()));
	        						}
	        					}
	    					}
	    					else if(packet.getAction()==MazewarPacket.ACTION_LEAVE)
	    					{
	    						//Quit game if message about local client, or remove remote player if they quit
	    						if(packet.getPlayerID()==clientID)
	    						{
	    							Mazewar.quit();
	    						}
	    						else
	    						{
	    				        	maze.removePlayerProjectilesOnQuit(clients.get(packet.getPlayerID()));
	    							maze.removeClient(clients.get(packet.getPlayerID()));
	    						}
	    					}
						}//end of apply action right away
    				}
    			} catch (IOException e) {
    				e.printStackTrace();
                	System.err.println("Error in joining and starting the game. Exiting...");
                	System.exit(-1);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
                

        }

		/**
         * Entry point for the game.  
         * @param args Command-line arguments.
         */
        public static void main(String args[]) {      			
        	
                /* Create the GUI */
                new Mazewar(args);
        }
        
        /**
         * Send message that local client moves backward.
         * @throws IOException
         */
        public static void sendMoveBack(int seqNo) throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_DOWN_BACKWARD);
            pts.setSeqNo(seqNo);
            
            pts.setPlayerID(clientID);
            
            ObjectOutputStream out = null;
            for(int key : clientSockets.keySet())
            {
            	out=clientSockets.get(key).toPlayer;
            	out.writeObject(pts);
            }
        }
        
        /**
         * Send message that local client moves forward.
         * @throws IOException
         */
        public static void sendMoveForward(int seqNo) throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_UP_FORWARD);
            pts.setSeqNo(seqNo);
            
            pts.setPlayerID(clientID);
            
            ObjectOutputStream out = null;
            for(int key : clientSockets.keySet())
            {
            	out=clientSockets.get(key).toPlayer;
            	out.writeObject(pts);
            }
        }
        
        /**
         * Send message that local client rotates left.
         * @throws IOException
         */
        public static void sendMoveLeft(int seqNo) throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_LEFT);
            pts.setSeqNo(seqNo);
            
            pts.setPlayerID(clientID);
            
            ObjectOutputStream out = null;
            for(int key : clientSockets.keySet())
            {
            	out=clientSockets.get(key).toPlayer;
            	out.writeObject(pts);
            }
        }
        
        /**
         * Send message that local client rotates right.
         * @throws IOException
         */
        public static void sendMoveRight(int seqNo) throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_RIGHT);
            pts.setSeqNo(seqNo);
            
            pts.setPlayerID(clientID);
            
            ObjectOutputStream out = null;
            for(int key : clientSockets.keySet())
            {
            	out=clientSockets.get(key).toPlayer;
            	out.writeObject(pts);
            }
        }
        
        /**
         * Send message that local client fires a projectile. Direction and location is consistent
         * across all clients in the game (due to design + TCPIP) so no position and direction is transmitted.
         * @throws IOException
         */
        public static void sendFire(int seqNo) throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_FIRE);
            pts.setSeqNo(seqNo);
            
            pts.setPlayerID(clientID);
            
            ObjectOutputStream out = null;
            for(int key : clientSockets.keySet())
            {
            	out=clientSockets.get(key).toPlayer;
            	out.writeObject(pts);
            }
        }

        /**
         * Send message that local client spawns at a point in the maze with specific direction.
         * @throws IOException
         */
        public static void sendSpawn(int seqNo) throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_SPAWN);
            pts.setSeqNo(seqNo);
            
            DirectedPoint dpt = maze.getNextSpawn();
            pts.setDir(dpt.getDirection().getDirection());
            pts.setXpos(dpt.getX());
            pts.setYpos(dpt.getY());
            pts.setPlayerID(clientID);
            pts.setPlayerName(name);
            
            ObjectOutputStream out = null;
            for(int key : clientSockets.keySet())
            {
//Mazewar.printLn("Before sending spawn for client ID "+key+" inside sendSpawn");
            	out=clientSockets.get(key).toPlayer;
            	out.writeObject(pts);
            }
            Mazewar.printLn("Sent respawn message for ("+dpt.getX()+","+dpt.getY()+") facing "+dpt.getDirection().toString());
        }
        
        /**
         * Send message that local client's projectile should move forward one position. Direction and location is
         * consistent across all clients in the game (due to design + TCPIP) so no position and direction is transmitted.
         * @throws IOException
         */
        public static void sendProjectileMove(int seqNo) throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_PROJECTILE);
            pts.setSeqNo(seqNo);
            
            pts.setPlayerID(clientID);
            
            ObjectOutputStream out = null;
            for(int key : clientSockets.keySet())
            {
            	out=clientSockets.get(key).toPlayer;
            	out.writeObject(pts);
            }
        }

        /**
         * Send message that local client quits the game.
         * @throws IOException
         */
        public static void sendLeave(int seqNo) throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_LEAVE);
            pts.setSeqNo(seqNo);
            
            pts.setPlayerID(clientID);
            
            ObjectOutputStream out = null;
            for(int key : clientSockets.keySet())
            {
            	out=clientSockets.get(key).toPlayer;
            	out.writeObject(pts);
            }
        }
        
        /**
         * Obtain sequence number from centralized sequencer component.
         * @return	Sequence number integer for use in broadcasting events.
         */
        public synchronized static int getSequenceNumber()
        {
        	MazewarPacket pts = new MazewarPacket();
        	pts.setAction(MazewarPacket.ACTION_REQ_SEQ);
        	
        	pts.setPlayerID(clientID);
        	try {
				pout.writeObject(pts);
	        	MazewarPacket packet = null;
				
				while(true)
				{
                	try {
						Thread.sleep(50);
					} catch (InterruptedException eeeee) {
						// TODO Auto-generated catch block
						eeeee.printStackTrace();
					}
					Mazewar.printLn("Before waiting to read given sequence number");
					packet=(MazewarPacket)(pin.readObject());
					
					if(packet==null)
						continue;
					
					//If packet is not next expected, queue it until you receive this next one.
					if(packet.getAction()==MazewarPacket.ACTION_REQ_SEQ)
					{
						Mazewar.printLn("Received assigned seqNo of "+packet.getSeqNo());
						return packet.getSeqNo();
					}
				}
    		} catch (SocketException e) {
    			//Reconnect and obtain sequence number
				while(true)
				{
	    			//Open connection and save In/Out stream objects for future communication
					try {
						clientSocket = new Socket(serverAddress, serverPort);
					} catch (UnknownHostException e1) {
						// TODO Auto-generated catch block
						continue;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						continue;
					}
		
					try {
						pout = new ObjectOutputStream(clientSocket.getOutputStream());
						
						//Resend SEQ_REQ packet
						pout.writeObject(pts);
						
						pin = new ObjectInputStream(clientSocket.getInputStream());
						
						MazewarPacket packet=null;
						
						while((packet=(MazewarPacket)(pin.readObject()))!=null)
						{
							if(packet.getAction()==MazewarPacket.ACTION_REQ_SEQ)
							{
								return packet.getSeqNo();
							}
						}
					} catch (IOException e8) {
						// Couldn't set up pin/pout
						e8.printStackTrace();
					} catch (ClassNotFoundException e9) {
						// Couldn't read from pin... try again
						e9.printStackTrace();
					}
				}
    		} catch (EOFException e4) {
    			//Reconnect and obtain sequence number
				while(true)
				{
	    			//Open connection and save In/Out stream objects for future communication
					try {
						clientSocket = new Socket(serverAddress, serverPort);
					} catch (UnknownHostException e1) {
						// TODO Auto-generated catch block
						continue;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						continue;
					}
		
					try {
						pout = new ObjectOutputStream(clientSocket.getOutputStream());
						pin = new ObjectInputStream(clientSocket.getInputStream());
									
						MazewarPacket packet=null;
					
						//Resend SEQ_REQ packet
						pout.writeObject(pts);
						
						//Try to reconnect + obtain sequence number
						while((packet=(MazewarPacket)(pin.readObject()))!=null)
						{
							if(packet.getAction()==MazewarPacket.ACTION_REQ_SEQ)
							{
								return packet.getSeqNo();
							}
						}
					} catch (IOException e8) {
						// Couldn't set up pin/pout
						e8.printStackTrace();
					} catch (ClassNotFoundException e9) {
						// Couldn't read from pin... try again
						e9.printStackTrace();
					}
				}
    		} catch (IOException e5) {
    			// TODO Auto-generated catch block
    			e5.printStackTrace();
    		} catch (ClassNotFoundException e6) {
    			// TODO Auto-generated catch block
    			e6.printStackTrace();
    		}
	        return -1;
        }
        
        /**
         * Print specified debug message if <code>debugMode</code> flag in Mazewar is turned on.
         * @param value	String debug message to print out
         */
        public static void printLn(String value)
        {
        	if(debugMode)
        	{
        		System.out.println(value);
        	}
        }
}
