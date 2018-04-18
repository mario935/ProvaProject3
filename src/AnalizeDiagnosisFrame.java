import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.postgis.PGgeometry;
import org.postgresql.util.PGobject;

import com.esri.core.geometry.CoordinateConversion;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.map.GraphicsLayer;
import com.esri.map.MapTip;

public class AnalizeDiagnosisFrame extends JInternalFrame {

	private static final long serialVersionUID = 1L;

	private GraphicsLayer diagnosisLayer;
	private GraphicsLayer pointPSLayer;
	private GraphicsLayer linearPSLayer;
	private GraphicsLayer pAreaLayer;

	private HashMap<Integer, Diagnosis> diagnosisGraphics = new HashMap<Integer, Diagnosis>();
	private HashMap<Integer, PointPollutionSource> pointPSGraphics = new HashMap<Integer, PointPollutionSource>();
	private HashMap<Integer, LinearPollutionSource> linearPSGraphics = new HashMap<Integer, LinearPollutionSource>();
	private HashMap<Integer, PollutedArea> pAreaGraphics = new HashMap<Integer, PollutedArea>();

	private MapTip diagnosisMapTip;
	private MapTip pointPSMapTip;
	private MapTip linearPSMapTip;
	private MapTip pSAreaMapTip;

	private LinkedHashMap diagnosisDisplayFields = new LinkedHashMap();
	private LinkedHashMap pointPSDisplayFields = new LinkedHashMap();
	private LinkedHashMap linearPSDisplayFields = new LinkedHashMap();
	private LinkedHashMap pAreaDisplayFields = new LinkedHashMap();

	private MainWindow mw;
	private SpatialReference mapSR;
	private Unit mapUnit;
	private Connection connection;
	private Point point = new Point();
	private static AnalizeDiagnosisFrame instance;
	private int counterResults = 0;

	private final static String TITLE = "Analisi della Diagnosi (Distanza max ";

	private static final SimpleMarkerSymbol DIAGNOSISICON = MainWindow.DIAGNOSISICON;
	private static final SimpleMarkerSymbol DIAGNOSISICON2 = MainWindow.DIAGNOSISICON2;
	private static final SimpleMarkerSymbol POINTICON = MainWindow.POINTICON;
	private static final SimpleLineSymbol LINESYMBOL = MainWindow.LINESYMBOL;
	private static final SimpleLineSymbol POLYGONOUTLINE = MainWindow.POLYGONOUTLINE;
	private static final SimpleFillSymbol POLYGONFILL = MainWindow.POLYGONFILL;
	private static final ImageIcon HINTICON = MainWindow.HINTICON;

	private JButton cancelButton;
	private JLabel hintLabel;
	private JTabbedPane tabbedPane;
	private JTable resultsTable;
	private JTable occurrenceTable;
	private JTable tipologyTable;

	private HashMap<Integer, Integer> resultsSelectionMap = new HashMap<Integer, Integer>();
	private HashMap<String, String> occurrenceSelectionMap = new HashMap<String, String>();
	private HashMap<String, String> tipologySelectionMap = new HashMap<String, String>();
	private HashMap<String, Integer> occurrenceData = new HashMap<String, Integer>();
	private HashMap<String, Integer> tipologyData = new HashMap<String, Integer>();
	private ArrayList<String> resultsData = new ArrayList<String>();

	// restituisce l'unica istanza della classe AnalizeDiagnosisFrame
	protected static synchronized AnalizeDiagnosisFrame getInstance() {
		if (instance == null) {
			instance = new AnalizeDiagnosisFrame();
		}
		return instance;
	}

	protected void setMainWindow(MainWindow mw) {
		this.mw = mw;
	}

	protected void setSpatialReferenceAndUnit(SpatialReference SR, Unit u) {
		mapSR = SR;
		mapUnit = u;
		connection = mw.getConnection();
		initLayers();
	}

	private void initLayers() {
		diagnosisLayer = mw.getDiagnosisLayer();
		pointPSLayer = mw.getPointPSLayer();
		linearPSLayer = mw.getLinearPSLayer();
		pAreaLayer = mw.getPAreaLayer();
	}

	private AnalizeDiagnosisFrame() {
		initComponent();
	}

	protected void startAnalysis(Diagnosis diagnosis, int influenceRadius) {
		setTitle(TITLE + (float) influenceRadius / 1000 + " km)");// imposta dinamicamente il titolo in base al raggio
																	// di influenza scelto dall'utente
		initMapTip();
		int mapID;
		Point diagnosisLocation = convertCoordinate(new Point(diagnosis.getLongitude(), diagnosis.getLatitude()));
		point.setX(diagnosis.getLongitude());
		point.setY(diagnosis.getLatitude());
		// aggiorno MapTips
		setDiagnosisMapTip(diagnosis);
		// imposto grafica
		Graphic pointGraphic = new Graphic(point, DIAGNOSISICON, diagnosisDisplayFields);
		mapID = diagnosisLayer.addGraphic(pointGraphic);
		pointGraphic = new Graphic(point, DIAGNOSISICON2, diagnosisDisplayFields);
		diagnosisLayer.addGraphic(pointGraphic);
		diagnosis.setMapID(mapID);
		diagnosisGraphics.put(mapID, diagnosis);

		searchPollutionSources(diagnosisLocation, influenceRadius);
		initTables();

	}

