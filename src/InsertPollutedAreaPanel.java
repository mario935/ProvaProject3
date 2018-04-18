import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.map.GraphicsLayer;

public class InsertPollutedAreaPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JDatePickerImpl dateStartPicker;
	private JDatePickerImpl dateEndPicker;
	private JComboBox<String> typeComboBox;
	private PollutedArea pArea = new PollutedArea();
	private MainWindow mw;
	private JDatePanelImpl dateStartPanel;
	private JDatePanelImpl dateEndPanel;
	private static final Calendar CALENDAR = Calendar.getInstance();

	private Connection connection;

	protected void setMainWindow(MainWindow mw) {
		this.mw = mw;
	}

	private static InsertPollutedAreaPanel instance;

	protected static synchronized InsertPollutedAreaPanel getInstance() {
		if (instance == null) {
			instance = new InsertPollutedAreaPanel();
		}
		return instance;
	}

	private InsertPollutedAreaPanel() {

		setBackground(Color.WHITE);
		setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Dati area inquinata",
				TitledBorder.LEADING, TitledBorder.TOP, null, SystemColor.activeCaption));
		setLayout(null);

		typeComboBox = new JComboBox<String>();
		typeComboBox.setBorder(
				new TitledBorder(null, "Tipo*", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		typeComboBox.setBackground(Color.WHITE);
		typeComboBox.setBounds(10, 23, 200, 47);
		typeComboBox.addItem("Seleziona..");
		add(typeComboBox);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMaximumIntegerDigits(6);
		nf.setMaximumFractionDigits(0);

		UtilDateModel model1 = new UtilDateModel();
		UtilDateModel model2 = new UtilDateModel();
		Properties p = new Properties();
		p.put("text.today", "Oggi");
		p.put("text.month", "Mese");
		p.put("text.year", "Anno");
		dateStartPanel = new JDatePanelImpl(model1, p);
		dateStartPicker = new JDatePickerImpl(dateStartPanel, new DateLabelFormatter());
		dateStartPicker.setBorder(new TitledBorder(null, "Data Inizio*", TitledBorder.LEADING, TitledBorder.TOP, null,
				new Color(0, 0, 0)));
		dateStartPicker.setBackground(Color.WHITE);
		dateStartPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateStartPicker.setBounds(10, 81, 200, 47);
		dateStartPicker.setVisible(true);
		add(dateStartPicker);
		dateEndPanel = new JDatePanelImpl(model2, p);
		dateEndPicker = new JDatePickerImpl(dateEndPanel, new DateLabelFormatter());
		dateEndPicker
				.setBorder(new TitledBorder(null, "Data Fine", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateEndPicker.setBackground(Color.WHITE);
		dateEndPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		;
		dateEndPicker.setBounds(10, 139, 200, 47);
		dateEndPicker.setVisible(true);
		add(dateEndPicker);

		JLabel lblcampiOblligatori = new JLabel("*Campi Oblligatori");
		lblcampiOblligatori.setBounds(12, 198, 114, 14);
		add(lblcampiOblligatori);
		lblcampiOblligatori.setFont(new Font("Tahoma", Font.ITALIC, 11));

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Color.WHITE);
		buttonPane.setBounds(35, 218, 175, 33);
		add(buttonPane);
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton insertButton = new JButton("Inserisci");
		insertButton.setActionCommand("OK");
		buttonPane.add(insertButton);

		JButton cancelButton = new JButton("Chiudi");
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);

		model1.setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH), CALENDAR.get(Calendar.DAY_OF_MONTH));
		model1.setSelected(true);

		dateStartPicker.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (dateStartPicker.getJFormattedTextField() == null
						|| dateStartPicker.getJFormattedTextField().getText().equals("")) {
					dateStartPicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
							CALENDAR.get(Calendar.DAY_OF_MONTH));
					model1.setSelected(true);
				}
			}
		});

		insertButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String startDate = dateStartPicker.getJFormattedTextField().getText();
				String endDate = dateEndPicker.getJFormattedTextField().getText();
				DateFormat format = new SimpleDateFormat(DateLabelFormatter.getPattern(), Locale.getDefault());
				String string = (String) typeComboBox.getSelectedItem();
				if (string != null && !string.equals("Seleziona..")) {
					pArea.setType(string);
				} else {
					JOptionPane.showMessageDialog(null, "Input non valido.\nSelezionare un tipo di area inquinata.",
							"Attenzione!", JOptionPane.WARNING_MESSAGE);
					return;
				}

				if (startDate != null && !startDate.equals("")) {
					try {
						if (!(new Date().before(format.parse(startDate)))) {

							pArea.setStartDate(startDate);
						} else {
							JOptionPane.showMessageDialog(null,
									"Input non valido.\nSelezionare una data precedente alla data corrente.",
									"Attenzione!", JOptionPane.WARNING_MESSAGE);
							return;
						}
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
					if (endDate != null && !endDate.equals("")) {
						if (checkDate(startDate, endDate)) {
							pArea.setEndDate(endDate);
						} else {
							JOptionPane.showMessageDialog(null,
									"Input non valido.\nSelezionare un intervallo di date coerente.", "Attenzione!",
									JOptionPane.WARNING_MESSAGE);
							return;
						}
					}
				} else {
					JOptionPane.showMessageDialog(null, "Input non valido.\nSelezionare una data di inizio.",
							"Attenzione!", JOptionPane.WARNING_MESSAGE);
					return;
				}
				addPArea();
			}
		});

		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();
			}
		});

	}

	protected void addTypeToComboBox() {
		// recupero tipologie aree inquinate dal DB
		LoadComboBoxItems.addPATypeItems(typeComboBox);
	}

	public void addPArea() {
		// aggiungo area inquinata al DB
		connection = mw.getConnection();
		if (connection != null) {
			Statement stmt = null;
			String coordinates = "";
			Polygon line = pArea.getPolygon();
			String string;
			for (int i = 0; i < line.getPointCount(); i++) {
				coordinates = coordinates + "ST_MakePoint(" + line.getPoint(i).getY() + ", " + line.getPoint(i).getX() + "),";
			}
			coordinates = coordinates.substring(0, coordinates.length() - 1); // rimuovo l'ultimo ", "
			string = "INSERT INTO pollutionsources	VALUES ('A', '" + pArea.getType() + "', '" + pArea.getStartDate() + "', ";
			if (pArea.getEndDate() == null) { // valore 'null' non accettato
				string = string + pArea.getEndDate() + ",";
			} else {
				string = string + "'" + pArea.getEndDate() + "', ";
			}
			string = string + "null, ST_MakePolygon(ST_MakeLine(ARRAY[" + coordinates + "])))";
			System.out.println(string);
			try {
				stmt = connection.createStatement();
				stmt.executeUpdate(string);
				stmt.close();
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(getParent(), "Errore inserimento.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
		System.out.println(pArea);
		closePanel();
	}

	public void closePanel() {
		removeIcons();
		cleanFields();
		setVisible(false);
		mw.resetInsertComboBox(MainWindow.INSERTPAREA);
	}

	protected void cleanFields() {
		typeComboBox.setSelectedIndex(0);
		dateStartPicker.getJFormattedTextField().setText("");
		dateStartPicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
				CALENDAR.get(Calendar.DAY_OF_MONTH));
		dateEndPicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
				CALENDAR.get(Calendar.DAY_OF_MONTH));
		dateEndPicker.getJFormattedTextField().setText("");
		pArea.setStartDate(null);
		pArea.setEndDate(null);
		pArea.setType(null);

		mw.confirmButton.setEnabled(false);
		mw.confirmButtonPressed = false;
		mw.startPoint = true;
		Polyline lines = mw.getLines();
		if (lines != null && lines.getPointCount() > 0) {
			lines.removePath(0);
		}
	}

	private void removeIcons() {
		if (mw != null) {
			GraphicsLayer gl = mw.getPAreaLayer();
			gl.removeAll();
		}
	}

	private boolean checkDate(String firstDate, String secondDate) {
		DateFormat format = new SimpleDateFormat(DateLabelFormatter.getPattern(), Locale.getDefault());
		try {
			return !format.parse(secondDate).before(format.parse(firstDate));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void setArea(Polygon polygon) {
		Polygon p = new Polygon();
		p.startPath(mw.convertCoordinate(polygon.getPoint(0)));
		for (int i = 1; i < polygon.getPointCount(); i++) {
			p.lineTo(mw.convertCoordinate(polygon.getPoint(i)));
		}
		pArea.setPolygon(p);
	}
}
