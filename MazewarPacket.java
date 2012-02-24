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
	public static final int ACTION_INVALID=-1;
	
	public static final int ID_INVALID=-1;
	
	private int action=ACTION_INVALID;
	private int type=TYPE_INVALID;
	private int playerID=ID_INVALID;
	private String playerName="";
	
	private int seed;

	private int xpos;
	private int ypos;
	private Direction dir;
}
