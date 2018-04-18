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

public class AreaSearchFrame extends JInternalFrame {

	private static final long serialVersionUID = 1L;
	private MainWindow mw;
	private SpatialReference mapSR;
	private Unit mapUnit;
	private JButton cancelButton;
	private JLabel hintLabel;
	private GraphicsLayer searchAreaLayer;
	private static AreaSearchFrame instance = null;
	private Polygon area = new Polygon();

	private MapTip diagnosisMapTip;
	private MapTip pointPSMapTip;
	private MapTip linearPSMapTip;
	private MapTip pSAreaMapTip;

	private GraphicsLayer diagnosisLayer;
	private GraphicsLayer pointPSLayer;
	private GraphicsLayer linearPSLayer;
	private GraphicsLayer pAreaLayer;

	private LinkedHashMap diagnosisDisplayFields = new LinkedHashMap();
	private LinkedHashMap pointPSDisplayFields = new LinkedHashMap();
	private LinkedHashMap linearPSDisplayFields = new LinkedHashMap();
	private LinkedHashMap pAreaDisplayFields = new LinkedHashMap();

	private HashMap<Integer, Diagnosis> diagnosisGraphics = new HashMap<Integer, Diagnosis>();
	private HashMap<Integer, PointPollutionSource> pointPSGraphics = new HashMap<Integer, PointPollutionSource>();
	private HashMap<Integer, LinearPollutionSource> linearPSGraphics = new HashMap<Integer, LinearPollutionSource>();
	private HashMap<Integer, PollutedArea> pAreaGraphics = new HashMap<Integer, PollutedArea>();

	private HashMap<Integer, Integer> resultsSelectionMap = new HashMap<Integer, Integer>();
	private HashMap<String, String> occurrenceSelectionMap = new HashMap<String, String>();
	private HashMap<String, String> tipologySelectionMap = new HashMap<String, String>();
	HashMap<String, Integer> occurrenceData = new HashMap<String, Integer>();
	HashMap<String, Integer> tipologyData = new HashMap<String, Integer>();
	ArrayList<String> resultsData = new ArrayList<String>();

	private JTabbedPane tabbedPane;
	private JTable resultsTable;
	private JTable occurrenceDiagnosisTable;
	private JTable occurrencePollutionTable;
	private JTable tipologyTable;

	private int counterResults = 0;
	private int countDiagnosis = 0;
	private int countPathology = 0;

	private Connection connection;

	private static final SimpleMarkerSymbol DIAGNOSISICON = MainWindow.DIAGNOSISICON;
	private static final SimpleMarkerSymbol DIAGNOSISICON2 = MainWindow.DIAGNOSISICON2;
	private static final SimpleMarkerSymbol POINTICON = MainWindow.POINTICON;
	private static final SimpleLineSymbol LINESYMBOL = MainWindow.LINESYMBOL;
	private static final SimpleLineSymbol POLYGONOUTLINE = MainWindow.POLYGONOUTLINE;
	private static final SimpleFillSymbol POLYGONFILL = MainWindow.POLYGONFILL;
	private static final SimpleFillSymbol POLYGONFILL2 = MainWindow.POLYGONFILL2;
	private static final ImageIcon HINTICON = MainWindow.HINTICON;

	private String polygon = "";

	public void setArea(Polygon a) {
		if (area.getPointCount() > 0) {
			area.removePath(0);
		}
		area.startPath(a.getPoint(0));
		for (int i = 1; i < a.getPointCount(); i++) {
			area.lineTo(a.getPoint(i));
		}
	}

