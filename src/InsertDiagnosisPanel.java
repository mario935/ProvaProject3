import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import com.esri.core.geometry.Point;
import com.esri.map.GraphicsLayer;

public class InsertDiagnosisPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JComboBox<String> pathologyComboBox;
	private JDatePickerImpl datePicker;
	private Diagnosis diagnosis = new Diagnosis();
	private static final Calendar CALENDAR = Calendar.getInstance();
	private MainWindow mw;

	private Connection connection;

	private static InsertDiagnosisPanel instance;

	protected void setMainWindow(MainWindow mw) {
		this.mw = mw;
	}

	protected static synchronized InsertDiagnosisPanel getInstance() {
		if (instance == null) {
			instance = new InsertDiagnosisPanel();
		}
		return instance;
	}

	private InsertDiagnosisPanel() {
		setBounds(new Rectangle(1, 220, 0, 0));
		setBounds(1, 220, 224, 189);
		setBackground(Color.WHITE);
		setBounds(0, 0, 218, 187);
		setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Dati diagnosi", TitledBorder.LEADING,
				TitledBorder.TOP, null, SystemColor.activeCaption));

		UtilDateModel model = new UtilDateModel();
		Properties p = new Properties();
		p.put("text.today", "Oggi");
		p.put("text.month", "Mese");
		p.put("text.year", "Anno");
		JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
		datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
		datePicker.setBorder(new TitledBorder(null, "Data", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		datePicker.setBackground(Color.WHITE);
		datePicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		datePicker.setBounds(12, 84, 200, 47);

		datePicker.setVisible(true);
		setLayout(null);
		setLayout(null);
		add(datePicker);
		datePicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
				CALENDAR.get(Calendar.DAY_OF_MONTH));
		model.setSelected(true);

		pathologyComboBox = new JComboBox<String>();
		pathologyComboBox
				.setBorder(new TitledBorder(null, "Patologia", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pathologyComboBox.setBackground(Color.WHITE);
		pathologyComboBox.setBounds(12, 26, 200, 47);
		pathologyComboBox.addItem("Seleziona..");
		add(pathologyComboBox);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(Color.WHITE);
		buttonPane.setBounds(12, 142, 191, 33);
		add(buttonPane);
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton insertButton = new JButton("Inserisci");
		insertButton.setActionCommand("OK");
		buttonPane.add(insertButton);

		JButton cancelButton = new JButton("Chiudi");
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);

		datePicker.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (datePicker.getJFormattedTextField() == null
						|| datePicker.getJFormattedTextField().getText().equals("")) {
					datePicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
							CALENDAR.get(Calendar.DAY_OF_MONTH));
					model.setSelected(true);
				}
			}
		});

		insertButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String string;
				DateFormat format = new SimpleDateFormat(DateLabelFormatter.getPattern(), Locale.getDefault());

				String date = datePicker.getJFormattedTextField().getText();

				try {
					if (date != null && !date.equals("") && !(new Date().before(format.parse(date)))) {
						diagnosis.setDate(date);

					} else {
						JOptionPane.showMessageDialog(null,
								"Input non valido.\nSelezionare una data precendente alla data corrente.",
								"Attenzione!", JOptionPane.WARNING_MESSAGE);
						return;
					}
				} catch (ParseException e1) {
					e1.printStackTrace();
				}

				string = (String) pathologyComboBox.getSelectedItem();
				if (string != null && !string.equals("Seleziona..")) {
					diagnosis.setPathology(string);
				} else {
					JOptionPane.showMessageDialog(null, "Input non valido.\nSelezionare una patologia", "Attenzione!",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				addDiagnosis();
			}
		});

		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();
			}
		});

	}

	public void setPoint(Point point) {
		diagnosis.setLongitude(point.getX());
		diagnosis.setLatitude(point.getY());
	}

	protected void addPathologiesToComboBox() {
		// recupero patologie dal DB
		LoadComboBoxItems.addPathologyItems(pathologyComboBox);
	}

	public void addDiagnosis() {
		// aggiungo diagnosi al DB
		System.out.println(diagnosis);
		connection = mw.getConnection();
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				stmt.executeUpdate("INSERT INTO Diagnosis(pathology, diagnosis_date, diagnosis_location)"
						+ " VALUES ('" + diagnosis.getPathology() + "','" + diagnosis.getDate() + "',"
						+ " 		ST_MakePoint(" + diagnosis.getLatitude() + ", " + diagnosis.getLongitude() + "))");
				stmt.close();
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(getParent(), "Errore inserimento.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}

		}
		closePanel();
	}

	public void closePanel() {
		removeIcon();
		cleanFields();
		setVisible(false);
		mw.resetInsertComboBox(MainWindow.INSERTDIAGNOSIS);
	}

	protected void cleanFields() {
		pathologyComboBox.setSelectedIndex(0);
		datePicker.getJFormattedTextField().setText("");
		diagnosis.setDate(null);
		diagnosis.setPathology(null);
		datePicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
				CALENDAR.get(Calendar.DAY_OF_MONTH));
	}

	private void removeIcon() {
		if (mw != null) {
			GraphicsLayer gl = mw.getDiagnosisLayer();
			gl.removeAll();
		}
	}
}
