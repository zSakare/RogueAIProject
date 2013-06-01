package model;

import java.util.LinkedList;
import java.util.List;

/** Refers to a compact world state, used for plan A* branching **/
public class State implements Comparable {

	public World base; // world base - our understanding of the world so far
	public Inventory inventory; // inventory for this state
	
	public int x, y;
	
	public State predecessor;
	
	public int cost, fcost;
	
	private static final int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
	
	public State(World base, Inventory inventory, int x, int y) {
		this.base = base;
		this.inventory = inventory != null ? new Inventory(inventory) : null;
		this.x = x;
		this.y = y;
	}
	
	public int cost(State from) {
		return (int) Math.sqrt(Math.pow(this.x - from.x, 2) + Math.pow(this.y - from.y, 2));
	}
	

	
	public List<State> getNeighbours() {
		int nx, ny;
		List<State> neighbours = new LinkedList<State>();
		for (int [] vector : moveVectors) {
			nx = x + vector[0];
			ny = y + vector[1];
			if (base.inVisibleBounds(nx, ny) && (base.w[ny][nx] == ' ' || base.isInteresting(nx, ny))) { // empty
				neighbours.add(new State(base, inventory, nx, ny));
			}
		}
		return neighbours;
	}
	
	/** same as above but also searches unexplored **/
	public List<State> getAllNeighbours() {
		int nx, ny;
		List<State> neighbours = new LinkedList<State>();
		for (int [] vector : moveVectors) {
			nx = x + vector[0];
			ny = y + vector[1];
			if (base.inBounds(nx, ny) && (base.w[ny][nx] == ' ' || base.w[ny][nx] == 'x')) { // empty or unexplored
				neighbours.add(new State(base, inventory, nx, ny));
			}
		}
		return neighbours;
	}
	@Override
	public int hashCode() {
		return x + World.LOCAL_MAP_SIZE * y;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			State s = (State)o;
			return x == s.x && y == s.y;
		}
		return false;
	}
	
	@Override
	public int compareTo(Object o) {
		if (o != null) {
			State position = (State) o;
			return Integer.compare(this.fcost, position.fcost);
			//return this.getCost().compareTo(position.getCost());
		}
		
		return 0;
	}
	
	@Override
	public String toString() { 
		return "[" + x + "," + y + "]";
	}
}