	protected static synchronized AreaSearchFrame getInstance() {
		if (instance == null) {
			instance = new AreaSearchFrame();
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
		searchAreaLayer = mw.getSearchAreaLayer();
	}

	private AreaSearchFrame() {
		initComponent();
	}

	protected void startAnalysis(int limit) {
		initMapTip();
		Graphic pointGraphic = new Graphic(area, POLYGONFILL2);
		searchAreaLayer.addGraphic(pointGraphic);
		AnalysisPanel.getInstance();

		for (int i = 0; i < area.getPointCount(); i++) {
			Point p = convertCoordinate(area.getPoint(i));
			polygon = polygon + p.getY() + " " + p.getX() + ",";
		}
		polygon = polygon.substring(0, polygon.length() - 1); // rimuovo l'ultima virgola

		searchDiagnosis();
		countDiagnosis = counterResults;
		countPathology = occurrenceData.size();
		searchPollutionSources(limit);
		initTable();

	}

	private void searchPollutionSources(int limit) {
		String query;
		if (limit != -1) {
			query = "select count(sourcetype) over (PARTITION by sourcetype) as num_ps, count(pollutiontype) over (PARTITION by pollutiontype) as num_pt, q.distance, q.pollutiontype, q.sourcetype, q.startdate, q.enddate, q.pslocation, q.radius "
					+ "from (select case when not pollutiontype = 'P' then (st_distance(ST_GeographyFromText('POLYGON((" + polygon + "))'), pslocation)/1000) "
					+ "			else (st_distance(ST_GeographyFromText('POLYGON((" + polygon + "))'), st_buffer(pslocation, radius))/1000) end as distance, "
					+ "			pollutiontype, sourcetype, startdate, enddate, pslocation, radius "
					+ "		 from pollutionsources " 
					+ "		 order by distance limit " + limit + " ) as q "
					+ "order by distance";
		} else {
			query = "select case when not pollutiontype = 'P' then (st_distance(ST_GeographyFromText('POLYGON((" + polygon + "))'), pslocation)/1000) " 
					+ "		else (st_distance(ST_GeographyFromText('POLYGON((" + polygon + "))'), st_buffer(pslocation, radius))/1000) end as distance, "
					+ "		pollutiontype, sourcetype, startdate, enddate, pslocation, radius, count(sourcetype) over (PARTITION by sourcetype) as num_ps, count(pollutiontype) over (PARTITION by pollutiontype) as num_pt "
					+ "from pollutionsources " 
					+ "order by distance";
		}
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
				JOptionPane.showMessageDialog(getParent(), "Errore nel caricamento delle diagnosi.\n" + ex.getMessage(),
						"Attenzione!", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	private void searchDiagnosis() {
		String query = "select (st_distance(ST_GeographyFromText('POLYGON((" + polygon + "))'), d.diagnosis_location)/1000) as distance, "
				+ "			d.pathology, d.diagnosis_date, d.diagnosis_location, count(d.pathology) over (PARTITION by d.pathology) as num_pathology "
				+ "		from diagnosis d "
				+ "		where st_contains(st_setsrid(ST_GeometryFromText('POLYGON(("+ polygon + "))'), 4326), d.diagnosis_location::geometry)";
		System.out.println(query);
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
				while (rs.next()) {
					counterResults++;
					getDiagnosis(rs);
				}
				rs.close();
				stmt.close();
				tipologyData.put("D", counterResults);
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(getParent(), "Errore nel caricamento delle diagnosi.\n" + ex.getMessage(),
						"Attenzione!", JOptionPane.ERROR_MESSAGE);
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

	// prende le informazioni relative alla diagnosi restituita dal resultSet
	protected void getDiagnosis(ResultSet rs) {
		Graphic pointGraphic;
		Point point;
		int mapID;
		Diagnosis diagnosis = new Diagnosis();
		try {
			// recupero dati dal DB
			String pathology = rs.getString("pathology");
			Date date = rs.getDate("diagnosis_date");
			PGobject geom = (PGobject) rs.getObject("diagnosis_location");
			float distance = rs.getFloat("distance");
			int count = rs.getInt("num_pathology");
			// converto la geometria Punto del DB in quella di ArcGis
			org.postgis.Point postgisPoint = (org.postgis.Point) PGgeometry.geomFromString(geom.getValue());
			point = convertStringToPoint(postgisPoint.getValue());
			// inizializzo campi dell'oggetto
			diagnosis.setPathology(pathology);
			diagnosis.setDate(date.toString());
			diagnosis.setLatitude(point.getY());
			diagnosis.setLongitude(point.getX());
			// aggiorno MapTips
			setDiagnosisMapTip(diagnosis, distance);
			// imposto la grafica
			pointGraphic = new Graphic(point, DIAGNOSISICON, diagnosisDisplayFields);
			mapID = diagnosisLayer.addGraphic(pointGraphic);
			pointGraphic = new Graphic(point, DIAGNOSISICON2, diagnosisDisplayFields);
			diagnosisLayer.addGraphic(pointGraphic);
			diagnosis.setMapID(mapID);
			diagnosisGraphics.put(mapID, diagnosis);
			// aggiorno lista e hash map che popoleranno le tabelle
			resultsData.add(counterResults + "");
			resultsData.add("D");
			resultsData.add(pathology);
			resultsData.add("");// radius
			resultsData.add(date.toString());
			resultsData.add("");// dateEnd
			resultsData.add(String.format("%.3f", distance));
			occurrenceData.put(pathology, count);
			// aggiorno hash map per permettere la selezione delle icone sulla mappa tramite
			// la selezione delle righe delle tabelle
			resultsSelectionMap.put(counterResults, mapID);
			if (occurrenceSelectionMap.containsKey(pathology)) {
				String s = occurrenceSelectionMap.get(pathology);
				s = s + "," + mapID;
				occurrenceSelectionMap.put(pathology, s);
			} else {
				occurrenceSelectionMap.put(pathology, "D;" + mapID);
			}

			if (tipologySelectionMap.containsKey("D")) {
				String s = tipologySelectionMap.get("D");
				s = s + "," + mapID;
				tipologySelectionMap.put("D", s);
			} else {
				tipologySelectionMap.put("D", "" + mapID);
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(), "Errore nel caricamento di una diagnosi.\n" + ex.getMessage(),
					"Attenzione!", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	protected void getPointPS(ResultSet rs) {
		Graphic pointGraphic;
		Point point;
		PointPollutionSource pointPS = new PointPollutionSource();
		int mapID;
		try {
			// recupero dati dal DB
			String pollutionType = "P";
			String sourceType = rs.getString("sourcetype");
			Date startDate = rs.getDate("startdate");
			Date endDate = rs.getDate("enddate");
			int radius = rs.getInt("radius");
			PGobject geom = (PGobject) rs.getObject("pslocation");
			float distance = rs.getFloat("distance");
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
			resultsData.add("P");
			resultsData.add(sourceType);
			resultsData.add(radius + "");
			resultsData.add(startDate.toString());
			if (endDate != null) {
				resultsData.add(endDate.toString());
			} else {
				resultsData.add("");
			}
			resultsData.add(String.format("%.3f", distance));
			if (!occurrenceData.containsKey(sourceType)) {
				occurrenceData.put(sourceType, countPS);
			}
			if (!tipologyData.containsKey(pollutionType)) {
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

	protected void getLinearPS(ResultSet rs) {
		Graphic pointGraphic;
		Polyline line = new Polyline();
		LinearPollutionSource linearPS = new LinearPollutionSource();
		int mapID;
		try {
			// recupero dati dal DB
			String pollutionType = "L";
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
			// aggiorno lista e hash map che popoleranno le tabelle
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
			if (!occurrenceData.containsKey(sourceType)) {
				occurrenceData.put(sourceType, countPS);
			}
			if (!tipologyData.containsKey(pollutionType)) {
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

	protected void getPArea(ResultSet rs) {
		Graphic pointGraphic;
		Polygon polygon = new Polygon();
		Polyline line = new Polyline();
		PollutedArea pArea = new PollutedArea();
		int mapID;
		try {
			// recupero dati dal DB
			String pollutionType = "A";
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
			// aggiorno lista e hash map che popoleranno le tabelle
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
			if (!occurrenceData.containsKey(sourceType)) {
				occurrenceData.put(sourceType, countPS);
			}
			if (!tipologyData.containsKey(pollutionType)) {
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

	private void initTable() {
		if (counterResults > 0) { // controlla se esiste almeno un risultato

			initResultsTable();
			initOccurenceTables();
			initTipologyTable();

		} else {
			closeInternalFrame();
			JOptionPane.showMessageDialog(mw.getFrame(), "Non è stato trovato nulla.", "Oops",
					JOptionPane.INFORMATION_MESSAGE);
		}
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
				return false;// impedisco la modifica delle celle della tabella
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
				// tipologyTable
				if (occurrenceDiagnosisTable.getSelectedRow() != -1 && selectedRow != -1) {
					occurrenceDiagnosisTable.getSelectionModel().clearSelection();
				}
				if (occurrencePollutionTable.getSelectedRow() != -1 && selectedRow != -1) {
					occurrencePollutionTable.getSelectionModel().clearSelection();
				}
				if (tipologyTable.getSelectedRow() != -1 && selectedRow != -1) {
					tipologyTable.getSelectionModel().clearSelection();
				}

				GraphicsLayer layer;
				String type;
				TableModel tableModel = resultsTable.getModel();
				if (selectedRow != -1) {// se è stata selezionata una riga
					for (int i = 0; i < resultsTable.getRowCount(); i++) {
						type = tableModel.getValueAt(i, 1).toString();
						if (type.equals("D")) {
							layer = diagnosisLayer;
						} else if (type.equals("P")) {
							layer = pointPSLayer;
						} else if (type.equals("L")) {
							layer = linearPSLayer;
						} else {
							layer = pAreaLayer;
						}
						layer.unselect(resultsSelectionMap.get(i + 1));
					}
					type = tableModel.getValueAt(selectedRow, 1).toString();
					if (type.equals("D")) {
						layer = diagnosisLayer;
					} else if (type.equals("P")) {
						layer = pointPSLayer;
					} else if (type.equals("L")) {
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
		tcm.getColumn(1).setPreferredWidth(20); // Sorgente / Diagnosi
		tcm.getColumn(2).setPreferredWidth(120); // Tipo / Patologia
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

	private void initOccurenceTables() {
		int col;
		int rowDiagnosis;
		int rowPollution;
		TableColumnModel tcm;
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		String[] occurenceColumnNames = { "Tipo", "Occorrenze", "Percentuale" };

		col = occurenceColumnNames.length;
		rowDiagnosis = countPathology;
		rowPollution = occurrenceData.size() - countPathology;
		Object[][] diagnosisData = new Object[rowDiagnosis][col];
		Object[][] pollutionData = new Object[rowPollution][col];
		int i = 0;
		int j = 0;
		float rate;
		for (String s : occurrenceData.keySet()) {
			if (LoadComboBoxItems.pathologies.contains(s)) {
				diagnosisData[i][0] = s;
				diagnosisData[i][1] = occurrenceData.get(s);
				rate = (float) (occurrenceData.get(s)) / (float) (countDiagnosis) * 100;
				diagnosisData[i++][2] = String.format("%.2f", rate) + "%";
			} else {
				pollutionData[j][0] = s;
				pollutionData[j][1] = occurrenceData.get(s);
				rate = (float) (occurrenceData.get(s)) / (float) (counterResults - countDiagnosis) * 100;
				pollutionData[j++][2] = String.format("%.2f", rate) + "%";
			}
		}

		occurrenceDiagnosisTable = new JTable(diagnosisData, occurenceColumnNames) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;// impedisco la modifica delle celle della tabella
			}
		};

		occurrencePollutionTable = new JTable(pollutionData, occurenceColumnNames) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;// impedisco la modifica delle celle della tabella
			}
		};

		ListSelectionModel listDiagnosisModel = occurrenceDiagnosisTable.getSelectionModel();
		listDiagnosisModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// controllo che non siano in atto cambiamenti multipli alla tabella
				if (e.getValueIsAdjusting())
					return;
				int row = listDiagnosisModel.getMinSelectionIndex();
				if (row != -1) { // se è stata selezionata una riga la converto nella riga corretta dopo
									// l'ordinamento
					row = occurrenceDiagnosisTable.convertRowIndexToModel(row);
				}
				int col = 0;
				// controllo se e quali tabelle azzerare prima di iniziare la selezione della
				// tipologyTable
				if (resultsTable.getSelectedRow() != -1 && row != -1) {
					resultsTable.getSelectionModel().clearSelection();
				}

				if (tipologyTable.getSelectedRow() != -1 && row != -1) {
					tipologyTable.getSelectionModel().clearSelection();

				}

				if (occurrencePollutionTable.getSelectedRow() != -1 && row != -1) {
					occurrencePollutionTable.getSelectionModel().clearSelection();

				}
				GraphicsLayer layer;
				if (row != -1) {
					// deseleziono tutte le patologie selezionate
					for (String s : occurrenceSelectionMap.values()) { // i values di analysisSelectionMap sono del tipo
																		// *.mapID1,mapID2,... con * in {P(oint),
																		// L(inear), A(rea)}
						String[] type = s.split(";"); // type[0] contiene la sorgente {P, L, A} mentre type[1] i vari
														// mapID
						String[] values = type[1].split(",");
						if (type[0].equals("D")) {
							layer = diagnosisLayer;
						} else if (type[0].equals("P")) {
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
					String cellType = (String) occurrenceDiagnosisTable.getModel().getValueAt(row, col);
					String[] selectedPollutionSource = occurrenceSelectionMap.get(cellType).split(";");
					String[] selectedPollutionType = selectedPollutionSource[1].split(",");

					if (selectedPollutionSource[0].equals("D")) {
						layer = diagnosisLayer;
						for (String s : selectedPollutionType) {
							layer.select(Integer.parseInt(s));
						}
					}
				}
			}
		});

		ListSelectionModel listPollutionModel = occurrencePollutionTable.getSelectionModel();
		listPollutionModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// controllo che non siano in atto cambiamenti multipli alla tabella
				if (e.getValueIsAdjusting())
					return;
				int row = listPollutionModel.getMinSelectionIndex();
				if (row != -1) { // se è stata selezionata una riga la converto nella riga corretta dopo
									// l'ordinamento
					row = occurrencePollutionTable.convertRowIndexToModel(row);
				}
				int col = 0;
				// controllo se e quali tabelle azzerare prima di iniziare la selezione della
				// tipologyTable
				if (resultsTable.getSelectedRow() != -1 && row != -1) {
					resultsTable.getSelectionModel().clearSelection();
				}

				if (tipologyTable.getSelectedRow() != -1 && row != -1) {
					tipologyTable.getSelectionModel().clearSelection();

				}

				if (occurrenceDiagnosisTable.getSelectedRow() != -1 && row != -1) {
					occurrenceDiagnosisTable.getSelectionModel().clearSelection();

				}
				GraphicsLayer layer;
				if (row != -1) {
					// deseleziono tutte le patologie selezionate
					for (String s : occurrenceSelectionMap.values()) { // i values di analysisSelectionMap sono del tipo
																		// X;mapID1,mapID2,... con X in {P(oint),
																		// L(inear), A(rea)}
						String[] type = s.split(";"); // type[0] contiene la sorgente {P, L, A} mentre type[1] i vari
														// mapID
						String[] values = type[1].split(",");
						if (type[0].equals("D")) {
							layer = diagnosisLayer;
						} else if (type[0].equals("P")) {
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
					String cellType = (String) occurrencePollutionTable.getModel().getValueAt(row, col);
					String[] selectedPollutionSource = occurrenceSelectionMap.get(cellType).split(";");
					String[] selectedPollutionType = selectedPollutionSource[1].split(",");
					if (selectedPollutionSource[0].equals("D")) {

					} else {
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
			}
		});

		// imposto l'ordinamento delle tabelle
		TableRowSorter<TableModel> pollutionSorter = new TableRowSorter<TableModel>(
				occurrencePollutionTable.getModel()) {
			@Override
			public boolean isSortable(int column) {
				return false;
			};
		};
		pollutionSorter.setComparator(1, new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return o1.compareTo(o2);
			}
		});
		TableRowSorter<TableModel> diagnosisSorter = new TableRowSorter<TableModel>(
				occurrenceDiagnosisTable.getModel()) {
			@Override
			public boolean isSortable(int column) {
				return false;
			};
		};
		diagnosisSorter.setComparator(1, new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return o1.compareTo(o2);
			}
		});
		List<RowSorter.SortKey> pollutionSortKeys = new ArrayList<>();
		pollutionSortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
		pollutionSorter.setSortKeys(pollutionSortKeys);
		pollutionSorter.sort();
		occurrencePollutionTable.setRowSorter(pollutionSorter);
		List<RowSorter.SortKey> doiagnosisSortKeys = new ArrayList<>();
		doiagnosisSortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
		diagnosisSorter.setSortKeys(doiagnosisSortKeys);
		diagnosisSorter.sort();
		occurrenceDiagnosisTable.setRowSorter(diagnosisSorter);

		// centro le stringhe all'interno della cella
		tcm = occurrenceDiagnosisTable.getColumnModel();
		tcm.getColumn(0).setCellRenderer(centerRenderer);
		tcm.getColumn(1).setCellRenderer(centerRenderer);
		tcm.getColumn(2).setCellRenderer(centerRenderer);
		tcm = occurrencePollutionTable.getColumnModel();
		tcm.getColumn(0).setCellRenderer(centerRenderer);
		tcm.getColumn(1).setCellRenderer(centerRenderer);
		tcm.getColumn(2).setCellRenderer(centerRenderer);
		// imposto le tabelle per una corretta visualizzazione
		occurrenceDiagnosisTable.setFillsViewportHeight(true);
		occurrenceDiagnosisTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		occurrenceDiagnosisTable.getTableHeader().setReorderingAllowed(false);
		occurrenceDiagnosisTable.setDefaultRenderer(String.class, centerRenderer);
		occurrencePollutionTable.setFillsViewportHeight(true);
		occurrencePollutionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		occurrencePollutionTable.getTableHeader().setReorderingAllowed(false);
		occurrencePollutionTable.setDefaultRenderer(String.class, centerRenderer);
		JScrollPane diagnosisScrollPane = new JScrollPane(occurrenceDiagnosisTable);
		tabbedPane.addTab("Analisi Diagnosi", null, diagnosisScrollPane, null);
		JScrollPane pollutionScrollPane = new JScrollPane(occurrencePollutionTable);
		tabbedPane.addTab("Analisi Sorgenti", null, pollutionScrollPane, null);
	}

	private void initTipologyTable() {
		int col;
		int row;
		TableColumnModel tcm;
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		String[] tipologyColumnNames = { "Tipologia", "Occorrenze", "Percentuale Sorgenti" };

		col = tipologyColumnNames.length;
		row = tipologyData.size();

		Object[][] tData = new Object[row][col];
		int i = 0;
		float rate;
		for (String s : tipologyData.keySet()) {
			if (s.equals("D")) {
				tData[i][0] = "Diagnosi";
				tData[i][1] = tipologyData.get(s);
				tData[i++][2] = "-";
			}
		}
		for (String s : tipologyData.keySet()) {
			if (s.equals("D")) {

			} else {
				rate = (float) tipologyData.get(s) / (float) (counterResults - countDiagnosis) * 100;
				tData[i][2] = String.format("%.2f", rate) + "%";

				if (s.equals("P")) {
					tData[i][0] = "Sorgente Puntiforme";
				} else if (s.equals("L")) {
					tData[i][0] = "Sorgente Lineare";
				} else {
					tData[i][0] = "Area Inquinata";
				}
				tData[i++][1] = tipologyData.get(s);
			}
		}

		tipologyTable = new JTable(tData, tipologyColumnNames) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;// impedisco la modifica delle celle della tabella
			}
		};

		ListSelectionModel model2 = tipologyTable.getSelectionModel();
		model2.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// controllo che non siano in atto cambiamenti multipli alla tabella
				if (e.getValueIsAdjusting())
					return;
				int row = model2.getMinSelectionIndex();
				if (row != -1) {// se è stata selezionata una riga la converto nella riga corretta dopo
								// l'ordinamento
					row = tipologyTable.convertRowIndexToModel(row);
				}
				int col = 0;
				// controllo se e quali tabelle azzerare prima di iniziare la selezione della
				// tipologyTable
				if (resultsTable.getSelectedRow() != -1 && row != -1) {
					resultsTable.getSelectionModel().clearSelection();
				}

				if (occurrenceDiagnosisTable.getSelectedRow() != -1 && row != -1) {
					occurrenceDiagnosisTable.getSelectionModel().clearSelection();

				}

				if (occurrencePollutionTable.getSelectedRow() != -1 && row != -1) {
					occurrencePollutionTable.getSelectionModel().clearSelection();

				}
				GraphicsLayer layer;
				if (row != -1) {
					// deseleziono tutte le patologie selezionate
					for (String s : tipologySelectionMap.keySet()) { // l'insieme delle chiavi è D(iagnosis), P(oint),
																		// L(inear), A(rea)
						if (s.equals("D")) {
							layer = diagnosisLayer;
						} else if (s.equals("P")) {
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

					String cellType = (String) tipologyTable.getModel().getValueAt(row, col);
					String tipology;
					if (cellType.equals("Diagnosi")) {
						layer = diagnosisLayer;
						tipology = tipologySelectionMap.get("D");
					} else if (cellType.equals("Sorgente Puntiforme")) {
						layer = pointPSLayer;
						tipology = tipologySelectionMap.get("P");
					} else if (cellType.equals("Sorgente Lineare")) {
						layer = linearPSLayer;
						tipology = tipologySelectionMap.get("L");
					} else {
						layer = pAreaLayer;
						tipology = tipologySelectionMap.get("A");
					}

					if (tipology != null) {
						for (String s : tipology.split(",")) {
							layer.select(Integer.parseInt(s));
						}
					}
				}
			}
		});

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

	private void initComponent() {
		setTitle("Analisi dell'area selezionata");
		setFrameIcon(null);
		getContentPane().setLayout(null);
		setResizable(false);

		hintLabel = new JLabel("Suggerimento ");
		hintLabel.setBounds(3, 255, 200, 49);
		getContentPane().add(hintLabel);
		hintLabel.setIcon(HINTICON);
		hintLabel.setIconTextGap(2);
		hintLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
		hintLabel.setVisible(true);

		cancelButton = new JButton("Chiudi");
		cancelButton.setBounds(395, 261, 75, 26);
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

	private void initMapTip() {
		diagnosisDisplayFields.put("Diagnosis", "<u>Diagnosi</u>"); // titolo
		diagnosisDisplayFields.put("Pathology", "Patologia: ");
		diagnosisDisplayFields.put("Date", "Data: ");
		diagnosisDisplayFields.put("Distance", "Distanza: ");
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

	private void setDiagnosisMapTip(Diagnosis diagnosis, float distance) {
		diagnosisDisplayFields.put("Diagnosis", "");
		diagnosisDisplayFields.put("Pathology", diagnosis.getPathology());
		diagnosisDisplayFields.put("Date", diagnosis.getDate());
		diagnosisDisplayFields.put("Distance", String.format("%.3f", distance) + " km");
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
		polygon = "";
		tabbedPane.removeAll();
		resultsSelectionMap.clear();
		occurrenceSelectionMap.clear();
		tipologySelectionMap.clear();
		resultsData.clear();
		occurrenceData.clear();
		tipologyData.clear();
		resultsTable = null;
		occurrenceDiagnosisTable = null;
		tipologyTable = null;
		counterResults = 0;
		countDiagnosis = 0;
		countPathology = 0;
		removeIcons();
	}

	private void removeIcons() {
		searchAreaLayer.removeAll();
		diagnosisLayer.removeAll();
		pointPSLayer.removeAll();
		linearPSLayer.removeAll();
		pAreaLayer.removeAll();
	}
}
