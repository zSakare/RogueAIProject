package logic;
/*********************************************
/*  Agent.java 
/*  Sample Agent for Text-Based Adventure Game
/*  COMP3411 Artificial Intelligence
/*  UNSW Session 1, 2013
 */

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import model.Goal;
import model.Inventory;
import model.State;
import model.World;
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

	public World w;
	
	private int turnNumber; 

	// Inventory represented by map.
	private Inventory inventory = new Inventory();
	
	final static int EAST = 0;
	final static int NORTH = 1;
	final static int WEST = 2;
	final static int SOUTH = 3;

	// Facing direction, initially east
	private int direction = EAST;

	
	private Goal currentGoal; // current goal
	private PriorityQueue<Goal> goals; // potential goals
	
	public Agent() {
		views = new LinkedList<IAgentView>();
		
		w = new World();

		posx = START_X;
		posy = START_Y;

		w.minx = w.maxx = posx;
		w.miny = w.maxy = posy;		
		
		turnNumber = 0;
		
		currentGoal = null;
		
		goals = new PriorityQueue<Goal>();

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
		return w.minx;
	}
	
	// Get our minimum explored Y value
	public int getMinY() {
		return w.miny;
	}
	
	// Get our maximum explored X value
	public int getMaxX() {
		return w.maxx;
	}
	
	// Get our maximum explored Y value
	public int getMaxY() {
		return w.maxy;
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
		return START_X;
	}
	
	public int getInitY() {
		return START_Y;
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
		return w.w[y][x];
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
			if (canMoveInto(w.w[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]])) {
				posx += moveVectors[direction][0];
				posy += moveVectors[direction][1];
				handleMoveInto(w.w[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]]);
			}
		} else if ((action == 'C') || (action == 'c')) { // chop down
			int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
			if (getItems('a') > 0 && w.w[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]] == 'T') {
				w.w[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]] = ' ';
			}
		}
	}
	
	/** handles moving into a certain character */
	private void handleMoveInto(char c) {
		switch (c) {
		case 'g':
			inventory.add('g');
			break;
		case 'a':
			inventory.add('a');
			break;
		case 'd':
			inventory.add('d');
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
		// Rotate the view so that 0 = east, 1 = north etc (i.e. back into world space)
		view = rotate_view(view, direction);

		last_view = view;
		
		w.update(posx, posy, view);
		
		// if we reached the goal, or no goal, find a new goal
		if (currentGoal == null || (posx == currentGoal.x && posy == currentGoal.y)) {
			currentGoal = findGoal();
		}
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

	/** find a goal. May return null, in which case.... TODO **/
	public Goal findGoal() {
		Goal g = null;
		
		// use breadth-first search to find the closest unexplored cell.
		// It is always preferable to explore
		g = explore();
		
		if (g != null) {
			System.out.println("Goal: " + g);
		}
		return g;
	}
	
	/**
	 * Uses breadth-first search to find the closest unexplored cell.
	 * @return
	 */
	public Goal explore() {
		State g = null;
		State s = new State(w, inventory, posx, posy), head;
		Queue<State> open = new LinkedList<State>();
		HashSet<State> explored = new HashSet<State>();
		open.add(s);
		explored.add(s);
		while (open.isEmpty() == false) {
			
			head = open.poll();
			
			System.out.println("explore " + head);
			if (w.w[head.y][head.x] == 'x') {
				Goal result = new Goal(head.predecessor.x, head.predecessor.y, ' ', 1000);
				result.setPath(pathFind(head.predecessor));
				return result;
			} else {
				List<State> neighbours = head.getAllNeighbours();
				for (State neighbour : neighbours) {
					System.out.println("Neighbour of " + head + ": " + neighbour);
					if (!explored.contains(neighbour)) {
						neighbour.predecessor = head;
						explored.add(neighbour);
						open.add(neighbour);
					}
				}
			}
		}
		
		return null;
	}
	public List<State> searchAStar(int goalX, int goalY, int currentX, int currentY) {
		PriorityQueue<State> queue = new PriorityQueue<State>();
		Set<State> explored = new HashSet<State>();
		
		// Create the goal state based on params.
		State goal = new State(w, inventory, goalX, goalY);
		
		// Unpathable goal do not bother searching or we will cause an infinite loop.
		if (!w.inBounds(goalX,  goalY) || !canMoveInto(w.w[goalY][goalX])) {
			return null;
		}
		
		// Add the current state.
		State initial = new State(w, inventory, currentX, currentY);
		queue.add(initial);
		
		initial.cost = 0;		
		initial.fcost = initial.cost(goal);
		
		HashSet<State> seen = new HashSet<State>();
		State current = null;
		
		// A star!
		while (!queue.isEmpty()) {
			// Take the top element
			current = queue.poll();
			if (current.equals(goal)) {
				// Save the current state, finish the loop.
				return pathFind(current);
			}
			System.out.println("Exploring " + current);
			for (State s : explored) {
				System.out.println("   Explored: " + s);
			}
			
			// Remove the element from the queue and add it to our explored set.
			explored.add(current);
			
			// Get all possible next moves.
			List<State> neighbours = current.getNeighbours();

			// Iterate through all next possible moves, find new, unexplored moves to explore.
			if (neighbours != null) {
				for (State neighbour : neighbours) {
					int potentialCost = current.cost + neighbour.cost(current);
					if (!seen.contains(neighbour)) { // if (neighbour has no cost) !neighbour.getCost()) {
						seen.add(neighbour);
						neighbour.cost = potentialCost;
					}
					if (explored.contains(neighbour)) {
						if (potentialCost >= neighbour.cost) {
							// ignore the neighbour since we've already explored it from another path
							// and the new path to the neighbour isn't any better.
						}
					} else if (!queue.contains(neighbour) || potentialCost < neighbour.cost) {
						// Map where we came from (to pathfind).
						neighbour.predecessor = current;
						// Update costs.
						neighbour.cost = potentialCost;
						neighbour.fcost = neighbour.cost(goal);
						if (!queue.contains(neighbour)) {
							System.out.println(current + " -> +" + neighbour);
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
	 * Helper function that iteratively finds the path taken by the nodes.
	 * 
	 * @param current - current position to back track
	 * @return - the path taken to reach goal.
	 */
	private List<State> pathFind(State current) {
		LinkedList<State> path = new LinkedList<State>();
		State p = current;
		while (p != null) {
			path.addFirst(p);
			p = p.predecessor;
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
			char atPosition = w.w[y][x];
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
		return (x > 0 && w.w[y][x-1] == 'x') || (x < LOCAL_MAP_SIZE-1 && w.w[y][x+1] == 'x') || 
				(y > 0 && w.w[y-1][x] == 'x') || (y < LOCAL_MAP_SIZE-1 && w.w[y+1][x] == 'x');
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
		
		
		Inventory v = new Inventory();
		assert(v.get('d') == 0);
		assert(v.get('x') == 0);
		v.add('d');
		assert(v.get('d') == 1);
		v.add('d');
		assert(v.get('d') == 2);
		Inventory v2 = new Inventory(v);
		assert(v2.get('d') == 2);
	}
}
