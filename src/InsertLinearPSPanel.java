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

import com.esri.core.geometry.Polyline;
import com.esri.map.GraphicsLayer;

public class InsertLinearPSPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JDatePickerImpl dateStartPicker;
	private JDatePickerImpl dateEndPicker;
	private JComboBox<String> typeComboBox;
	private LinearPollutionSource linearPS = new LinearPollutionSource();
	private MainWindow mw;
	private JDatePanelImpl dateStartPanel;
	private JDatePanelImpl dateEndPanel;
	private static final Calendar CALENDAR = Calendar.getInstance();

	private Connection connection;

	private static InsertLinearPSPanel instance;

	protected void setMainWindow(MainWindow mw) {
		this.mw = mw;
	}

	protected static synchronized InsertLinearPSPanel getInstance() {
		if (instance == null) {
			instance = new InsertLinearPSPanel();
		}
		return instance;
	}

	private InsertLinearPSPanel() {
		setBounds(1, 220, 282, 366);
		setBackground(Color.WHITE);
		setBounds(0, 0, 281, 366);
		setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Dati sorgente lineare",
				TitledBorder.LEADING, TitledBorder.TOP, null, SystemColor.activeCaption));
		setLayout(null);

		typeComboBox = new JComboBox<String>();
		typeComboBox.setBackground(Color.WHITE);
		typeComboBox.setBorder(new TitledBorder(null, "Tipo*", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		typeComboBox.setBounds(12, 25, 200, 47);
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
		dateStartPicker.setBackground(Color.WHITE);
		dateStartPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateStartPicker
				.setBorder(new TitledBorder(null, "Data Inizio*", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateStartPicker.setBounds(12, 85, 200, 47);
		dateStartPicker.setVisible(true);
		add(dateStartPicker);
		dateEndPanel = new JDatePanelImpl(model2, p);
		dateEndPicker = new JDatePickerImpl(dateEndPanel, new DateLabelFormatter());
		dateEndPicker.setBackground(Color.WHITE);
		dateEndPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateEndPicker
				.setBorder(new TitledBorder(null, "Data Fine", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateEndPicker.setBounds(12, 144, 200, 47);
		dateEndPicker.setVisible(true);
		add(dateEndPicker);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Color.WHITE);
		buttonPane.setBounds(36, 218, 175, 33);
		add(buttonPane);
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton insertButton = new JButton("Inserisci");
		insertButton.setActionCommand("OK");
		buttonPane.add(insertButton);

		JButton cancelButton = new JButton("Chiudi");
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);

		JLabel lblcampiOblligatori = new JLabel("*Campi Oblligatori");
		lblcampiOblligatori.setBounds(12, 203, 114, 14);
		add(lblcampiOblligatori);
		lblcampiOblligatori.setFont(new Font("Tahoma", Font.ITALIC, 11));

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
					linearPS.setType(string);
				} else {
					JOptionPane.showMessageDialog(null,
							"Input non valido.\nSelezionare un tipo di sorgente di inquinamento lineare.",
							"Attenzione!", JOptionPane.WARNING_MESSAGE);
					return;
				}

				if (startDate != null && !startDate.equals("")) {
					try {
						if (!(new Date().before(format.parse(startDate)))) {

							linearPS.setStartDate(startDate);
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
							linearPS.setEndDate(endDate);
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
				addLinearPS();
			}
		});

		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();
			}
		});
		model1.setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH), CALENDAR.get(Calendar.DAY_OF_MONTH));
		model1.setSelected(true);
	}

	public void setLines(Polyline lines) {
		Polyline l = new Polyline();
		l.startPath(mw.convertCoordinate(lines.getPoint(0)));
		for (int i = 1; i < lines.getPointCount(); i++) {
			l.lineTo(mw.convertCoordinate(lines.getPoint(i)));
		}
		linearPS.setPointList(l);
	}

	protected void addTypeToComboBox() {
		// recupero tipologie sorgenti lineari dal DB
		LoadComboBoxItems.addLinearPSTypeItems(typeComboBox);
	}

	public void addLinearPS() {
		// aggiungo sorgente linear al DB
		connection = mw.getConnection();
		if (connection != null) {
			Statement stmt = null;
			String coordinates = "";
			Polyline line = linearPS.getPolyline();
			String string;
			boolean morePoints = false;
			if (linearPS.getPolyline().getPointCount() > 2) {
				morePoints = true;
				coordinates = "ARRAY[";
			}

			for (int i = 0; i < line.getPointCount(); i++) {
				coordinates = coordinates + "ST_MakePoint(" + line.getPoint(i).getY() + ", " + line.getPoint(i).getX()
						+ "),";
			}
			coordinates = coordinates.substring(0, coordinates.length() - 1); // rimuovo l'ultimo ", "
			if (morePoints) {
				coordinates = coordinates + "]";
			}
			string = "INSERT INTO pollutionsources(pollutiontype, sourcetype, startdate, enddate, radius, pslocation) VALUES ('L', '"
					+ linearPS.getType() + "', '" + linearPS.getStartDate() + "', ";
			if (linearPS.getEndDate() == null) {// rimozione quote altrimenti 'null'
				string = string + linearPS.getEndDate() + ", ";
			} else {
				string = string + "'" + linearPS.getEndDate() + "', ";
			}
			string = string + "null, ST_MakeLine(" + coordinates + "))";

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
		System.out.println(linearPS);
		closePanel();
	}

	public void closePanel() {
		removeIcons();
		cleanFields();
		setVisible(false);
		mw.resetInsertComboBox(MainWindow.INSERTLINEARPS);
	}

	protected void cleanFields() {
		typeComboBox.setSelectedIndex(0);
		dateStartPicker.getJFormattedTextField().setText("");
		dateStartPicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
				CALENDAR.get(Calendar.DAY_OF_MONTH));
		dateEndPicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
				CALENDAR.get(Calendar.DAY_OF_MONTH));
		dateEndPicker.getJFormattedTextField().setText("");
		linearPS.setStartDate(null);
		linearPS.setEndDate(null);
		linearPS.setType(null);

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
			GraphicsLayer gl = mw.getLinearPSLayer();
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
}
