import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.GraphicsLayer;

public class InsertPointPSPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	protected static final int DEFAULTRADIUS = MainWindow.getDefaultRadius();
	private JFormattedTextField radiusFormattedTextField;
	private JDatePickerImpl dateStartPicker;
	private JDatePickerImpl dateEndPicker;
	private JComboBox<String> typeComboBox;
	private PointPollutionSource pointPS = new PointPollutionSource();
	private JDatePanelImpl dateStartPanel;
	private JDatePanelImpl dateEndPanel;
	private static final Calendar CALENDAR = Calendar.getInstance();

	private MainWindow mw;

	private Connection connection;

	private static final Color MYBLUE = new Color(0f, 0f, 1f, .3f);
	private static final SimpleLineSymbol POLYGONOUTLINE = new SimpleLineSymbol(Color.DARK_GRAY, 2,
			SimpleLineSymbol.Style.SOLID);
	private static final SimpleFillSymbol POLYGONFILL = new SimpleFillSymbol(MYBLUE, POLYGONOUTLINE,
			SimpleFillSymbol.Style.SOLID);
	private static final SimpleMarkerSymbol POINTICON = new SimpleMarkerSymbol(Color.BLUE, 10, Style.CIRCLE);

	private static InsertPointPSPanel instance;

	protected void setMainWindow(MainWindow mw) {
		this.mw = mw;
	}

	protected static synchronized InsertPointPSPanel getInstance() {
		if (instance == null) {
			instance = new InsertPointPSPanel();
		}
		return instance;
	}

	private InsertPointPSPanel() {
		setBackground(Color.WHITE);
		setBounds(1, 220, 297, 414);
		setBackground(Color.WHITE);
		setBounds(0, 0, 300, 414);
		setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Dati sorgente puntiforme",
				TitledBorder.LEADING, TitledBorder.TOP, null, SystemColor.activeCaption));
		setLayout(null);

		typeComboBox = new JComboBox<String>();
		typeComboBox.setBorder(
				new TitledBorder(null, "Tipo Sorgente*", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		typeComboBox.setBackground(Color.WHITE);
		typeComboBox.setBounds(12, 19, 200, 47);
		typeComboBox.addItem("Seleziona..");
		add(typeComboBox);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMaximumIntegerDigits(6);
		nf.setMaximumFractionDigits(0);
		radiusFormattedTextField = new JFormattedTextField(nf);
		radiusFormattedTextField.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Raggio*",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
		radiusFormattedTextField.setBackground(Color.WHITE);
		radiusFormattedTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		radiusFormattedTextField.setText("");
		radiusFormattedTextField.setBounds(125, 196, 74, 39);
		radiusFormattedTextField.setText("" + DEFAULTRADIUS);
		add(radiusFormattedTextField);

		UtilDateModel model1 = new UtilDateModel();
		UtilDateModel model2 = new UtilDateModel();
		Properties p = new Properties();
		p.put("text.today", "Oggi");
		p.put("text.month", "Mese");
		p.put("text.year", "Anno");
		dateStartPanel = new JDatePanelImpl(model1, p);
		dateStartPicker = new JDatePickerImpl(dateStartPanel, new DateLabelFormatter());
		dateStartPicker
				.setBorder(new TitledBorder(null, "Data Inizio*", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateStartPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateStartPicker.setBackground(Color.WHITE);
		dateStartPicker.setBounds(12, 77, 200, 47);
		dateStartPicker.setVisible(true);
		add(dateStartPicker);
		dateEndPanel = new JDatePanelImpl(model2, p);
		dateEndPicker = new JDatePickerImpl(dateEndPanel, new DateLabelFormatter());
		dateEndPicker
				.setBorder(new TitledBorder(null, "Data Fine", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateEndPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateEndPicker.setBackground(Color.WHITE);
		dateEndPicker.setBounds(12, 137, 200, 47);
		dateEndPicker.setVisible(true);
		add(dateEndPicker);
		model1.setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH), CALENDAR.get(Calendar.DAY_OF_MONTH));
		model1.setSelected(true);

		JLabel lblM = new JLabel("m");
		lblM.setBackground(Color.WHITE);
		lblM.setHorizontalAlignment(SwingConstants.CENTER);
		lblM.setBounds(197, 215, 16, 14);
		add(lblM);

		JPanel buttonPane = new JPanel();
		buttonPane.setBounds(37, 240, 175, 36);
		add(buttonPane);
		buttonPane.setBackground(Color.WHITE);
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton insertButton = new JButton("Inserisci");
		insertButton.setActionCommand("OK");
		buttonPane.add(insertButton);

		JButton cancelButton = new JButton("Chiudi");
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);

		JLabel lblcampiOblligatori = new JLabel("*Campi Oblligatori");
		lblcampiOblligatori.setBounds(12, 216, 88, 14);
		add(lblcampiOblligatori);
		lblcampiOblligatori.setBackground(Color.WHITE);
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
				String radiusString = radiusFormattedTextField.getText();
				DateFormat format = new SimpleDateFormat(DateLabelFormatter.getPattern(), Locale.getDefault());
				int radius;
				String string = (String) typeComboBox.getSelectedItem();

				if (string != null && !string.equals("Seleziona..")) {
					pointPS.setType(string);
				} else {
					JOptionPane.showMessageDialog(null,
							"Input non valido.\nSelezionare un tipo di sorgente di inquinamento puntiforme.",
							"Attenzione!", JOptionPane.WARNING_MESSAGE);
					return;
				}

				if (startDate != null && !startDate.equals("")) {
					try {
						if (!(new Date().before(format.parse(startDate)))) {

							pointPS.setStartDate(startDate);
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
							pointPS.setEndDate(endDate);
						} else {
							JOptionPane.showMessageDialog(null,
									"Input non valido.\nSelezionare un intervallo di date coerente.", "Attenzione!",
									JOptionPane.WARNING_MESSAGE);
							return;
						}
					}

					if (!radiusString.equals("")) {
						radius = Integer.parseInt(radiusString);
						if (radius > 0) {
							pointPS.setRadius(radius);
						} else {
							JOptionPane.showMessageDialog(null, "Input non valido.\nSelezionare un raggio > 0.",
									"Attenzione!", JOptionPane.WARNING_MESSAGE);
							return;
						}
					} else {
						JOptionPane.showMessageDialog(null, "Input non valido.\nSelezionare un raggio.", "Attenzione!",
								JOptionPane.WARNING_MESSAGE);
						return;
					}
				} else {
					JOptionPane.showMessageDialog(null, "Input non valido.\nSelezionare una data di inizio.",
							"Attenzione!", JOptionPane.WARNING_MESSAGE);
					return;
				}
				addPointPS();
			}
		});

		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();
			}
		});

		radiusFormattedTextField.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				char key = e.getKeyChar();
				if (!Character.isDigit(key)) {
					e.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				e.getKeyChar();
				if (radiusFormattedTextField != null && !radiusFormattedTextField.getText().equals("")) {
					removeIcons();
					Point p = new Point(pointPS.getLongitude(), pointPS.getLatitude());
					setIcon(convertCoordinateToMap(p));
				} else {
					radiusFormattedTextField.setValue(null);
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
	}

	private Point convertCoordinateToMap(Point point) {
		if (mw == null)
			return null;
		return GeometryEngine.project(point.getX(), point.getY(), mw.getMapSR());
	}

	public void setPoint(Point point) {
		pointPS.setLongitude(point.getX());
		pointPS.setLatitude(point.getY());
		radiusFormattedTextField.setText("" + DEFAULTRADIUS);
	}

	protected void addTypeToComboBox() {
		// recupero tipologie sorgenti puntiformi dal DB
		LoadComboBoxItems.addPointPSTypeItems(typeComboBox);
	}

	public void addPointPS() {
		// aggiungo sorgente puntiforme al DB
		System.out.println(pointPS);
		connection = mw.getConnection();
		if (connection != null) {
			Statement stmt = null;
			String string = "INSERT INTO pollutionsources(pollutiontype, sourcetype, startdate, enddate, radius, pslocation) "
					+ "	VALUES ('P', '" + pointPS.getType() + "', '" + pointPS.getStartDate() + "', ";
			if (pointPS.getEndDate() == null) {// valore 'null' non accettato
				string = string + pointPS.getEndDate() + ", ";
			} else {
				string = string + "'" + pointPS.getEndDate() + "', ";
			}
			string = string + pointPS.getRadius() + ", ST_MakePoint(" + pointPS.getLatitude() + ", "	+ pointPS.getLongitude() + "))";
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
		closePanel();
	}

	protected void cleanFields() {
		typeComboBox.setSelectedIndex(0);
		dateStartPicker.getJFormattedTextField().setText("");
		dateStartPicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
				CALENDAR.get(Calendar.DAY_OF_MONTH));
		dateEndPicker.getModel().setDate(CALENDAR.get(Calendar.YEAR), CALENDAR.get(Calendar.MONTH),
				CALENDAR.get(Calendar.DAY_OF_MONTH));
		dateEndPicker.getJFormattedTextField().setText("");
		radiusFormattedTextField.setText("" + DEFAULTRADIUS);
		pointPS.setStartDate(null);
		pointPS.setEndDate(null);
		pointPS.setRadius(DEFAULTRADIUS);
		pointPS.setType(null);
	}

	private void removeIcons() {
		if (mw != null) {
			GraphicsLayer gl = mw.getPointPSLayer();
			gl.removeAll();
		}
	}

	private void setIcon(Point p) {
		int radius = DEFAULTRADIUS;
		String string = radiusFormattedTextField.getText();
		if (mw != null && string != null && !string.equals("")) {
			radius = Integer.parseInt(string);
			radius = Math.abs(radius);
			radiusFormattedTextField.setText("" + radius);
			GraphicsLayer gl = mw.getPointPSLayer();
			Graphic pointGraphic = getBuffer(p, POLYGONFILL, radius);
			gl.addGraphic(pointGraphic);
			pointGraphic = new Graphic(p, POINTICON);
			gl.addGraphic(pointGraphic);
		}
	}

	private Graphic getBuffer(Point point, SimpleFillSymbol fill, double distance) {
		return new Graphic(GeometryEngine.buffer(point, mw.getMapSR(), distance, mw.getMapUnit()), fill);
	}

	protected void closePanel() {
		removeIcons();
		cleanFields();
		setVisible(false);
		mw.resetInsertComboBox(MainWindow.INSERTPOINTPS);
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