	private void searchPollutionSources(Point diagnosisLocation, int influenceRadius) {
		String query = " select p.pollutiontype, p.sourcetype, p.radius, p.startdate, p.enddate, p.radius, p.pslocation, case \n"
				+ "			when not pollutiontype = 'P' then (st_distance(pslocation::geography, d.diagnosis_location::geography)/1000)\n"
				+ "			else  (st_distance(st_buffer(pslocation::geography, radius), d.diagnosis_location::geography)/1000)\n"
				+ "			end as distance, count(sourcetype) over (PARTITION by sourcetype) as num_ps, count(pollutiontype) over (PARTITION by pollutiontype) as num_pt  "
				+ " 	from pollutionsources p join diagnosis d on " + "case \n"
				+ "			when not pollutiontype = 'P' then st_dwithin(pslocation::geography, d.diagnosis_location::geography, " + influenceRadius + ")"
				+ " 		else st_dwithin(st_buffer(pslocation::geography, radius), d.diagnosis_location::geography, " + influenceRadius + ")" + " end " 
				+ " 	where (d.diagnosis_location) in (ST_GeometryFromText('POINT(" + diagnosisLocation.getY() + " " + diagnosisLocation.getX() + ")'))" 
				+ " 	order by distance";
		System.out.println(query);
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
				while (rs.next()) {
					counterResults++;
					if (rs.getString("pollutiontype").equals("P")) {
						getPointPS(rs);
					} else if (rs.getString("pollutiontype").equals("L")) {
						getLinearPS(rs);
					} else {
						getPArea(rs);
					}
				}
				rs.close();
				stmt.close();
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(getParent(),
						"Errore nel caricamento delle sorgenti puntiformi.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	// converte le coordinate da latitutine e longitudine a coordinate per la mappa
	protected Point convertCoordinate(Point p) {
		Point newPoint = null;
		String latitude;
		String longitude;
		int cut;
		String coordinateString = CoordinateConversion.pointToDecimalDegrees(p, mapSR, 6);
		cut = coordinateString.indexOf(' ');
		latitude = coordinateString.substring(0, cut);
		longitude = coordinateString.substring(cut, coordinateString.length());
		if (latitude.contains("N")) {
			latitude = latitude.replace('N', ' ');
		} else {
			latitude = latitude.replace('S', ' ');
			latitude = "-".concat(latitude);
		}
		if (longitude.contains("E")) {
			longitude = longitude.replace('E', ' ');
		} else {
			longitude = longitude.replace('W', ' ');
			longitude = "-".concat(longitude);
		}

		newPoint = new Point(Double.parseDouble(longitude.replaceAll(" ", "")),
				Double.parseDouble(latitude.replaceAll(" ", "")));

		return newPoint;
	}

	// prende le informazioni relative alla sorgente puntiforme restituita dal
	// resultSet
	protected void getPointPS(ResultSet rs) {
		Graphic pointGraphic;
		Point point;
		PointPollutionSource pointPS = new PointPollutionSource();
		int mapID;
		try {
			// recupero dati dal DB
			String pollutionType = rs.getString("pollutiontype");
			String sourceType = rs.getString("sourcetype");
			Date startDate = rs.getDate("startdate");
			Date endDate = rs.getDate("enddate");
			int radius = rs.getInt("radius");
			float distance = rs.getFloat("distance");
			PGobject geom = (PGobject) rs.getObject("pslocation");
			int countPS = rs.getInt("num_ps");
			int countPT = rs.getInt("num_pt");
			// converto la geometria Punto del DB in quella di ArcGis
			org.postgis.Point postgisPoint = (org.postgis.Point) PGgeometry.geomFromString(geom.getValue());
			point = convertStringToPoint(postgisPoint.getValue());
			// inizializzo campi dell'oggetto
			pointPS.setType(sourceType);
			pointPS.setStartDate(startDate.toString());
			if (endDate != null) {
				pointPS.setEndDate(endDate.toString());
			}
			pointPS.setLongitude(point.getX());
			pointPS.setLatitude(point.getY());
			pointPS.setRadius(radius);
			// aggiorno MapTips
			setPointPSMapTip(pointPS, distance);
			// imposto la grafica
			pointGraphic = new Graphic(point, POINTICON, pointPSDisplayFields);
			pointPSLayer.addGraphic(pointGraphic);
			pointGraphic = getBuffer(point, POLYGONFILL, radius);
			mapID = pointPSLayer.addGraphic(pointGraphic);
			pointPS.setMapID(mapID);
			pointPSGraphics.put(mapID, pointPS);
			// aggiorno lista e hash map che popoleranno le tabelle
			resultsData.add(counterResults + "");
			resultsData.add(pollutionType);
			resultsData.add(sourceType);
			resultsData.add(radius + "");
			resultsData.add(startDate.toString());
			if (endDate != null) {
				resultsData.add(endDate.toString());
			} else {
				resultsData.add("");
			}
			resultsData.add(String.format("%.3f", distance));
			if (!occurrenceData.containsKey(sourceType)) { // occurenceData del tipo <Tipo Sorgente T, # Sorgenti del
															// tipo T>
				occurrenceData.put(sourceType, countPS);
			}
			if (!tipologyData.containsKey(pollutionType)) { // tipologyData del tipo <X, # di sorgenti di inquinamento
															// di tipo X> con X in {P(oint), L(inear), A(rea)}
				tipologyData.put(pollutionType, countPT);
			}
			// aggiorno hash map per permettere la selezione delle icone sulla mappa tramite
			// la selezione delle righe delle tabelle
			resultsSelectionMap.put(counterResults, mapID);
			if (occurrenceSelectionMap.containsKey(sourceType)) {
				String s = occurrenceSelectionMap.get(sourceType);
				s = s + "," + mapID;
				occurrenceSelectionMap.put(sourceType, s);
			} else {
				occurrenceSelectionMap.put(sourceType, pollutionType + ";" + mapID);
			}
			if (tipologySelectionMap.containsKey(pollutionType)) {
				String s = tipologySelectionMap.get(pollutionType);
				s = s + "," + mapID;
				tipologySelectionMap.put(pollutionType, s);
			} else {
				tipologySelectionMap.put(pollutionType, "" + mapID);
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(),
					"Errore nel caricamento di una sorgente puntiforme.\n" + ex.getMessage(), "Attenzione!",
					JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	// prende le informazioni relative alla sorgente lineare restituita dal
	// resultSet
	protected void getLinearPS(ResultSet rs) {
		Graphic pointGraphic;
		Polyline line = new Polyline();
		LinearPollutionSource linearPS = new LinearPollutionSource();
		int mapID;
		try {
			// recupero dati dal DB
			String pollutionType = rs.getString("pollutiontype");
			String sourceType = rs.getString("sourcetype");
			Date startDate = rs.getDate("startdate");
			Date endDate = rs.getDate("enddate");
			PGobject geom = (PGobject) rs.getObject("pslocation");
			float distance = rs.getFloat("distance");
			int countPS = rs.getInt("num_ps");
			int countPT = rs.getInt("num_pt");
			// converto la geometria LineString del DB in Polyline di ArcGis
			org.postgis.LineString postgisLine = (org.postgis.LineString) PGgeometry.geomFromString(geom.getValue());
			org.postgis.Point[] postgisPoints = postgisLine.getPoints();
			Point[] points = new Point[postgisPoints.length];
			// inizializzo campi dell'oggetto
			linearPS.setType(sourceType);
			linearPS.setStartDate(startDate.toString());
			if (endDate != null) {
				linearPS.setEndDate(endDate.toString());
			}
			// aggiorno MapTips
			setLinearPSMapTip(linearPS, distance);
			// imposto la grafica
			for (int i = 0; i < postgisPoints.length; i++) {
				points[i] = convertStringToPoint(postgisPoints[i].getValue());
			}
			line.startPath(points[0]);
			for (int i = 1; i < points.length; i++) {
				line.lineTo(points[i]);
			}
			linearPS.setPointList(line);
			pointGraphic = new Graphic(line, LINESYMBOL, linearPSDisplayFields);
			mapID = linearPSLayer.addGraphic(pointGraphic);
			linearPS.setMapID(mapID);
			linearPSGraphics.put(mapID, linearPS);

			// aggiorno liste e hash map che popoleranno le tabelle
			resultsData.add(counterResults + "");
			resultsData.add(pollutionType);
			resultsData.add(sourceType);
			resultsData.add("");
			resultsData.add(startDate.toString());
			if (endDate != null) {
				resultsData.add(endDate.toString());
			} else {
				resultsData.add("");
			}
			resultsData.add(String.format("%.3f", distance));
			if (!occurrenceData.containsKey(sourceType)) { // occurenceData del tipo <Tipo Sorgente T, # Sorgenti del
															// tipo T>
				occurrenceData.put(sourceType, countPS);
			}
			if (!tipologyData.containsKey(pollutionType)) { // tipologyData del tipo <X, # di sorgenti di inquinamento
															// di tipo X> con X = {P(oint), L(inear), A(rea)}
				tipologyData.put(pollutionType, countPT);
			}

			// aggiorno hash map per permettere la selezione delle icone sulla mappa tramite
			// la selezione delle righe delle tabelle
			resultsSelectionMap.put(counterResults, mapID);
			if (occurrenceSelectionMap.containsKey(sourceType)) {
				String s = occurrenceSelectionMap.get(sourceType);
				s = s + "," + mapID;
				occurrenceSelectionMap.put(sourceType, s);
			} else {
				occurrenceSelectionMap.put(sourceType, pollutionType + ";" + mapID);
			}
			if (tipologySelectionMap.containsKey(pollutionType)) {
				String s = tipologySelectionMap.get(pollutionType);
				s = s + "," + mapID;
				tipologySelectionMap.put(pollutionType, s);
			} else {
				tipologySelectionMap.put(pollutionType, "" + mapID);
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(),
					"Errore nel caricamento di una sorgente lineare.\n" + ex.getMessage(), "Attenzione!",
					JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}

	}

	// prende le informazioni relative all'area inquinata restituita dal resultSet
	protected void getPArea(ResultSet rs) {
		Graphic pointGraphic;
		Polygon polygon = new Polygon();
		Polyline line = new Polyline();
		PollutedArea pArea = new PollutedArea();
		int mapID;
		try {
			// recupero dati dal DB
			String pollutionType = rs.getString("pollutiontype");
			String sourceType = rs.getString("sourcetype");
			Date startDate = rs.getDate("startdate");
			Date endDate = rs.getDate("enddate");
			PGobject geom = (PGobject) rs.getObject("pslocation");
			float distance = rs.getFloat("distance");
			int countPS = rs.getInt("num_ps");
			int countPT = rs.getInt("num_pt");
			// converto la geometria Polygon del DB in quella di ArcGis
			org.postgis.Polygon postgisPolygon = (org.postgis.Polygon) PGgeometry.geomFromString(geom.getValue());
			org.postgis.LinearRing polygonLine = (org.postgis.LinearRing) postgisPolygon.getSubGeometry(0);
			org.postgis.Point[] postgisPoints = polygonLine.getPoints();
			Point[] points = new Point[postgisPoints.length];
			// inizializzo campi dell'oggetto
			pArea.setType(sourceType);
			pArea.setStartDate(startDate.toString());
			if (endDate != null) {
				pArea.setEndDate(endDate.toString());
			}
			// aggiorno MapTips
			setPAreaMapTip(pArea, distance);
			// imposto la grafica
			for (int i = 0; i < postgisPoints.length; i++) {
				points[i] = convertStringToPoint(postgisPoints[i].getValue());
			}
			line.startPath(points[0]);
			polygon.startPath(points[0]);
			for (int i = 1; i < points.length; i++) {
				line.lineTo(points[i]);
				polygon.lineTo(points[i]);
			}
			pArea.setPolygon(polygon);
			pointGraphic = new Graphic(line, POLYGONOUTLINE, pAreaDisplayFields);
			pAreaLayer.addGraphic(pointGraphic);
			pointGraphic = new Graphic(polygon, POLYGONFILL, pAreaDisplayFields);
			mapID = pAreaLayer.addGraphic(pointGraphic);
			pArea.setMapID(mapID);
			pAreaGraphics.put(mapID, pArea);

			// aggiorno liste e hash map che popoleranno le tabelle
			resultsData.add(counterResults + "");
			resultsData.add(pollutionType);
			resultsData.add(sourceType);
			resultsData.add("");
			resultsData.add(startDate.toString());
			if (endDate != null) {
				resultsData.add(endDate.toString());
			} else {
				resultsData.add("");
			}
			resultsData.add(String.format("%.3f", distance));
			if (!occurrenceData.containsKey(sourceType)) { // occurenceData del tipo <Tipo Sorgente T, # Sorgenti del
															// tipo T>
				occurrenceData.put(sourceType, countPS);
			}
			if (!tipologyData.containsKey(pollutionType)) { // tipologyData del tipo <X, # di sorgenti di inquinamento
															// di tipo X> con X = {P(oint), L(inear), A(rea)}
				tipologyData.put(pollutionType, countPT);
			}

			// aggiorno hash map per permettere la selezione delle icone sulla mappa tramite
			// la selezione delle righe delle tabelle
			resultsSelectionMap.put(counterResults, mapID);
			if (occurrenceSelectionMap.containsKey(sourceType)) {
				String s = occurrenceSelectionMap.get(sourceType);
				s = s + "," + mapID;
				occurrenceSelectionMap.put(sourceType, s);
			} else {
				occurrenceSelectionMap.put(sourceType, pollutionType + ";" + mapID);
			}
			if (tipologySelectionMap.containsKey(pollutionType)) {
				String s = tipologySelectionMap.get(pollutionType);
				s = s + "," + mapID;
				tipologySelectionMap.put(pollutionType, s);
			} else {
				tipologySelectionMap.put(pollutionType, "" + mapID);
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(),
					"Errore nel caricamento di un'area inquinata.\n" + ex.getMessage(), "Attenzione!",
					JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}

	}

	// proietta le coordinate di un punto sulla mappa con il relativo map Spatial
	// Reference
	private Point projectCoordinateToMap(Point point) {
		return GeometryEngine.project(point.getX(), point.getY(), mapSR);
	}

	// converto delle coordinate di latitudine e longitudine in coordinate di un
	// Punto per la mappa
	private Point convertStringToPoint(String coordinates) {
		String latitude = "", longitude = "";
		int whitespace;
		whitespace = coordinates.indexOf(' ');

		latitude = coordinates.substring(1, whitespace);
		longitude = coordinates.substring(whitespace, coordinates.length() - 1);

		return projectCoordinateToMap(new Point(Double.parseDouble(longitude), Double.parseDouble(latitude)));
	}

	// restituisce l'immagine del buffer costruito intorno ad un punto con raggio =
	// "distance" e come simbolo di riempimento "fill"
	private Graphic getBuffer(Point point, SimpleFillSymbol fill, double distance) {
		return new Graphic(GeometryEngine.buffer(point, mapSR, distance, mapUnit), fill, pointPSDisplayFields);
	}

	// inizializza le componenti del frame
	private void initComponent() {

		setFrameIcon(null);
		getContentPane().setLayout(null);

		hintLabel = new JLabel("Suggerimento ");
		hintLabel.setBounds(3, 255, 200, 49);
		getContentPane().add(hintLabel);
		hintLabel.setIcon(HINTICON);
		hintLabel.setIconTextGap(2);
		hintLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
		hintLabel.setVisible(true);

		cancelButton = new JButton("Chiudi");
		cancelButton.setBounds(415, 268, 75, 26);
		getContentPane().add(cancelButton);

		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closeInternalFrame();
			}
		});

		hintLabel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {
				hintLabel.setText("Suggerimento");
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				hintLabel.setText(
						"<html>Clicca una riga della tabella per evidenziare gli elementi corrispondenti sulla mappa</html>");
			}

			@Override
			public void mouseClicked(MouseEvent e) {

			}
		});

		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		tabbedPane.setBounds(0, 0, 490, 256);
		getContentPane().add(tabbedPane);
	}

	private void initTables() {
		if (counterResults > 0) { // controlla se esiste almeno un risultato
			initResultsTable();
			initOccurrenceTable();
			initTipologyTable();

		} else {
			closeInternalFrame();
			JOptionPane.showMessageDialog(mw.getFrame(), "Non è stato trovato nulla.", "Oops",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void initTipologyTable() {
		int col;
		int row;
		TableColumnModel tcm;
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		String[] tipologyColumnNames = { "Tipo Sorgente", "Occorrenze", "Percentuale" };

		col = tipologyColumnNames.length;
		row = tipologyData.size();

		Object[][] tData = new Object[row][col];
		int i = 0;
		float rate;
		for (String s : tipologyData.keySet()) {
			if (s.equals("P")) {
				tData[i][0] = "Puntiforme";
			} else if (s.equals("L")) {
				tData[i][0] = "Lineare";
			} else {
				tData[i][0] = "Area Inquinata";
			}

			tData[i][1] = tipologyData.get(s);
			rate = (float) (tipologyData.get(s)) / (float) (counterResults) * 100;
			tData[i++][2] = String.format("%.2f", rate) + "%";
		}

		tipologyTable = new JTable(tData, tipologyColumnNames) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // impedisco la modifica delle celle della tabella
			}
		};

		ListSelectionModel tipologyTableModel = tipologyTable.getSelectionModel();
		tipologyTableModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// controllo che non siano in atto cambiamenti multipli alla tabella
				if (e.getValueIsAdjusting())
					return;

				int row = tipologyTableModel.getMinSelectionIndex();
				if (row != -1) { // se è stata selezionata una riga la converto nella riga corretta dopo
									// l'ordinamento
					row = tipologyTable.convertRowIndexToModel(row);
				}
				int col = 0;
				// controllo se e quali tabelle azzerare prima di iniziare la selezione della
				// tipologyTable
				if (resultsTable.getSelectedRow() != -1 && row != -1) {
					resultsTable.getSelectionModel().clearSelection();
				}

				if (occurrenceTable.getSelectedRow() != -1 && row != -1) {
					occurrenceTable.getSelectionModel().clearSelection();

				}
				GraphicsLayer layer;
				if (row != -1) {
					// deseleziono tutte le patologie selezionate
					for (String s : tipologySelectionMap.keySet()) { // l'insieme delle chiavi è P(oint), L(inear),
																		// A(rea)
						// seleziono il layer corretto
						if (s.equals("P")) {
							layer = pointPSLayer;
						} else if (s.equals("L")) {
							layer = linearPSLayer;
						} else {
							layer = pAreaLayer;
						}
						for (String mapID : tipologySelectionMap.get(s).split(",")) { // i values di
																						// tipologySelectionMap sono del
																						// tipo mapID1,mapID2,...
							layer.unselect(Integer.parseInt(mapID));
						}
					}

					// prendo la tipologia corretta dalla cella della riga selezionata e considero
					// il layer corrispondente
					String cellType = (String) tipologyTable.getModel().getValueAt(row, col);
					String tipology;
					if (cellType.equals("Puntiforme")) {
						layer = pointPSLayer;
						tipology = tipologySelectionMap.get("P");
					} else if (cellType.equals("Lineare")) {
						layer = linearPSLayer;
						tipology = tipologySelectionMap.get("L");
					} else {
						layer = pAreaLayer;
						tipology = tipologySelectionMap.get("A");
					}

					// seleziono tutti gli elementi sulla mappa che fanno parte della tipologia
					// selezionata
					for (String s : tipology.split(",")) {// i values di tipology sono del tipo mapID1,mapID2,...
						layer.select(Integer.parseInt(s));
					}
				}
			}
		});

		// imposto l'ordinamento della tabella
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tipologyTable.getModel()) {
			@Override
			public boolean isSortable(int column) {
				return false;
			};
		};
		sorter.setComparator(1, new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return o1.compareTo(o2);
			}
		});
		List<RowSorter.SortKey> sortKeys = new ArrayList<>();
		sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
		sorter.setSortKeys(sortKeys);
		sorter.sort();
		tipologyTable.setRowSorter(sorter);

		// centro le stringhe all'interno della cella
		tcm = tipologyTable.getColumnModel();
		tcm.getColumn(0).setCellRenderer(centerRenderer);
		tcm.getColumn(1).setCellRenderer(centerRenderer);
		tcm.getColumn(2).setCellRenderer(centerRenderer);
		// imposto la tabella per una corretta visualizzazione
		tipologyTable.setFillsViewportHeight(true);
		tipologyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tipologyTable.getTableHeader().setReorderingAllowed(false);
		tipologyTable.setDefaultRenderer(String.class, centerRenderer);
		JScrollPane tipologyScrollPane = new JScrollPane(tipologyTable);
		tabbedPane.addTab("Analisi Tipologie", null, tipologyScrollPane, null);
	}

	private void initOccurrenceTable() {
		int col;
		int row;
		TableColumnModel tcm;
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		String[] occurrenceColumnNames = { "Tipo", "Occorrenze", "Percentuale" };

		col = occurrenceColumnNames.length;
		row = occurrenceData.size();

		Object[][] oData = new Object[row][col];
		int i = 0;
		float rate;
		for (String s : occurrenceData.keySet()) {
			oData[i][0] = s;
			oData[i][1] = occurrenceData.get(s);
			rate = (float) (occurrenceData.get(s)) / (float) (counterResults) * 100;
			oData[i++][2] = String.format("%.2f", rate) + "%";
		}

		occurrenceTable = new JTable(oData, occurrenceColumnNames) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // impedisco la modifica delle celle della tabella
			}
		};

		ListSelectionModel occurrenceTableModel = occurrenceTable.getSelectionModel();
		occurrenceTableModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// controllo che non siano in atto cambiamenti multipli alla tabella
				if (e.getValueIsAdjusting())
					return;
				int row = occurrenceTableModel.getMinSelectionIndex();
				if (row != -1) { // se è stata selezionata una riga la converto nella riga corretta dopo
									// l'ordinamento
					row = occurrenceTable.convertRowIndexToModel(row);
				}
				int col = 0;
				// controllo se e quali tabelle azzerare prima di iniziare la selezione della
				// occurrenceTable
				if (resultsTable.getSelectedRow() != -1 && row != -1) {
					resultsTable.getSelectionModel().clearSelection();
				}
				if (tipologyTable.getSelectedRow() != -1 && row != -1) {
					tipologyTable.getSelectionModel().clearSelection();

				}
				GraphicsLayer layer;
				if (row != -1) {
					// deseleziono tutte le patologie selezionate
					for (String s : occurrenceSelectionMap.values()) { // i values di occurrenceSelectionMap sono del
																		// tipo X;mapID1,mapID2,... con X in {P(oint),
																		// L(inear), A(rea)}
						String[] type = s.split(";"); // type[0] contiene la sorgente {P, L, A} mentre type[1] i vari
														// mapID
						String[] values = type[1].split(",");
						// seleziono il layer corretto
						if (type[0].equals("P")) {
							layer = pointPSLayer;
						} else if (type[0].equals("L")) {
							layer = linearPSLayer;
						} else {
							layer = pAreaLayer;
						}

						for (String mapID : values) {
							layer.unselect(Integer.parseInt(mapID));
						}
					}

					// prendo la tipologia corretta dalla cella della riga selezionata e considero
					// il layer corrispondente
					String cellType = (String) occurrenceTable.getModel().getValueAt(row, col);
					String[] selectedPollutionSource = occurrenceSelectionMap.get(cellType).split(";");
					String[] selectedPollutionType = selectedPollutionSource[1].split(",");
					if (selectedPollutionSource[0].equals("P")) {
						layer = pointPSLayer;
					} else if (selectedPollutionSource[0].equals("L")) {
						layer = linearPSLayer;
					} else {
						layer = pAreaLayer;
					}

					for (String s : selectedPollutionType) {
						layer.select(Integer.parseInt(s));
					}
				}
			}
		});

		// imposto l'ordinamento della tabella
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(occurrenceTable.getModel()) {
			@Override
			public boolean isSortable(int column) {
				return false;
			};
		};
		sorter.setComparator(1, new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return o1.compareTo(o2);
			}
		});
		List<RowSorter.SortKey> sortKeys = new ArrayList<>();
		sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
		sorter.setSortKeys(sortKeys);
		sorter.sort();
		occurrenceTable.setRowSorter(sorter);

		// centro le stringhe all'interno della cella
		tcm = occurrenceTable.getColumnModel();
		tcm.getColumn(0).setCellRenderer(centerRenderer);
		tcm.getColumn(1).setCellRenderer(centerRenderer);
		tcm.getColumn(2).setCellRenderer(centerRenderer);
		// imposto la tabella per una corretta visualizzazione
		occurrenceTable.setFillsViewportHeight(true);
		occurrenceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		occurrenceTable.getTableHeader().setReorderingAllowed(false);
		occurrenceTable.setDefaultRenderer(String.class, centerRenderer);
		JScrollPane occurrenceScrollPane = new JScrollPane(occurrenceTable);
		tabbedPane.addTab("Analisi Occorrenze", null, occurrenceScrollPane, null);
	}

	private void initResultsTable() {
		int col;
		int row;
		TableColumnModel tcm;
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		String[] resultsColumnNames = { "#", "Sorg.", "Tipo", "Raggio", "Data Inizio", "Data Fine", "Dist(km)" };

		col = resultsColumnNames.length;
		row = resultsData.size() / col;

		Object[][] rData = new Object[row][col];

		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				rData[i][j] = resultsData.get(i * col + j);
			}
		}

		resultsTable = new JTable(rData, resultsColumnNames) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false; // impedisco la modifica delle celle della tabella
			}
		};

		ListSelectionModel model = resultsTable.getSelectionModel();
		model.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// controllo che non siano in atto cambiamenti multipli alla tabella
				if (e.getValueIsAdjusting())
					return;
				int selectedRow = model.getMinSelectionIndex();
				// controllo se e quali tabelle azzerare prima di iniziare la selezione della
				// resultsTable
				if (occurrenceTable.getSelectedRow() != -1 && selectedRow != -1) {
					occurrenceTable.getSelectionModel().clearSelection();
				}
				if (tipologyTable.getSelectedRow() != -1 && selectedRow != -1) {
					tipologyTable.getSelectionModel().clearSelection();
				}
				GraphicsLayer layer;
				String pollutionType;
				TableModel tableModel = resultsTable.getModel();
				if (selectedRow != -1) { // se è stata selezionata una riga
					for (int i = 0; i < resultsTable.getRowCount(); i++) {
						pollutionType = tableModel.getValueAt(i, 1).toString();
						// seleziono il layer corretto
						if (pollutionType.equals("P")) {
							layer = pointPSLayer;
						} else if (pollutionType.equals("L")) {
							layer = linearPSLayer;
						} else {
							layer = pAreaLayer;
						}
						layer.unselect(resultsSelectionMap.get(i + 1));
					}
					pollutionType = tableModel.getValueAt(selectedRow, 1).toString();
					if (pollutionType.equals("P")) {
						layer = pointPSLayer;
					} else if (pollutionType.equals("L")) {
						layer = linearPSLayer;
					} else {
						layer = pAreaLayer;
					}
					layer.select(resultsSelectionMap.get(selectedRow + 1));
				}
			}
		});

		// imposto dimensioni colonne
		tcm = resultsTable.getColumnModel();
		tcm.getColumn(0).setPreferredWidth(20); // #
		tcm.getColumn(1).setPreferredWidth(20); // Sorgente
		tcm.getColumn(2).setPreferredWidth(120); // Tipo
		tcm.getColumn(3).setPreferredWidth(35); // Raggio
		tcm.getColumn(4).setPreferredWidth(65); // Data inizio
		tcm.getColumn(5).setPreferredWidth(65); // Data fine
		tcm.getColumn(6).setPreferredWidth(65); // Distanza
		// centro le stringhe all'interno della cella
		tcm.getColumn(0).setCellRenderer(centerRenderer);
		tcm.getColumn(1).setCellRenderer(centerRenderer);
		tcm.getColumn(2).setCellRenderer(centerRenderer);
		tcm.getColumn(3).setCellRenderer(centerRenderer);
		tcm.getColumn(4).setCellRenderer(centerRenderer);
		tcm.getColumn(5).setCellRenderer(centerRenderer);
		tcm.getColumn(6).setCellRenderer(centerRenderer);
		// imposto la tabella per una corretta visualizzazione
		resultsTable.setFillsViewportHeight(true);
		resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsTable.getTableHeader().setReorderingAllowed(false);
		JScrollPane resultsScrollPane = new JScrollPane(resultsTable);
		tabbedPane.addTab("Dati Recuperati", null, resultsScrollPane, null);
	}

	int debug = 0;

	protected void closeInternalFrame() {
		cleanFields();
		setVisible(false);
	}

	protected void cleanFields() {
		pAreaDisplayFields.clear();
		pAreaGraphics.clear();
		linearPSDisplayFields.clear();
		linearPSGraphics.clear();
		pointPSDisplayFields.clear();
		pointPSGraphics.clear();
		diagnosisDisplayFields.clear();
		tabbedPane.removeAll();
		resultsSelectionMap.clear();
		occurrenceSelectionMap.clear();
		tipologySelectionMap.clear();
		resultsData.clear();
		occurrenceData.clear();
		tipologyData.clear();
		resultsTable = null;
		occurrenceTable = null;
		tipologyTable = null;
		counterResults = 0;
		removeIcons();
	}

	private void removeIcons() {
		diagnosisLayer.removeAll();
		pointPSLayer.removeAll();
		linearPSLayer.removeAll();
		pAreaLayer.removeAll();
	}

	private void initMapTip() {
		diagnosisDisplayFields.put("Diagnosis", "<u>Diagnosi</u>"); // titolo
		diagnosisDisplayFields.put("Pathology", "Patologia: ");
		diagnosisDisplayFields.put("Date", "Data: ");
		diagnosisMapTip = new MapTip(diagnosisDisplayFields);

		diagnosisLayer.setMapTip(diagnosisMapTip);

		pointPSDisplayFields.put("PPS", "<u>Sorgente Puntiforme</u>"); // titolo
		pointPSDisplayFields.put("Type", "Tipo: ");
		pointPSDisplayFields.put("StartDate", "Data Inizio: ");
		pointPSDisplayFields.put("EndDate", "Data Fine: ");
		pointPSDisplayFields.put("Radius", "Raggio: ");
		pointPSDisplayFields.put("Distance", "Distanza: ");
		pointPSMapTip = new MapTip(pointPSDisplayFields);

		pointPSLayer.setMapTip(pointPSMapTip);

		linearPSDisplayFields.put("LPS", "<u>Sorgente Lineare</u>"); // titolo
		linearPSDisplayFields.put("Type", "Tipo: ");
		linearPSDisplayFields.put("StartDate", "Data Inizio: ");
		linearPSDisplayFields.put("EndDate", "Data Fine: ");
		linearPSDisplayFields.put("Distance", "Distanza: ");
		linearPSMapTip = new MapTip(linearPSDisplayFields);

		linearPSLayer.setMapTip(linearPSMapTip);

		pAreaDisplayFields.put("PA", "<u>Area Inquinata</u>"); // titolo
		pAreaDisplayFields.put("Type", "Tipo: ");
		pAreaDisplayFields.put("StartDate", "Data Inizio: ");
		pAreaDisplayFields.put("EndDate", "Data Fine: ");
		pAreaDisplayFields.put("Distance", "Distanza: ");
		pSAreaMapTip = new MapTip(pAreaDisplayFields);

		pAreaLayer.setMapTip(pSAreaMapTip);

		diagnosisMapTip.setEnabled(true);
		pointPSMapTip.setEnabled(true);
		linearPSMapTip.setEnabled(true);
		pSAreaMapTip.setEnabled(true);
	}

	private void setDiagnosisMapTip(Diagnosis diagnosis) {
		diagnosisDisplayFields.put("Diagnosis", "");
		diagnosisDisplayFields.put("Pathology", diagnosis.getPathology());
		diagnosisDisplayFields.put("Date", diagnosis.getDate());
	}

	private void setPointPSMapTip(PointPollutionSource pointPS, float distance) {
		pointPSDisplayFields.put("PPS", "");
		pointPSDisplayFields.put("Type", pointPS.getType());
		pointPSDisplayFields.put("StartDate", pointPS.getStartDate());
		pointPSDisplayFields.put("EndDate", pointPS.getEndDate());
		pointPSDisplayFields.put("Radius", pointPS.getRadius());
		pointPSDisplayFields.put("Distance", String.format("%.3f", distance) + " km");
	}

	private void setLinearPSMapTip(LinearPollutionSource linearPS, float distance) {
		linearPSDisplayFields.put("LPS", "");
		linearPSDisplayFields.put("Type", linearPS.getType());
		linearPSDisplayFields.put("StartDate", linearPS.getStartDate());
		linearPSDisplayFields.put("EndDate", linearPS.getEndDate());
		linearPSDisplayFields.put("Distance", String.format("%.3f", distance) + " km");
	}

	private void setPAreaMapTip(PollutedArea pArea, float distance) {
		pAreaDisplayFields.put("PA", "");
		pAreaDisplayFields.put("Type", pArea.getType());
		pAreaDisplayFields.put("StartDate", pArea.getStartDate());
		pAreaDisplayFields.put("EndDate", pArea.getEndDate());
		pAreaDisplayFields.put("Distance", String.format("%.3f", distance) + " km");
	}
}
