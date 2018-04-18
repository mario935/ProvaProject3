import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.postgis.PGgeometry;
import org.postgresql.util.PGobject;

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

public class AnalysisPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;

	private static final byte DIAGNOSIS = 1;
	private static final byte POINTPS = 2;
	private static final byte LINEARPS = 3;
	private static final byte PAREA = 4;
	private static final SimpleMarkerSymbol DIAGNOSISICON = MainWindow.DIAGNOSISICON;
	private static final SimpleMarkerSymbol DIAGNOSISICON2 = MainWindow.DIAGNOSISICON2;
	private static final SimpleMarkerSymbol POINTICON = MainWindow.POINTICON;
	private static final SimpleLineSymbol LINESYMBOL = MainWindow.LINESYMBOL;
	private static final SimpleLineSymbol POLYGONOUTLINE = MainWindow.POLYGONOUTLINE;
	private static final SimpleFillSymbol POLYGONFILL = MainWindow.POLYGONFILL;
	
	private JTabbedPane jTabbedPane;
	private JPanel diagnosisPanel;
	private JPanel pointPSPanel;
	private JPanel linearPSPanel;
	private JPanel pAreaPanel;

	private static final DistancePanel distancePanel = new DistancePanel();

	// Diagnosis fields
	private JComboBox<String> pathologyComboBox;
	private JDatePanelImpl dateDiagnosisPanel;
	private JDatePickerImpl dateDiagnosisPicker;
	private JButton cancelDiagnosisButton;
	private JButton diagnosisFilterButton;
	private JCheckBox diagnosisCheckBox;
	private JComboBox<String> dateDiagnosisComboBox;

	// PointPS fields
	private JFormattedTextField radiusFormattedTextField;
	private JComboBox<String> typePointComboBox;
	private JDatePanelImpl datePointStartPanel;
	private JDatePanelImpl datePointEndPanel;
	private JDatePickerImpl datePointStartPicker;
	private JDatePickerImpl datePointEndPicker;
	private JButton cancelPointButton;
	private JButton pointPSFilterButton;
	private JCheckBox pointPSCheckBox;
	private JComboBox<String> radiusChoice;
	private JComboBox<String> dateStartPointComboBox;
	private JComboBox<String> dateEndPointComboBox;

	// LinearPS fields
	private JDatePickerImpl dateLinearStartPicker;
	private JDatePickerImpl dateLinearEndPicker;
	private JComboBox<String> typeLinearComboBox;
	private JDatePanelImpl dateLinearStartPanel;
	private JDatePanelImpl dateLinearEndPanel;
	private JButton cancelLinearButton;
	private JButton linearPSFilterButton;
	private JCheckBox linearPSCheckBox;
	private JComboBox<String> dateStartLinearComboBox;
	private JComboBox<String> dateEndLinearComboBox;

	// PollutedArea fields
	private JDatePickerImpl dateAreaStartPicker;
	private JDatePickerImpl dateAreaEndPicker;
	private JDatePanelImpl dateAreaStartPanel;
	private JDatePanelImpl dateAreaEndPanel;
	private JComboBox<String> typeAreaComboBox;
	private JButton cancelAreaButton;
	private JButton pAreaFilterButton;
	private JCheckBox pAreaCheckBox;
	private JComboBox<String> dateStartAreaComboBox;
	private JComboBox<String> dateEndAreaComboBox;

	private MapTip diagnosisMapTip;
	private MapTip pointPSMapTip;
	private MapTip linearPSMapTip;
	private MapTip pSAreaMapTip;
	
	private LinkedHashMap diagnosisDisplayFields = new LinkedHashMap();
	private LinkedHashMap pointPSDisplayFields = new LinkedHashMap();
	private LinkedHashMap linearPSDisplayFields = new LinkedHashMap();
	private LinkedHashMap pAreaDisplayFields = new LinkedHashMap();

	private HashMap<Integer, Diagnosis> diagnosisGraphics = new HashMap<Integer, Diagnosis>();
	private HashMap<Integer, PointPollutionSource> pointPSGraphics = new HashMap<Integer, PointPollutionSource>();
	private HashMap<Integer, LinearPollutionSource> linearPSGraphics = new HashMap<Integer, LinearPollutionSource>();
	private HashMap<Integer, PollutedArea> pAreaGraphics = new HashMap<Integer, PollutedArea>();

	private Connection connection;

	private MainWindow mw;
	private SpatialReference mapSR;
	private Unit mapUnit;

	private GraphicsLayer diagnosisLayer;
	private GraphicsLayer pointPSLayer;
	private GraphicsLayer linearPSLayer;
	private GraphicsLayer pAreaLayer;

	private static AnalysisPanel instance;

	private static final String TABLE = "pollutionsources";

	//restituisce l'unica istanza della classe AnalysisPanel
	protected static synchronized AnalysisPanel getInstance() {
		if (instance == null) {
			instance = new AnalysisPanel();
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

	
	private AnalysisPanel() {
		initComponent();
	}

	//prende le informazioni relative alla diagnosi restituita dal resultSet
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
			// converto la geometria Punto del DB in quella di ArcGis
			org.postgis.Point postgisPoint = (org.postgis.Point) PGgeometry.geomFromString(geom.getValue());
			point = convertStringToPoint(postgisPoint.getValue());
			// inizializzo campi dell'oggetto
			diagnosis.setPathology(pathology);
			diagnosis.setDate(date.toString());
			diagnosis.setLatitude(point.getY());
			diagnosis.setLongitude(point.getX());
			// aggiorno MapTips
			setDiagnosisMapTip(diagnosis);
			// imposto la grafica
			pointGraphic = new Graphic(point, DIAGNOSISICON, diagnosisDisplayFields);
			mapID = diagnosisLayer.addGraphic(pointGraphic);
			pointGraphic = new Graphic(point, DIAGNOSISICON2, diagnosisDisplayFields);
			diagnosisLayer.addGraphic(pointGraphic);
			diagnosis.setMapID(mapID);
			diagnosisGraphics.put(mapID, diagnosis);
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(), "Errore nel caricamento di una diagnosi.\n" + ex.getMessage(), "Attenzione!", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}
	
	//prende le informazioni relative alla sorgente puntiforme restituita dal resultSet
	protected void getPointPS(ResultSet rs) {
		Graphic pointGraphic;
		Point point;
		PointPollutionSource pointPS = new PointPollutionSource();
		int mapID;
		try {
			// recupero dati dal DB
			String sourceType = rs.getString("sourcetype");
			Date startDate = rs.getDate("startdate");
			Date endDate = rs.getDate("enddate");
			int radius = rs.getInt("radius");
			PGobject geom = (PGobject) rs.getObject("pslocation");
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
			setPointPSMapTip(pointPS);
			// imposto la grafica
			pointGraphic = new Graphic(point, POINTICON, pointPSDisplayFields);
			pointPSLayer.addGraphic(pointGraphic);
			pointGraphic = getBuffer(point, POLYGONFILL, radius);
			mapID = pointPSLayer.addGraphic(pointGraphic);
			pointPS.setMapID(mapID);
			pointPSGraphics.put(mapID, pointPS);
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(),
					"Errore nel caricamento di una sorgente puntiforme.\n" + ex.getMessage(), "Attenzione!", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	//prende le informazioni relative alla sorgente lineare restituita dal resultSet
	protected void getLinearPS(ResultSet rs) {
		Graphic pointGraphic;
		Polyline line = new Polyline();
		LinearPollutionSource linearPS = new LinearPollutionSource();
		int mapID;
		try {
			// recupero dati dal DB
			String sourceType = rs.getString("sourcetype");
			Date startDate = rs.getDate("startdate");
			Date endDate = rs.getDate("enddate");
			PGobject geom = (PGobject) rs.getObject("pslocation");
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
			setLinearPSMapTip(linearPS);
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
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(),
					"Errore nel caricamento di una sorgente lineare.\n" + ex.getMessage(), "Attenzione!", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	//prende le informazioni relative all'area inquinata restituita dal resultSet
	protected void getPArea(ResultSet rs) {
		Graphic pointGraphic;
		Polygon polygon = new Polygon();
		Polyline line = new Polyline();
		PollutedArea pArea = new PollutedArea();
		int mapID;
		try {
			// recupero dati dal DB
			String sourceType = rs.getString("sourcetype");
			Date startDate = rs.getDate("startdate");
			Date endDate = rs.getDate("enddate");
			PGobject geom = (PGobject) rs.getObject("pslocation");
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
			setPAreaMapTip(pArea);
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
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(),
					"Errore nel caricamento di un'area inquinata.\n" + ex.getMessage(), "Attenzione!",
					JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	private void getFilteredDiagnosis() {
		Statement stmt = null;
		String table = "diagnosis";
		String whereClause = " ";
		String pathology = "";
		String date = " ";
		boolean whereActive = false;
		removeIcons(DIAGNOSIS);

		if (pathologyComboBox.getSelectedIndex() != 0) {
			pathology = pathology + pathologyComboBox.getSelectedItem().toString();
			whereActive = true;
			whereClause = " WHERE pathology = '" + pathology + "' ";
		}
		if (dateDiagnosisPicker != null && !dateDiagnosisPicker.getJFormattedTextField().getText().equals("")) {
			date = date + "'" + dateDiagnosisPicker.getJFormattedTextField().getText() + "'";

			switch (dateDiagnosisComboBox.getSelectedIndex()) {
			case 0: // il
				date = " =" + date;
				break;
			case 1: // prima del
				date = " <=" + date;
				break;
			case 2: // dopo del
				date = " >=" + date;
				break;
			}
			if (whereActive) {
				whereClause = whereClause + " AND " + " diagnosis_date  " + date + " ";
			} else {
				whereClause = " WHERE diagnosis_date  " + date + " ";
			}
		}
		System.out.println(table + whereClause);
		try {
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + whereClause);
			while (rs.next()) {
				getDiagnosis(rs);
			}
			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(), "Errore nel filtraggio.\n" + ex.getMessage(), "Attenzione!",	JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	protected void startFilteredQuery() {
		if (connection != null) {
			if (diagnosisCheckBox.isSelected()) {
				diagnosisGraphics.clear();
				getFilteredDiagnosis();
			}
			if (pointPSCheckBox.isSelected() || linearPSCheckBox.isSelected() || pAreaCheckBox.isSelected()) {
				getFilteredPollutionSources(pointPSCheckBox.isSelected(), linearPSCheckBox.isSelected(),	pAreaCheckBox.isSelected());
			}
		}
	}

	private void getFilteredPollutionSources(boolean pointPSSelected, boolean linearPSSelected, boolean pAreaSelected) {
		Statement stmt = null;
		String whereClause = "";

		if (pointPSSelected) {
			whereClause = " WHERE " + getPointPSField();
			pointPSGraphics.clear();
		}

		if (linearPSSelected) {
			if (whereClause.equals("")) {
				whereClause = " WHERE ";
			} else {
				whereClause = whereClause + " OR ";
			}
			whereClause = whereClause + getLinearPSField();
			linearPSGraphics.clear();
		}

		if (pAreaSelected) {
			if (whereClause.equals("")) {
				whereClause = " WHERE ";
			} else {
				whereClause = whereClause + " OR ";
			}
			whereClause = whereClause + getPAreaField();
			pAreaGraphics.clear();
		}
		System.out.println(TABLE + whereClause);
		try {
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + TABLE + whereClause);
			String pollutionType;
			while (rs.next()) {
				pollutionType = rs.getString("pollutiontype");
				if (pollutionType.equals("P")) {
					getPointPS(rs);
				} else if (pollutionType.equals("L")) {
					getLinearPS(rs);
				} else {
					getPArea(rs);
				}
			}
			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(), "Errore nel filtraggio.\n" + ex.getMessage(), "Attenzione!",	JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	private String getPointPSField() {
		String whereClause = " (pollutiontype = 'P' ";
		String type = "";
		String dateStart = " ";
		String dateEnd = " ";
		String radius = radiusFormattedTextField.getText();
		removeIcons(POINTPS);

		if (typePointComboBox.getSelectedIndex() != 0) {
			type = type + typePointComboBox.getSelectedItem().toString();
			whereClause = whereClause + " AND sourcetype = '" + type + "' ";
		}

		if (datePointStartPicker != null && !datePointStartPicker.getJFormattedTextField().getText().equals("")) {
			dateStart = dateStart + "'" + datePointStartPicker.getJFormattedTextField().getText() + "'";

			switch (dateStartPointComboBox.getSelectedIndex()) {
			case 0: // il
				dateStart = " =" + dateStart;
				break;
			case 1: // prima del
				dateStart = " <=" + dateStart;
				break;
			case 2: // dopo del
				dateStart = " >=" + dateStart;
				break;
			}
			whereClause = whereClause + " AND " + " startdate  " + dateStart + " ";
		}

		if (datePointEndPicker != null && !datePointEndPicker.getJFormattedTextField().getText().equals("")) {
			dateEnd = dateEnd + "'" + datePointEndPicker.getJFormattedTextField().getText() + "'";

			switch (dateEndPointComboBox.getSelectedIndex()) {
			case 0: // il
				dateEnd = " =" + dateEnd;
				break;
			case 1: // prima del
				dateEnd = " <=" + dateEnd;
				break;
			case 2: // dopo del
				dateEnd = " >=" + dateEnd + " OR enddate IS NULL";
				break;
			}
			whereClause = whereClause + " AND " + " (enddate  " + dateEnd + ")";
		}

		if (!radius.equals("")) {
			switch (radiusChoice.getSelectedIndex()) {
			case 0: // >=
				radius = " >=" + radius;
				break;
			case 1: // <=
				radius = " <=" + radius;
				break;
			case 2: // =
				radius = " =" + radius;
				break;
			}
			whereClause = whereClause + " AND " + " radius  " + radius + " ";
		}
		return whereClause + ")";
	}

	private String getLinearPSField() {
		String whereClause = " (pollutiontype = 'L' ";
		String type = "";
		String dateStart = " ";
		String dateEnd = " ";
		removeIcons(LINEARPS);

		if (typeLinearComboBox.getSelectedIndex() != 0) {
			type = type + typeLinearComboBox.getSelectedItem().toString();
			whereClause = whereClause + " AND sourcetype = '" + type + "' ";
		}

		if (dateLinearStartPicker != null && !dateLinearStartPicker.getJFormattedTextField().getText().equals("")) {
			dateStart = dateStart + "'" + dateLinearStartPicker.getJFormattedTextField().getText() + "'";

			switch (dateStartLinearComboBox.getSelectedIndex()) {
			case 0: // il
				dateStart = " =" + dateStart;
				break;
			case 1: // prima del
				dateStart = " <=" + dateStart;
				break;
			case 2: // dopo del
				dateStart = " >=" + dateStart;
				break;
			}
			whereClause = whereClause + " AND " + " startdate  " + dateStart + " ";
		}

		if (dateLinearEndPicker != null && !dateLinearEndPicker.getJFormattedTextField().getText().equals("")) {
			dateEnd = dateEnd + "'" + dateLinearEndPicker.getJFormattedTextField().getText() + "'";

			switch (dateEndLinearComboBox.getSelectedIndex()) {
			case 0: // il
				dateEnd = " =" + dateEnd;
				break;
			case 1: // prima del
				dateEnd = " <=" + dateEnd;
				break;
			case 2: // dopo del
				dateEnd = " >=" + dateEnd + " OR enddate IS NULL";
				break;
			}
			whereClause = whereClause + " AND " + " (enddate  " + dateEnd + ")";
		}
		return whereClause + ")";
	}

	private String getPAreaField() {
		String whereClause = " (pollutiontype = 'A' ";
		String type = "";
		String dateStart = " ";
		String dateEnd = " ";
		removeIcons(PAREA);

		if (typeAreaComboBox.getSelectedIndex() != 0) {
			type = type + typeAreaComboBox.getSelectedItem().toString();
			whereClause = whereClause + " AND sourcetype = '" + type + "' ";
		}

		if (dateAreaStartPicker != null && !dateAreaStartPicker.getJFormattedTextField().getText().equals("")) {
			dateStart = dateStart + "'" + dateAreaStartPicker.getJFormattedTextField().getText() + "'";

			switch (dateStartAreaComboBox.getSelectedIndex()) {
			case 0: // il
				dateStart = " =" + dateStart;
				break;
			case 1: // prima del
				dateStart = " <=" + dateStart;
				break;
			case 2: // dopo del
				dateStart = " >=" + dateStart;
				break;
			}
			whereClause = whereClause + " AND " + " startdate  " + dateStart + " ";
		}

		if (dateAreaEndPicker != null && !dateAreaEndPicker.getJFormattedTextField().getText().equals("")) {
			dateEnd = dateEnd + "'" + dateAreaEndPicker.getJFormattedTextField().getText() + "'";

			switch (dateEndAreaComboBox.getSelectedIndex()) {
			case 0: // il
				dateEnd = " =" + dateEnd;
				break;
			case 1: // prima del
				dateEnd = " <=" + dateEnd;
				break;
			case 2: // dopo del
				dateEnd = " >=" + dateEnd + " OR enddate IS NULL";
				break;
			}
			whereClause = whereClause + " AND " + " (enddate  " + dateEnd + ")";
		}
		return whereClause + ")";
	}

	protected void startSelection(Point point) {
		Point location = null;
		Polygon buffer = null;
		AnalizePollutionSourceFrame aps = AnalizePollutionSourceFrame.getInstance();
		AnalizeDiagnosisFrame adf = AnalizeDiagnosisFrame.getInstance();

		GraphicsLayer layer;
		int mapID;
		buffer = GeometryEngine.buffer(point, mapSR, 1000, mapUnit); //crea un buffer intorno al punto cliccato
		layer = diagnosisLayer;
		for (Diagnosis d : diagnosisGraphics.values()) {
			location = new Point(d.getLongitude(), d.getLatitude());
			if (GeometryEngine.intersects(buffer, location, mapSR) || GeometryEngine.contains(buffer, location, mapSR)) {
				mapID = d.getMapID();
				if (layer.isGraphicSelected(mapID)) {
					layer.unselect(mapID);
				} else {
					layer.select(mapID);
					layer.getMapTip().setEnabled(false);
					int decision = JOptionPane.showConfirmDialog(mw.getFrame(), distancePanel, "Iniziare analisi diagnosi?", JOptionPane.YES_NO_OPTION);
					layer.unselect(mapID);
					switch (decision) {
					case JOptionPane.YES_OPTION:
						closePanel();
						adf.setVisible(true);
						adf.startAnalysis(d, distancePanel.getInfluenceRadius());
						adf.setLocation(1, 200);
						break;
					case JOptionPane.NO_OPTION:
					case JOptionPane.CLOSED_OPTION:
						distancePanel.resetInfluenceRadiusText();
						break;
					}
				}
				if (!layer.getMapTip().isEnabled()) {
					layer.getMapTip().setEnabled(true);
				}
				return; // trovato un elemento esce
			}
		}

		layer = linearPSLayer;
		for (LinearPollutionSource l : linearPSGraphics.values()) {
			Polygon bufferLocation = GeometryEngine.buffer(l.getPolyline(), mapSR, 1000, mapUnit);
			if (GeometryEngine.intersects(buffer, bufferLocation, mapSR) || GeometryEngine.contains(bufferLocation, buffer, mapSR)) {
				mapID = l.getMapID();
				if (layer.isGraphicSelected(mapID)) {
					layer.unselect(mapID);
				} else {
					layer.select(mapID);
					layer.getMapTip().setEnabled(false);
					int decision = JOptionPane.showConfirmDialog(mw.getFrame(), distancePanel, "Iniziare analisi sorgente?", JOptionPane.YES_NO_OPTION);
					layer.unselect(mapID);
					switch (decision) {
					case JOptionPane.YES_OPTION:
						closePanel();
						aps.setVisible(true);
						aps.startAnalysis(l, distancePanel.getInfluenceRadius());
						aps.setLocation(1, 200);
						break;
					case JOptionPane.NO_OPTION:
					case JOptionPane.CLOSED_OPTION:
						distancePanel.resetInfluenceRadiusText();
						break;
					}
				}
				if (!layer.getMapTip().isEnabled()) {
					layer.getMapTip().setEnabled(true);
				}
				return; // trovato un elemento esce
			}
		}

		layer = pointPSLayer;
		for (PointPollutionSource p : pointPSGraphics.values()) {
			location = new Point(p.getLongitude(), p.getLatitude());
			Polygon bufferLocation = GeometryEngine.buffer(location, mapSR, p.getRadius(), mapUnit);
			if (GeometryEngine.intersects(buffer, bufferLocation, mapSR) || GeometryEngine.contains(bufferLocation, buffer, mapSR)) {
				mapID = p.getMapID();
				if (layer.isGraphicSelected(mapID)) {
					layer.unselect(mapID);
				} else {
					layer.select(mapID);
					layer.getMapTip().setEnabled(false);
					int decision = JOptionPane.showConfirmDialog(mw.getFrame(), distancePanel,	"Iniziare analisi sorgente?", JOptionPane.YES_NO_OPTION);
					layer.unselect(mapID);
					switch (decision) {
					case JOptionPane.YES_OPTION:
						closePanel();
						aps.setVisible(true);
						aps.startAnalysis(p, distancePanel.getInfluenceRadius());
						aps.setLocation(1, 200);
						break;
					case JOptionPane.NO_OPTION:
					case JOptionPane.CLOSED_OPTION:
						distancePanel.resetInfluenceRadiusText();
						break;
					}
				}
				if (!layer.getMapTip().isEnabled()) {
					layer.getMapTip().setEnabled(true);
				}
				return; // trovato un elemento esce
			}
		}

		layer = pAreaLayer;
		for (PollutedArea a : pAreaGraphics.values()) {
			Polygon bufferLocation = GeometryEngine.buffer(a.getPolygon(), mapSR, 1, mapUnit);
			if (GeometryEngine.intersects(buffer, bufferLocation, mapSR) || GeometryEngine.contains(bufferLocation, buffer, mapSR) || GeometryEngine.contains(buffer, bufferLocation, mapSR)) {
				mapID = a.getMapID();
				if (layer.isGraphicSelected(mapID)) {
					layer.unselect(mapID);
				} else {
					layer.select(mapID);
					layer.getMapTip().setEnabled(false);
					int decision = JOptionPane.showConfirmDialog(mw.getFrame(), distancePanel,	"Iniziare analisi sorgente?", JOptionPane.YES_NO_OPTION);
					layer.unselect(mapID);
					switch (decision) {
					case JOptionPane.YES_OPTION:
						closePanel();
						aps.setVisible(true);
						aps.startAnalysis(a, distancePanel.getInfluenceRadius());
						aps.setLocation(1, 200);
						break;
					case JOptionPane.NO_OPTION:
					case JOptionPane.CLOSED_OPTION:
						distancePanel.resetInfluenceRadiusText();
						break;
					}
				}
				if (!layer.getMapTip().isEnabled()) {
					layer.getMapTip().setEnabled(true);
				}
				return; // trovato un elemento esce
			}
		}
	}

	//proietta le coordinate di un punto sulla mappa con il relativo map Spatial Reference
	private Point projectCoordinateToMap(Point point) {
		return GeometryEngine.project(point.getX(), point.getY(), mapSR);
	}

	//converto delle coordinate di latitudine e longitudine in coordinate di un Punto per la mappa
	private Point convertStringToPoint(String coordinates) {
		String latitude = "", longitude = "";
		int whitespace;
		whitespace = coordinates.indexOf(' ');

		latitude = coordinates.substring(1, whitespace);
		longitude = coordinates.substring(whitespace, coordinates.length() - 1);

		return projectCoordinateToMap(new Point(Double.parseDouble(longitude), Double.parseDouble(latitude)));
	}

	//restituisce l'immagine del buffer costruito intorno ad un punto con raggio = "distance" e come simbolo di riempimento "fill"
	private Graphic getBuffer(Point point, SimpleFillSymbol fill, double distance) {
		return new Graphic(GeometryEngine.buffer(point, mapSR, distance, mapUnit), fill, pointPSDisplayFields);
	}
	
	//inizializza le componenti del frame
	private void initComponent() {
		setOpaque(false);
		jTabbedPane = new JTabbedPane();

		GroupLayout layout = new GroupLayout(this);
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addComponent(jTabbedPane, GroupLayout.PREFERRED_SIZE, 224, GroupLayout.PREFERRED_SIZE)
						.addContainerGap(30, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(jTabbedPane,
				GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE));
		setLayout(layout);

		diagnosisPanel = new JPanel();
		diagnosisPanel.setBackground(Color.WHITE);
		jTabbedPane.addTab("Diagnosi", null, diagnosisPanel, null);
		diagnosisPanel.setLayout(null);

		pointPSPanel = new JPanel();
		pointPSPanel.setBackground(Color.WHITE);
		jTabbedPane.addTab("Sorgenti Puntiformi", null, pointPSPanel, null);
		pointPSPanel.setLayout(null);

		linearPSPanel = new JPanel();
		linearPSPanel.setBackground(Color.WHITE);
		jTabbedPane.addTab("Sorgenti Lineari", null, linearPSPanel, null);
		linearPSPanel.setLayout(null);

		pAreaPanel = new JPanel();
		pAreaPanel.setBackground(Color.WHITE);
		jTabbedPane.addTab("Aree Inquinate", null, pAreaPanel, null);
		pAreaPanel.setLayout(null);

		for (int i = 0; i < jTabbedPane.getTabCount(); i++) {
			jTabbedPane.setBackgroundAt(i, Color.WHITE);
		}

		initDiagnosisPart();
		initPointPSPart();
		initLinearPSPart();
		initPAreaPart();

		initPanelListeners();
		initButtonListeners();
		initCheckBoxListeners();
		initDateComboBoxes();
	}

	private void initCheckBoxListeners() {
		diagnosisCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				enableDiagnosisFields();
				if (diagnosisCheckBox.isSelected()) {
					if (connection != null) {
						Statement stmt = null;
						try {
							stmt = connection.createStatement();
							ResultSet rs = stmt.executeQuery("SELECT * FROM diagnosis;");
							while (rs.next()) {
								getDiagnosis(rs);
							}
							rs.close();
							stmt.close();
						} catch (SQLException ex) {
							JOptionPane.showMessageDialog(getParent(),	"Errore nel caricamento delle diagnosi.\n" + ex.getMessage(), "Attenzione!", JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();
						}
					}
				} else {
					cleanDiagnosisFields();
					removeIcons(DIAGNOSIS);
				}
			}
		});

		pointPSCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				enablePointPSFields();
				if (pointPSCheckBox.isSelected()) {
					if (connection != null) {
						Statement stmt = null;
						try {
							stmt = connection.createStatement();
							ResultSet rs = stmt
									.executeQuery("SELECT * FROM pollutionsources where pollutiontype = 'P';");
							while (rs.next()) {
								getPointPS(rs);
							}
							rs.close();
							stmt.close();
						} catch (SQLException ex) {
							JOptionPane.showMessageDialog(getParent(), "Errore nel caricamento delle sorgenti puntiformi.\n" + ex.getMessage(),	"Attenzione!", JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();
						}
					}
				} else {
					cleanPointPSFields();
					removeIcons(POINTPS);
				}
			}
		});

		linearPSCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				enableLinearPSFields();
				if (linearPSCheckBox.isSelected()) {
					if (connection != null) {
						Statement stmt = null;
						try {
							stmt = connection.createStatement();
							ResultSet rs = stmt
									.executeQuery("SELECT * FROM pollutionsources where pollutiontype = 'L';");
							while (rs.next()) {
								getLinearPS(rs);
							}
							rs.close();
							stmt.close();
						} catch (SQLException ex) {
							JOptionPane.showMessageDialog(getParent(), "Errore nel caricamento delle sorgenti lineari.\n" + ex.getMessage(), "Attenzione!",	JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();
						}
					}
				} else {
					cleanLinearPSFields();
					removeIcons(LINEARPS);
				}
			}
		});

		pAreaCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				enablePAreaFields();
				if (pAreaCheckBox.isSelected()) {
					if (connection != null) {
						Statement stmt = null;
						try {
							stmt = connection.createStatement();
							ResultSet rs = stmt
									.executeQuery("SELECT * FROM pollutionsources where pollutiontype = 'A';");
							while (rs.next()) {
								getPArea(rs);
							}
							rs.close();
							stmt.close();
						} catch (SQLException ex) {
							JOptionPane.showMessageDialog(getParent(),	"Errore nel caricamento delle aree inquinate.\n" + ex.getMessage(), "Attenzione!",	JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();
						}
					}
				} else {
					cleanPAreaFields();
					removeIcons(PAREA);
				}
			}
		});
	}

	protected void initMapTip() {
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
		pointPSMapTip = new MapTip(pointPSDisplayFields);

		pointPSLayer.setMapTip(pointPSMapTip);

		linearPSDisplayFields.put("LPS", "<u>Sorgente Lineare</u>"); // titolo
		linearPSDisplayFields.put("Type", "Tipo: ");
		linearPSDisplayFields.put("StartDate", "Data Inizio: ");
		linearPSDisplayFields.put("EndDate", "Data Fine: ");
		linearPSMapTip = new MapTip(linearPSDisplayFields);

		linearPSLayer.setMapTip(linearPSMapTip);

		pAreaDisplayFields.put("PA", "<u>Area Inquinata</u>"); // titolo
		pAreaDisplayFields.put("Type", "Tipo: ");
		pAreaDisplayFields.put("StartDate", "Data Inizio: ");
		pAreaDisplayFields.put("EndDate", "Data Fine: ");
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

	private void setPointPSMapTip(PointPollutionSource pointPS) {
		pointPSDisplayFields.put("PPS", "");
		pointPSDisplayFields.put("Type", pointPS.getType());
		pointPSDisplayFields.put("StartDate", pointPS.getStartDate());
		pointPSDisplayFields.put("EndDate", pointPS.getEndDate());
		pointPSDisplayFields.put("Radius", pointPS.getRadius());
	}

	private void setLinearPSMapTip(LinearPollutionSource linearPS) {
		linearPSDisplayFields.put("LPS", "");
		linearPSDisplayFields.put("Type", linearPS.getType());
		linearPSDisplayFields.put("StartDate", linearPS.getStartDate());
		linearPSDisplayFields.put("EndDate", linearPS.getEndDate());
	}

	private void setPAreaMapTip(PollutedArea pArea) {
		pAreaDisplayFields.put("PA", "");
		pAreaDisplayFields.put("Type", pArea.getType());
		pAreaDisplayFields.put("StartDate", pArea.getStartDate());
		pAreaDisplayFields.put("EndDate", pArea.getEndDate());
	}

	private void initPAreaPart() {
		pAreaFilterButton = new JButton("Filtra risultati");
		pAreaFilterButton.setBounds(15, 355, 108, 26);
		pAreaPanel.add(pAreaFilterButton);

		pAreaCheckBox = new JCheckBox("Visualizza sulla mappa");
		pAreaCheckBox.setBackground(Color.WHITE);
		pAreaCheckBox.setBounds(6, 7, 230, 23);
		pAreaPanel.add(pAreaCheckBox);

		typeAreaComboBox = new JComboBox<String>();
		typeAreaComboBox.setBorder(new TitledBorder(null, "Tipo", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		typeAreaComboBox.setBackground(Color.WHITE);
		typeAreaComboBox.setBounds(6, 38, 176, 47);
		typeAreaComboBox.addItem("Tutti");
		pAreaPanel.add(typeAreaComboBox);

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
		dateAreaStartPanel = new JDatePanelImpl(model1, p);
		dateAreaStartPicker = new JDatePickerImpl(dateAreaStartPanel, new DateLabelFormatter());
		dateAreaStartPicker.setBorder(new TitledBorder(null, "Data Inizio", TitledBorder.LEADING, TitledBorder.TOP,	null, new Color(0, 0, 0)));
		dateAreaStartPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateAreaStartPicker.setBackground(Color.WHITE);
		dateAreaStartPicker.setBounds(6, 135, 176, 47);
		dateAreaStartPicker.setVisible(true);
		pAreaPanel.add(dateAreaStartPicker);
		dateAreaEndPanel = new JDatePanelImpl(model2, p);
		dateAreaEndPicker = new JDatePickerImpl(dateAreaEndPanel, new DateLabelFormatter());
		dateAreaEndPicker.setBorder(new TitledBorder(null, "Data Fine", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateAreaEndPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateAreaEndPicker.setBackground(Color.WHITE);
		dateAreaEndPicker.setBounds(6, 237, 176, 47);
		dateAreaEndPicker.setVisible(true);
		pAreaPanel.add(dateAreaEndPicker);

		cancelAreaButton = new JButton("Chiudi");
		cancelAreaButton.setBounds(134, 355, 77, 26);
		pAreaPanel.add(cancelAreaButton);

		dateStartAreaComboBox = new JComboBox<String>();
		dateStartAreaComboBox.setBorder(new TitledBorder(null, "Quando", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateStartAreaComboBox.setBackground(Color.WHITE);
		dateStartAreaComboBox.setBounds(6, 85, 90, 50);
		pAreaPanel.add(dateStartAreaComboBox);

		dateEndAreaComboBox = new JComboBox<String>();
		dateEndAreaComboBox.setBorder(new TitledBorder(null, "Quando", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateEndAreaComboBox.setBackground(Color.WHITE);
		dateEndAreaComboBox.setBounds(6, 187, 90, 50);
		pAreaPanel.add(dateEndAreaComboBox);
	}

	private void initLinearPSPart() {
		linearPSCheckBox = new JCheckBox("Visualizza sulla mappa");
		linearPSCheckBox.setBackground(Color.WHITE);
		linearPSCheckBox.setBounds(6, 7, 230, 23);
		linearPSPanel.add(linearPSCheckBox);

		linearPSFilterButton = new JButton("Filtra risultati");
		linearPSFilterButton.setBounds(15, 355, 108, 26);
		linearPSPanel.add(linearPSFilterButton);

		typeLinearComboBox = new JComboBox<String>();
		typeLinearComboBox.setBackground(Color.WHITE);
		typeLinearComboBox.setBorder(new TitledBorder(null, "Tipo", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		typeLinearComboBox.setBounds(6, 38, 176, 47);
		typeLinearComboBox.addItem("Tutti");
		linearPSPanel.add(typeLinearComboBox);

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
		dateLinearStartPanel = new JDatePanelImpl(model1, p);
		dateLinearStartPicker = new JDatePickerImpl(dateLinearStartPanel, new DateLabelFormatter());
		dateLinearStartPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateLinearStartPicker.setBackground(Color.WHITE);
		dateLinearStartPicker.setBorder(new TitledBorder(null, "Data Inizio", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateLinearStartPicker.setBounds(6, 135, 176, 47);
		dateLinearStartPicker.setVisible(true);
		linearPSPanel.add(dateLinearStartPicker);
		dateLinearEndPanel = new JDatePanelImpl(model2, p);
		dateLinearEndPicker = new JDatePickerImpl(dateLinearEndPanel, new DateLabelFormatter());
		dateLinearEndPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateLinearEndPicker.setBackground(Color.WHITE);
		dateLinearEndPicker.setBorder(new TitledBorder(null, "Data Fine", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateLinearEndPicker.setBounds(6, 237, 176, 47);
		dateLinearEndPicker.setVisible(true);
		linearPSPanel.add(dateLinearEndPicker);

		cancelLinearButton = new JButton("Chiudi");
		cancelLinearButton.setBounds(134, 355, 77, 26);
		linearPSPanel.add(cancelLinearButton);

		dateStartLinearComboBox = new JComboBox<String>();
		dateStartLinearComboBox.setBorder(new TitledBorder(null, "Quando", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateStartLinearComboBox.setBackground(Color.WHITE);
		dateStartLinearComboBox.setBounds(6, 85, 90, 50);
		linearPSPanel.add(dateStartLinearComboBox);

		dateEndLinearComboBox = new JComboBox<String>();
		dateEndLinearComboBox.setBorder(new TitledBorder(null, "Quando", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateEndLinearComboBox.setBackground(Color.WHITE);
		dateEndLinearComboBox.setBounds(6, 187, 90, 50);
		linearPSPanel.add(dateEndLinearComboBox);
		}

	private void initPointPSPart() {
		pointPSFilterButton = new JButton("Filtra risultati");
		pointPSFilterButton.setBounds(15, 355, 108, 26);
		pointPSPanel.add(pointPSFilterButton);

		pointPSCheckBox = new JCheckBox("Visualizza sulla mappa");
		pointPSCheckBox.setBackground(Color.WHITE);
		pointPSCheckBox.setBounds(6, 7, 230, 23);
		pointPSPanel.add(pointPSCheckBox);

		typePointComboBox = new JComboBox<String>();
		typePointComboBox.setBackground(Color.WHITE);
		typePointComboBox.setBounds(6, 38, 176, 47);
		typePointComboBox.addItem("Tutti");
		typePointComboBox.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Tipo", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(0, 0, 0)));
		pointPSPanel.add(typePointComboBox);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMaximumIntegerDigits(6);
		nf.setMaximumFractionDigits(0);
		radiusFormattedTextField = new JFormattedTextField(nf);
		radiusFormattedTextField.setBackground(Color.WHITE);
		radiusFormattedTextField.setBorder(new TitledBorder(null, "Raggio", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		radiusFormattedTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		radiusFormattedTextField.setValue(null);
		radiusFormattedTextField.setBounds(92, 296, 74, 39);
		radiusFormattedTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				if (radiusFormattedTextField.getText().equals("")) {
					radiusFormattedTextField.setValue(null);
				}
			}

			@Override
			public void focusGained(FocusEvent arg0) {

			}
		});
		pointPSPanel.add(radiusFormattedTextField);

		UtilDateModel model1 = new UtilDateModel();
		UtilDateModel model2 = new UtilDateModel();
		Properties p = new Properties();
		p.put("text.today", "Oggi");
		p.put("text.month", "Mese");
		p.put("text.year", "Anno");
		datePointStartPanel = new JDatePanelImpl(model1, p);
		datePointStartPicker = new JDatePickerImpl(datePointStartPanel, new DateLabelFormatter());
		datePointStartPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		datePointStartPicker.setBackground(Color.WHITE);
		datePointStartPicker.setBorder(new TitledBorder(null, "Data Inizio", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		datePointStartPicker.setBounds(6, 135, 176, 47);
		datePointStartPicker.setVisible(true);
		pointPSPanel.add(datePointStartPicker);
		datePointEndPanel = new JDatePanelImpl(model2, p);
		datePointEndPicker = new JDatePickerImpl(datePointEndPanel, new DateLabelFormatter());
		datePointEndPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		datePointEndPicker.setBackground(Color.WHITE);
		datePointEndPicker.setBorder(new TitledBorder(null, "Data Fine", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		datePointEndPicker.setBounds(6, 237, 176, 47);
		datePointEndPicker.setVisible(true);
		pointPSPanel.add(datePointEndPicker);

		radiusChoice = new JComboBox<String>();
		radiusChoice.setBackground(Color.WHITE);
		radiusChoice.setBorder(new TitledBorder(null, "Influenza", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		radiusChoice.setBounds(6, 292, 74, 47);
		radiusChoice.addItem("\u2265"); // simbolo >=
		radiusChoice.addItem("\u2264"); // simbolo <=
		radiusChoice.addItem("=");
		pointPSPanel.add(radiusChoice);

		JLabel lblM = new JLabel("m");
		lblM.setBounds(167, 314, 40, 16);
		pointPSPanel.add(lblM);

		cancelPointButton = new JButton("Chiudi");
		cancelPointButton.setBounds(134, 355, 77, 26);
		pointPSPanel.add(cancelPointButton);

		dateStartPointComboBox = new JComboBox<String>();
		dateStartPointComboBox.setBorder(new TitledBorder(null, "Quando", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateStartPointComboBox.setBackground(Color.WHITE);
		dateStartPointComboBox.setBounds(6, 85, 90, 50);
		pointPSPanel.add(dateStartPointComboBox);

		dateEndPointComboBox = new JComboBox<String>();
		dateEndPointComboBox.setBorder(new TitledBorder(null, "Quando", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateEndPointComboBox.setBackground(Color.WHITE);
		dateEndPointComboBox.setBounds(6, 187, 90, 50);
		pointPSPanel.add(dateEndPointComboBox);
		
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

			}

			@Override
			public void keyPressed(KeyEvent e) {

			}
		});
	}

	private void initDiagnosisPart() {
		diagnosisCheckBox = new JCheckBox("Visualizza sulla mappa");
		diagnosisCheckBox.setBackground(Color.WHITE);
		diagnosisCheckBox.setBounds(6, 7, 230, 23);
		diagnosisPanel.add(diagnosisCheckBox);

		diagnosisFilterButton = new JButton("Filtra risultati");
		diagnosisFilterButton.setBounds(15, 355, 108, 26);
		diagnosisPanel.add(diagnosisFilterButton);

		UtilDateModel model = new UtilDateModel();
		Properties p = new Properties();
		p.put("text.today", "Oggi");
		p.put("text.month", "Mese");
		p.put("text.year", "Anno");
		dateDiagnosisPanel = new JDatePanelImpl(model, p);

		pathologyComboBox = new JComboBox<String>();
		pathologyComboBox.setBackground(Color.WHITE);
		pathologyComboBox.setBorder(new TitledBorder(null, "Patologia", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pathologyComboBox.setBounds(6, 38, 160, 47);
		pathologyComboBox.addItem("Tutte");
		diagnosisPanel.add(pathologyComboBox);

		cancelDiagnosisButton = new JButton("Chiudi");
		cancelDiagnosisButton.setBounds(134, 355, 77, 26);
		diagnosisPanel.add(cancelDiagnosisButton);
		dateDiagnosisPicker = new JDatePickerImpl(dateDiagnosisPanel, new DateLabelFormatter());
		dateDiagnosisPicker.setBounds(6, 135, 174, 49);
		diagnosisPanel.add(dateDiagnosisPicker);
		dateDiagnosisPicker.getJFormattedTextField().setHorizontalAlignment(SwingConstants.CENTER);
		dateDiagnosisPicker.setBackground(Color.WHITE);
		dateDiagnosisPicker.setBorder(new TitledBorder(null, "Data", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateDiagnosisPicker.setVisible(true);

		dateDiagnosisComboBox = new JComboBox<String>();
		dateDiagnosisComboBox.setBorder(new TitledBorder(null, "Quando", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dateDiagnosisComboBox.setBounds(6, 85, 90, 50);
		diagnosisPanel.add(dateDiagnosisComboBox);
		dateDiagnosisComboBox.setBackground(Color.WHITE);
	}

	private void initDateComboBoxes() {
		dateDiagnosisComboBox.addItem("il");
		dateDiagnosisComboBox.addItem("prima del");
		dateDiagnosisComboBox.addItem("dopo del");

		dateStartPointComboBox.addItem("il");
		dateStartPointComboBox.addItem("prima del");
		dateStartPointComboBox.addItem("dopo del");
		dateEndPointComboBox.addItem("il");
		dateEndPointComboBox.addItem("prima del");
		dateEndPointComboBox.addItem("dopo del");

		dateStartLinearComboBox.addItem("il");
		dateStartLinearComboBox.addItem("prima del");
		dateStartLinearComboBox.addItem("dopo del");
		dateEndLinearComboBox.addItem("il");
		dateEndLinearComboBox.addItem("prima del");
		dateEndLinearComboBox.addItem("dopo del");

		dateStartAreaComboBox.addItem("il");
		dateStartAreaComboBox.addItem("prima del");
		dateStartAreaComboBox.addItem("dopo del");
		dateEndAreaComboBox.addItem("il");
		dateEndAreaComboBox.addItem("prima del");
		dateEndAreaComboBox.addItem("dopo del");
	}

	protected void addAreaTypesToComboBox() {
		//recupero i tipi di aree inquinate 
		LoadComboBoxItems.addPATypeItems(typeAreaComboBox);
	}

	protected void addLinearTypesToComboBox() {
		// recupero i tipi di sorgenti lineari 
		LoadComboBoxItems.addLinearPSTypeItems(typeLinearComboBox);
	}

	protected void addPointTypesToComboBox() {
		// recupero i tipi di sorgenti puntiformi
		LoadComboBoxItems.addPointPSTypeItems(typePointComboBox);
	}

	protected void addPathologiesTypesToComboBox() {
		// recupero le patologie
		LoadComboBoxItems.addPathologyItems(pathologyComboBox);
	}

	private void initButtonListeners() {
		cancelDiagnosisButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();
			}
		});

		cancelPointButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();
			}
		});

		cancelLinearButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();
			}
		});

		cancelAreaButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				closePanel();
			}
		});

		diagnosisFilterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				startFilteredQuery();
			}
		});

		pointPSFilterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				startFilteredQuery();
			}
		});

		linearPSFilterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				startFilteredQuery();
			}
		});

		pAreaFilterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				startFilteredQuery();
			}
		});
	}

	private void enableDiagnosisFields() {
		pathologyComboBox.setEnabled(true);
		dateDiagnosisPanel.setEnabled(true);
		dateDiagnosisComboBox.setEnabled(true);
		for (Component c : dateDiagnosisPicker.getComponents()) {
			c.setEnabled(true);
		}
	}

	private void enablePointPSFields() {
		typePointComboBox.setEnabled(true);
		datePointStartPanel.setEnabled(true);
		datePointEndPanel.setEnabled(true);
		dateStartPointComboBox.setEnabled(true);
		dateEndPointComboBox.setEnabled(true);
		radiusChoice.setEnabled(true);
		radiusFormattedTextField.setEnabled(true);
		for (Component c : datePointStartPicker.getComponents()) {
			c.setEnabled(true);
		}
		for (Component c : datePointEndPicker.getComponents()) {
			c.setEnabled(true);
		}
	}

	private void enableLinearPSFields() {
		dateLinearStartPanel.setEnabled(true);
		dateLinearEndPanel.setEnabled(true);
		dateStartLinearComboBox.setEnabled(true);
		dateEndLinearComboBox.setEnabled(true);
		typeLinearComboBox.setEnabled(true);
		for (Component c : dateLinearStartPicker.getComponents()) {
			c.setEnabled(true);
		}
		for (Component c : dateLinearEndPicker.getComponents()) {
			c.setEnabled(true);
		}
	}

	private void enablePAreaFields() {
		dateAreaStartPanel.setEnabled(true);
		dateAreaEndPanel.setEnabled(true);
		dateStartAreaComboBox.setEnabled(true);
		dateEndAreaComboBox.setEnabled(true);
		typeAreaComboBox.setEnabled(true);
		for (Component c : dateAreaStartPicker.getComponents()) {
			c.setEnabled(true);
		}
		for (Component c : dateAreaEndPicker.getComponents()) {
			c.setEnabled(true);
		}
	}

	private void cleanDiagnosisFields() {
		pathologyComboBox.setSelectedIndex(0);
		dateDiagnosisPicker.getJFormattedTextField().setText("");
		diagnosisCheckBox.setSelected(false);
		dateDiagnosisComboBox.setSelectedIndex(0);

		pathologyComboBox.setEnabled(false);
		dateDiagnosisPanel.setEnabled(false);
		dateDiagnosisComboBox.setEnabled(false);
		for (Component c : dateDiagnosisPicker.getComponents()) {
			c.setEnabled(false);
		}
		diagnosisDisplayFields.clear();
		diagnosisGraphics.clear();

	}

	private void cleanPointPSFields() {
		typePointComboBox.setSelectedIndex(0);
		datePointStartPicker.getJFormattedTextField().setText("");
		datePointEndPicker.getJFormattedTextField().setText("");
		pointPSCheckBox.setSelected(false);
		radiusChoice.setSelectedIndex(0);
		radiusFormattedTextField.setValue(null);
		dateStartPointComboBox.setSelectedIndex(0);
		dateEndPointComboBox.setSelectedIndex(0);

		typePointComboBox.setEnabled(false);
		datePointStartPanel.setEnabled(false);
		datePointEndPanel.setEnabled(false);
		dateStartPointComboBox.setEnabled(false);
		dateEndPointComboBox.setEnabled(false);
		radiusChoice.setEnabled(false);
		radiusFormattedTextField.setEnabled(false);
		for (Component c : datePointStartPicker.getComponents()) {
			c.setEnabled(false);
		}
		for (Component c : datePointEndPicker.getComponents()) {
			c.setEnabled(false);
		}
		pointPSDisplayFields.clear();
		pointPSGraphics.clear();
	}

	private void cleanLinearPSFields() {
		dateLinearStartPicker.getJFormattedTextField().setText("");
		dateLinearEndPicker.getJFormattedTextField().setText("");
		typeLinearComboBox.setSelectedIndex(0);
		linearPSCheckBox.setSelected(false);
		dateStartLinearComboBox.setSelectedIndex(0);
		dateEndLinearComboBox.setSelectedIndex(0);

		dateLinearStartPanel.setEnabled(false);
		dateLinearEndPanel.setEnabled(false);
		dateStartLinearComboBox.setEnabled(false);
		dateEndLinearComboBox.setEnabled(false);
		typeLinearComboBox.setEnabled(false);
		for (Component c : dateLinearStartPicker.getComponents()) {
			c.setEnabled(false);
		}
		for (Component c : dateLinearEndPicker.getComponents()) {
			c.setEnabled(false);
		}
		linearPSDisplayFields.clear();
		linearPSGraphics.clear();
	}

	private void cleanPAreaFields() {
		dateAreaStartPicker.getJFormattedTextField().setText("");
		dateAreaEndPicker.getJFormattedTextField().setText("");
		typeAreaComboBox.setSelectedIndex(0);
		pAreaCheckBox.setSelected(false);
		dateStartAreaComboBox.setSelectedIndex(0);
		dateEndAreaComboBox.setSelectedIndex(0);

		dateAreaStartPanel.setEnabled(false);
		dateAreaEndPanel.setEnabled(false);
		dateStartAreaComboBox.setEnabled(false);
		dateEndAreaComboBox.setEnabled(false);
		typeAreaComboBox.setEnabled(false);
		for (Component c : dateAreaStartPicker.getComponents()) {
			c.setEnabled(false);
		}
		for (Component c : dateAreaEndPicker.getComponents()) {
			c.setEnabled(false);
		}
		pAreaDisplayFields.clear();
		pAreaGraphics.clear();
	}

	protected void cleanFields() {
		cleanDiagnosisFields();
		cleanPointPSFields();
		cleanLinearPSFields();
		cleanPAreaFields();
		jTabbedPane.setSelectedIndex(0);
		removeIcons(0);
	}

	protected void closePanel() {
		cleanFields();
		setVisible(false);
		mw.resetAnalysisComboBox();
	}

	private void removeIcons(int selected) {
		if (mw != null) {
			switch (selected) {
			case DIAGNOSIS:
				diagnosisLayer.removeAll();
				break;
			case POINTPS:
				pointPSLayer.removeAll();
				break;
			case LINEARPS:
				linearPSLayer.removeAll();
				break;
			case PAREA:
				pAreaLayer.removeAll();
				break;
			default:
				diagnosisLayer.removeAll();
				pointPSLayer.removeAll();
				linearPSLayer.removeAll();
				pAreaLayer.removeAll();
				break;
			}
		}
	}

	private void initPanelListeners() {
		diagnosisPanel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

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
		pointPSPanel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

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
		linearPSPanel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

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
		pAreaPanel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

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
	}
}
