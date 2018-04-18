import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class DistancePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	protected static final int DEFAULTRADIUS = MainWindow.getDefaultRadius();

	private JFormattedTextField influenceRadius;

	protected int getInfluenceRadius() {
		String text = influenceRadius.getText();
		influenceRadius.setText("" + DEFAULTRADIUS);
		if (!text.equals(""))
			return Integer.parseInt(text);
		return MainWindow.getDefaultRadius();
	}

	public DistancePanel() {
		setLayout(null);
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMaximumIntegerDigits(6);
		nf.setMaximumFractionDigits(0);
		influenceRadius = new JFormattedTextField(nf);

		JPanel distancePanel = new JPanel();
		distancePanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Distanza",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
		distancePanel.setBounds(40, 0, 80, 40);
		add(distancePanel);
		distancePanel.setLayout(null);

		influenceRadius.setText("" + DEFAULTRADIUS);
		influenceRadius.setHorizontalAlignment(SwingConstants.RIGHT);
		influenceRadius.setBounds(4, 17, 73, 20);
		distancePanel.add(influenceRadius);
		setSize(197, 40);

		JLabel lblM = new JLabel("m");
		lblM.setBounds(120, 24, 30, 16);
		add(lblM);

		influenceRadius.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (influenceRadius.getText().equals("" + DEFAULTRADIUS)) {
					influenceRadius.setValue(null);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});

		influenceRadius.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				char key = e.getKeyChar();
				if (!Character.isDigit(key)) {
					e.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
	}

	protected void resetInfluenceRadiusText() {
		influenceRadius.setText("" + DEFAULTRADIUS);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(200, 40);
	}
}
