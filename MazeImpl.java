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

import java.lang.Thread;
import java.lang.Runnable;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;  
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * A concrete implementation of a {@link Maze}.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: MazeImpl.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class MazeImpl extends Maze implements Serializable, ClientListener, Runnable {
	
        /**
         * Create a {@link Maze}.
         * @param point Treat the {@link Point} as a magintude specifying the
         * size of the maze.
         * @param seed Initial seed for the random number generator.
         */
        public MazeImpl(Point point, long seed) {
                maxX = point.getX();
                assert(maxX > 0);
                maxY = point.getY();
                assert(maxY > 0);
                
                // Initialize the maze matrix of cells
                mazeVector = new Vector(maxX);
                for(int i = 0; i < maxX; i++) {
                        Vector colVector = new Vector(maxY);
                        
                        for(int j = 0; j < maxY; j++) {
                                colVector.insertElementAt(new CellImpl(), j);
                        }
                        
                        mazeVector.insertElementAt(colVector, i);
                }

                thread = new Thread(this);

                // Initialized the random number generator
                randomGen = new Random(seed);
                
                // Build the maze starting at the corner
                buildMaze(new Point(0,0));

                thread.start();
        }
       
        /** 
         * Create a maze from a serialized {@link MazeImpl} object written to a file.
         * @param mazefile The filename to load the serialized object from.
         * @return A reconstituted {@link MazeImpl}. 
         */
        public static Maze readMazeFile(String mazefile)
                throws IOException, ClassNotFoundException {
                        assert(mazefile != null);
                        FileInputStream in = new FileInputStream(mazefile);
                        ObjectInputStream s = new ObjectInputStream(in);
                        Maze maze = (Maze) s.readObject();
                        
                        return maze;
                }
        
        /** 
         * Serialize this {@link MazeImpl} to a file.
         * @param mazefile The filename to write the serialized object to.
         * */
        public void save(String mazefile)
                throws IOException {
                        assert(mazefile != null);
                        FileOutputStream out = new FileOutputStream(mazefile);
                        ObjectOutputStream s = new ObjectOutputStream(out);
                        s.writeObject(this);
                        s.flush();
                }
       
        /** 
         * Display an ASCII version of the maze to stdout for debugging purposes.  
         */
        public void print() {
                for(int i = 0; i < maxY; i++) {
                        for(int j = 0; j < maxX; j++) {
                                CellImpl cell = getCellImpl(new Point(j,i));
                                if(j == maxY - 1) {
                                        if(cell.isWall(Direction.South)) {
                                                System.out.print("+-+");
                                        } else {
                                                System.out.print("+ +");
                                        }
                                } else {
                                        if(cell.isWall(Direction.South)) {
                                                System.out.print("+-");
                                        } else {
                                                System.out.print("+ ");
                                        }
                                }
                                
                        }	    
                        System.out.print("\n");
                        for(int j = 0; j < maxX; j++) {
                                CellImpl cell = getCellImpl(new Point(j,i));
                                if(cell.getContents() != null) {
                                        if(cell.isWall(Direction.West)) {
                                                System.out.print("|*");
                                        } else {
                                                System.out.print(" *");
                                        }
                                } else {
                                        if(cell.isWall(Direction.West)) {
                                                System.out.print("| ");
                                        } else {
                                                System.out.print("  ");
                                        }
                                }
                                if(j == maxY - 1) {
                                        if(cell.isWall(Direction.East)) {
                                                System.out.print("|");
                                        } else {
                                                System.out.print(" ");
                                        }
                                }
                        }
                        System.out.print("\n");
                        if(i == maxX - 1) {
                                for(int j = 0; j < maxX; j++) {
                                        CellImpl cell = getCellImpl(new Point(j,i));
                                        if(j == maxY - 1) {
                                                if(cell.isWall(Direction.North)) {
                                                        System.out.print("+-+");
                                                } else {
                                                        System.out.print("+ +");
                                                }
                                        } else {
                                                if(cell.isWall(Direction.North)) {
                                                        System.out.print("+-");
                                                } else {
                                                        System.out.print("+ ");
                                                }
                                        }		
                                }
                                System.out.print("\n");     
                        }   
                }
                
        }
       
        
        public boolean checkBounds(Point point) {
                assert(point != null);
                return (point.getX() >= 0) && (point.getY() >= 0) && 
                        (point.getX() < maxX) && (point.getY() < maxY);
        }
        
        public Point getSize() {
                return new Point(maxX, maxY);
        }
        
        public synchronized Cell getCell(Point point) {
                assert(point != null);
                return getCellImpl(point);
        }
        
        public synchronized void addClient(Client client) {
                assert(client != null);
                // Pick a random starting point, and check to see if it is already occupied
                Point point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
                CellImpl cell = getCellImpl(point);
                // Repeat until we find an empty cell
                while(cell.getContents() != null) {
                        point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
                        cell = getCellImpl(point);
                } 
                addClient(client, point);
        }
        
        public synchronized Point getClientPoint(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                assert(o instanceof Point);
                return (Point)o;
        }
        
        public synchronized Direction getClientOrientation(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                if(o==null)return null;
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                return dp.getDirection();
        }
       
        public synchronized void removeClient(Client client) {
                assert(client != null);
                if(client==null) return;
                Object o = clientMap.remove(client);
                assert(o instanceof Point);
                if(o==null || !(o instanceof Point)) return;
                Point point = (Point)o;
                CellImpl cell = getCellImpl(point);
                cell.setContents(null);
                clientMap.remove(client);
                client.unregisterMaze();
                client.removeClientListener(this);
                update();
                notifyClientRemove(client);
        }
        
        public synchronized void removePlayerProjectilesOnQuit(Client client)
        {
            assert(client != null);
            if(client==null)
            	return;
            
            //Remove projectile from maze structures
            if(clientFired.contains(client))
            {
                Iterator it = projectileMap.keySet().iterator();
                synchronized(projectileMap) {
                        while(it.hasNext()) {
                            Object o = it.next();
                            //Mazewar.printLn("Projectile owner = "+((Projectile)o).getOwner()+" and packet client = "+clients.get(packet.getPlayerID()));
                            if(o instanceof Projectile)
                            {
                            	DirectedPoint dp = (DirectedPoint) projectileMap.remove((Projectile)o);
                            	if(dp!=null)
                            	{
	                            	CellImpl newCell = getCellImpl(dp);
	                            	if(newCell!=null)
	                            	{
		                            	newCell.setContents(null);
		                                clientFired.remove(client);
	                            	}
                            	}
                            }
                        }
                }
            }
        }

        public synchronized boolean clientFire(Client client) {
                assert(client != null);
                // If the client already has a projectile in play, fail.
                if(clientFired.contains(client)) {
                        return false;
                }
                
                Point point = getClientPoint(client);
                if(point==null) return false;
                Direction d = getClientOrientation(client);
                CellImpl cell = getCellImpl(point);
                
                /* Check that you can fire in that direction */
                if(cell.isWall(d)) {
                        return false;
                }
                
                DirectedPoint newPoint = new DirectedPoint(point.move(d), d);
                /* Is the point within the bounds of maze? */
                assert(checkBounds(newPoint));
                
                CellImpl newCell = getCellImpl(newPoint);
                Object contents = newCell.getContents();
                if(contents != null) {
                        // If it is a Client, kill it outright
                        if(contents instanceof Client) {
                                notifyClientFired(client);
                                killClient(client, (Client)contents);
                                update();
                                return true; 
                        } else {
                        	// Otherwise fail (bullets will destroy each other)
                        	// Assuming no other object classes that could be in the maze
                        	// => only a bullet can be in the cell, as it's not a client
                        	Projectile otherProj = (Projectile)contents;
                        	//Check if we can remove other projectile entry before removing anything
                        	// => remove all or nothing
                        	if(projectileMap.containsKey(otherProj) && clientFired.contains(otherProj.getOwner()))
                        	{
                        		synchronized(projectileMap)
                        		{
	                        		//Remove other projectile from cell, and tracking arrays for projectile and user
	                        		newCell.setContents(null);
	                        		projectileMap.remove(otherProj);
	                        		clientFired.remove(otherProj.getOwner());
                        		}
                        	}
                        	else
                        	{
                        		System.err.println("Cannot reference the other projectile object or owner correctly. Fire cancellation gameplay bug is not fixed!");
                        	}
                        	
                            return false;
                        }
                }
                
                clientFired.add(client);
                Projectile prj = new Projectile(client);
                
                /* Write the new cell */
                projectileMap.put(prj, newPoint);
                newCell.setContents(prj);
                notifyClientFired(client);
                update();
                return true; 
        }
        
        public synchronized boolean moveClientForward(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                if(o==null)return false;
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                return moveClient(client, dp.getDirection());
        }
        
        public synchronized boolean moveClientBackward(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                if(o==null)return false;
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                return moveClient(client, dp.getDirection().invert());
        }
        
       
        public synchronized Iterator getClients() {
                return clientMap.keySet().iterator();
        }
        
        
        public void addMazeListener(MazeListener ml) {
                listenerSet.add(ml);
        }

        public void removeMazeListener(MazeListener ml) {
                listenerSet.remove(ml);
        }

        /**
         * Listen for notifications about action performed by 
         * {@link Client}s in the maze.
         * @param c The {@link Client} that acted.
         * @param ce The action the {@link Client} performed.
         */
        public void clientUpdate(Client c, ClientEvent ce) {
                // When a client turns, update our state.
                if(ce == ClientEvent.turnLeft) {
                        rotateClientLeft(c);
                } else if(ce == ClientEvent.turnRight) {
                        rotateClientRight(c);
                }
        }

        /**
         * Control loop for {@link Projectile}s.
         */
        public void run() {
                Collection deadPrj = new HashSet();
                while(true) {
                        if(!projectileMap.isEmpty()) {
                                Iterator it = projectileMap.keySet().iterator();
                                synchronized(projectileMap) {
                                        while(it.hasNext()) {
                                                Object o = it.next();
                                                assert(o instanceof Projectile);
                                                if(	deadPrj.contains(o)==false &&
                                                	((Projectile)o).getOwner()==Mazewar.clients.get(Mazewar.clientID))
                                                {
                                                	//Send MOVE_PROJECTILE message
                                                	try {
														Mazewar.sendProjectileMove(Mazewar.getSequenceNumber());
													} catch (IOException e) {
														// TODO Auto-generated catch block
														e.printStackTrace();
													}
                                                }
                                        }
                                        //Commented out because this is done in Mazewar upon receiving
                                        //MOVE_PROJECTILE message
                                        /*it = deadPrj.iterator();
                                        while(it.hasNext()) {
                                                Object o = it.next();
                                                assert(o instanceof Projectile);
                                                Projectile prj = (Projectile)o;
                                                projectileMap.remove(prj);
                                                clientFired.remove(prj.getOwner());
                                        }
                                        deadPrj.clear();*/
                                }
                        }
                        try {
                                Thread.sleep(200);
                        } catch(Exception e) {
                                // shouldn't happen
                        }
                }
        }
        
        /* Internals */
        
        public synchronized Collection moveProjectile(Projectile prj) {
                Collection deadPrj = new LinkedList();
                assert(prj != null);
                
                Object o = projectileMap.get(prj);
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                Direction d = dp.getDirection();
                CellImpl cell = getCellImpl(dp);
                
                /* Check for a wall */
                if(cell.isWall(d)) {
                        // If there is a wall, the projectile goes away.
                        cell.setContents(null);
                        deadPrj.add(prj);
                        update();
                        return deadPrj;
                }
                
                DirectedPoint newPoint = new DirectedPoint(dp.move(d), d);
                /* Is the point within the bounds of maze? */
                assert(checkBounds(newPoint));
                
                CellImpl newCell = getCellImpl(newPoint);
                Object contents = newCell.getContents();
                if(contents != null) {
                        // If it is a Client, kill it outright
                        if(contents instanceof Client) {
                                killClient(prj.getOwner(), (Client)contents);
                                cell.setContents(null);
                                deadPrj.add(prj);
                                update();
                                return deadPrj;
                        } else {
                        // Bullets destroy each other
                                assert(contents instanceof Projectile);
                                newCell.setContents(null);
                                cell.setContents(null);
                                deadPrj.add(prj);
                                deadPrj.add(contents);
                                update();
                                return deadPrj;
                        }
                }

                /* Clear the old cell */
                cell.setContents(null);
                /* Write the new cell */
                projectileMap.put(prj, newPoint);
                newCell.setContents(prj);
                update();
                return deadPrj;
        }
        
        /**
         * Internal helper for adding a {@link Client} to the {@link Maze}.
         * @param client The {@link Client} to be added.
         * @param point The location the {@link Client} should be added.
         */
        private synchronized void addClient(Client client, Point point) {
                assert(client != null);
                assert(checkBounds(point));
                CellImpl cell = getCellImpl(point);
                Direction d = Direction.random();
                while(cell.isWall(d)) {
                  d = Direction.random();
                }
                cell.setContents(client);
                clientMap.put(client, new DirectedPoint(point, d));
                client.registerMaze(this);
                client.addClientListener(this);
                update();
                notifyClientAdd(client);
        }
        
        
        /**
         * Try to add a client at specified position in maze. Return true if successful, false if not.
         * @param client	Client to add in maze
         * @param point		Point in maze to add new client in the maze
         * @return			True if successful, false if not
         */
        public synchronized boolean addClient(Client client, Point point, int dir) {
            assert(client != null);
            assert(checkBounds(point));
            CellImpl cell = getCellImpl(point);
            Direction d = new Direction(dir);
            
            //Make sure cell is empty and we can place player there
            if(cell==null || cell.getContents()!=null)
            {
            	return false;
            }
            
           if(cell.isWall(d)) {
              return false;
            }
           
            cell.setContents(client);
            clientMap.put(client, new DirectedPoint(point, 
            		(d.equals(Direction.North)?Direction.North:
            			(d.equals(Direction.East)?Direction.East:
            				(d.equals(Direction.South)?Direction.South:Direction.West)))
            				));
            client.registerMaze(this);
            client.addClientListener(this);
            update();
            notifyClientAdd(client);

            return true;
        }
        
        /**
         * Return new spawn point calculated from random variable.
         * @return	New Spawn point (X and Y coordinates in maze)
         */
        public synchronized DirectedPoint getNextSpawn()
        {
        	DirectedPoint dp = new DirectedPoint(randomGen.nextInt(maxX),randomGen.nextInt(maxY),Direction.random());
        	Point pt = new Point(dp.getX(), dp.getY());
        	CellImpl cell = getCellImpl(pt);
        	while(	dp==null || cell.isWall(dp.getDirection()) || cell.getContents()!=null || 
        			checkBounds(pt)==false)
        	{
            	dp = new DirectedPoint(randomGen.nextInt(maxX),randomGen.nextInt(maxY),Direction.random());
            	pt = new Point(dp.getX(), dp.getY());
            	cell = getCellImpl(pt);
        	}
        	return dp;
        }
        
        /**
         * Internal helper for handling the death of a {@link Client}.
         * @param source The {@link Client} that fired the projectile.
         * @param target The {@link Client} that was killed.
         */
        private synchronized void killClient(Client source, Client target) {
                assert(source != null);
                assert(target != null);
                Mazewar.consolePrintLn(source.getName() + " just vaporized " + 
                                target.getName());
                Object o = clientMap.remove(target);
                assert(o instanceof Point);
                Point point = (Point)o;
                Mazewar.printLn("Removing killed player from ("+point.getX()+","+point.getY()+").");
                CellImpl cell = getCellImpl(point);
                cell.setContents(null);
                // Pick a random starting point, and check to see if it is already occupied
                //Commented out due to design in distributed game.. Need to send spawn message first
                /*point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
                cell = getCellImpl(point);
                // Repeat until we find an empty cell
                while(cell.getContents() != null) {
                        point = new Point(randomGen.nextInt(maxX),randomGen.nextInt(maxY));
                        cell = getCellImpl(point);
                }
                Direction d = Direction.random();
                while(cell.isWall(d)) {
                        d = Direction.random();
                }
                cell.setContents(target);
                clientMap.put(target, new DirectedPoint(point, d));
                */
                if(target instanceof GUIClient || target instanceof RobotClient)
                {
                	try {
						Mazewar.sendSpawn(Mazewar.getSequenceNumber());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                update();
                notifyClientKilled(source, target);
        }
        
        public synchronized boolean respawnClient(Client source, Point point, Direction d)
        {
        	//This was needed when each client managed projectiles even for remote clients
        	//Now with network messages for moving projectiles, this is probably not needed, but it's left in
        	//just to be on the safe side. The behaviour it exhibits is:
        	//Wait until source is killed before replacing it in our local map... maybe other client's
        	//timer was really fast and we received the respawn message before we processed our local kill
        	boolean printedMsg=false;
        	while(clientMap.containsKey(source))
        	{
        		if(printedMsg==false)
        		{
        			Mazewar.printLn("Yielding until client is killed by projectile in play before respawning it.");
        		}
        		printedMsg=true;
        		Thread.yield();
        	}
        	
            CellImpl cell = getCellImpl(point);
            // Repeat until we find an empty cell
            if(cell.getContents() != null || cell.isWall(d)) {
            	return false;
            }
            cell.setContents(source);
            //Mazewar.printLn("Respawned client at ("+point.getX()+","+point.getY()+").");
            clientMap.put(source, new DirectedPoint(point, 
            		(d.equals(Direction.North)?Direction.North:
            			(d.equals(Direction.East)?Direction.East:
            				(d.equals(Direction.South)?Direction.South:Direction.West)))
            				));
            Mazewar.printLn("Client map contains our respawned client at ("+
            		((DirectedPoint)(clientMap.get(source))).getX()+"."+
            		((DirectedPoint)(clientMap.get(source))).getY()+") facing "+
            		((DirectedPoint)(clientMap.get(source))).getDirection().toString());
            update();
        	return true;
        }
        
        /**
         * Internal helper called when a {@link Client} emits a turnLeft action.
         * @param client The {@link Client} to rotate.
         */
        private synchronized void rotateClientLeft(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                //If client is dead, do nothing
                if(o==null)return;
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                clientMap.put(client, new DirectedPoint(dp, dp.getDirection().turnLeft()));
                update();
        }
        
        /**
         * Internal helper called when a {@link Client} emits a turnRight action.
         * @param client The {@link Client} to rotate.
         */
        private synchronized void rotateClientRight(Client client) {
                assert(client != null);
                Object o = clientMap.get(client);
                //If client is dead, do nothing
                if(o==null)return;
                assert(o instanceof DirectedPoint);
                DirectedPoint dp = (DirectedPoint)o;
                clientMap.put(client, new DirectedPoint(dp, dp.getDirection().turnRight()));
                update();
        }
        
        /**
         * Internal helper called to move a {@link Client} in the specified
         * {@link Direction}.
         * @param client The {@link Client} to be move.
         * @param d The {@link Direction} to move.
         * @return If the {@link Client} cannot move in that {@link Direction}
         * for some reason, return <code>false</code>, otherwise return 
         * <code>true</code> indicating success.
         */
        private synchronized boolean moveClient(Client client, Direction d) {
                assert(client != null);
                assert(d != null);
                Point oldPoint = getClientPoint(client);
                CellImpl oldCell = getCellImpl(oldPoint);
                
                /* Check that you can move in the given direction */
                if(oldCell.isWall(d)) {
                        /* Move failed */
                        clientMap.put(client, oldPoint);
                        return false;
                }
                
                DirectedPoint newPoint = new DirectedPoint(oldPoint.move(d), getClientOrientation(client));
                
                /* Is the point withint the bounds of maze? */
                assert(checkBounds(newPoint));
                CellImpl newCell = getCellImpl(newPoint);
                if(newCell.getContents() != null) {
                        /* Move failed */
                        clientMap.put(client, oldPoint);
                        return false;
                }
                
                /* Write the new cell */
                clientMap.put(client, newPoint);
                newCell.setContents(client);
                /* Clear the old cell */
                oldCell.setContents(null);	
                
                update();
                return true; 
        }
       
        /**
         * The random number generator used by the {@link Maze}.
         */
        private final Random randomGen;

        /**
         * The maximum X coordinate of the {@link Maze}.
         */
        private final int maxX;

        /**
         * The maximum Y coordinate of the {@link Maze}.
         */
        private final int maxY;

        /** 
         * The {@link Vector} of {@link Vector}s holding the
         * {@link Cell}s of the {@link Maze}.
         */
        private final Vector mazeVector;

        /**
         * A map between {@link Client}s and {@link DirectedPoint}s
         * locating them in the {@link Maze}.
         */
        private final Map clientMap = new HashMap();

        /**
         * The set of {@link MazeListener}s that are presently
         * in the notification queue.
         */
        private final Set listenerSet = new HashSet();

        /**
         * Mapping from {@link Projectile}s to {@link DirectedPoint}s. 
         */
        private final Map projectileMap = new HashMap();
        
        /**
         * The set of {@link Client}s that have {@link Projectile}s in 
         * play.
         */
        private final Set clientFired = new HashSet();
       
        /**
         * The thread used to manage {@link Projectile}s.
         */
        private final Thread thread;
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has been added.
         * @param c The {@link Client} that was added.
         */
        private void notifyClientAdd(Client c) {
                assert(c != null);
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientAdded(c);
                } 
        }
        
        public synchronized Map getProjectileMap()
        {
        	return this.projectileMap;
        }
        
        public synchronized Set getClientFired()
        {
        	return this.clientFired;
        }
        
        /**
         * Generate a notification to listeners that a 
         * {@link Client} has been removed.
         * @param c The {@link Client} that was removed.
         */
        private void notifyClientRemove(Client c) {
                assert(c != null);
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientRemoved(c);
                } 
        }
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has fired.
         * @param c The {@link Client} that fired.
         */
        private void notifyClientFired(Client c) {
                assert(c != null);
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientFired(c);
                } 
        }
        
        /**
         * Generate a notification to listeners that a
         * {@link Client} has been killed.
         * @param source The {@link Client} that fired the projectile.
         * @param target The {@link Client} that was killed.
         */
        private void notifyClientKilled(Client source, Client target) {
                assert(source != null);
                assert(target != null);
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.clientKilled(source, target);
                } 
        }
        
        /**
         * Generate a notification that the {@link Maze} has 
         * changed in some fashion.
         */
        private void update() {
                Iterator i = listenerSet.iterator();
                while (i.hasNext()) {
                        Object o = i.next();
                        assert(o instanceof MazeListener);
                        MazeListener ml = (MazeListener)o;
                        ml.mazeUpdate();
                } 
        }

        /**
         * A concrete implementation of the {@link Cell} class that
         * special to this implementation of {@link Maze}s.
         */
        private class CellImpl extends Cell implements Serializable {
                /**
                 * Has this {@link CellImpl} been visited while
                 * constructing the {@link Maze}.
                 */
                private boolean visited = false;

                /**
                 * The walls of this {@link Cell}.
                 */
                private boolean walls[] = {true, true, true, true};

                /**
                 * The contents of the {@link Cell}. 
                 * <code>null</code> indicates that it is empty.
                 */
                private Object contents = null;
                
                /**
                 * Helper function to convert a {@link Direction} into 
                 * an array index for easier access.
                 * @param d The {@link Direction} to convert.
                 * @return An integer index into <code>walls</code>.
                 */
                private int directionToArrayIndex(Direction d) {
                        assert(d != null);
                        if(d.equals(Direction.North)) {
                                return 0;
                        } else if(d.equals(Direction.East)) {
                                return 1;
                        } else if(d.equals(Direction.South)) {
                                return 2;
                        } else if(d.equals(Direction.West)) {
                                return 3;
                        }
                        /* Impossible */
                        return -1; 
                }
                
                /* Required for the abstract implementation */
                
                public boolean isWall(Direction d) {
                        assert(d != null);
                        return this.walls[directionToArrayIndex(d)];
                }
                
                public synchronized Object getContents() {
                        return this.contents;
                }
                
                /* Internals used by MazeImpl */
                
                /**
                 * Indicate that this {@link Cell} has been
                 * visited while building the {@link MazeImpl}.
                 */
                public void setVisited() {
                        visited = true;
                }
                
                /**
                 * Has this {@link Cell} been visited in the process
                 * of recursviely building the {@link Maze}?
                 * @return <code>true</code> if visited, <code>false</code> 
                 * otherwise.
                 */
                public boolean visited() {
                        return visited;
                }
                
                /**
                 * Add a wall to this {@link Cell} in the specified
                 * Cardinal {@link Direction}.
                 * @param d Which wall to add.
                 */
                public void setWall(Direction d) {
                        assert(d != null);
                        this.walls[directionToArrayIndex(d)] = true;
                }
                
                /**
                 * Remove the wall from this {@link Cell} in the specified
                 * Cardinal {@link Direction}.
                 * @param d Which wall to remove.
                 */
                public void removeWall(Direction d) {
                        assert(d != null);
                        this.walls[directionToArrayIndex(d)] = false;
                }
                
                /**
                 * Set the contents of this {@link Cell}.
                 * @param contents Object to place in the {@link Cell}.
                 * Use <code>null</code> if you want to empty it.
                 */
                public synchronized void setContents(Object contents) {
                        this.contents = contents;
                }
                
        }
        
        /** 
         * Removes the wall in the {@link Cell} at the specified {@link Point} and 
         * {@link Direction}, and the opposite wall in the adjacent {@link Cell}.
         * @param point Location to remove the wall.
         * @param d Cardinal {@link Direction} specifying the wall to be removed.
         */
        private void removeWall(Point point, Direction d) {
                assert(point != null);
                assert(d != null);
                CellImpl cell = getCellImpl(point);
                cell.removeWall(d);
                Point adjacentPoint = point.move(d);
                CellImpl adjacentCell = getCellImpl(adjacentPoint);
                adjacentCell.removeWall(d.invert());
        }
        
        /** 
         * Pick randomly pick an unvisited neighboring {@link CellImpl}, 
         * if none return <code>null</code>. 
         * @param point The location to pick a neighboring {@link CellImpl} from.
         * @return The Cardinal {@link Direction} of a {@link CellImpl} that hasn't
         * yet been visited.
         */
        private Direction pickNeighbor(Point point) {
                assert(point != null);
                Direction directions[] = { 
                        Direction.North, 
                        Direction.East, 
                        Direction.West, 
                        Direction.South };
                        
                        // Create a vector of the possible choices
                        Vector options = new Vector();	       
                        
                        // Iterate through the directions and see which
                        // Cells have been visited, adding those that haven't
                        for(int i = 0; i < 4; i++) {
                                Point newPoint = point.move(directions[i]);
                                if(checkBounds(newPoint)) {
                                        CellImpl cell = getCellImpl(newPoint);
                                        if(!cell.visited()) {
                                                options.add(directions[i]);
                                        }
                                }
                        }
                        
                        // If there are no choices just return null
                        if(options.size() == 0) {
                                return null;
                        }
                        
                        // If there is at least one option, randomly choose one.
                        int n = randomGen.nextInt(options.size());
                        
                        Object o = options.get(n);
                        assert(o instanceof Direction);
                        return (Direction)o;
        }
        
        /**
         * Recursively carve out a {@link Maze}
         * @param point The location in the {@link Maze} to start carving.
         */
        private void buildMaze(Point point) {
                assert(point != null);
                CellImpl cell = getCellImpl(point);
                cell.setVisited();
                Direction d = pickNeighbor(point);
                while(d != null) {	    
                        removeWall(point, d);
                        Point newPoint = point.move(d);
                        buildMaze(newPoint);
                        d = pickNeighbor(point);
                }
        }
       
        /** 
         * Obtain the {@link CellImpl} at the specified point. 
         * @param point Location in the {@link Maze}.
         * @return The {@link CellImpl} representing that location.
         */
        private CellImpl getCellImpl(Point point) {
                assert(point != null);
                Object o1 = mazeVector.get(point.getX());
                assert(o1 instanceof Vector);
                Vector v1 = (Vector)o1;
                Object o2 = v1.get(point.getY());
                assert(o2 instanceof CellImpl);
                return (CellImpl)o2;
        }

		@Override
		public boolean canMoveForward(Client client) {
			//Check if client can move forward (used for RobotClients)
			
			assert(client!=null);
			if(client==null)
			{
				//Mazewar.printLn("cmf: Client is null");				
				return false;
			}
			
			//Check client is in maze
			Object o = clientMap.get(client);
			if(o==null)
			{
				//Mazewar.printLn("cmf: Object for client location is null");	
				return false;
			}
			
			DirectedPoint dp = null;
			if(o instanceof DirectedPoint)
			{
				dp = (DirectedPoint)o;
			}
			else
			{
				//Mazewar.printLn("cmf: Object is not of class DirectedPoint");	
				return false;
			}
			
			//Check no wall in forward direction of client
			if(getCellImpl(getClientPoint(client)).isWall(dp.getDirection()))
			{
				//Mazewar.printLn("cmf: Client is facing a wall");
				return false;
			}

			DirectedPoint newPoint=new DirectedPoint(getClientPoint(client).move(dp.getDirection()), dp.getDirection());
			
			//Check next cell is in bounds
			if(checkBounds(newPoint)==false)
			{
				//Mazewar.printLn("cmf: New point is out of bounds");	
				return false;
			}
			
			//Check forward cell is not occupied)
			if(getCellImpl(newPoint).getContents()!=null)
			{
				//Mazewar.printLn("cmf: Client at ("+dp.getX()+","+dp.getY()+") cannot move to ("+newPoint.getX()+","+newPoint.getY()+") as it is occupied");	
				return false;
			}
			return true;
		}

		@Override
		public boolean canFire(Client client) {
			if(client==null)
				return false;
			
            // If the client already has a projectile in play, fail.
            if(clientFired.contains(client)) {
                    return false;
            }
            
            Point point = getClientPoint(client);
            if(point==null) return false;
            Direction d = getClientOrientation(client);
            CellImpl cell = getCellImpl(point);
            
            /* Check that you can fire in that direction */
            if(cell.isWall(d)) {
                    return false;
            }
            
			return true;
		}
}
