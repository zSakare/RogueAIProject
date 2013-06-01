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
import model.Position;
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
	public Inventory inventory = new Inventory();
	
	final static int EAST = 0;
	final static int NORTH = 1;
	final static int WEST = 2;
	final static int SOUTH = 3;

	// Facing direction, initially east
	private int direction = EAST;

	
	private Goal currentGoal; // current goal
	private PriorityQueue<Goal> goals; // potential goals
	private PriorityQueue<Goal> pathableGoals; // goals that can be traversed
	
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
		pathableGoals = new PriorityQueue<Goal>();

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
				handleMoveInto(w.w[posy][posx]);
			}
		} else if ((action == 'C') || (action == 'c')) { // chop down NOTE: if chops, will always think it's clear in front
			int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
			w.w[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]] = ' ';
		} else if ((action == 'B') || (action == 'b')) { // blast NOTE: if blast, will always think it's clear in front
			int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
			w.w[posy+moveVectors[direction][1]][posx+moveVectors[direction][0]] = ' ';
			inventory.use('d'); // use dynamite
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
		case 'k':
			inventory.add('k');
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
		
		// update new goals if we can find a more interesting one based on the new information.
		for (int y = posy - VIEW_HALF_SIZE; y <= posy + VIEW_HALF_SIZE; ++y) {
			for (int x = posx - VIEW_HALF_SIZE; x <= posx + VIEW_HALF_SIZE; ++x) {
				if (w.isInteresting(w.w[y][x])) {
					Goal goalToAdd = createNewGoal(x, y);
					if (goals.contains(goalToAdd)) {
						goals.remove(goalToAdd);
					}
					goals.add(goalToAdd);
				}
			}
		}
		
		// only perform A* pulse every 10 moves
		/*if (turnNumber % 10 == 0 && !goals.isEmpty()) {
			processGoals();
		}*/
		
		/*if (!pathableGoals.isEmpty()) {
			currentGoal = pathableGoals.poll();
		}*/
		
		// If we have gold, find the path to the start.
		if (inventory.get('g') > 0) {
			Goal immediateGoal = createNewGoal(START_X, START_Y);
			immediateGoal.setPath(searchAStar(START_X, START_Y, posx, posy));
			currentGoal = immediateGoal;
		}
		
		// if we reached the goal, or no goal, find a new goal
		if (currentGoal == null || (posx == currentGoal.x && posy == currentGoal.y)) {
			currentGoal = findGoal();
		}
		
		this.turnNumber ++;
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

	/** look at all the unpathable goals, and see if we can path to them.
	 * if so, put them in the pathable goal list
	 */
	private void processGoals() {
		List<Goal> removedGoals = new LinkedList<Goal>();
		for (Goal goal : goals) {
			List<State> path = searchAStar(goal.x, goal.y, posx, posy);
			if (path != null) {
				System.out.println("Found path to: " + goal);
				goal.setPath(path);
				pathableGoals.add(goal);
				removedGoals.add(goal);
			}
		}
		for (Goal g : removedGoals) {
			goals.remove(g);
		}
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
			System.out.println("Explore Goal: " + g);
		} else {
			System.out.println("Out of exploration. Trying a goal...");
			// nowhere to explore. Start planning.
			processGoals();
			if (!pathableGoals.isEmpty()) {
				g = pathableGoals.poll();
			}
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
			
			//System.out.println("explore " + head);
			//if (w.w[head.y][head.x] == 'x') {
			if (hasNeighboursUnexplored(head.x, head.y) || w.isInteresting(w.w[head.y][head.x])) {
				//System.out.println(" -> decided " + head);
				Goal result = new Goal(head.x, head.y, ' ', 20);
				result.setPath(pathFind(head));
				return result;
			} else {
				List<State> neighbours = head.getNeighbours(false); // don't wanna use items
				for (State neighbour : neighbours) {
					//System.out.println("Neighbour of " + head + ": " + neighbour);
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
		State goal = new State(w, null, goalX, goalY);
		
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
			//if (current.equals(goal)) {
			if (current.x == goal.x && current.y == goal.y) {
				// Save the current state, finish the loop.
				return pathFind(current);
			}
			System.out.println("Exploring " + current + " towards " + goal);
			//for (State s : explored) {
				//System.out.println("   Explored: " + s);
			//}
			
			// Remove the element from the queue and add it to our explored set.
			explored.add(current);
			
			// Get all possible next moves.
			List<State> neighbours = current.getNeighbours(true); // wanna use items

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
							//System.out.println(current + " -> +" + neighbour);
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
				// depending on whether this cell is next to unexplored or not
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
	public boolean hasNeighboursUnexplored(int x, int y) {
		// check left, right, up then down (with array bound checking)
		if ((x > 0 && w.w[y][x-1] == 'x') || (x < LOCAL_MAP_SIZE-1 && w.w[y][x+1] == 'x') || 
				(y > 0 && w.w[y-1][x] == 'x') || (y < LOCAL_MAP_SIZE-1 && w.w[y+1][x] == 'x')) {
			return true;
		}
		// slower check, within 2 cells of unexplored
		for (int dx = -VIEW_HALF_SIZE; dx <= VIEW_HALF_SIZE; ++dx) {
			if (w.w[y-VIEW_HALF_SIZE][x+dx] == 'x' || w.w[y+VIEW_HALF_SIZE][x+dx] == 'x') { // top and bottom
				return true;
			}
		}
		for (int dy = -VIEW_HALF_SIZE + 1; dy <= VIEW_HALF_SIZE - 1; ++dy) {
			if (w.w[y+dy][x-VIEW_HALF_SIZE] == 'x' || w.w[y+dy][x+VIEW_HALF_SIZE] == 'x') { // left and right
				return true;
			}
		}
		
		
		return false;
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
	 * Generates a new unpathed goal based on coordinates.
	 * @param x - x coordinate of interesting point.
	 * @param y - y coordinate of interesting point.
	 * @return unpathed goal.
	 */
	private Goal createNewGoal(int x, int y) {
		Goal unpathedGoal = new Goal(x, y, w.w[y][x], getScore(x, y));
		
		return unpathedGoal;
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
	
	public static String printMatrix(char [][] matrix) {
		String res = "";
		for (int y = 0; y < matrix.length; ++y) {
			for (int x = 0; x < matrix[0].length; ++x) {
				res += matrix[y][x] + " ";
			}
			res += "\n";
		}
		return res;
	}
	
	public static boolean matrixEquals(char [][] m1, char [][] m2) {
		for (int y = 0; y < m1.length; ++y) {
			for (int x = 0; x < m1[0].length; ++x) {
				if (m1[y][x] != m2[y][x]) {
					return false;
				}
			}
		}
		return true;
	}
	public static void testAgent() {
		// test rotate view
		{
			Agent agent = new Agent();
			char [][] map = {{'c', 'f', 'i'}, {'b', 'e', 'h'}, {'a', 'd', 'g'}};
			char [][] res = agent.rotate_view(map, EAST);
			char [][] expected = {{'a','b','c'},{'d','e','f'},{'g','h','i'}};
			System.out.println("map:\n" + printMatrix(map));
			System.out.println("res:\n" + printMatrix(res));
			System.out.println("expected:\n" + printMatrix(expected));
			
			assert(matrixEquals(res, expected));
		}
		{
			Agent agent = new Agent();
			char [][] map = {{'g', 'd', 'a'}, {'h', 'e', 'b'}, {'i', 'f', 'c'}};
			char [][] res = agent.rotate_view(map, WEST);
			char [][] expected = {{'a','b','c'},{'d','e','f'},{'g','h','i'}};
			
			assert(matrixEquals(res, expected));
		}
		{
			Agent agent = new Agent();
			char [][] map = {{'i', 'h', 'g'}, {'f', 'e', 'd'}, {'c', 'b', 'a'}};
			char [][] res = agent.rotate_view(map, SOUTH);
			char [][] expected = {{'a','b','c'},{'d','e','f'},{'g','h','i'}};
			
			assert(matrixEquals(res, expected));
		}
		{
			Agent agent = new Agent();
			char [][] map = {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}};
			char [][] res = agent.rotate_view(map, NORTH);
			char [][] expected = {{'a','b','c'},{'d','e','f'},{'g','h','i'}};
			
			assert(matrixEquals(res, expected));
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
		
		// hashset hashcode equals testing
		Position p1, p2, p3;
		p1 = new Position(5, 5);
		p2 = new Position(5, 5);
		p3 = new Position(5, 6);
		assert(p1.hashCode() == p2.hashCode());
		assert(p1.equals(p2));
		assert(p2.equals(p1));
		assert(p2.hashCode() != p3.hashCode());
		assert(!p2.equals(p3));
		assert(!p3.equals(p2));
		
		
		
	}
}
