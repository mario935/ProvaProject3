import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class LimitPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JFormattedTextField limit;
	private JLabel hintLabel;
	protected static final ImageIcon HINTICON = MainWindow.HINTICON;

	protected int getInfluenceRadius() {
		String text = limit.getText();
		limit.setText("");
		if (!text.equals(""))
			return Integer.parseInt(text);
		return -1;
	}

	public LimitPanel() {
		setLayout(null);
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMaximumIntegerDigits(6);
		nf.setMaximumFractionDigits(0);
		limit = new JFormattedTextField(nf);

		JPanel distancePanel = new JPanel();
		distancePanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "# Max Ris.",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
		distancePanel.setBounds(10, 0, 80, 40);
		add(distancePanel);
		distancePanel.setLayout(null);

		limit.setValue(null);

		limit.setHorizontalAlignment(SwingConstants.RIGHT);
		limit.setBounds(4, 17, 73, 20);
		distancePanel.add(limit);
		setSize(197, 40);

		limit.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (limit.getText().equals("")) {
					limit.setText("");
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

		limit.addKeyListener(new KeyListener() {

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

		hintLabel = new JLabel("Suggerimento ");
		hintLabel.setBounds(91, -8, 186, 53);
		add(hintLabel);
		hintLabel.setIcon(HINTICON);
		hintLabel.setIconTextGap(2);
		hintLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
		hintLabel.setVisible(true);

		hintLabel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				hintLabel.setText("Suggerimento");
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				hintLabel.setText("<html>Lascia vuoto per<br>visualizzare tutte</br><br>le sorgenti</br></html>");
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
			}
		});
	}

	protected void resetLimitText() {
		limit.setValue(null);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(200, 40);
	}
}
