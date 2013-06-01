package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import logic.Agent;
import model.Goal;
import model.State;
import view.IAgentView;

public class AgentConsoleController implements IAgentController {

	private IAgentView view;
	private Agent agent;
	
	private InputStream in = null;
	private OutputStream out = null;
	private Socket socket = null;
	
	public AgentConsoleController(Agent agent, IAgentView view) {
		this.agent = agent;
		this.view = view;
		view.setController(this);
	}
	
	@Override
	public void setView(IAgentView view) {
		this.view = view;
		view.setController(this);
	}

	@Override
	public void setAgent(Agent agent) {
		this.agent = agent;
	}
	
	public void initialiseNetwork(int port) {
		try { // open socket to Game Engine
			socket = new Socket("localhost", port);
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (IOException e) {
			System.out.println("Could not bind to port: " + port + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void shutdown() {
		/*
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Error closing socket");
		}*/
	}
	/**
	 * Receive a viewport from the engine.
	 * Let the agent process it.
	 * @return viewport, or null if error
	 */
	public char[][] waitForViewport() {
		int i, j, ch;
		char view[][] = new char[5][5];
		
		try { // scan 5-by-5 window around current location
			for (i = 0; i < 5; i++) {
				for (j = 0; j < 5; j++) {
					if (!((i == 2) && (j == 2))) {
						ch = in.read();
						if (ch == -1) {
							System.exit(-1);
						}
						view[i][j] = (char) ch;
					}
				}
			}

			agent.parse_view(view);
			
			agent.updateViews();
			
		} catch (IOException e) {
			System.out.println("Lost connection, could not ETC ETC");
			view = null;
		}
		
		return view;
	}
	
	@Override
	public void onAction(char action) {
		agent.handle_action(action);
		
		try {
			out.write(action);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void step() {

		// panel told us to take a step that the AI would...
		// this belongs in a controller
		Goal goal = agent.getCurrentGoal();
		if (goal == null) { return; };
		// Get path to POI
		List<State> pathToPOI = goal.getPath();
		
		// get the first step for the AI on this path
		if (pathToPOI.size() > 1) {
			//State currentPos = new State(agent.w, agent., agent.getX(), agent.getY());
			//State nextPos = goal.getPositionAfter(currentPos);
			State nextPos = goal.getPath().get(goal.pos+1);
			if (nextPos == null) {
				System.err.println("Seems there is no next position...");
			}
			int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
			/*System.out.println("Current: " + currentPos + ", next: " + nextPos);
			for (Position p : pathToPOI) {
				System.out.println(p);
			}*/
			int requiredDirection = 0;
			int direction = agent.getDirection();
			char inFront = agent.charAt(agent.getX() + moveVectors[direction][0], agent.getY() + moveVectors[direction][1]);
			while (nextPos.x != agent.getX() + moveVectors[requiredDirection][0] || nextPos.y != agent.getY() + moveVectors[requiredDirection][1]) {
				requiredDirection++;
			}
			/* TODO: fix this shit up - a little hacky */
			if (agent.getDirection() == requiredDirection) {
				char action = 'x'; // x by default (fatal)
				switch (inFront) {
					case ' ':
					case 'a': // axe
					case 'k': // key
					case 'd': // dynamite OK
					case 'g': // gold
					action = 'F';
					++goal.pos;
					break;
					case 'T': // tree
						if (agent.getItems('a') > 0) {
							action = 'C'; // chop down ya
						} else if (agent.getItems('d') > 0) {
							action = 'B'; // blast it
						} else {
							action = 'x'; // error
						}
						break;
					case '*': // wall
						if (agent.getItems('d') > 0) {
							action = 'B'; // blast it
						} else {
							action = 'x'; // error
						}
						break;
					case '-': // door
						if (agent.getItems('k') > 0) {
							action = 'O'; // open
						} else if (agent.getItems('d') > 0) {
							action = 'B'; // blast it
						} else {
							action = 'x'; // error
						}
						break;
					default:
						action = 'x';
				}
				if (action == 'x') {
					System.err.println("AI tried to move into something unexpected... (" + inFront + ")");
					action = 'L';
				}
				onAction(action);
			} else if (agent.getDirection() < requiredDirection && (requiredDirection - agent.getDirection()) <= 2) {
				// if i must turn left, and it will take me at most than 2 turns to get there, turn left
				onAction('L');
			} else if ((agent.getDirection() - requiredDirection) <= 2){
				onAction('R');
			} else {
				onAction('L');
			}
		} else {
			onAction('L');
		}
		
	}

}
