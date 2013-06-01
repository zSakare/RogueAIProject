package view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import logic.Agent;
import model.Goal;
import model.Position;
import model.State;
import view.gui.IMovePanelSubscriber;
import view.gui.MovePanel;
import view.gui.TurnIndicator;
import view.gui.WorldMap;
import controller.IAgentController;

public class AgentGUIView implements IAgentView, IMovePanelSubscriber, KeyListener {

	Agent agent;
	
	JPanel panel;

	JButton btnExit;
	
	TurnIndicator tiTurns; 
	MovePanel mpMove;
	
	WorldMap wmWorld;
	JScrollPane scrWorld; // world scroller
	
	private IAgentController controller;
	
	public AgentGUIView(Agent agent) {
		this.agent = agent;
		controller = null;
		
		panel = new JPanel(new GridBagLayout());
		panel.setPreferredSize(new Dimension(800, 600));
		
		GridBagConstraints ctrs = new GridBagConstraints();
		
		ctrs.ipadx = 8;
		ctrs.ipady = 8;
		ctrs.fill = GridBagConstraints.BOTH; // fill the whole cell
		ctrs.weightx = 0.25; // 1/4 of width
		
		
		
		tiTurns = new TurnIndicator(agent);
		ctrs.gridx = 1;
		ctrs.gridy = 0;
		ctrs.weighty = 0.15; // 15% of height
		panel.add(tiTurns, ctrs);

		mpMove = new MovePanel(agent);
		ctrs.gridx = 1;
		ctrs.gridy = 1;
		ctrs.weighty = 0.75; // 75% of height
		panel.add(mpMove, ctrs);
		mpMove.addSubscriber(this); // subscribe to move panel updates

		btnExit = new JButton("Exit");
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				System.exit(0);
			}
		});
		ctrs.gridx = 1;
		ctrs.gridy = 3;
		ctrs.weighty = 0.1; // 10% of height
		panel.add(btnExit, ctrs);
		
		
		wmWorld = new WorldMap(agent);
		
		// put the world map in a scroller that always shows the scrollbars
		scrWorld = new JScrollPane(wmWorld, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		ctrs.gridx = 0;
		ctrs.gridy = 0;
		ctrs.gridheight = 4;
		ctrs.weightx = 0.75;
		panel.add(scrWorld, ctrs);
		
		panel.repaint();
		
	}
	
	private static class WindowCloseManager extends WindowAdapter {
		public void windowClosing(WindowEvent evt) {
			System.exit(0);
		}
	}
	
	@Override
	public void setAgent(Agent agent) {
		this.agent = agent;
		
		agent.addView(this);
	}
	
	public void onUpdate() {
		onUpdate(agent.getX(), agent.getY());
	}

	@Override
	public void onUpdate(int posx, int posy) {
		int cx, cy;
		// update everything
		tiTurns.update();
		
		// update the world map with just the area explored so far
		wmWorld.update(agent.getMinX(), agent.getMaxX(), agent.getMinY(), agent.getMaxY());

		// scroll the character into view
		cx = wmWorld.getCharacterX();
		cy = wmWorld.getCharacterY();
		wmWorld.scrollRectToVisible(new Rectangle(cx - 100, cy - 100, 200, 200));
	}
	
	@Override
	public void setController(IAgentController controller) {
		this.controller = controller;		
	}
	
	@Override
	public void notifyAction(char action) {
		controller.onAction(action);
	}

	@Override
	public void run(int port) {
		char [][] view;
		char action;
		JFrame frame = new JFrame("Agent 007");
		frame.getContentPane().add(panel);
		frame.addWindowListener(new WindowCloseManager());
		frame.addKeyListener(this);
		frame.pack();
		frame.setVisible(true);
		
		agent.addView(this);
		
		/* Perform one update. */
		view = controller.waitForViewport();
		if (view == null) {
			System.out.println("Did not receive viewport from engine! Exiting...");
			System.exit(1);
		}
		
		onUpdate(agent.getX(), agent.getY());		
	}

	@Override
	public void onLeft() {
		notifyAction('L');
		// get new state
		controller.waitForViewport();
		onUpdate();
	}

	@Override
	public void onRight() {
		notifyAction('R');
		controller.waitForViewport();
		onUpdate();
	}

	@Override
	public void onForward() {
		notifyAction('F');
		controller.waitForViewport();
		onUpdate();
	}
	
	public void onAIStep(int moves) {
		Timer t = new Timer();
	      t.scheduleAtFixedRate(new StepTimer(this, moves), 10, 25);   
	}
	
	private class StepTimer extends TimerTask {
		int moves;
		AgentGUIView agent;
		int count = 0;  
		public StepTimer(AgentGUIView agent, int moves) {
			this.moves = moves;
			this.agent = agent;
		}
		
		public void run() {
			agent.step();
			count ++;         
			
			if(count == moves)   
				this.cancel();
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
				notifyAction(action);
			} else if (agent.getDirection() < requiredDirection && (requiredDirection - agent.getDirection()) <= 2) {
				// if i must turn left, and it will take me at most than 2 turns to get there, turn left
				notifyAction('L');
			} else if ((agent.getDirection() - requiredDirection) <= 2){
				notifyAction('R');
			} else {
				notifyAction('L');
			}
		} else {
			notifyAction('L');
		}
		
		controller.waitForViewport();
		onUpdate();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_UP) {
			onForward();
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			onLeft();
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			onRight();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

}
