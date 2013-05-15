/**
 * Class to represent a single point on the map.
 */
public class Position implements Comparable<Position> {
	private int x;
	private int y;
	private int currX;
	private int currY;
	private int reward;
	
	public Position(int x, int y, int currX, int currY, int reward) {
		this.setX(x);
		this.setY(y);
		this.setCurrX(currX);
		this.setCurrY(currY);
		this.setReward(reward);
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

	public int getCurrX() {
		return currX;
	}

	public void setCurrX(int currX) {
		this.currX = currX;
	}

	public int getCurrY() {
		return currY;
	}

	public void setCurrY(int currY) {
		this.currY = currY;
	}

	public int getReward() {
		return reward;
	}

	public void setReward(int reward) {
		this.reward = reward;
	}
	
	/**
	 * Get the absolute value of the distance of the point from our current position.
	 * 
	 * @return - the absolute distance.
	 */
	public Integer getAbsoluteDistance() {
		return (int) Math.sqrt((currX - x)^2 + (currY - y)^2);
	}
	
	public Integer getCost() {
		return (int) getAbsoluteDistance() - getReward();
	}
	
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
}
