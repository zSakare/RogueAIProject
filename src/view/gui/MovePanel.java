package view.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import logic.Agent;

public class MovePanel extends JPanel {

	private Agent agent;
	
	private JButton btnLeft;
	private JButton btnRight;
	private JButton btnForward;
	
	private JButton btnAI; // ai single step
	private JButton btnAI5; // ai 5 step
	private JButton btnAI1000; // ai 1000 step
	
	
	
	private List<IMovePanelSubscriber> subscribers;
	
	public MovePanel(Agent agent) {
		super(new GridLayout(3,3));
		subscribers = new LinkedList<IMovePanelSubscriber>();
		
		this.setBackground(Color.DARK_GRAY);
		
		btnForward = new JButton("^ Forward");
		btnForward.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				notifyForward();
			}
		});
		
		btnLeft = new JButton("< Turn Left");
		btnLeft.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				notifyLeft();
			}
		});
		
		btnRight = new JButton("Turn Right >");
		btnRight.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				notifyRight();
			}
		});
		
		btnAI = new JButton("AI Step 1");
		btnAI.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				notifyAI(1);
			}
		});
		
		btnAI5 = new JButton("AI Step 5");
		btnAI5.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				notifyAI(5);
			}
		});
		
		btnAI1000 = new JButton("AI Step 1000");
		btnAI1000.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				notifyAI(1000);
			}
		});
		
		/* 0  1  2
		 * 3  4  5
		 * 6  7  8
		 */
		
		this.add(new JLabel("")); // 0
		this.add(btnForward); // 1
		this.add(new JLabel("")); // 2
		this.add(btnLeft); // 3
		this.add(new JLabel("")); //4
		this.add(btnRight); // 5
		this.add(btnAI); // 6
		this.add(btnAI5); // 7
		this.add(btnAI1000); // 8
		
		
	}
	
	private void notifyLeft() {
		for (IMovePanelSubscriber sub : subscribers) {
			sub.onLeft();
		}
	}
	
	private void notifyRight() {
		for (IMovePanelSubscriber sub : subscribers) {
			sub.onRight();
		}
	}
	
	private void notifyForward() {
		for (IMovePanelSubscriber sub : subscribers) {
			sub.onForward();
		}
	}
	
	private void notifyAI(int moves) {
		for (IMovePanelSubscriber sub : subscribers) {
			sub.onAIStep(moves);
		}
	}
	
	public void addSubscriber(IMovePanelSubscriber sub) {
		subscribers.add(sub);
	}
	
	public void update() {
		
	}
}
