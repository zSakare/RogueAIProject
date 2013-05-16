package view.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Represents a piece on the world
 * @author Randal Grant
 *
 */
public class WorldPiece extends JPanel implements MouseListener {

	char myType;
	private JLabel lblIndicator;
	boolean mousedOver; // is this tile moused over?
	Color tagged; // tagged by map this colour
	boolean highlight; // grey/dkgrey alternating
	WorldMap map;
	
	int x, y;
	
	public WorldPiece(WorldMap map, boolean highlight, int xx, int yy) {
		super(new BorderLayout());
		this.map = map;
		this.highlight = highlight;
		this.tagged = null;
		this.x = xx;
		this.y = yy;
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
		this.addMouseListener(this);
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
		if (tagged != null) {
			this.setBackground(tagged);
		} else if (mousedOver) {
			this.setBackground(Color.RED);
		} else {
			if (this.highlight) {
				this.setBackground(Color.DARK_GRAY);
			} else {
				this.setBackground(Color.GRAY);
			}
		}
	}
	
	public void setTagged(Color color) {
		this.tagged = color;
		update();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		mousedOver = true;
		map.onMouseOver(this);
		update();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		mousedOver = false;
		update();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
