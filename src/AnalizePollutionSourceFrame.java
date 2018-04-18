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
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.map.GraphicsLayer;
import com.esri.map.MapTip;

public class AnalizePollutionSourceFrame extends JInternalFrame {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private GraphicsLayer diagnosisLayer;
	private GraphicsLayer pointPSLayer;
	private GraphicsLayer linearPSLayer;
	private GraphicsLayer pAreaLayer;
	private MainWindow mw;
	private SpatialReference mapSR;
	private Connection connection;

	private HashMap<Integer, Diagnosis> diagnosisGraphics = new HashMap<Integer, Diagnosis>();
	private HashMap<Integer, PointPollutionSource> pointPSGraphics = new HashMap<Integer, PointPollutionSource>();
	private HashMap<Integer, LinearPollutionSource> linearPSGraphics = new HashMap<Integer, LinearPollutionSource>();
	private HashMap<Integer, PollutedArea> pAreaGraphics = new HashMap<Integer, PollutedArea>();

	private HashMap<Integer, Integer> resultsSelectionMap = new HashMap<Integer, Integer>();
	private HashMap<String, String> analysisSelectionMap = new HashMap<String, String>();

	private MapTip diagnosisMapTip;
	private MapTip pointPSMapTip;
	private MapTip linearPSMapTip;
	private MapTip pSAreaMapTip;

	private LinkedHashMap diagnosisDisplayFields = new LinkedHashMap();
	private LinkedHashMap pointPSDisplayFields = new LinkedHashMap();
	private LinkedHashMap linearPSDisplayFields = new LinkedHashMap();
	private LinkedHashMap pAreaDisplayFields = new LinkedHashMap();

	private static final SimpleMarkerSymbol DIAGNOSISICON = MainWindow.DIAGNOSISICON;
	private static final SimpleMarkerSymbol DIAGNOSISICON2 = MainWindow.DIAGNOSISICON2;
	private static final SimpleMarkerSymbol POINTICON = MainWindow.POINTICON;
	private static final SimpleLineSymbol LINESYMBOL = MainWindow.LINESYMBOL;
	private static final SimpleFillSymbol POLYGONFILL = MainWindow.POLYGONFILL;
	private static final ImageIcon HINTICON = MainWindow.HINTICON;

	private static AnalizePollutionSourceFrame instance;

	private JTabbedPane tabbedPane;
	private JTable resultsTable;
	private JTable analysisTable;
	private JButton cancelButton;
	private JLabel hintLabel;

	private Point point = new Point();
	private int counterResults = 0;
	private final static String TITLE = "Analisi della sorgente di inquinamento (Distanza max ";

	// restituisce l'unica istanza della classe AnalizePollutionSourceFrame
	protected static synchronized AnalizePollutionSourceFrame getInstance() {
		if (instance == null) {
			instance = new AnalizePollutionSourceFrame();
		}
		return instance;
	}

	protected void setMainWindow(MainWindow mw) {
		this.mw = mw;
	}

	protected void setSpatialReference(SpatialReference SR) {
		mapSR = SR;
		connection = mw.getConnection();
		initLayers();
	}

	private void initLayers() {
		diagnosisLayer = mw.getDiagnosisLayer();
		pointPSLayer = mw.getPointPSLayer();
		linearPSLayer = mw.getLinearPSLayer();
		pAreaLayer = mw.getPAreaLayer();
	}

	private AnalizePollutionSourceFrame() {
		initComponent();
	}

