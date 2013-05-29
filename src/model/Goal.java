package model;

import java.util.LinkedList;
import java.util.List;

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
	
	@Override
	public int compareTo(Goal g) {
		return score - g.getScore();
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
	
}
