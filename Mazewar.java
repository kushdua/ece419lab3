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

import java.io.IOException;
import java.io.Serializable;

import java.io.*;
import java.net.*;
import java.util.HashMap;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

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

        /**
         * The {@link Maze} that the game uses.
         */
        private static Maze maze = null;

        /**
         * The {@link GUIClient} for the game.
         */
        private GUIClient guiClient = null;
        
        /**
         * ID for this client (as returned from server)
         */
        private static int clientID = -1;

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
        private String serverAddress="";
        
        /**
         * Server port command line parameter
         */
        private int serverPort=0;
        
        /**
         * Client socket for communication with server
         */
        private Socket clientSocket=null;
        
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
        private static HashMap<Integer, Client> clients = new HashMap<Integer, Client>();
        
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

	    		if(args.length!=2)
	    		{
	    			System.out.println("Usage: java Mazewar <server address> <server port>");
	    			System.exit(-1);
	    		}
	    		else
	    		{
	    			serverAddress=args[0];
	    			serverPort=Integer.parseInt(args[1]);
	    		}
	    		
                
                // Throw up a dialog to get the GUIClient name.
                name = JOptionPane.showInputDialog("Enter your name");
                if((name == null) || (name.length() == 0)) {
                  Mazewar.quit();
                }
                
                ScoreTableModel scoreModel = null;

                // You may want to put your network initialization code somewhere in
                // here.
    			try
    			{
    				ObjectOutputStream out = null;
    				ObjectInputStream in = null;
    				if(clientSocket!=null)
    				{
    					clientSocket.close();
    				}
    	
    				clientSocket = new Socket(serverAddress, serverPort);
    	
    				out = new ObjectOutputStream(clientSocket.getOutputStream());
    				in = new ObjectInputStream(clientSocket.getInputStream());
    				
    				pout=out;
    				pin=in;
    				
    				MazewarPacket packet=null;
    				MazewarPacket pts=null;
    				boolean ackJoined=false;
    				int playersJoined=0;
    				
    				while((packet=(MazewarPacket)(in.readObject()))!=null)
    				{
    					System.out.println("Received packet of action="+packet.getAction()+" and type="+packet.getType()+
    							" and I have spawned only "+(playersJoined+1)+" out of "+numplayers+" players.");
    					if(packet.getAction()==MazewarPacket.ACTION_JOIN)
    					{
    						mazeSeed=packet.getSeed();
    						clientID=packet.getPlayerID();
    						numplayers=packet.getMaxplayer();
    						ackJoined=true;
    						System.out.println("Client joined the game. Waiting for other players to join before starting...");
    					}
    					else if(packet.getAction()==MazewarPacket.ACTION_START)
    					{
    						if(ackJoined==true && clientID!=-1)
    						{
    							//System.out.println("Trying to init maze");
    							//Init maze
    							consolePrintLn("ECE419 Mazewar started!");
    							//System.out.println("Printed to console");
    							
				                // Create the maze
				                maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
    							//System.out.println("Created maze");
				                assert(maze != null);
				                
				                // Have the ScoreTableModel listen to the maze to find
				                // out how to adjust scores.
				                scoreModel = new ScoreTableModel();
				                assert(scoreModel != null);
				                maze.addMazeListener(scoreModel);
				                
    							//Send spawn message
				                pts=new MazewarPacket();
				                pts.setAction(MazewarPacket.ACTION_MOVE);
				                pts.setType(MazewarPacket.TYPE_SPAWN);

    							//System.out.println("Before getting spawn point and direction");
				                DirectedPoint dpt = maze.getNextSpawn();
				                pts.setDir(dpt.getDirection().getDirection());
				                pts.setXpos(dpt.getX());
				                pts.setYpos(dpt.getY());
				                pts.setPlayerID(clientID);
				                pts.setPlayerName(name);

    							//System.out.println("Before sending spawn packet");
				                out.writeObject(pts);
				                
				                //System.out.println("Created maze and sent spawn message");
    						}
    						else
    						{
    							System.err.println("Unknown error in starting the game. Join message not received most probably.");
    							System.exit(-1);
    						}
    					}
    					else if(packet.getAction()==MazewarPacket.ACTION_MOVE && 
    							packet.getType()==MazewarPacket.TYPE_SPAWN)
    					{
    						//System.out.println("Received spawn for client "+packet.getPlayerID());
    						if(clientID==packet.getPlayerID())
    						{
    			                // Create the GUIClient and connect it to the KeyListener queue
    			                guiClient = new GUIClient(packet.getPlayerName());
    			                clients.put(clientID, guiClient);
    			                
    			                //maze.addClient(guiClient);
    			                if(maze.addClient(guiClient, 
    			                			new Point(packet.getXpos(), packet.getYpos()), packet.getDir())==false)
    			                {
    			                	//System.out.println("Trying to respawn client "+clientID);
    			                	//Respawn because we couldn't add ourselves at this point... someone else is here now
    				                /*pts=new MazewarPacket();
    				                pts.setAction(MazewarPacket.ACTION_MOVE);
    				                pts.setType(MazewarPacket.TYPE_SPAWN);
    				                
    				                DirectedPoint dpt = maze.getNextSpawn();
    				                pts.setDir(dpt.getDirection().getDirection());
    				                pts.setXpos(dpt.getX());
    				                pts.setYpos(dpt.getY());
    				                pts.setPlayerID(clientID);
    				                pts.setPlayerName(name);
    				                
    				                out.writeObject(pts);*/
    			                	
    				                continue;
    			                }
    			                //System.out.println("Player "+packet.getPlayerID()+" added at ("+packet.getXpos()+","+packet.getYpos()+") facing "+packet.getDir());
    			                this.addKeyListener(guiClient);
    			                playersJoined++;
    						}
    						else
    						{
    							//maze.addClient(new RemoteClient(packet.getPlayerName()));
    							//{
    							RemoteClient rc = new RemoteClient(packet.getPlayerName());
    							if(maze.addClient(rc,
    										new Point(packet.getXpos(), packet.getYpos()), packet.getDir())==true)
    							{
    								clients.put(packet.getPlayerID(), rc);
    								//System.out.println("Player "+packet.getPlayerID()+" added at ("+packet.getXpos()+","+packet.getYpos()+") facing "+packet.getDir());
    								playersJoined++;
    							}
    						}
    						
    						if(playersJoined==numplayers)
    						{
    							//Start game because all players have been placed
    							break;
    						}
    					}
    				}
System.out.println("1");    				
                    // Create the panel that will display the maze.
                    overheadPanel = new OverheadMazePanel(maze, guiClient);
                    assert(overheadPanel != null);
                    maze.addMazeListener(overheadPanel);

System.out.println("2");                      
                    // Don't allow editing the console from the GUI
                    console.setEditable(false);
                    console.setFocusable(false);
                    console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

System.out.println("3");                      
                    // Allow the console to scroll by putting it in a scrollpane
                    JScrollPane consoleScrollPane = new JScrollPane(console);
                    assert(consoleScrollPane != null);
                    consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));

