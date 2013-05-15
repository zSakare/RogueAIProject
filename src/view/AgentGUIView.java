package view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import view.gui.*;

import logic.Agent;

public class AgentGUIView implements IAgentView {

	Agent agent;
	
	JPanel panel;

	JButton btnExit;
	
	public AgentGUIView() {
		panel = new JPanel(new GridBagLayout());
		panel.setPreferredSize(new Dimension(800, 600));
		
		btnExit = new JButton("Exit");
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				System.exit(0);
			}
		});
		btnExit.setBounds(650, 500, 100, 50);
		panel.add("ExitButton", btnExit);
		
		panel.repaint();
		
		JFrame frame = new JFrame("Agent 007");
		frame.getContentPane().add(panel);
		frame.addWindowListener(new WindowCloseManager());
		frame.pack();
		frame.setVisible(true);
		
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

	@Override
	public void onUpdate() {
		// TODO Auto-generated method stub

	}

}