	protected void startAnalysis(PointPollutionSource pointPS, int influenceRadius) {
		setTitle(TITLE + (float) influenceRadius / 1000 + " km)");// imposta dinamicamente il titolo in base al raggio
																	// di influenza scelto dall'utente
		initMapTip();
		setPointPSMapTip(pointPS);
		int mapID;
		HashMap<String, String> analysisData = new HashMap<String, String>();
		ArrayList<String> resultsData = new ArrayList<String>();
		point.setX(pointPS.getLongitude());
		point.setY(pointPS.getLatitude());
		Point sourceLocation = convertCoordinate(new Point(point.getX(), point.getY()));
		// imposto grafica
		Graphic pointGraphic = getBuffer(point, POLYGONFILL, pointPS.getRadius());
		mapID = pointPSLayer.addGraphic(pointGraphic);
		pointGraphic = new Graphic(point, POINTICON, pointPSDisplayFields);
		pointPSLayer.addGraphic(pointGraphic);
		pointPS.setMapID(mapID);
		pointPSGraphics.put(mapID, pointPS);

		String query = "select (st_distance(st_buffer(pslocation::geography, radius), d.diagnosis_location::geography)/1000) as distance,"
				+ "			d.pathology, d.diagnosis_date, d.diagnosis_location, count(d.pathology) over (PARTITION by d.pathology) as num_pathology "
				+ "		from pollutionsources join diagnosis d on st_dwithin(st_buffer(pslocation::geography, radius), d.diagnosis_location::geography, " + influenceRadius + ")" 
				+ " 	where pslocation in (ST_GeometryFromText('POINT(" + sourceLocation.getY() + " " + sourceLocation.getX() + ")'))  " 
				+ "		order by distance";
		System.out.println(query);
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
				while (rs.next()) {
					counterResults++;
					getDiagnosis(rs, resultsData, analysisData);
				}
				rs.close();
				stmt.close();
				initTable(resultsData, analysisData);
				counterResults = 0;
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(getParent(),
						"Errore nel caricamento delle sorgenti puntiformi.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	protected void startAnalysis(LinearPollutionSource linearPS, int influenceRadius) {
		setTitle(TITLE + (float) influenceRadius / 1000 + " km)");// imposta dinamicamente il titolo in base al raggio
																	// di influenza scelto dall'utente
		initMapTip();
		setLinearPSMapTip(linearPS);
		int mapID;
		HashMap<String, String> analysisData = new HashMap<String, String>();
		ArrayList<String> resultsData = new ArrayList<String>();
		Graphic pointGraphic = new Graphic(linearPS.getPolyline(), LINESYMBOL, linearPSDisplayFields);
		mapID = linearPSLayer.addGraphic(pointGraphic);
		linearPS.setMapID(mapID);
		linearPSGraphics.put(mapID, linearPS);
		Polyline line = linearPS.getPolyline();
		String lineString = "";
		for (int i = 0; i < line.getPointCount(); i++) {
			Point p = convertCoordinate(line.getPoint(i));
			lineString = lineString + p.getY() + " " + p.getX() + ",";
		}
		lineString = lineString.substring(0, lineString.length() - 1); // rimozione ultima virgola

		String query = "select (st_distance(pslocation::geography, d.diagnosis_location::geography)/1000) as distance, d.pathology, d.diagnosis_date,"
				+ " 		d.diagnosis_location, count(d.pathology) over (PARTITION by d.pathology) as num_pathology "
				+ "		from pollutionsources join diagnosis d on st_dwithin(pslocation::geography, d.diagnosis_location::geography, "
				+ influenceRadius + ") " + " 	where pslocation in (ST_GeometryFromText('LINESTRING(" + lineString
				+ ")')) " + "		order by distance";
		System.out.println(query);
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
				while (rs.next()) {
					counterResults++;
					getDiagnosis(rs, resultsData, analysisData);
				}
				rs.close();
				stmt.close();
				initTable(resultsData, analysisData);
				counterResults = 0;
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(getParent(),
						"Errore nel caricamento delle sorgenti lineari.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	protected void startAnalysis(PollutedArea pArea, int influenceRadius) {
		setTitle(TITLE + (float) influenceRadius / 1000 + " km)");// imposta dinamicamente il titolo in base al raggio
																	// di influenza scelto dall'utente
		initMapTip();
		setPAreaMapTip(pArea);
		int mapID;
		HashMap<String, String> analysisData = new HashMap<String, String>();
		ArrayList<String> resultsData = new ArrayList<String>();
		Graphic pointGraphic;
		Polygon polygon = pArea.getPolygon();
		pointGraphic = new Graphic(polygon, POLYGONFILL, pAreaDisplayFields);
		mapID = pAreaLayer.addGraphic(pointGraphic);
		pArea.setMapID(mapID);
		pAreaGraphics.put(mapID, pArea);
		String polygonString = "";
		for (int i = 0; i < polygon.getPointCount(); i++) {
			Point p = convertCoordinate(polygon.getPoint(i));
			polygonString = polygonString + p.getY() + " " + p.getX() + ",";
		}
		polygonString = polygonString.substring(0, polygonString.length() - 1); // rimozione ultima virgola

		String query = "select (st_distance(pslocation::geography, d.diagnosis_location::geography)/1000) as distance, d.pathology,"
				+ "			d.diagnosis_date, d.diagnosis_location, count(d.pathology) over (PARTITION by d.pathology) as num_pathology "
				+ "		from pollutionsources join diagnosis d on st_dwithin(pslocation::geography, d.diagnosis_location::geography, " + influenceRadius + ")  " 
				+ "		where pslocation in (ST_GeometryFromText('POLYGON((" + polygonString + "))')) " 
				+ "		order by distance";
		System.out.println(query);
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);
				while (rs.next()) {
					counterResults++;
					getDiagnosis(rs, resultsData, analysisData);
				}
				rs.close();
				stmt.close();
				initTable(resultsData, analysisData);
				counterResults = 0;
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(getParent(),
						"Errore nel caricamento delle aree inquinate.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	// prende le informazioni relative alla diagnosi restituita dal resultSet
	protected void getDiagnosis(ResultSet rs, ArrayList<String> rData, HashMap<String, String> aData) {
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
			String count = rs.getString("num_pathology");
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
			rData.add(counterResults + "");
			rData.add(pathology);
			rData.add(date.toString());
			rData.add(String.format("%.3f", distance));
			aData.put(pathology, count);
			// aggiorno hash map per permettere la selezione delle icone sulla mappa tramite
			// la selezione delle righe delle tabelle
			resultsSelectionMap.put(counterResults, mapID);
			if (analysisSelectionMap.containsKey(pathology)) {
				String s = analysisSelectionMap.get(pathology);
				s = s + "," + mapID;
				analysisSelectionMap.put(pathology, s);
			} else {
				analysisSelectionMap.put(pathology, "" + mapID);
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(getParent(), "Errore nel caricamento di una diagnosi.\n" + ex.getMessage(),
					"Attenzione!", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	// restituisce l'immagine del buffer costruito intorno ad un punto con raggio =
	// "distance" e come simbolo di riempimento "fill"
	private Graphic getBuffer(Point point, SimpleFillSymbol fill, double distance) {
		return new Graphic(GeometryEngine.buffer(point, mw.getMapSR(), distance, mw.getMapUnit()), fill,
				pointPSDisplayFields);
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

	// proietta le coordinate di un punto sulla mappa con il relativo map Spatial
	// Reference
	private Point projectCoordinateToMap(Point point) {
		return GeometryEngine.project(point.getX(), point.getY(), mapSR);
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

	// inizializza le componenti del frame
	private void initComponent() {
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
		tabbedPane.setBounds(0, 0, 470, 256);
		getContentPane().add(tabbedPane);
	}

	private void initTable(ArrayList<String> resultsData, HashMap<String, String> analysisData) {
		if (counterResults > 0) { // controlla se esiste almeno un risultato
			initResultTable(resultsData);
			initAnalysisTable(analysisData);

		} else {
			closeInternalFrame();
			JOptionPane.showMessageDialog(mw.getFrame(), "Non è stato trovato nulla.", "Oops",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void initResultTable(ArrayList<String> resultsData) {
		int col;
		int row;
		TableColumnModel tcm;
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		String[] resultsColumnNames = { "#", "Patologia", "Data", "Distanza (km)" };

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
				if (analysisTable.getSelectedRow() != -1 && selectedRow != -1) {
					analysisTable.getSelectionModel().clearSelection();
				}
				for (int i : resultsSelectionMap.values()) {
					diagnosisLayer.unselect(i);
				}
				if(selectedRow != -1)
					diagnosisLayer.select(resultsSelectionMap.get(selectedRow + 1));
			}
		});

		// imposto dimensioni colonne
		tcm = resultsTable.getColumnModel();
		tcm.getColumn(0).setPreferredWidth(40); // #
		tcm.getColumn(1).setPreferredWidth(200); // Patologia
		tcm.getColumn(2).setPreferredWidth(100); // Data
		tcm.getColumn(3).setPreferredWidth(130); // Distanza
		// centro le stringhe all'interno della cella
		tcm.getColumn(0).setCellRenderer(centerRenderer);
		tcm.getColumn(1).setCellRenderer(centerRenderer);
		tcm.getColumn(2).setCellRenderer(centerRenderer);
		tcm.getColumn(3).setCellRenderer(centerRenderer);
		// imposto la tabella per una corretta visualizzazione
		resultsTable.setFillsViewportHeight(true);
		resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsTable.getTableHeader().setReorderingAllowed(false);
		JScrollPane resultsScrollPane = new JScrollPane(resultsTable);
		tabbedPane.addTab("Dati Recuperati", null, resultsScrollPane, null);
	}

	private void initAnalysisTable(HashMap<String, String> analysisData) {
		int col;
		int row;
		TableColumnModel tcm;
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		String[] analysisColumnNames = { "Patologia", "Occorrenze", "Percentuale" };

		col = analysisColumnNames.length;
		row = analysisData.size();

		Object[][] aData = new Object[row][col];
		int i = 0;
		float rate;
		for (String s : analysisData.keySet()) {
			aData[i][0] = s;
			aData[i][1] = analysisData.get(s);
			rate = Float.parseFloat(analysisData.get(s)) / counterResults * 100;
			aData[i++][2] = String.format("%.2f", rate) + "%";

		}

		analysisTable = new JTable(aData, analysisColumnNames) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;// impedisco la modifica delle celle della tabella
			}
		};

		ListSelectionModel model2 = analysisTable.getSelectionModel();
		model2.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// controllo che non siano in atto cambiamenti multipli alla tabella
				if (e.getValueIsAdjusting())
					return;
				int selectedRow = model2.getMinSelectionIndex();
				// controllo se e quali tabelle azzerare prima di iniziare la selezione della
				// resultsTable
				if (resultsTable.getSelectedRow() != -1 && selectedRow != -1) {
					resultsTable.getSelectionModel().clearSelection();
				}
				// deseleziono tutte le patologie selezionate
				for (String s : analysisSelectionMap.values()) {
					for (String mapID : s.split(",")) {
						diagnosisLayer.unselect(Integer.parseInt(mapID));
					}
				}

				if (selectedRow != -1) {// se è stata selezionata una riga la converto nella riga corretta dopo
								// l'ordinamento
					selectedRow = analysisTable.convertRowIndexToModel(selectedRow);
					int col = 0;
					String selectedPathology = (String) analysisTable.getModel().getValueAt(selectedRow, col);
					for (String s : analysisSelectionMap.get(selectedPathology).split(",")) {
						diagnosisLayer.select(Integer.parseInt(s));
					}
				}
			}
		});

		// imposto l'ordinamento della tabella
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(analysisTable.getModel()) {
			@Override
			public boolean isSortable(int column) {
				return false;
			};
		};
		sorter.setComparator(1, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				Integer i1 = Integer.parseInt(o1); // da controllare per le eccezioni di parsing
				Integer i2 = Integer.parseInt(o2);
				return i1.compareTo(i2);
			}
		});
		List<RowSorter.SortKey> sortKeys = new ArrayList<>();
		sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
		sorter.setSortKeys(sortKeys);
		sorter.sort();
		analysisTable.setRowSorter(sorter);

		// centro le stringhe all'interno della cella
		tcm = analysisTable.getColumnModel();
		tcm.getColumn(0).setCellRenderer(centerRenderer);
		tcm.getColumn(1).setCellRenderer(centerRenderer);
		tcm.getColumn(2).setCellRenderer(centerRenderer);
		// imposto la tabella per una corretta visualizzazione
		analysisTable.setFillsViewportHeight(true);
		analysisTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		analysisTable.getTableHeader().setReorderingAllowed(false);
		analysisTable.setDefaultRenderer(String.class, centerRenderer);
		JScrollPane analysisScrollPane = new JScrollPane(analysisTable);
		tabbedPane.addTab("Analisi Patologie", null, analysisScrollPane, null);
	}

	@Override
	public void dispose() {
		closeInternalFrame();
		super.dispose();
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
		tabbedPane.removeAll();
		resultsSelectionMap.clear();
		analysisSelectionMap.clear();
		resultsTable = null;
		analysisTable = null;
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
		diagnosisDisplayFields.put("Distance", "Distanza: ");
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

	private void setDiagnosisMapTip(Diagnosis diagnosis, float distance) {
		diagnosisDisplayFields.put("Diagnosis", "");
		diagnosisDisplayFields.put("Pathology", diagnosis.getPathology());
		diagnosisDisplayFields.put("Date", diagnosis.getDate());
		diagnosisDisplayFields.put("Distance", String.format("%.3f", distance) + " km");
	}

	private void setPointPSMapTip(PointPollutionSource pointPS) {
		pointPSDisplayFields.put("PPS", "");
		pointPSDisplayFields.put("Type", pointPS.getType());
		pointPSDisplayFields.put("StartDate", pointPS.getStartDate());
		pointPSDisplayFields.put("EndDate", pointPS.getEndDate());
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
}
