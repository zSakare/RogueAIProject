package model;


/** World: the base for all States, with the knowledge of explored cells before any demolition etc is made. **/
public class World {

	public char [][] w;

	// Size of the local map (i.e. how far the agent can explore)
	public static final int LOCAL_MAP_SIZE = 160;

	// Width and height of view we get from server
	public static final int VIEW_SIZE = 5; 
	public static final int VIEW_HALF_SIZE = 2; 
	
	// Agent initially starts here
	public static final int START_X = LOCAL_MAP_SIZE / 2;
	public static final int START_Y = LOCAL_MAP_SIZE / 2;

	public int minx, miny, maxx, maxy; // maximally explored area (for
										// debugging output and optimisation)
	
	public World() {
		w = new char[LOCAL_MAP_SIZE][LOCAL_MAP_SIZE];
		/* Mark all cells as unexplored */
		for (int y = 0; y < LOCAL_MAP_SIZE; ++y) {
			for (int x = 0; x < LOCAL_MAP_SIZE; ++x) {
				w[y][x] = 'x';
			}
		}
	}
	
	public boolean inVisibleBounds (int x, int y) {
		return (x >= minx && x <= maxx && y >= miny && y <= maxy);
	}
	
	public boolean inBounds (int x, int y) {
		return (x >= 0 && x < LOCAL_MAP_SIZE && y >= 0 && y < LOCAL_MAP_SIZE);
	}
	
	/**
	 * Update the world with the VIEW_SIZE*VIEW_SIZE view centered around the given coordinates.
	 * Ensure the view is rotated appropriately!
	 */
	public void update(int posx, int posy, char [][] view) {
		char piece;
		for (int y = posy - VIEW_HALF_SIZE, yy = 0; y <= posy + VIEW_HALF_SIZE; ++y, ++yy) {
			for (int x = posx - VIEW_HALF_SIZE, xx = 0; x <= posx + VIEW_HALF_SIZE; ++x, ++xx) {
				piece = view[yy][xx];
				w[y][x] = piece;
			}
		}
		
		minx = Math.max(0, Math.min(minx, posx - VIEW_HALF_SIZE));
		maxx = Math.min(LOCAL_MAP_SIZE - 1, Math.max(maxx, posx + VIEW_HALF_SIZE + 1));
		miny = Math.max(0, Math.min(miny, posy - VIEW_HALF_SIZE));
		maxy = Math.min(LOCAL_MAP_SIZE - 1, Math.max(maxy, posy + VIEW_HALF_SIZE + 1));
		
	}

	public boolean isInteresting(int nx, int ny) {
		boolean isInteresting = false;
		
		switch (w[ny][nx]) {
			case 'a':
			case 'd':
			case 'g':
			case 'k':
				isInteresting = true;
				break;
			default:
				isInteresting = false;
				break;
		}
		
		return isInteresting;
	}
}
