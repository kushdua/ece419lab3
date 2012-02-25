import java.io.Serializable;

public class MazewarPacket implements Serializable {
	public static final int TYPE_MOVE_LEFT=1;
	public static final int TYPE_MOVE_RIGHT=2;
	public static final int TYPE_MOVE_UP_FORWARD=3;
	public static final int TYPE_MOVE_DOWN_BACKWARD=4;
	public static final int TYPE_FIRE=5;
	public static final int TYPE_DEAD=6;
	public static final int TYPE_SPAWN=7;
	public static final int TYPE_QUIT=8;
	public static final int TYPE_INVALID=-1;
	
	public static final int ACTION_MOVE=1;
	public static final int ACTION_JOIN=2;
	public static final int ACTION_START=3;
	public static final int ACTION_INVALID=-1;
	
	public static final int ID_INVALID=-1;
	
	private int action=ACTION_INVALID;
	private int type=TYPE_INVALID;
	private int playerID=ID_INVALID;
	private int maxplayer = 0;
	private String playerName="";
	
	private int seed;

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
}
