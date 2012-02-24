import java.io.Serializable;

public class MazewarPacket implements Serializable {
	public static final int TYPE_MOVE_LEFT=1;
	public static final int TYPE_MOVE_RIGHT=2;
	public static final int TYPE_MOVE_UP_FORWARD=3;
	public static final int TYPE_MOVE_DOWN_BACKWARD=4;
	public static final int TYPE_FIRE=5;
	public static final int TYPE_DEAD=6;
	public static final int TYPE_QUIT=7;
	public static final int TYPE_INVALID=-1;
	
	public static final int ACTION_MOVE=1;
	public static final int ACTION_JOIN=2;
	public static final int ACTION_INVALID=-1;
	
	public static final int ID_INVALID=-1;
	
	public int action=ACTION_INVALID;
	public int type=TYPE_INVALID;
	public int playerID=ID_INVALID;
	public String playerName="";
	
	public int seed;

	public int xpos;
	public int ypos;
	public Direction dir;
}
