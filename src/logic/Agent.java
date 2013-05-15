package logic;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import model.Position;
import view.AgentConsoleView;
import view.AgentGUIView;
import view.IAgentView;

public class Agent {

	private List<IAgentView> views; // list of all the views observing this agent
	
	private char local_map[][]; // our local representation of the map

	private char last_view[][]; // last view received by server
	
	// Size of the local map (i.e. how far the agent can explore)
	public static final int LOCAL_MAP_SIZE = 160;

	// Width and height of view we get from server
	public static final int VIEW_SIZE = 5; 
	public static final int VIEW_HALF_SIZE = 2; 
	// Agent initially starts here
	private static final int START_X = LOCAL_MAP_SIZE / 2;
	private static final int START_Y = LOCAL_MAP_SIZE / 2;

	private int posx, posy; // x, y position
	private int initx, inity; // initial x, y (to return here)
	private int minx, miny, maxx, maxy; // maximally explored area (for
										// debugging output)
	
	private int turnNumber; 

	// Inventory represented by map.
	private Map<Character, Integer> inventory = new HashMap<Character, Integer>();
	
	final static int EAST = 0;
	final static int NORTH = 1;
	final static int WEST = 2;
	final static int SOUTH = 3;

	// Facing direction, initially east
	private int direction = EAST;

	private char lastAction = ' ';

	public Agent() {
		views = new LinkedList<IAgentView>();
		
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
		
		turnNumber = 0;
		
		// Populate inventory.
		inventory.put('d', 0);
		inventory.put('k', 0);
		inventory.put('a', 0);
		inventory.put('g', 0);
	}
	
	// adds a view to our view list
	public void addView(IAgentView view) {
		views.add(view);
	}
	
	// signal to all views to refresh
	public void updateViews() {
		for (IAgentView v : views) {
			v.onUpdate(posx, posy);
		}
	}

	// Get our minimum explored X value
	public int getMinX() {
		return minx;
	}
	
	// Get our minimum explored Y value
	public int getMinY() {
		return miny;
	}
	
	// Get our maximum explored X value
	public int getMaxX() {
		return maxx;
	}
	
	// Get our maximum explored Y value
	public int getMaxY() {
		return maxy;
	}
	
	// Get our current X value
	public int getX() {
		return posx;
	}
	
	// Get our current Y value
	public int getY() {
		return posy;
	}
	
	// Get the position that our agent started at (typically halfway point)
	public int getInitX() {
		return initx;
	}
	
	public int getInitY() {
		return inity;
	}
	
	// Get the direction (0 = EAST, increasing counterclockwise) our agent is facing
	public int getDirection() {
		return direction;
	}
	
	public char [][] getLastView() {
		return last_view;
		
	}
	// Get the character at the given position
	public char charAt(int x, int y) {
		return local_map[y][x];
	}
	
	public int getTurnNumber() {
		return turnNumber;
	}
	
	/** returns whether a block can be moved into **/
	public static boolean canMoveInto(char block) {
		return (block != '*' && block != '-' && block != 'T');
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

		last_view = view;
		
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
		//int ch = searchAStar();
		
		// REPLACE THIS CODE WITH AI TO CHOOSE ACTION
		
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
		
		return (char) ch;
	}

	private List<Position> searchAStar() {
		PriorityQueue<Position> queue = new PriorityQueue<Position>();
		Set<Position> explored = new HashSet<Position>();
		
		// Remember each position's successor.
		Map<Position, Position> cameFrom = new HashMap<Position, Position>();
		
		// Find interesting point (scan map) and put into PriQ.
		Position goal = findPOI();
		
		// Add the current position.
		queue.add(new Position(goal.getX(), goal.getY(), posx, posy, 10));
		
		// Mark the initial scores.
		Map<Position, Integer> cost = new HashMap<Position, Integer>();
		// Predicted cost.
		Map<Position, Integer> fCost = new HashMap<Position, Integer>();
		
		cost.put(queue.peek(), 0);
		fCost.put(queue.peek(), (cost.get(queue.peek()) + queue.peek().getCost()));
		
		// A star!
		while (!queue.isEmpty()) {
			// Take the top element
			Position current = queue.peek();
			if (current.equals(goal)) {
				// Save the current state, finish the loop.
				return pathFind(cameFrom, current);
			}
			
			// Remove the element from the queue and add it to our explored set.
			explored.add(queue.remove());
			
			// Get all possible next moves.
			List<Position> neighbours = getLegalNeighbours(current);
			
			// Iterate through all next possible moves, find new, unexplored moves to explore.
			for (Position neighbour : neighbours) {
				int potentialCost = cost.get(current) + neighbour.absoluteDistanceFrom(current);
				if (explored.contains(neighbour)) {
					if (potentialCost >= cost.get(neighbour)) {
						// ignore the neighbour since we've already explored it from another path
						// and the new path to the neighbour isn't any better.
					}
				} else if (!queue.contains(neighbour) || potentialCost < cost.get(neighbour)) {
					// Map where we came from (to pathfind).
					cameFrom.put(neighbour, current);
					// Update costs.
					cost.put(neighbour, potentialCost);
					fCost.put(neighbour, neighbour.getCost());
					if (!queue.contains(neighbour)) {
						queue.add(neighbour);
					}
				}
			}
		}
		
		// We haven't found a viable path to take.
		return null;
	}

