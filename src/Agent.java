/*********************************************
/*  Agent.java 
/*  Sample Agent for Text-Based Adventure Game
/*  COMP3411 Artificial Intelligence
/*  UNSW Session 1, 2013
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Agent {

	private char local_map[][]; // our local representation of the map

	// Size of the local map (i.e. how far the agent can explore)
	private static final int LOCAL_MAP_SIZE = 160;

	// Width and height of view we get from server
	private static final int VIEW_SIZE = 5; 
	private static final int VIEW_HALF_SIZE = 2; 
	// Agent initially starts here
	private static final int START_X = LOCAL_MAP_SIZE / 2;
	private static final int START_Y = LOCAL_MAP_SIZE / 2;

	private int posx, posy; // x, y position
	private int initx, inity; // initial x, y (to return here)
	private int minx, miny, maxx, maxy; // maximally explored area (for
										// debugging output)

	final static int EAST = 0;
	final static int NORTH = 1;
	final static int WEST = 2;
	final static int SOUTH = 3;

	// Facing direction, initially east
	private int direction = EAST;

	private char lastAction = ' ';

	public Agent() {
		local_map = new char[LOCAL_MAP_SIZE][];
		for (int i = 0; i < LOCAL_MAP_SIZE; ++i) {
			local_map[i] = new char[LOCAL_MAP_SIZE];
		}

		for (int y = 0; y < LOCAL_MAP_SIZE; ++y) {
			for (int x = 0; x < LOCAL_MAP_SIZE; ++x) {
				local_map[y][x] = 'x'; // x represents unexplored
				// System.out.print(String.valueOf(local_map[x][y]) + " ");
			}
			// System.out.println();
		}

		posx = START_X;
		posy = START_Y;

		minx = maxx = posx;
		miny = maxy = posy;		
		initx = posx;
		inity = posy;
	}

	/**
	 * Returns a representation of the map currently explored by the agent.
	 * 
	 * @return String representing map
	 */
	public String getExploredArea() {
		String res = "-- Agent Map: --\n";
		res += " - This is what the AI can see... -\n";
		// draw top border
		for (int x = minx; x < maxx + 2; ++x) {
			res += '%';
		}
		res += '\n';
		for (int y = miny; y < maxy; ++y) {
			res += '%';
			for (int x = minx; x < maxx; ++x) {
				if (x == posx && y == posy) {
					res += getCharacterForDirection(direction);
				} else if (x == initx && y == inity) {
					res += 'S';
				} else {
					res += String.valueOf(local_map[y][x]);
				}
			}
			res += "%\n";
		}
		// draw bottom border
		for (int x = minx; x < maxx + 2; ++x) {
			res += '%';
		}
		return res;
	}

	/** returns whether a block can be moved into **/
	public static boolean canMoveInto(char block) {
		return (block != '*' && block != '-' && block != 'T');
	}
	
	public static final char [] arrows = {'>','^','<','v'};
	
	public static char getCharacterForDirection(int dirn) {
		return arrows[dirn];
	}
	
	public void handle_action(int action) {

		// Adjust our direction based on our last action.
		if ((action == 'L') || (action == 'l')) {
			direction = (direction + 1) % 4;
		} else if ((action == 'R') || (action == 'r')) {
			direction = (direction + 3) % 4;
		} else if ((action == 'F') || (action == 'f')) {
			switch(direction) {
			case NORTH:
				if (canMoveInto(local_map[posy-1][posx])) {
					--posy;
				}
				break;
			case SOUTH:
				if (canMoveInto(local_map[posy+1][posx])) {
					++posy;
				}
				break;
			case EAST:
				if (canMoveInto(local_map[posy][posx+1])) {
					++posx;
				}
				break;
			case WEST:
				if (canMoveInto(local_map[posy][posx-1])) {
					--posx;
				}
				break;
				default:
					System.err.println("Unknown direction " + direction);
					System.exit(0);
			}
		}
	}
	/**
	 * Parse a view
	 * 
	 * @param view
	 */
	public void parse_view(char view[][]) {
		int x, xx, y, yy;		
		// Rotate the view so that 0 = east, 1 = north etc (i.e. back into world space)
		view = rotate_view(view, direction);

		print_view(view);
		// Now overlay the rotated view onto the global map
		for (y = posy - VIEW_HALF_SIZE, yy = 0; y <= posy + VIEW_HALF_SIZE; ++y, ++yy) {
			for (x = posx - VIEW_HALF_SIZE, xx = 0; x <= posx + VIEW_HALF_SIZE; ++x, ++xx) {
				local_map[y][x] = view[yy][xx];
			}
		}
		
		minx = Math.max(0, Math.min(minx, posx - VIEW_HALF_SIZE));
		maxx = Math.min(LOCAL_MAP_SIZE - 1, Math.max(maxx, posx + VIEW_HALF_SIZE + 1));
		miny = Math.max(0, Math.min(miny, posy - VIEW_HALF_SIZE));
		maxy = Math.min(LOCAL_MAP_SIZE - 1, Math.max(maxy, posy + VIEW_HALF_SIZE + 1));
	}
	
	// rotate a view into north direction (world space) given the existing
	// direction
	public char[][] rotate_view(char view[][], int dir) {
		int i, j, r = 0, c = 0;
		int size = view.length;
		char [][] view_temp = new char[size][];
		for (i = 0; i < size; ++i) {
			view_temp[i] = new char[size];
			for (j = 0; j < size; ++j) {
				view_temp[i][j] = view[i][j];
			}
		}
		// scan given 5-by-5 window, from agent's point of view, into world point of view (north)
		for (i = 0; i < size; ++i) { // row
			for (j = 0; j < size; ++j) { // col
				switch (dir) {
				case NORTH:
					r = i;
					c = j;
					break;
				case SOUTH:
					r = size - 1 - i;
					c = size - 1 - j;
					break;
				case EAST:
					r = size - 1 - j;
					c = i;
					break;
				case WEST:
					r = j;
					c = size - 1 - i;
					break;
				}
				view_temp[i][j] = view[r][c];
			}
		}

		return view_temp;
	}

	
	public char get_action(char view[][]) {
		
		// Perform a search and pick best next move.
		int ch = searchAStar();
		
		// REPLACE THIS CODE WITH AI TO CHOOSE ACTION
		/**
		int ch = 0;

		System.out.print("Enter Action(s): ");

		try {
			while (ch != -1) {
				// read character from keyboard
				ch = System.in.read();

				switch (ch) { // if character is a valid action, return it
				case 'F':
				case 'L':
				case 'R':
				case 'C':
				case 'O':
				case 'B':
				case 'f':
				case 'l':
				case 'r':
				case 'c':
				case 'o':
				case 'b':
					return ((char) ch);
				}
			}
		} catch (IOException e) {
			System.out.println("IO error:" + e);
		}
		**/
		return (char) ch;
	}

	private int searchAStar() {
		PriorityQueue<Position> queue = new PriorityQueue<Position>();
		// Find interesting points (scan map) and put into PriQ.
		List<Position> pointsOfInterest = findPOI();
		
		// Add all the points of interest in.
		queue.addAll(pointsOfInterest);
		
		// A star!
		while (!queue.isEmpty()) {
			
		}
		
		// Return the next move to take.
		return 0;
	}

	private List<Position> findPOI() {
		// all points of interest to return.
		List<Position> points = new ArrayList<Position>();
		
		// Brute force search for interesting points.
		for (int x = 0; x < LOCAL_MAP_SIZE; x++) {
			for (int y = 0; y < LOCAL_MAP_SIZE; y++) {
				// If its not a floor space or water, it must be interesting.
				switch (local_map[x][y]) {
				case 'T':
					points.add(new Position(x, y, posx, posy, 20));
				case '*':
					points.add(new Position(x, y, posx, posy, 20));
				case 'd':
					points.add(new Position(x, y, posx, posy, 50));
				case 'g':
					points.add(new Position(x, y, posx, posy, 100));
				case 'a':
					points.add(new Position(x, y, posx, posy, 50));
				case 'k':
					points.add(new Position(x, y, posx, posy, 50));
				case 'x':
					points.add(new Position(x, y, posx, posy, 20));
				}
			}
		}
		
		return points;
	}

	void print_view(char view[][], boolean show_arrow) {
		int i, j;
		int size = view.length;
		int half_size = size / 2;
		System.out.println("\n+-----+");
		for (i = 0; i < size; i++) {
			System.out.print("|");
			for (j = 0; j < size; j++) {
				if (show_arrow && (i == half_size) && (j == half_size)) {
					System.out.print('^');
				} else {
					System.out.print(view[i][j]);
				}
			}
			System.out.println("|");
		}
		System.out.println("+-----+");
	}
	
	void print_view(char view[][]) {
		print_view(view, false);
	}

	public static void main(String[] args) {
		InputStream in = null;
		OutputStream out = null;
		Socket socket = null;
		Agent agent = new Agent();
		char view[][] = new char[5][5];
		int port;
		int ch;
		int i, j;
		
		// TODO remove me
		testAgent();

		if (args.length < 2) {
			System.out.println("Usage: java Agent -p <port>\n");
			System.exit(-1);
		}

		port = Integer.parseInt(args[1]);

		try { // open socket to Game Engine
			socket = new Socket("localhost", port);
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (IOException e) {
			System.out.println("Could not bind to port: " + port);
			System.exit(-1);
		}

		try { // scan 5-by-5 wintow around current location
			while (true) {
				for (i = 0; i < 5; i++) {
					for (j = 0; j < 5; j++) {
						if (!((i == 2) && (j == 2))) {
							ch = in.read();
							if (ch == -1) {
								System.exit(-1);
							}
							view[i][j] = (char) ch;
						}
					}
				}
				agent.print_view(view); // COMMENT THIS OUT BEFORE SUBMISSION

				agent.parse_view(view);
				System.out.println(agent.getExploredArea());
				agent.lastAction = agent.get_action(view);
				
				agent.handle_action(agent.lastAction);
				out.write(agent.lastAction);
			}
		} catch (IOException e) {
			System.out.println("Lost connection to port: " + port);
			System.exit(-1);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}
	
	public static void testAgent() {
		// test rotate view
		{
			Agent agent = new Agent();
			char [][] map = {{'c', 'f', 'i'}, {'b', 'e', 'h'}, {'a', 'd', 'g'}};
			char [][] res = agent.rotate_view(map, EAST);
			char [][] expected = {{'a','b','c'},{'d','e','f'},{'g','h','i'}};
			assert(res.equals(expected));
		}
		{
			Agent agent = new Agent();
			char [][] map = {{'g', 'd', 'a'}, {'h', 'e', 'b'}, {'i', 'f', 'c'}};
			char [][] res = agent.rotate_view(map, WEST);
			char [][] expected = {{'a','b','c'},{'d','e','f'},{'g','h','i'}};
			assert(res.equals(expected));
		}
		{
			Agent agent = new Agent();
			char [][] map = {{'i', 'h', 'g'}, {'f', 'e', 'd'}, {'c', 'b', 'a'}};
			char [][] res = agent.rotate_view(map, SOUTH);
			char [][] expected = {{'a','b','c'},{'d','e','f'},{'g','h','i'}};
			assert(res.equals(expected));
		}
		{
			Agent agent = new Agent();
			char [][] map = {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}};
			char [][] res = agent.rotate_view(map, NORTH);
			char [][] expected = {{'a','b','c'},{'d','e','f'},{'g','h','i'}};
			assert(res.equals(expected));
		}
	}
}
