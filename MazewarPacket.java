import java.io.Serializable;
import java.util.HashMap;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class MazewarPacket implements Serializable {
	//Type of MOVE messages
	public static final int TYPE_MOVE_LEFT=1;
	public static final int TYPE_MOVE_RIGHT=2;
	public static final int TYPE_MOVE_UP_FORWARD=3;
	public static final int TYPE_MOVE_DOWN_BACKWARD=4;
	public static final int TYPE_FIRE=5;
	public static final int TYPE_DEAD=6;
	public static final int TYPE_SPAWN=7;
	public static final int TYPE_QUIT=8;
	public static final int TYPE_MOVE_PROJECTILE=9;
	public static final int TYPE_INVALID=-1;
	
	//Type of action messages
	public static final int ACTION_MOVE=1;
	public static final int ACTION_JOIN=2;
	public static final int ACTION_START=3;
	public static final int ACTION_LEAVE=4;
	public static final int ACTION_REQ_SEQ=5;
	public static final int ACTION_INVALID=-1;
	
	//Invalid constant for types
	public static final int ID_INVALID=-1;
	
	private int action=ACTION_INVALID;
	private int type=TYPE_INVALID;
	private int playerID=ID_INVALID;
	
	/**
	 * Max number of players in the game
	 */
	private int maxplayer = 0;
	
	/**
	 * Name for player this message concerns
	 */
	private String playerName="";
	
	/**
	 * Sequence number for event packet.
	 */
	private int seqNo=0;
	
	/**
	 * HashMap distributed on JOIN events containing connection info for all players
	 */
	private HashMap<Integer,NetworkAddress> players=null;
	
	/**
	 * Random seed provided by server initially
	 */
	private int seed;

	//Position and direction variables for location synchronization when needed
	private int xpos;
	private int ypos;
	private int dir;

	public void setAction(int action) {
		this.action = action;
	}
	
	public int getAction() {
		return action;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}

	public void setPlayerID(int playerID) {
		this.playerID = playerID;
	}

	public int getPlayerID() {
		return playerID;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setSeed(int seed) {
		this.seed = seed;
	}

	public int getSeed() {
		return seed;
	}

	public void setXpos(int xpos) {
		this.xpos = xpos;
	}

	public int getXpos() {
		return xpos;
	}

	public void setYpos(int ypos) {
		this.ypos = ypos;
	}

	public int getYpos() {
		return ypos;
	}

	public void setDir(int dir) {
		this.dir = dir;
	}

	public int getDir() {
		return dir;
	}

	public void setMaxplayer(int maxplayer) {
		this.maxplayer = maxplayer;
	}

	public int getMaxplayer() {
		return maxplayer;
	}

	public HashMap<Integer,NetworkAddress> getPlayers() {
		return players;
	}

	public void setPlayers(HashMap<Integer,NetworkAddress> players) {
		this.players = (HashMap<Integer, NetworkAddress>) players.clone();
	}

	public int getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(int seqNo) {
		this.seqNo = seqNo;
	}	
}

class NetworkAddress implements Serializable
{
	public String address="";
	public int port=-1;
	
	public NetworkAddress(String newAddress, int newPort)
	{
		address=newAddress;
		port=newPort;
	}
}
