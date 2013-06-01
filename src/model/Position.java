package model;


/**
 * Class to represent a single point on the map.
 */
public class Position {
	private int x;
	private int y;
	
	public Position(int x, int y) {
		this.setX(x);
		this.setY(y);
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

	public int[] getCoords() {
		int[] coords = new int[2];
		coords[0] = x;
		coords[1] = y;
		
		return coords;
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

}
