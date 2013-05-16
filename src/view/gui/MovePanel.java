package view.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;

import logic.*;

public class MovePanel extends JPanel {

	private Agent agent;
	
	private JButton btnLeft;
	private JButton btnRight;
	private JButton btnForward;
	
	private List<IMovePanelSubscriber> subscribers;
	
	public MovePanel(Agent agent) {
		super(new GridLayout(3,3));
		/* 0  1  2
		 * 3  4  5
		 * 6  7  8
		 */
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
		
		
		this.add(new JLabel("")); // 0
		this.add(btnForward); // 1
		this.add(new JLabel("")); // 2
		this.add(btnLeft); // 3
		this.add(new JLabel("")); //4
		this.add(btnRight); // 5
		this.add(new JLabel("")); // 6
		this.add(new JLabel("")); // 7
		this.add(new JLabel("")); // 8
		
		
	}
	
	public void notifyLeft() {
		for (IMovePanelSubscriber sub : subscribers) {
			sub.onLeft();
		}
	}
	
	public void notifyRight() {
		for (IMovePanelSubscriber sub : subscribers) {
			sub.onRight();
		}
	}
	
	public void notifyForward() {
		for (IMovePanelSubscriber sub : subscribers) {
			sub.onForward();
		}
	}
	
	public void addSubscriber(IMovePanelSubscriber sub) {
		subscribers.add(sub);
	}
	
	public void update() {
		
	}
}
