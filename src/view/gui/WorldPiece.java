package view.gui;

import java.awt.*;

import javax.swing.*;

/**
 * Represents a piece on the world
 * @author Randal Grant
 *
 */
public class WorldPiece extends JPanel {

	char myType;
	private JLabel lblIndicator;
	
	public WorldPiece(boolean highlight) {
		super(new BorderLayout());
		if (highlight) {
			this.setBackground(Color.DARK_GRAY);
		} else {
			this.setBackground(Color.GRAY);
		}
		lblIndicator = new JLabel("...");
		lblIndicator.setFont(new Font("Arial", Font.BOLD, 15));
		lblIndicator.setHorizontalAlignment(SwingConstants.CENTER);
		lblIndicator.setForeground(Color.WHITE);
	
		this.add(lblIndicator, BorderLayout.CENTER);
		update();
	}
	
	/**
	 * Updates the piece's label with the appropriate character.
	 * It is NOT this function's responsibility to repaint, this is for performance reasons.
	 */
	public void update() {
		//System.out.println("WorldPiece at " + this.getX() + ", " + this.getY() + " updated to " + String.valueOf(myType) + " (" + (int)myType + ")");
		lblIndicator.setText(String.valueOf(myType));
		//lblIndicator.repaint();
	}
}
