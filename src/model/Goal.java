package model;

import java.util.LinkedList;
import java.util.List;

import logic.Agent;

public class Goal implements Comparable<Goal> {
	
	private int x;
	private int y;
	private char type;
	private List<Position> path;
	private int requiredDynamite; // dynamite required to complete this path
	private int score;
	
	public Goal(int x, int y, char type, int score) {
		this.x = x;
		this.y = y;
		this.type = type;
		this.path = new LinkedList<Position>();
		this.requiredDynamite = 0;
		this.score = score;
	}
	
	/**
	 * Returns whether agent can achieve this goal
	 * @param a
	 * @return
	 */
	public boolean isAchievable(Agent a) {
		switch (type) {
		case 'T':
			if (a.getItems('a') > 0) {
				return true;
			}
		case '*':
			return a.getItems('d') >= requiredDynamite;

		default:
				return true;
		}
		
	}
	
	@Override
	public int compareTo(Goal g) {
		return g.getScore() - score;
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

	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}

	public List<Position> getPath() {
		return path;
	}

	public void setPath(List<Position> path) {
		this.path = path;
	}

	public int getRequiredDynamite() {
		return requiredDynamite;
	}

	public void setRequiredDynamite(int requiredDynamite) {
		this.requiredDynamite = requiredDynamite;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}
	
	/**
	 * Try to find the position that would come after the given position.
	 * @param p
	 * @return next position
	 */
	public Position getPositionAfter(Position p) {
		int index = path.indexOf(p);
		if (index == -1 || index == path.size() - 1) {
			return null;
		} else {
			return path.get(index + 1);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass() == this.getClass()) {
			Goal g = (Goal)o;
			return (g.x == x && g.y == y);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() { 
		return "'" + type + "' @ [" + x + "," + y + "] (" + score + ")";
	}
}
