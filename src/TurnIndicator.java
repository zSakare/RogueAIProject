


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


/**
 * Indicates what turn the agent is in
 * @author Randal Grant
 *
 */
public class TurnIndicator extends JPanel {

	private Agent agent;
	
	private JLabel lblTurn;
	
	public TurnIndicator(Agent agent) {
		super(new BorderLayout());
		this.agent = agent;
		this.setBackground(new Color(45, 85, 61));
		
		// create white center-aligned text
		lblTurn = new JLabel("...");
		lblTurn.setFont(new Font("Arial", Font.BOLD, 15));
		lblTurn.setHorizontalAlignment(SwingConstants.CENTER);
		lblTurn.setForeground(Color.WHITE);
		this.add(lblTurn, BorderLayout.CENTER);
		update();
	}
	
	public void update() {
		lblTurn.setText("Move " + agent.getTurnNumber());
	}
	
}