	/**
	 * Helper function to return a list of legal positions to move into.
	 * 
	 * @param current - current position to check from.
	 * @return - list of legal positions
	 */
	private List<Position> getLegalNeighbours(Position current) {
		// List of legal positions.
		List<Position> legalPositions = new ArrayList<Position>();
		
		// Checking we do not go out of bounds.
		if (current.getCurrX() != 0) {
			if (canMoveInto(local_map[current.getCurrY()][current.getCurrX()-1])) {
				legalPositions.add(new Position(current.getX(), current.getY(), current.getCurrX()-1, current.getCurrY(), current.getReward()));
			}
		} else if (current.getCurrX() != LOCAL_MAP_SIZE-1) {
			if (canMoveInto(local_map[current.getCurrY()][current.getCurrX()+1])) {
				legalPositions.add(new Position(current.getX(), current.getY(), current.getCurrX()+1, current.getCurrY(), current.getReward()));
			}
		}
		
		// Checking we do not go out of bounds.
		if (current.getCurrY() != 0) {
			if (canMoveInto(local_map[current.getCurrY()-1][current.getCurrX()])) {
				legalPositions.add(new Position(current.getX(), current.getY(), current.getCurrX(), current.getCurrY()-1, current.getReward()));
			}
		} else if (current.getCurrY() != LOCAL_MAP_SIZE-1) {
			if (canMoveInto(local_map[current.getCurrY()+1][current.getCurrX()])) {
				legalPositions.add(new Position(current.getX(), current.getY(), current.getCurrX(), current.getCurrY()+1, current.getReward()));
			}
		}
		
		return null;
	}

	/**
	 * Helper function that recursively finds the path taken by the nodes.
	 * 
	 * @param cameFrom - Map of child positions to parent positions
	 * @param current - current position to back track
	 * @return - the path taken to reach goal.
	 */
	private List<Position> pathFind(Map<Position, Position> cameFrom, Position current) {
		if (cameFrom.containsKey(current)) {
			List<Position> pathToTake = pathFind(cameFrom, cameFrom.get(current));
			pathToTake.add(current);
			return pathToTake;
		} else {
			List<Position> pathToTake = new ArrayList<Position>();
			pathToTake.add(current);
			return pathToTake;
		}
	}

	/**
	 * Helper function that finds the most interesting point on the map.
	 * 
	 * @return - an interesting point.
	 */
	private Position findPOI() {
		// all points of interest to return.
		Position point = null;
		
		if (inventory.get('g') > 0) {
			// We have gold, add interest to returning to starting position.
			point = new Position(START_X, START_Y, START_X, START_Y, 100);
		} else {
			// Brute force search for interesting points.
			for (int y = 0; y < LOCAL_MAP_SIZE; y++) {
				for (int x = 0; x < LOCAL_MAP_SIZE; x++) {
					if (point == null) {
						switch (local_map[y][x]) {
						case 'T':
							// If we have a tree, it's more interesting if we have an axe.
							if (inventory.get('a') > 0) {
								point = new Position(x, y, x, y, 70);
							} else {
								point = new Position(x, y, x, y, 20);
							}
							break;
						case '*':
							if (inventory.get('d') > 0) {
								point = new Position(x, y, x, y, 70);
							} else {
								point = new Position(x, y, x, y, 20);
							}
							break;
						case 'd':
							if (inventory.get('k') > 0) {
								point = new Position(x, y, x, y, 70);
							} else {
								point = new Position(x, y, x, y, 10);
							}
							break;
						case 'g':
							point = new Position(x, y, x, y, 100);
							break;
						case 'a':
							point = new Position(x, y, x, y, 50);
							break;
						case 'k':
							point = new Position(x, y, x, y, 50);
							break;
						case 'x':
							point = new Position(x, y, x, y, 20);
							break;
						case '~':
							point = new Position(x, y, x, y, 0);
							break;
						case ' ':
							point = new Position(x, y, x, y, 10);
							break;
						}
					}
				}
			}
		}
		
		return point;
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
			System.out.println("Usage: java Agent -p <port> [-gui]\n");
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

		// attach a view to the agent
		IAgentView agentView = null;
		
		if (args.length >= 3 && args[2].equals("-gui")) {
			agentView = new AgentGUIView(agent);
		} else {
			agentView = new AgentConsoleView(agent);
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

				agent.parse_view(view);
				
				agent.updateViews();
				
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
