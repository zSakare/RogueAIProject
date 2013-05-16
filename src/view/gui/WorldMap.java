package view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import logic.Agent;
import model.Position;

/**
 * Displays the worldmap (local map knowlegde) for a given agent
 * @author Randal Grant
 *
 */
public class WorldMap extends JPanel {

	private Agent agent;
	private WorldPiece [][] pieces;
	
	public static final int WORLDMAP_WIDTH = 3200;
	public static final int WORLDMAP_HEIGHT = 3200;
	
	private java.util.List<WorldPiece> pathPieces; 
	
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
	 * We limit the operation to the dimensions given.
	 * !!Max coordinates are inclusive!!
	 * It is the responsibility of the caller to limit the max, and min coordinates
	 * such that they do not try to update over the edge of the map.
	 */
	public void update(int minx, int maxx, int miny, int maxy) {
		int xx, yy;
		WorldPiece piece;
		char chr;
		//System.out.println("Update: " + minx + "," + miny + " to " + maxx + "," + maxy);
		for (yy = miny; yy <= maxy; ++yy) {
			for (xx = minx; xx <= maxx; ++xx) {
				chr = agent.charAt(xx, yy);
				piece = pieces[yy][xx];
				if (piece == null) {
					piece = new WorldPiece(this, (xx % 2) == (yy % 2), xx, yy);
					pieces[yy][xx] = piece;
					this.add(piece);
					piece.setBounds(xx * PIECE_WIDTH, yy * PIECE_HEIGHT, PIECE_WIDTH, PIECE_HEIGHT);
				}
				
				if (xx == agent.getX() && yy == agent.getY()) {
					piece.myType = getCharacterForDirection(agent.getDirection());
				} else {
					piece.myType = chr;
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
	public void onMouseOver(WorldPiece piece) {
		/* clear the existing path tags */
		for (WorldPiece pc : pathPieces) {
			pc.setTagged(null);
		}
		pathPieces.clear();
		/* get A* path */
		List<Position> pathPositions = agent.searchAStar(piece.x, piece.y, agent.getX(), agent.getY());
		if (pathPositions == null) {
			return;
		} else {
			System.out.println("Total Cost: " + pathPositions.size());
		}
		/* tag all the pieces on the path to the goal */
		for (Position p : pathPositions) {
			/* tag the piece */
			WorldPiece pc = pieces[p.getCurrY()][p.getCurrX()];
			if (pc != null) {
				pc.setTagged(Color.GREEN);
				pathPieces.add(pc);
			}
		}
		
	}
	
	public static final char [] arrows = {'>','^','<','v'};
	
	public static char getCharacterForDirection(int dirn) {
		return arrows[dirn];
	}


}
