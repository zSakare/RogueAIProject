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

import controller.*;
import model.*;
import view.*;

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

	// next action to be given to the agent
	private char giveAction = ' ';
	
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
		return (block != '*' && block != '-' && block != 'T' && block != 'x' && block != '~');
	}
	
	/**
	 * Returns whether a block can be traversed.
	 * 
	 * @param block - block to check
	 * @return - whether a block can be traversed.
	 */
	public static boolean canMoveThrough(char block) {
		return (block != '*' && block != '-' && block != 'T');
	}
	
	public void handle_action(int action) {

		// Adjust our direction based on our last action.
		if ((action == 'L') || (action == 'l')) {
			direction = (direction + 1) % 4;
		} else if ((action == 'R') || (action == 'r')) {
			direction = (direction + 3) % 4;
		} else if ((action == 'F') || (action == 'f')) {
			int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
			if (canMoveInto(local_map[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]])) {
				posx += moveVectors[direction][0];
				posy += moveVectors[direction][1];
				handleMoveInto(local_map[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]]);
			}
		}
	}
	
	/** handles moving into a certain character */
	private void handleMoveInto(char c) {
		switch (c) {
		case 'g':
			inventory.put('g', inventory.get('g') + 1);
			break;
			default:
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
		/* Add later, commented out for gui view.
		// Find interesting point (scan map) and put into PriQ.
		/*Position goal = findPOI();
		
		// Perform a search and pick best next move.
		List<Position> path = searchAStar(goal.getX(), goal.getY(), posx, posy);
		
		// TODO: Remove debug prints later.
		for (Position step : path) {
			System.out.println("Move: (" + step.getCurrX() + "," + step.getCurrY() + ").");
		}
		*/
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

	public List<Position> searchAStar(int goalX, int goalY, int currentX, int currentY) {
		PriorityQueue<Position> queue = new PriorityQueue<Position>();
		Set<Position> explored = new HashSet<Position>();
		
		// Remember each position's successor.
		Map<Position, Position> cameFrom = new HashMap<Position, Position>();
		
		// Create the goal state based on params.
		Position goal = new Position(goalX, goalY, goalX, goalY, 0);
		
		// Unpathable goal do not bother searching or we will cause an infinite loop.
		if (!canMoveThrough(local_map[goalY][goalX])) {
			return null;
		}
		
		// Add the current position.
		queue.add(new Position(goal.getX(), goal.getY(), currentX, currentY, 20));
		
		// Mark the initial scores.
		Map<Position, Integer> cost = new HashMap<Position, Integer>();
		// Predicted cost.
		Map<Position, Integer> fCost = new HashMap<Position, Integer>();
		 
		cost.put(queue.peek(), 0);
		queue.peek().setCost(0);
		fCost.put(queue.peek(), (cost.get(queue.peek()) + queue.peek().absoluteDistanceFrom(goal)));
		
		Position current = null;
		Position lastBest = null; // last best position explored
		// A star!
		while (!queue.isEmpty()) {
			// Take the top element
			current = queue.peek();
			if (current.equals(goal)) {
				// Save the current state, finish the loop.
				return pathFind(cameFrom, current);
			}
			
			// Remove the element from the queue and add it to our explored set.
			explored.add(queue.remove());
			
			// Get all possible next moves.
			List<Position> neighbours = getLegalNeighbours(current);
			
			// Iterate through all next possible moves, find new, unexplored moves to explore.
			if (neighbours != null) {
				for (Position neighbour : neighbours) {
					int potentialCost = cost.get(current) + neighbour.absoluteDistanceFrom(current);
					if (!cost.containsKey(neighbour)) {
						cost.put(neighbour, potentialCost);
					}
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
						neighbour.setCost(potentialCost);
						lastBest = neighbour;
						fCost.put(neighbour, neighbour.absoluteDistanceFrom(goal));
						if (!queue.contains(neighbour)) {
							// TODO: Remove debug prints later.
							//System.out.println("Exploring: " + neighbour.getCurrX() + "," + neighbour.getCurrY() + " with cost: " + cost.get(neighbour));
							queue.add(neighbour);
						}
					}
				}
			}
		}
		
		// We haven't found a viable path to take.
		if (lastBest != null) {
			return pathFind(cameFrom, lastBest);
		} else {
			return null;
		}
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
		}
		
		if (current.getCurrX() != LOCAL_MAP_SIZE-1) {
			if (canMoveInto(local_map[current.getCurrY()][current.getCurrX()+1])) {
				legalPositions.add(new Position(current.getX(), current.getY(), current.getCurrX()+1, current.getCurrY(), current.getReward()));
			}
		}
		
		// Checking we do not go out of bounds.
		if (current.getCurrY() != 0) {
			if (canMoveInto(local_map[current.getCurrY()-1][current.getCurrX()])) {
				legalPositions.add(new Position(current.getX(), current.getY(), current.getCurrX(), current.getCurrY()-1, current.getReward()));
			}
		}
		
		if (current.getCurrY() != LOCAL_MAP_SIZE-1) {
			if (canMoveInto(local_map[current.getCurrY()+1][current.getCurrX()])) {
				legalPositions.add(new Position(current.getX(), current.getY(), current.getCurrX(), current.getCurrY()+1, current.getReward()));
			}
		}
		
		return legalPositions;
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
	 * Get the score for a certain position
	 * @return The score of the given position
	 */
	public int getScore(int x, int y) {
		
		if (x == START_X && y == START_Y && inventory.get('g') > 0) {
			// We have gold, add interest to returning to starting position.
			return 100;
		} else {
			char atPosition = local_map[y][x];
			switch (atPosition) {
			case 'T':
				// If we have a tree, it's more interesting if we have an axe.
				if (inventory.get('a') > 0) {
					return 70;
				} else {
					return 0;
				}
			case '*':
				if (inventory.get('d') > 0) {
					return 70;
				} else {
					return 0;
				}
			case 'd':
				return 50;
			case 'g':
				return 100;
			case 'a':
				return 50;
			case 'k':
				return 50;
			case 'x':
				return -50;
			case '~':
				return -100;
			case ' ':
				// dependong on whether this cell is next to unexplored or no
				if (hasNeighboursUnexplored(x, y)) {
					return 20;
				} else {
					return 0;
				}
			case '-':
				if (inventory.get('k') > 0) {
					return 70;
				} else {
					return 0;
				}
			}
		}
		return 0;
	}

	/**
	 * returns the number of neighbours that are unexplored for a cell
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean hasNeighboursUnexplored(int x, int y) {
		// check left, right, up then down (with array bound checking)
		return (x > 0 && local_map[y][x-1] == 'x') || (x < LOCAL_MAP_SIZE-1 && local_map[y][x+1] == 'x') || 
				(y > 0 && local_map[y-1][x] == 'x') || (y < LOCAL_MAP_SIZE-1 && local_map[y+1][x] == 'x');
	}
	
	/**
	 * Returns the minimum number of turns required to get from a position to another position
	 * @param xStart Initial X
	 * @param yStart Initial Y
	 * @param direction Initial Direction
	 * @param xGoal Goal X
	 * @param yGoal Goal Y
	 * @return Minimum steps to get to goal
	 */
	private int getTurningPenalty(int xStart, int yStart, int direction, int xGoal, int yGoal) {
		int turns = 0;
		switch (direction) {
		case EAST:
			if (xGoal >= xStart && yStart == yGoal) { // straight line
				turns = 0;
			} else if (xGoal >= xStart) { // must turn right or left
				turns = 1;
			} else { // must turn around
				turns = 2;
			}
			break;
		case NORTH:
			if (xGoal == xStart && yGoal <= yStart) { // straight line
				turns = 0;
			} else if (yGoal <= yStart) { // must turn right or left
				turns = 1;
			} else { // must turn around
				turns = 2;
			}
			break;
		case WEST:
			if (xGoal <= xStart && yStart == yGoal) { // straight line
				turns = 0;
			} else if (xGoal <= xStart) { // must turn right or left
				turns = 1;
			} else { // must turn around
				turns = 2;
			}
			break;	
		case SOUTH:
			if (xGoal == xStart && yGoal >= yStart) { // straight line
				turns = 0;
			} else if (yGoal >= yStart) { // must turn right or left
				turns = 1;
			} else { // must turn around
				turns = 2;
			}
			break;
		}
		return turns;
	}
	/**
	 * Helper function that finds the most interesting point on the map.
	 * 
	 * @return - an interesting point.
	 */
	public Position findPOI() {
		// all points of interest to return.
		List<Position> points = new ArrayList<Position>();
		
		// Brute force search for interesting points.
		for (int y = miny; y <= maxx; y++) {
			for (int x = minx; x < maxx; x++) {
				// consider the turning penalty to prioritise moves in front of us
				points.add(new Position(x, y, posx, posy, getScore(x, y) - getTurningPenalty(posx, posy, direction, x, y)));
			}
		}
		
		// Find the most interesting point.
		Position pointOfInterest = points.get(0);
		for (int i = 1; i < points.size(); i++) {
			Position next = points.get(i);
			
			if (pointOfInterest.getInterest() > next.getInterest()) {
				pointOfInterest = next;
			}
		}
		
		return (new Position(pointOfInterest.getX(), pointOfInterest.getY(), pointOfInterest.getX(), pointOfInterest.getY(), pointOfInterest.getReward()));
	}

	public static void main(String[] args) {
		Agent agent = new Agent();
		int port;
		// TODO remove me
		testAgent();

		if (args.length < 2) {
			System.out.println("Usage: java Agent -p <port> [-gui]\n");
			System.exit(-1);
		}

		port = Integer.parseInt(args[1]);

		// attach a view to the agent
		IAgentView agentView = null;
		
		if (args.length >= 3 && args[2].equals("-gui")) {
			agentView = new AgentGUIView(agent);
		} else {
			agentView = new AgentConsoleView(agent);
		}
		
		// Create the controller
		IAgentController controller = new AgentConsoleController(agent, agentView);
		
		controller.initialiseNetwork(port);
		
		// hack, pass the port through to the view (instead of the controller)
		agentView.run(port);
		
		controller.shutdown();
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
