import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Created by Thai Flowers on 6/22/2017.
 *
 * Based on:
 *      "Swing Hacks" by Joshua Marinacci and Chris Adamson, O'Reilly Media Inc, 2005
 *       Hack #36 Add Status Bars to Windows (page 187, Chapter 5 "Windows, Dialogs, and Frames")
 */
public class BoardStatusBar extends JPanel {

	private JLabel left;
	private JLabel right;

	public BoardStatusBar() {
		super();
		setPreferredSize(new Dimension(getWidth(), 23));
		setLayout(new BorderLayout());

		EmptyBorder border = new EmptyBorder(0,20,0,20);

		left = new JLabel("");
		left.setVisible(true);
		left.setBorder(border);
		this.add(left, BorderLayout.WEST);

		right = new JLabel("");
		right.setVisible(true);
		right.setBorder(border);
		this.add(right, BorderLayout.EAST);
	}

	public void setLeft(String str) {
		left.setText(str);
		repaint();
	}

	public void setRight(String str) {
		right.setText(str);
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		int y=0;
		g.setColor(new Color(156, 154, 140));
		g.drawLine(0, y, getWidth(), y);
		y++;

		g.setColor(new Color(196, 194, 183));
		g.drawLine(0, y, getWidth(), y);
		y++;

		g.setColor(new Color(218, 215, 201));
		g.drawLine(0, y, getWidth(), y);
		y++;

		g.setColor(new Color(233, 231, 217));
		g.drawLine(0, y, getWidth(), y);

		y=getHeight()-3;
		g.setColor(new Color(233, 232, 218));
		g.drawLine(0, y, getWidth(), y);
		y++;

		g.setColor(new Color(233, 231, 216));
		g.drawLine(0, y, getWidth(), y);

		y=getHeight()-1;
		g.setColor(new Color(221, 221, 220));
		g.drawLine(0, y, getWidth(), y);

		g.setColor(Color.BLACK);
		left.repaint();
	}
}
