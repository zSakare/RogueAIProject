package view;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;

import controller.IAgentController;

import view.gui.*;
import model.*;
import logic.*;

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
		int minx, maxx, miny, maxy;
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
	
	public void onAIStep() {
		// panel told us to take a step that the AI would...
		// this belongs in a controller
		Position poi = agent.findPOI();
		// Get path to POI
		List<Position> pathToPOI = agent.searchAStar(poi.getCurrX(), poi.getCurrY(), agent.getX(), agent.getY());
		
		// get the first step for the AI on this path
		if (pathToPOI.size() > 1) {
			Position nextPos = pathToPOI.get(1);
			int [][] moveVectors = {{1,0},{0,-1},{-1,0},{0,1}}; // {{x,y} E N W S}
			int requiredDirection = 0;
			while (nextPos.getCurrX() != agent.getX() + moveVectors[requiredDirection][0] || nextPos.getCurrY() != agent.getY() + moveVectors[requiredDirection][1]) {
				requiredDirection++;
			}
			/* TODO: fix this shit up - a little hacky */
			if (agent.getDirection() == requiredDirection) {
				notifyAction('F');
			} else if (agent.getDirection() < requiredDirection && (requiredDirection - agent.getDirection()) <= 2) {
				// if i must turn left, and it will take me at most than 2 turns to get there, turn left
				notifyAction('L');
			} else if ((agent.getDirection() - requiredDirection) <= 2){
				notifyAction('R');
			} else {
				notifyAction('L');
			}
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
