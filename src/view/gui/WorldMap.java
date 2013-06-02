package view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import logic.Agent;
import model.Goal;
import model.State;

/**
 * Displays the worldmap (local map knowledge) for a given agent
 * @author Randal Grant
 *
 */
public class WorldMap extends JPanel {

	private Agent agent;
	private WorldPiece [][] pieces;
	
	public static final int WORLDMAP_WIDTH = 6400;
	public static final int WORLDMAP_HEIGHT = 6400;
	
	private java.util.List<WorldPiece> pathPieces; 
	private Goal lastPOI;
	
	// ideally the pieces are square... make your decision
	private static final int PIECE_WIDTH = WORLDMAP_WIDTH / Agent.LOCAL_MAP_SIZE; // width of pieces
	private static final int PIECE_HEIGHT = WORLDMAP_HEIGHT / Agent.LOCAL_MAP_SIZE; // height of pieces
	
	
	public WorldMap(Agent agent) {
		super(null);
		this.setPreferredSize(new Dimension(WORLDMAP_WIDTH, WORLDMAP_HEIGHT));
		this.agent = agent;
		this.pieces = new WorldPiece[Agent.LOCAL_MAP_SIZE][];
		for (int i = 0; i < Agent.LOCAL_MAP_SIZE; ++i) {
			this.pieces[i] = new WorldPiece[Agent.LOCAL_MAP_SIZE];
		}
		
		// black colour to represent unexplored
		this.setBackground(Color.BLACK);
		
		lastPOI = null;
		
		// initialise A* debugging list
		pathPieces = new LinkedList<WorldPiece>();
		//update(0, Agent.LOCAL_MAP_SIZE-1, 0, Agent.LOCAL_MAP_SIZE-1);
	}
	
	
	public int getCharacterX() {
		return PIECE_WIDTH * agent.getX();
	}
	
	public int getCharacterY() {
		return PIECE_HEIGHT * agent.getY();
	}
	
	/**
	 * update the local map pieces with knowledge from the agent (expensive operation!)
	 * Limit updates to coordinates given
	 * !!Max coordinates are inclusive!!
	 * It is the responsibility of the caller to limit the max, and min coordinates
	 * such that they do not try to update over the edge of the map.
	 */
	public void update(int minx, int maxx, int miny, int maxy) {
		int xx, yy;
		WorldPiece piece;
		char chr;
		Goal agentPOI;
		
		agentPOI = agent.getCurrentGoal();
		if (agentPOI != null) {
			if (lastPOI != null && pieces[lastPOI.getY()][lastPOI.getX()] != null) {
				pieces[lastPOI.getY()][lastPOI.getX()].setTagged(null);
			}
			if (agentPOI != null && pieces[agentPOI.getY()][agentPOI.getX()] != null) {
				pieces[agentPOI.getY()][agentPOI.getX()].setTagged(Color.ORANGE);
				lastPOI = agentPOI;
			}
			
			for (WorldPiece pc : pathPieces) {
				pc.setTagged(null);
			}
			pathPieces.clear();
			
			/* get A* path */
			List<State> pathPositions = agentPOI.getPath();
			if (pathPositions == null) {
				return;
			}
			/* tag all the pieces on the path to the goal */
			for (State p : pathPositions) {
				// tag the piece 
				WorldPiece pc = pieces[p.y][p.x];
				if (pc != null) {
					pc.setTagged(Color.BLUE);
					pathPieces.add(pc);
				}
			}
		}
		

		

		//System.out.println("Update: " + minx + "," + miny + " to " + maxx + "," + maxy);
		for (yy = miny; yy <= maxy; ++yy) {
			for (xx = minx; xx <= maxx; ++xx) {
				chr = agent.charAt(xx, yy);
				if (chr == 'x') { // unexplored
					continue;
				}
				piece = pieces[yy][xx];
				if (piece == null) {
					piece = new WorldPiece(this, (xx % 2) == (yy % 2), xx, yy);
					pieces[yy][xx] = piece;
					this.add(piece);
					piece.setBounds(xx * PIECE_WIDTH, yy * PIECE_HEIGHT, PIECE_WIDTH, PIECE_HEIGHT);
				}
				piece.score = agent.getScore(xx,  yy);
				if (xx == agent.getX() && yy == agent.getY()) {
					piece.myType = getCharacterForDirection(agent.getDirection());
				} else if (xx == agent.getInitX() && yy == agent.getInitY()){
					piece.myType = 'S';
				} else {
					piece.myType = chr;
				}
				if (agent.hasNeighboursUnexplored(xx, yy)) {
					piece.u = 1;
				} else {
					piece.u = 0;
				}
				//System.out.print(piece.myType);
				piece.update();
			}
			//System.out.println();
		}
		// Repaint the whole darn thing
		repaint();
	}
	
	/**
	 * Called when a piece is moused over
	 * @param piece
	 */
	@SuppressWarnings("unused")
	public void onMouseOver(WorldPiece piece) {
		// DISABLED

		if (false) {
			/* clear the existing path tags */
			for (WorldPiece pc : pathPieces) {
				pc.setTagged(null);
			}
			pathPieces.clear();
			/* get A* path */
			List<State> pathPositions = agent.searchAStar(piece.x, piece.y, agent.getX(), agent.getY());
			if (pathPositions == null) {
				return;
			}
			/* tag all the pieces on the path to the goal */
			for (State p : pathPositions) {
				// tag the piece 
				WorldPiece pc = pieces[p.y][p.x];
				if (pc != null) {
					pc.setTagged(Color.GREEN);
					pathPieces.add(pc);
				}
			}
		}
	}
	
	public static final char [] arrows = {'>','^','<','v'};
	
	public static char getCharacterForDirection(int dirn) {
		return arrows[dirn];
	}


}
