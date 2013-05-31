package model;


/**
 * Class to represent a single point on the map.
 */
public class Position implements Comparable<Position> {
	private int x;
	private int y;
	private int cost;
	private int fcost;
	private Position parent;
	public char piece;
	
	public Position(int x, int y) {
		this.setX(x);
		this.setY(y);
		setCost(0);
		setFcost(0);
		this.piece = ' ';
	}

	
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Position getParent() {
		return parent;
	}
	
	public void setParent(Position parent) {
		this.parent = parent;
	}
	
	public int[] getCoords() {
		int[] coords = new int[2];
		coords[0] = x;
		coords[1] = y;
		
		return coords;
	}

	
	/**
	 * Gets how interesting the position is.
	 * 
	 * @return - how interesting the object is. Based on distance and reward.
	 */
	/*public Integer getInterest() {
		return (int) getAbsoluteDistance() - getReward();
	}*/
	
	/**
	 * Comparison function for priority queue.
	 * 
	 * @param o - position object.
	 * @return - return the comparison.
	 */
	public int compareTo(Position o) {
		if (o != null) {
			Position position = (Position) o;
			return this.getCost().compareTo(position.getCost());
		}
		
		return 0;
	}
	
	@Override
	public String toString() { 
		return "[" + x + "," + y + "]";
	}
	/**
	 * Object equals function.
	 */
	public boolean equals(Object o) {
		boolean equal = false;
		
		if (o != null) {
			Position position = (Position) o;
			if (position.x == this.x 
					&& position.y == this.y) {
				equal = true;
			}
		}
		
		return equal;
	}

	public Integer absoluteDistanceFrom(Position positionFrom) {
		return (int) Math.sqrt(Math.pow(this.x - positionFrom.x, 2) + Math.pow(this.y - positionFrom.y, 2));
	}
	
	/**
	 * Hash for hashset/hashmap.
	 */
	public int hashCode() {
		return x + 160 * y;
	}

	public Integer getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public int getFcost() {
		return fcost;
	}

	public void setFcost(int fcost) {
		this.fcost = fcost;
	}
}