System.out.println("4");                       
                    // Create the score table
                    scoreTable = new JTable(scoreModel);
                    assert(scoreTable != null);
                    scoreTable.setFocusable(false);
                    scoreTable.setRowSelectionAllowed(false);

System.out.println("5");   
                    // Allow the score table to scroll too.
                    JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
                    assert(scoreScrollPane != null);
                    scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));

System.out.println("6");                       
                    // Create the layout manager
                    GridBagLayout layout = new GridBagLayout();
                    GridBagConstraints c = new GridBagConstraints();
                    getContentPane().setLayout(layout);

System.out.println("7");   
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

System.out.println("8");   
                    // Add the components
                    getContentPane().add(overheadPanel);
                    getContentPane().add(consoleScrollPane);
                    getContentPane().add(scoreScrollPane);

System.out.println("9");   
                    // Pack everything neatly.
                    pack();

System.out.println("10");   
                    // Let the magic begin.
                    setVisible(true);
                    overheadPanel.repaint();
System.out.println("11");   
                    this.requestFocusInWindow();
System.out.println("12");   
System.out.println("Created and hopefully displayed maze");
					//Listen for events and perform appropriate GUI update action
    				while((packet=(MazewarPacket)(in.readObject()))!=null)
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
    							//Try without passing projectile movement, just init
    							if(clients.containsKey(packet.getPlayerID()))
    							{
    								clients.get(packet.getPlayerID()).fire();
    							}
    							/*if(packet.getPlayerID()==clientID)
    							{
	    							//Fire projectile as it's mine -> I track and transmit messages for it
	    							if(clients.containsKey(packet.getPlayerID()))
	    							{
	    								clients.get(packet.getPlayerID()).fire();
	    							}
    							}
    							else
    							{
    								//Create or move projectile
    							}*/
    						}
        					else if(packet.getType()==MazewarPacket.TYPE_SPAWN)
        					{
        						//System.out.println("Received spawn for client "+packet.getPlayerID());
        						if(clientID==packet.getPlayerID())
        						{
        							System.out.println("Trying to respawn GUIClient");
        							//TODO: Update GUIClient with new spawn position
        							if(maze.respawnClient(clients.get(clientID), 
        									new Point(packet.getXpos(), packet.getYpos()),
        									new Direction(packet.getDir()))==false)
        							{
        								//Try to respawn again as we couldn't place ourselves in the maze
        								sendSpawn();
        								System.out.println("Could not respawn GUIClient so sending another spawn message");
        							}
        						}
        						else
        						{
        							//TODO: Update RemoteClient with new spawn position
        							System.out.println("Respawned remote player="+maze.respawnClient(clients.get(packet.getPlayerID()), 
        									new Point(packet.getXpos(), packet.getYpos()),
        									new Direction(packet.getDir())));
        						}
        					}
    					}
    					else if(packet.getAction()==MazewarPacket.ACTION_LEAVE)
    					{
    						if(packet.getPlayerID()==clientID)
    						{
    							Mazewar.quit();
    						}
    						else
    						{
    							maze.removeClient(clients.get(packet.getPlayerID()));
    						}
    					}
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
        
        public static void sendMoveBack() throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_DOWN_BACKWARD);
            
            pts.setPlayerID(clientID);
            
            pout.writeObject(pts);
        }
        
        public static void sendMoveForward() throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_UP_FORWARD);
            
            pts.setPlayerID(clientID);
            
            pout.writeObject(pts);
        }
        
        public static void sendMoveLeft() throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_LEFT);
            
            pts.setPlayerID(clientID);
            
            pout.writeObject(pts);
        }
        
        public static void sendMoveRight() throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_MOVE_RIGHT);
            
            pts.setPlayerID(clientID);
            
            pout.writeObject(pts);
        }
        
        public static void sendFire() throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_FIRE);
            
            DirectedPoint dpt = maze.getNextSpawn();
            //pts.setDir(clients.get(clientID).getOrientation().getDirection());
            pts.setPlayerID(clientID);
            
            pout.writeObject(pts);
        }

        public static void sendSpawn() throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_MOVE);
            pts.setType(MazewarPacket.TYPE_SPAWN);
            
            DirectedPoint dpt = maze.getNextSpawn();
            pts.setDir(dpt.getDirection().getDirection());
            pts.setXpos(dpt.getX());
            pts.setYpos(dpt.getY());
            pts.setPlayerID(clientID);
            pts.setPlayerName(name);
            
            pout.writeObject(pts);
            System.out.println("Sent respawn message for ("+dpt.getX()+","+dpt.getY()+") facing "+dpt.getDirection().toString());
        }
        
        public static void sendLeave() throws IOException
        {
            MazewarPacket pts=new MazewarPacket();
            pts.setAction(MazewarPacket.ACTION_LEAVE);
            
            pts.setPlayerID(clientID);
            
            pout.writeObject(pts);
        }
}
