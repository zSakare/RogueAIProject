package model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import logic.Agent;

/** Refers to a compact world state, used for plan A* branching **/
public class State implements Comparable {

	
	
	public World base; // world base - our understanding of the world so far
	public Inventory inventory; // participates in hashcode,equals inventory for this state
	
	public int x, y; /** participates in hashcode/equals */
	
	public State predecessor;
	public int fromDirection; // direction we came from
	
	public int cost, fcost;
	public int move; // move counter
	
	public HashSet<Position> destroyed; // participates in hashcode/equalsdestroyed cells in this state
	
	private static final int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
	
	public State(World base, Inventory inventory, int x, int y) {
		this.base = base;
		this.inventory = inventory;
		this.x = x;
		this.y = y;
		this.destroyed = new HashSet<Position>();
	}
	
	public int cost(State from) {
		return (int) Math.sqrt(Math.pow(this.x - from.x, 2) + Math.pow(this.y - from.y, 2));
	}
	

	/**
	 * Gets all the neighbours for this state.
	 * Parameter determines whether we want to use consumables (namely dynamite) to get somewhere.
	 * @param useItems whether we are allowed to use dynamite while searching
	 * @param direction initial direction (to make bfs faster basically by queuing straight moves first)
	 * @return
	 */
	public List<State> getNeighbours(boolean useItems) {
		int nx, ny;
		boolean isInteresting, isBreakable;
		char c;
		Inventory newInventory;
		State next;
		List<State> neighbours = new LinkedList<State>();
		
		if (move == Agent.MAX_MOVES) { // no more branching...
			System.err.println("BRANCH NO MORE!!!!");
			return neighbours;
		}
		
		//System.out.println("getNeighbours: " + this);
		for (int [] vector : moveVectors) {
		//for (int i = fromDirection, ii = 0; ii < moveVectors.length; i = (i + 1) % moveVectors.length, ++ii) {
			//nx = x + moveVectors[i][0];
			//ny = y + moveVectors[i][1];
			nx = x + vector[0];
			ny = y + vector[1];
			c = cell(nx, ny);
			isInteresting = base.isInteresting(c);
			isBreakable = breakable(c);
			if (base.inVisibleBounds(nx, ny) && (c == ' ' || isInteresting || isBreakable)) { // empty or walkable cell
				next = new State(base, inventory, nx, ny);
				next.move = move+1;
				//next.fromDirection = i;
				next.destroyed = new HashSet<Position>(destroyed);
				if (isInteresting) { // we walked onto an item
					newInventory = new Inventory(inventory); // create a copy
					newInventory.add(base.w[ny][nx]); // add the item to inventory
					next.breakCell(nx, ny); // mark the item as broken, so it can't be picked up again
				} else if (isBreakable) { // we broke an item
					// determine what has to be consumed to break this
					newInventory = inventory;
					switch (c) {
					case 'T': // tree
						if (inventory.get('a') == 0) { // tree and no axe
							if (!useItems) continue; // don't wanna use items, don't try dynamite
							newInventory = new Inventory(inventory); // create a copy
							newInventory.use('d'); // use a dynamite
							// also mark the given cell as broken
							next.breakCell(nx, ny);
						}
						break;
					case '-': // key
						if (inventory.get('k') == 0) { // wall and no key
							if (!useItems) continue; // don't wanna use items, don't try dynamite
							newInventory = new Inventory(inventory); // create a copy
							newInventory.use('d'); // use a dynamite
							// also mark the given cell as broken
							next.breakCell(nx, ny);
						}
						break;
					case '*': // wall
						if (!useItems) continue; // don't wanna use items, don't try dynamite
						newInventory = new Inventory(inventory); // create a copy
						newInventory.use('d'); // use a dynamite
						// also mark the given cell as broken
						next.breakCell(nx, ny);
						break;
					}
					
				} else {
					newInventory = inventory; // just use the existing inventory
				}
				
				next.inventory = newInventory;
				neighbours.add(next);
			}
		}
		return neighbours;
	}
	
	/**
	 * return the character that should appear at a set of coordinates
	 * useful to track the state of destroyed walls etc
	 * @param x
	 * @param y
	 * @return
	 */
	public char cell(int x, int y) {
		if (destroyed.contains(new Position(x, y))) {
			return ' '; // empty
		}
		return base.w[y][x];
	}
	
	/**
	 * mark the given cell as broken
	 * @param x
	 * @param y
	 */
	public void breakCell(int x, int y) {
		destroyed.add(new Position(x, y));
	}
	
	/**
	 * Returns whether we can break the given cell type given our inventory
	 * @param c
	 * @return
	 */
	public boolean breakable(char c) {
		
		switch (c) {
		case 'T': // tree needs an axe or dynamite
			return inventory.get('a') > 0 || inventory.get('d') > 0;
		case '*': // wall needs dynamite
			return inventory.get('d') > 0;
		case '-': // door needs key
			return inventory.get('k') > 0 || inventory.get('d') > 0;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return x
				+ y * World.LOCAL_MAP_SIZE
				+ inventory.hashCode() * (World.LOCAL_MAP_SIZE * World.LOCAL_MAP_SIZE)
				+ destroyed.hashCode() * (World.LOCAL_MAP_SIZE * World.LOCAL_MAP_SIZE * World.LOCAL_MAP_SIZE);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			State s = (State)o;
			// note: if inventory is null, then it implies inventory is not important for this comparison
			return x == s.x && y == s.y && (inventory == null || s.inventory == null || inventory.equals(s.inventory)) && destroyed.equals(s.destroyed);
		}
		return false;
	}
	
	@Override
	public int compareTo(Object o) {
		if (o != null) {
			State position = (State) o;
			return this.fcost - position.fcost;
			//return this.getCost().compareTo(position.getCost());
		}
		
		return 0;
	}
	
	public String getMap() {
		String res = "";
		// draw top border
		for (int xx = base.minx; xx < base.maxx + 2; ++xx) {
			res += '%';
		}
		res += '\n';
		for (int yy = base.miny; yy < base.maxy; ++yy) {
			res += '%';
			for (int xx = base.minx; xx < base.maxx; ++xx) {
				if (xx == x && yy == y) {
					res += '#';
				} else if (xx == World.START_X && yy == World.START_Y) {
					res += 'S';
				} else {
					res += cell(xx,yy);
				}
			}
			res += "%\n";
		}
		// draw bottom border
		for (int xx = base.minx; xx < base.maxx + 2; ++xx) {
			res += '%';
		}
		return res;
	}
	@Override
	public String toString() { 
		return "[" + x + "," + y + "] " + (inventory != null ? inventory.toString() : "null") + " [" + destroyed.size() + "]";
	}
}
