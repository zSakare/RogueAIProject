package logic;
/*********************************************
/*  Agent.java 
/*  Sample Agent for Text-Based Adventure Game
/*  COMP3411 Artificial Intelligence
/*  UNSW Session 1, 2013
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import model.Goal;
import model.Position;
import view.AgentConsoleView;
import view.AgentGUIView;
import view.IAgentView;
import controller.AgentConsoleController;
import controller.IAgentController;

// TODO: Immediate Goal
/* 1. Cache a point of interest (path) when one is found.
 * 2. If a greater reward is found after more of the map is revealed, replace current path.
 */
// TODO: Later Goals:
/* 1. Consider how to build a plan (not just a path) to reach a location.
 * 2. E.g. if a tree is blocking path, how to break open tree to get to gold.
 * 3. Has to consider possible waste of items (blowing up random walls or trees).
 */
public class Agent {

	private List<IAgentView> views; // list of all the views observing this agent
	
	private Position local_map[][]; // our local representation of the map

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
	
	private Goal currentGoal; // current goal
	private PriorityQueue<Goal> goals; // potential goals
	
	public Agent() {
		views = new LinkedList<IAgentView>();
		
		local_map = new Position[LOCAL_MAP_SIZE][];
		for (int i = 0; i < LOCAL_MAP_SIZE; ++i) {
			local_map[i] = new Position[LOCAL_MAP_SIZE];
		}

		for (int y = 0; y < LOCAL_MAP_SIZE; ++y) {
			for (int x = 0; x < LOCAL_MAP_SIZE; ++x) {
				local_map[y][x] = new Position(x, y);
				local_map[y][x].piece = 'x'; // x represents unexplored
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
		
		currentGoal = null;
		
		goals = new PriorityQueue<Goal>();
		
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
		return local_map[y][x].piece;
	}
	
	public int getTurnNumber() {
		return turnNumber;
	}
	
	/** returns whether a block can be moved into **/
	public boolean canMoveInto(char block) {
		return (block != '*' && block != '-' && (block != 'T' || getItems('a') > 0) && block != 'x' && block != '~');
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
			if (canMoveInto(local_map[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]].piece)) {
				posx += moveVectors[direction][0];
				posy += moveVectors[direction][1];
				handleMoveInto(local_map[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]].piece);
			}
		} else if ((action == 'C') || (action == 'c')) { // chop down
			int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
			if (getItems('a') > 0 && local_map[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]].piece == 'T') {
				local_map[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]].piece = ' ';
			}
		}
	}
	
	/** handles moving into a certain character */
	private void handleMoveInto(char c) {
		switch (c) {
		case 'g':
			inventory.put('g', getItems('g') + 1);
			break;
		case 'a':
			inventory.put('a', getItems('a') + 1);
			break;
		case 'd':
			inventory.put('d', getItems('d') + 1);
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

		char piece;
		int score;
		last_view = view;
		
		// Now overlay the rotated view onto the global map
		for (y = posy - VIEW_HALF_SIZE, yy = 0; y <= posy + VIEW_HALF_SIZE; ++y, ++yy) {
			for (x = posx - VIEW_HALF_SIZE, xx = 0; x <= posx + VIEW_HALF_SIZE; ++x, ++xx) {
				piece = view[yy][xx];
				local_map[y][x].piece = piece;
			}
		}
		
		// Parse anything new that came in (i.e. beyond the min/max boundaries)
		// This must be done after viewing.
		for (y = posy - VIEW_HALF_SIZE; y <= posy + VIEW_HALF_SIZE; ++y) {
			for (x = posx - VIEW_HALF_SIZE; x <= posx + VIEW_HALF_SIZE; ++x) {
				if ((y > miny && y <= maxy && x >= minx && x <= maxx)
					&& local_map[y][x].piece != ' ') {
					score = getScore(x, y);
					if (score <= 0) continue; // dumb to waste processing on this
					Goal g = new Goal(x, y, local_map[y][x].piece, score);
					if (goals.contains(g)) {
						goals.remove(g);
					}
					goals.add(g);
				}
			}
		}
		
		
		minx = Math.max(0, Math.min(minx, posx - VIEW_HALF_SIZE));
		maxx = Math.min(LOCAL_MAP_SIZE - 1, Math.max(maxx, posx + VIEW_HALF_SIZE + 1));
		miny = Math.max(0, Math.min(miny, posy - VIEW_HALF_SIZE));
		maxy = Math.min(LOCAL_MAP_SIZE - 1, Math.max(maxy, posy + VIEW_HALF_SIZE + 1));
		
		// process the local information as goals
		// No goal, or already at goal - find a new goal
		if (currentGoal == null || (posx == currentGoal.getX() && posy == currentGoal.getY())) {
			updateGoals();
			currentGoal = findGoal();
		}
	}
	
	/**
	 * update all local goal scores
	 */
	private void updateGoals() {
		int score;
		PriorityQueue<Goal> newGoals = new PriorityQueue<Goal>();
		for (Goal g : goals) {
			score = getScore(g.getX(), g.getY());
			g.setScore(score);
			newGoals.add(g);
		}
		goals = newGoals;
	}
	private Goal findGoal() {
		Goal answer = null;
		// find exploring cell
		PriorityQueue<Goal> explorable = new PriorityQueue<Goal>();

		// Brute force search for interesting points in our window of explored area.
		for (int y = miny; y <= maxy; y++) {
			for (int x = minx; x <= maxx; x++) {
				if (local_map[y][x].piece == ' ') {
					// consider the turning penalty to prioritise moves in front of us
					int score = getScore(x, y) - getTurningPenalty(posx, posy, direction, x, y);
					explorable.add(new Goal(x, y, local_map[y][x].piece, score));
				}
			}
		}
		// Put in any entries from our spotted goal list that we can achieve easily
		for (Goal g : goals) {
			if (g.isAchievable(this)) {
				System.out.println("Able to achieve goal " + g);
				explorable.add(g);
			} else {
				System.out.println("Unable to achieve goal " + g);
			}
		}
		
		// pop down until we find a pathable explorable goal
		while (explorable.size() > 0) {
			Goal head = explorable.poll();
			List<Position> path = searchAStar(head.getX(), head.getY(), posx, posy);
			if (path != null) {
				head.setPath(path);
				answer = head;
				break;
			}
		}
		
		
		return answer;
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

	public Goal getCurrentGoal() {
		return currentGoal;
	}
	
	public int getItems(char item) {
		Integer itm = inventory.get(item);
		return itm != null ? itm.intValue() : 0;
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
		// no need for this, use references
		//Map<Position, Position> cameFrom = new HashMap<Position, Position>();
		
		// Create the goal state based on params.
		Position goal = new Position(goalX, goalY);
		
		// Unpathable goal do not bother searching or we will cause an infinite loop.
		if (!canMoveThrough(local_map[goalY][goalX].piece)) {
			return null;
		}
		
		// Add the current position.
		Position startPosition = new Position(currentX, currentY);
		queue.add(startPosition);
		
		startPosition.setCost(0);		
		startPosition.setFcost( startPosition.absoluteDistanceFrom(goal));
		
		HashSet<Position> seen = new HashSet<Position>();
		Position current = null;
		
		// A star!
		while (!queue.isEmpty()) {
			// Take the top element
			current = queue.poll();
			if (current.equals(goal)) {
				// Save the current state, finish the loop.
				return pathFind(current);
			}
			
			// Remove the element from the queue and add it to our explored set.
			explored.add(current);
			
			// Get all possible next moves.
			List<Position> neighbours = getLegalNeighbours(current);

			// Iterate through all next possible moves, find new, unexplored moves to explore.
			if (neighbours != null) {
				for (Position neighbour : neighbours) {
					int potentialCost = current.getCost() + neighbour.absoluteDistanceFrom(current);
					if (!seen.contains(neighbour)) { // if (neighbour has no cost) !neighbour.getCost()) {
						seen.add(neighbour);
						neighbour.setCost(potentialCost);
					}
					if (explored.contains(neighbour)) {
						if (potentialCost >= neighbour.getCost()) {
							// ignore the neighbour since we've already explored it from another path
							// and the new path to the neighbour isn't any better.
						}
					} else if (!queue.contains(neighbour) || potentialCost < neighbour.getCost()) {
						// Map where we came from (to pathfind).
						neighbour.setParent(current);
						// Update costs.
						neighbour.setCost(potentialCost);
						neighbour.setFcost(neighbour.absoluteDistanceFrom(goal));
						if (!queue.contains(neighbour)) {
							queue.add(neighbour);
						}
					}
				}
			}
		}
		
		// We haven't found a viable path to take.
		return null;
	}
	
	static final int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
	
	/**
	 * Helper function to return a list of legal positions to move into.
	 * 
	 * @param current - current position to check from.
	 * @return - list of legal positions
	 */
	private List<Position> getLegalNeighbours(Position current) {
		// List of legal positions.
		List<Position> legalPositions = new ArrayList<Position>();
		
		// Checking we do not go out of bounds or into walls.
		for (int [] vector : moveVectors) {
			int nx = current.getX() + vector[0];
			int ny = current.getY() + vector[1];
			if (nx >= 0 && nx < LOCAL_MAP_SIZE && ny >= 0 && ny < LOCAL_MAP_SIZE
					&& canMoveInto(local_map[ny][nx].piece)) {
				legalPositions.add(local_map[ny][nx]);
			}
		}
		
		return legalPositions;
	}

	/**
	 * Helper function that iteratively finds the path taken by the nodes.
	 * 
	 * @param current - current position to back track
	 * @return - the path taken to reach goal.
	 */
	private List<Position> pathFind(Position current) {
		LinkedList<Position> path = new LinkedList<Position>();
		Position p = current;
		while (p != null) {
			path.addFirst(p);
			p = p.getParent();
		}
		return path;
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
			char atPosition = local_map[y][x].piece;
			switch (atPosition) {
			case 'T':
				// If we have a tree, it's more interesting if we have an axe.
				if (inventory.get('a') > 0) {
					return 70;
				} else {
					return 1;
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
				// also prioritise distance (manhattan) - range of 5-20
				if (hasNeighboursUnexplored(x, y)) {
					return Math.max(20 - manhattan(posx, posy, x, y), 5);
				} else {
					return 0;
				}
			case '-':
				if (inventory.get('k') > 0) {
					return 70;
				} else {
					return 1;
				}
			}
		}
		return 0;
	}

	/**
	 * Return the manhattan distance between two points
	 * @param startx
	 * @param starty
	 * @param goalx
	 * @param goaly
	 * @return Manhattan distance
	 */
	private int manhattan(int startx, int starty, int goalx, int goaly) {
		return Math.abs(startx - goalx) + Math.abs(starty - goaly);
	}
	/**
	 * returns the number of neighbours that are unexplored for a cell
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean hasNeighboursUnexplored(int x, int y) {
		// check left, right, up then down (with array bound checking)
		return (x > 0 && local_map[y][x-1].piece == 'x') || (x < LOCAL_MAP_SIZE-1 && local_map[y][x+1].piece == 'x') || 
				(y > 0 && local_map[y-1][x].piece == 'x') || (y < LOCAL_MAP_SIZE-1 && local_map[y+1][x].piece == 'x');
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
