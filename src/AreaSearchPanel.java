import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.esri.core.geometry.Polyline;
import com.esri.map.GraphicsLayer;

public class AreaSearchPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private MainWindow mw;
	private GraphicsLayer searchAreaLayer;
	protected JButton confirmButton;
	protected JButton analizeButton;
	protected JButton newFigureButton;
	protected boolean confirmButtonPressed = false;
	protected boolean startPoint = true;
	protected Polyline lines = null;
	private boolean listenMouseClick = false;

	private static AreaSearchPanel instance = null;

	private static final LimitPanel limitPanel = new LimitPanel();

	protected void setMainWindow(MainWindow mw) {
		this.mw = mw;
	}

	protected void setListenMouseClick(boolean value) {
		listenMouseClick = value;
	}

	protected boolean getListenMouseClick() {
		return listenMouseClick;
	}

	protected static synchronized AreaSearchPanel getInstance() {
		if (instance == null) {
			instance = new AreaSearchPanel();
		}
		return instance;
	}

	private AreaSearchPanel() {
		setBackground(Color.WHITE);
		setBorder(new TitledBorder(null, "Analisi per area", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setLayout(null);

		analizeButton = new JButton("Analizza");
		analizeButton.setBounds(12, 98, 106, 23);
		add(analizeButton);
		analizeButton.setEnabled(false);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMaximumIntegerDigits(6);
		nf.setMaximumFractionDigits(0);

		confirmButton = new JButton("Conferma");
		confirmButton.setBounds(12, 24, 106, 22);
		confirmButton.setEnabled(false);
		add(confirmButton);

		JButton cancelButton = new JButton("Chiudi");
		cancelButton.setBounds(134, 96, 74, 26);
		add(cancelButton);
		cancelButton.setVisible(true);

		newFigureButton = new JButton("Nuova figura");
		newFigureButton.setBounds(12, 58, 106, 26);
		add(newFigureButton);
		newFigureButton.setEnabled(false);

		newFigureButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cleanFields();
				listenMouseClick = true;
				mw.cleanWindow();
			}
		});

		confirmButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent event) {
				mw.callSetPSArea(); // crea e visualizza il poligono sulla mappa
				confirmButtonPressed = true;
				confirmButton.setEnabled(false);
			}

		});

		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();

			}
		});

		analizeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				AreaSearchFrame asf = AreaSearchFrame.getInstance();
				Object[] options = { "OK" };
				int decision = JOptionPane.showOptionDialog(mw.getFrame(), limitPanel, "Quanti risultati visualizzare?",
						JOptionPane.PLAIN_MESSAGE, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				switch (decision) {
				case JOptionPane.OK_OPTION:
					closePanel();
					asf.setVisible(true);
					asf.startAnalysis(limitPanel.getInfluenceRadius());
					asf.setLocation(1, 200);
					break;
				case JOptionPane.CLOSED_OPTION:
					limitPanel.resetLimitText();
					cleanFields();
					mw.cleanWindow();
					listenMouseClick = true; // continua
					break;
				}
			}
		});
	}

	protected void closePanel() {
		cleanFields();
		setVisible(false);
		mw.resetAnalysisComboBox();
	}

	protected void cleanFields() {
		confirmButtonPressed = false;
		newFigureButton.setEnabled(false);
		confirmButton.setEnabled(false);
		analizeButton.setEnabled(false);
		listenMouseClick = false;
		removeIcon();
	}

	private void removeIcon() {
		searchAreaLayer.removeAll();
	}

	protected void setSearchAreaLayer() {
		searchAreaLayer = mw.getSearchAreaLayer();
	}
}
