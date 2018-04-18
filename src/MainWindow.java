import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

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
import com.esri.core.symbol.SimpleMarkerSymbol.Style;
import com.esri.map.GraphicsLayer;
import com.esri.map.JMap;
import com.esri.map.MapEvent;
import com.esri.map.MapEventListenerAdapter;
import com.esri.map.MapOptions;
import com.esri.map.MapOptions.MapType;
import com.esri.map.MapOverlay;

public class MainWindow {
	private JFrame frame;
	private JMap map;
	protected JPanel menuPanel;

	private Point pointSelected;

	private GraphicsLayer diagnosisGraphicsLayer;
	private GraphicsLayer pointPSGraphicsLayer;
	private GraphicsLayer linearPSGraphicsLayer;
	private GraphicsLayer pAreaGraphicsLayer;
	private GraphicsLayer searchAreaGraphicsLayer;

	private LinkedList<Point> areaPointsList = new LinkedList<Point>();
	protected boolean startPoint = true;
	protected Polyline lines = null;

	private Polygon polygon;
	protected boolean confirmButtonPressed = false;
	private int lastPolygonLineMapID;
	private int lastPolygonPointMapID;

	private JComboBox<String> analysisComboBox;
	private JComboBox<String> insertComboBox;
	protected JButton confirmButton;
	protected JButton cancelButton;
	private JLabel hintLabel;

	private static final double DEFAULTLATITUDE = 40.85;
	private static final double DEFAULTLONGITUDE = 14.20;
	private static final Point DEFAULTCENTER = new Point(DEFAULTLATITUDE, DEFAULTLONGITUDE);
	private static final int DEFAULTRADIUS = 2500; // 2.5 km

	private SpatialReference mapSR;
	private Unit mapUnit;

	protected static final byte NOTHINGSELECTED = 0;
	protected static final byte INSERTDIAGNOSIS = 1;
	protected static final byte INSERTPOINTPS = 2;
	protected static final byte INSERTLINEARPS = 3;
	protected static final byte INSERTPAREA = 4;
	private static final byte ANALIZEDATA = 1;
	private static final byte SELECTAREA = 2;

	protected static final Color MYRED = new Color(1f, 0f, 0f, .2f);
	protected static final Color MYGREEN = new Color(0f, 1f, 0f, .3f);
	protected static final Color MYBLUE = new Color(0f, 0f, 1f, .3f);

	protected static final SimpleMarkerSymbol DIAGNOSISICON = new SimpleMarkerSymbol(Color.RED, 16, Style.CIRCLE);
	protected static final SimpleMarkerSymbol DIAGNOSISICON2 = new SimpleMarkerSymbol(Color.WHITE, 10, Style.CROSS);
	protected static final SimpleMarkerSymbol POINTICON = new SimpleMarkerSymbol(Color.BLUE, 10, Style.CIRCLE);
	protected static final SimpleMarkerSymbol POINTLINEICON = new SimpleMarkerSymbol(Color.RED, 10, Style.SQUARE);
	protected static final SimpleLineSymbol LINESYMBOL = new SimpleLineSymbol(Color.BLUE, 2,
			SimpleLineSymbol.Style.SOLID);
	protected static final SimpleLineSymbol POLYGONOUTLINE = new SimpleLineSymbol(Color.DARK_GRAY, 2,
			SimpleLineSymbol.Style.SOLID);
	protected static final SimpleFillSymbol POLYGONFILL = new SimpleFillSymbol(MYBLUE, POLYGONOUTLINE,
			SimpleFillSymbol.Style.SOLID);
	protected static final SimpleFillSymbol POLYGONFILL2 = new SimpleFillSymbol(MYRED, POLYGONOUTLINE,
			SimpleFillSymbol.Style.SOLID);
	protected static final ImageIcon HINTICON = new ImageIcon(MainWindow.class.getResource("/icons/full/help.png"));

	private InsertDiagnosisPanel insertDiagnosisPanel;
	private InsertPointPSPanel insertPointPSPanel;
	private InsertLinearPSPanel insertLinearPSPanel;
	private InsertPollutedAreaPanel insertPollutedAreaPanel;
	private AnalysisPanel analysisPanel;
	private AreaSearchPanel areaSearchPanel;
	private AreaSearchFrame areaSearchFrame;
	private AnalizeDiagnosisFrame analizeDiagnosisFrame;
	private AnalizePollutionSourceFrame analizePollutionSourceFrame;

	private Connection connection;

	public Connection getConnection() {
		return connection;
	}

	/**
	 *
	 * Getter
	 */
	public static int getDefaultRadius() {
		return DEFAULTRADIUS;
	}

	public GraphicsLayer getDiagnosisLayer() {
		return diagnosisGraphicsLayer;
	}

	public GraphicsLayer getPointPSLayer() {
		return pointPSGraphicsLayer;
	}

	public GraphicsLayer getLinearPSLayer() {
		return linearPSGraphicsLayer;
	}

	public GraphicsLayer getPAreaLayer() {
		return pAreaGraphicsLayer;
	}

	public GraphicsLayer getSearchAreaLayer() {
		return searchAreaGraphicsLayer;
	}

	public SpatialReference getMapSR() {
		return mapSR;
	}

	public Unit getMapUnit() {
		return mapUnit;
	}

	public JMap getMap() {
		return map;
	}

	public Polyline getLines() {
		return lines;
	}

	protected JFrame getFrame() {
		return frame;
	}

	public MainWindow(Connection conn) {
		initComponent();
		frame.setVisible(true);
		connection = conn;
	}

	private void initComponent() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Rectangle windowRectangle = ge.getMaximumWindowBounds();
		frame = new JFrame() {

			private static final long serialVersionUID = 1L;

			@Override
			public void dispose() {
				try {
					connection.close();
				} catch (SQLException ex) {
					JOptionPane.showMessageDialog(getParent(), "Errore chiusura connessione.\n" + ex.getMessage(),
							"Attenzione!", JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
				super.dispose();
			}
		};
		frame.setResizable(false);
		frame.setBounds(windowRectangle);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		initMap(windowRectangle);
		frame.getContentPane().setLayout(null);

		menuPanel = new JPanel();
		menuPanel.setBackground(Color.WHITE);
		menuPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Menu", TitledBorder.LEADING,
				TitledBorder.TOP, null, SystemColor.activeCaption));
		menuPanel.setBounds(1, 1, 190, 194);
		frame.getContentPane().add(menuPanel);
		menuPanel.setVisible(true);
		menuPanel.setLayout(null);

		insertComboBox = new JComboBox<String>();
		insertComboBox.setBackground(Color.WHITE);
		insertComboBox
				.setBorder(new TitledBorder(null, "Inserimento", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		insertComboBox.setBounds(10, 15, 167, 44);
		insertComboBox.addItem("Seleziona..");
		insertComboBox.addItem("Diagnosi");
		insertComboBox.addItem("Sorgenti Puntiformi");
		insertComboBox.addItem("Sorgenti Lineari");
		insertComboBox.addItem("Area Inquinamento");
		menuPanel.add(insertComboBox);

		analysisComboBox = new JComboBox<String>();
		analysisComboBox.setBackground(Color.WHITE);
		analysisComboBox
				.setBorder(new TitledBorder(null, "Analisi", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		analysisComboBox.setBounds(10, 96, 167, 44);
		analysisComboBox.addItem("Seleziona..");
		analysisComboBox.addItem("Analizza Dati");
		analysisComboBox.addItem("Seleziona Area");
		menuPanel.add(analysisComboBox);

		hintLabel = new JLabel("Suggerimento ");
		hintLabel.setBounds(3, 143, 186, 49);
		menuPanel.add(hintLabel);
		hintLabel.setIcon(HINTICON);
		hintLabel.setIconTextGap(2);
		hintLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
		hintLabel.setVisible(false);

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
				switch (insertComboBox.getSelectedIndex()) {
				case INSERTDIAGNOSIS:
					hintLabel.setText("<html>Clicca un punto sulla mappa per inserire una diagnosi</html>");
					break;
				case INSERTPOINTPS:
					hintLabel.setText("<html>Clicca un punto sulla mappa per inserire una sorgente puntiforme</html>");
					break;
				case INSERTLINEARPS:
					hintLabel.setText("<html>Clicca almeno due punti sulla mappa poi conferma la figura</html>");
					break;
				case INSERTPAREA:
					hintLabel.setText(
							"<html>Clicca almeno tre punti sulla mappa poi conferma o chiudi la figura (click sul primo punto)</html>");
					break;
				}
				switch (analysisComboBox.getSelectedIndex()) {
				case ANALIZEDATA:
					hintLabel.setText(
							"<html>Clicca su un'icona della mappa per iniziare l'analisi sulla sorgente o sulla diagnosi scelta</html>");
					break;
				case SELECTAREA:
					hintLabel.setText(
							"<html>Clicca almeno tre punti sulla mappa poi conferma o chiudi la figura (click sul primo punto)</html>");
					break;
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
			}
		});

		confirmButton = new JButton("Conferma");
		confirmButton.setBounds(10, 62, 90, 22);
		confirmButton.setEnabled(false);
		menuPanel.add(confirmButton);

		cancelButton = new JButton("Annulla");
		cancelButton.setBounds(102, 62, 76, 22);
		cancelButton.setEnabled(false);
		menuPanel.add(cancelButton);

		analysisPanel = AnalysisPanel.getInstance();
		analysisPanel.setMainWindow(this);
		analysisPanel.setSize(230, 435);
		analysisPanel.setLocation(1, 200);
		analysisPanel.setVisible(false);
		frame.getContentPane().add(analysisPanel);

		areaSearchPanel = AreaSearchPanel.getInstance();
		areaSearchPanel.setMainWindow(this);
		areaSearchPanel.setSize(215, 130);
		areaSearchPanel.setLocation(1, 200);
		areaSearchPanel.setVisible(false);
		frame.getContentPane().add(areaSearchPanel);

		areaSearchFrame = AreaSearchFrame.getInstance();
		areaSearchFrame.setMainWindow(this);
		areaSearchFrame.setMinimumSize(new Dimension(505, 335));
		areaSearchFrame.setMaximumSize(new Dimension(505, 335));
		areaSearchFrame.setSize(505, 335);
		areaSearchFrame.setLocation(1, 200);
		areaSearchFrame.setVisible(false);
		areaSearchFrame.setResizable(false);
		frame.getContentPane().add(areaSearchFrame);

		analizeDiagnosisFrame = AnalizeDiagnosisFrame.getInstance();
		analizeDiagnosisFrame.setMainWindow(this);
		analizeDiagnosisFrame.setMinimumSize(new Dimension(505, 335));
		analizeDiagnosisFrame.setMaximumSize(new Dimension(505, 335));
		analizeDiagnosisFrame.setSize(505, 335);
		analizeDiagnosisFrame.setLocation(1, 200);
		analizeDiagnosisFrame.setVisible(false);
		analizeDiagnosisFrame.setResizable(false);
		frame.getContentPane().add(analizeDiagnosisFrame);

		analizePollutionSourceFrame = AnalizePollutionSourceFrame.getInstance();
		analizePollutionSourceFrame.setMainWindow(this);
		analizePollutionSourceFrame.setMinimumSize(new Dimension(485, 335));
		analizePollutionSourceFrame.setMaximumSize(new Dimension(485, 335));
		analizePollutionSourceFrame.setSize(485, 335);
		analizePollutionSourceFrame.setLocation(1, 200);
		analizePollutionSourceFrame.setVisible(false);
		analizePollutionSourceFrame.setResizable(false);
		frame.getContentPane().add(analizePollutionSourceFrame);

		insertDiagnosisPanel = InsertDiagnosisPanel.getInstance();
		insertDiagnosisPanel.setMainWindow(this);
		insertDiagnosisPanel.setSize(218, 187);
		insertDiagnosisPanel.setLocation(1, 200);
		insertDiagnosisPanel.setVisible(false);
		frame.getContentPane().add(insertDiagnosisPanel);

		insertPointPSPanel = InsertPointPSPanel.getInstance();
		insertPointPSPanel.setMainWindow(this);
		insertPointPSPanel.setSize(220, 280);
		insertPointPSPanel.setLocation(1, 200);
		insertPointPSPanel.setVisible(false);
		frame.getContentPane().add(insertPointPSPanel);

		insertLinearPSPanel = InsertLinearPSPanel.getInstance();
		insertLinearPSPanel.setMainWindow(this);
		insertLinearPSPanel.setSize(220, 260);
		insertLinearPSPanel.setLocation(1, 200);
		insertLinearPSPanel.setVisible(false);
		frame.getContentPane().add(insertLinearPSPanel);

		insertPollutedAreaPanel = InsertPollutedAreaPanel.getInstance();
		insertPollutedAreaPanel.setMainWindow(this);
		insertPollutedAreaPanel.setSize(220, 260);
		insertPollutedAreaPanel.setLocation(1, 200);
		insertPollutedAreaPanel.setVisible(false);
		frame.getContentPane().add(insertPollutedAreaPanel);

		addPanelListener(menuPanel);
		addPanelListener(insertDiagnosisPanel);
		addPanelListener(insertPointPSPanel);
		addPanelListener(insertLinearPSPanel);
		addPanelListener(insertPollutedAreaPanel);
		addPanelListener(analysisPanel);
		addPanelListener(areaSearchPanel);

		confirmButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				switch (insertComboBox.getSelectedIndex()) {
				case INSERTLINEARPS:
					insertLinearPSPanel.setLines(lines);
					insertLinearPSPanel.setVisible(true);
					confirmButtonPressed = true;
					break;
				case INSERTPAREA:
					setPSArea(areaPointsList, false);
					insertPollutedAreaPanel.setArea(polygon);
					insertPollutedAreaPanel.setVisible(true);
					confirmButtonPressed = true;
					break;
				default:
					break;
				}
				confirmButton.setEnabled(false);
			}
		});

		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				insertComboBox.setSelectedIndex(NOTHINGSELECTED);
				cancelButton.requestFocus();
			}
		});

		analysisComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (insertComboBox.getSelectedIndex() != NOTHINGSELECTED) {
					if (analysisComboBox.getSelectedIndex() != 0) {
						insertComboBox.setSelectedIndex(0);
					}
				} else {
					hintLabel.setVisible(false);
				}
				switch (analysisComboBox.getSelectedIndex()) {
				case ANALIZEDATA:
					cleanWindowAnalysis();
					hintLabel.setVisible(true);
					analysisPanel.setVisible(true);
					analizeDiagnosisFrame.setVisible(false);
					areaSearchFrame.setVisible(false);
					analizePollutionSourceFrame.setVisible(false);
					areaSearchPanel.setVisible(false);
					areaSearchPanel.setListenMouseClick(false);
					areaSearchPanel.newFigureButton.setEnabled(false);
					areaSearchPanel.analizeButton.setEnabled(false);
					analysisPanel.initMapTip();
					break;
				case SELECTAREA:
					cleanWindowAnalysis();
					areaSearchPanel.setVisible(true);
					areaSearchPanel.setListenMouseClick(true);
					analizeDiagnosisFrame.setVisible(false);
					areaSearchFrame.setVisible(false);
					analizePollutionSourceFrame.setVisible(false);
					analysisPanel.setVisible(false);
					analysisPanel.cleanFields();
					hintLabel.setVisible(true);
					break;
				default:
					cleanWindowAnalysis();
					areaSearchPanel.setVisible(false);
					areaSearchPanel.cleanFields();
					areaSearchPanel.setListenMouseClick(false);
					areaSearchPanel.newFigureButton.setEnabled(false);
					areaSearchPanel.analizeButton.setEnabled(false);
					analysisPanel.cleanFields();
					analysisPanel.setVisible(false);
					analizeDiagnosisFrame.setVisible(false);
					analizePollutionSourceFrame.setVisible(false);
					areaSearchFrame.setVisible(false);
					break;
				}
			}
		});

		insertComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				if (insertComboBox.getSelectedIndex() != NOTHINGSELECTED) {
					cancelButton.setEnabled(true);
					hintLabel.setVisible(true);
					if (analysisComboBox.getSelectedIndex() != NOTHINGSELECTED) {
						analysisComboBox.setSelectedIndex(0);
					}
				} else {
					cancelButton.setEnabled(false);
					hintLabel.setVisible(false);
				}

				cleanWindowInsert();
				areaSearchFrame.closeInternalFrame();
				analizeDiagnosisFrame.closeInternalFrame();
				analizePollutionSourceFrame.closeInternalFrame();
			}
		});

		frame.getContentPane().add(map);
	}

	protected void cleanWindow() {
		int count;
		if (lines != null) {
			count = lines.getPointCount();
			if (count == 1) {
				lines.removePoint(0);
			} else if (count > 1) {
				lines.removePath(0);
			}
		}

		if (polygon != null) {
			count = polygon.getPointCount();
			if (count == 1) {
				polygon.removePoint(0);
			} else if (count > 1) {
				polygon.removePath(0);
			}
		}

		if (areaPointsList != null) {
			areaPointsList.removeAll(areaPointsList);
		}
		startPoint = true;
	}

	protected void cleanWindowAnalysis() {
		cleanWindow();

		areaSearchPanel.setVisible(false);
		areaSearchPanel.setListenMouseClick(false);
		areaSearchPanel.newFigureButton.setEnabled(false);
		areaSearchPanel.analizeButton.setEnabled(false);
		areaSearchPanel.cleanFields();

		analysisPanel.cleanFields();
		analizeDiagnosisFrame.setVisible(false);
		analizePollutionSourceFrame.setVisible(false);
		analizeDiagnosisFrame.cleanFields();
		analizePollutionSourceFrame.cleanFields();

		areaSearchFrame.setVisible(false);
		areaSearchFrame.cleanFields();
	}

	protected void cleanWindowInsert() {
		cleanWindow();

		areaSearchPanel.confirmButtonPressed = false;
		confirmButton.setEnabled(false);

		diagnosisGraphicsLayer.removeAll();
		pointPSGraphicsLayer.removeAll();
		linearPSGraphicsLayer.removeAll();
		pAreaGraphicsLayer.removeAll();

		closeOtherDialogs();
	}

	protected void resetAnalysisComboBox() {
		analysisComboBox.setSelectedIndex(NOTHINGSELECTED);
	}

	protected void resetInsertComboBox(int choice) {
		insertComboBox.setSelectedIndex(choice);
	}

	protected void closeOtherDialogs() {
		insertDiagnosisPanel.cleanFields();
		insertDiagnosisPanel.setVisible(false);
		insertPointPSPanel.cleanFields();
		insertPointPSPanel.setVisible(false);
		insertLinearPSPanel.cleanFields();
		insertLinearPSPanel.setVisible(false);
		insertPollutedAreaPanel.cleanFields();
		insertPollutedAreaPanel.setVisible(false);
	}

	private void initMap(Rectangle windowRectangle) {
		MapOptions mapOptions = new MapOptions(MapType.OSM); // Open Stree Map
		// MapOptions mapOptions = new MapOptions(MapType.TOPO);
		map = new JMap(mapOptions);

		map.setBounds(windowRectangle);

		map.setShowingCopyright(false);
		map.setShowingEsriLogo(false);

		map.addMapEventListener(new MapEventListenerAdapter() {
			@Override
			public void mapReady(MapEvent event) {
				centeringMap();

				mapSR = map.getSpatialReference();
				mapUnit = mapSR.getUnit();
				try {
					areaSearchPanel.setSearchAreaLayer();
					analysisPanel.setSpatialReferenceAndUnit(mapSR, mapUnit);
					analizeDiagnosisFrame.setSpatialReferenceAndUnit(mapSR, mapUnit);
					analizePollutionSourceFrame.setSpatialReference(mapSR);
					areaSearchFrame.setSpatialReferenceAndUnit(mapSR, mapUnit);
					new LoadComboBoxItems().setConnection(connection);
					insertDiagnosisPanel.addPathologiesToComboBox();
					insertPointPSPanel.addTypeToComboBox();
					insertLinearPSPanel.addTypeToComboBox();
					insertPollutedAreaPanel.addTypeToComboBox();
					analysisPanel.addPathologiesTypesToComboBox();
					analysisPanel.addPointTypesToComboBox();
					analysisPanel.addLinearTypesToComboBox();
					analysisPanel.addAreaTypesToComboBox();
				} catch (NullPointerException e) {
					System.out.println("NullPointerException");
				}
			}
		});

		initGraphicLayers();

		map.addMapOverlay(new MapOverlay() {

			private static final long serialVersionUID = 1L;

			@Override
			public void onMouseClicked(MouseEvent event) {
				boolean check = false;
				int size;
				if (SwingUtilities.isLeftMouseButton(event)) { // prendo in considerazione solo i click con il tasto
																// sinistro
					pointSelected = getCoordinateFromMouseClicked(event);
					switch (insertComboBox.getSelectedIndex()) {
					case INSERTDIAGNOSIS:
						showPointSelected(pointSelected, 0);
						insertDiagnosisPanel.setPoint(convertCoordinate(pointSelected));
						insertDiagnosisPanel.setVisible(true);
						break;
					case INSERTPOINTPS:
						showPointSelected(pointSelected, DEFAULTRADIUS);
						insertPointPSPanel.setPoint(convertCoordinate(pointSelected));
						insertPointPSPanel.setVisible(true);
						break;
					case INSERTLINEARPS:
						if (!confirmButtonPressed) {
							if (startPoint) {
								lines = new Polyline();
								lines.startPath(pointSelected);
								startPoint = false;
							} else {
								confirmButton.setEnabled(true);
								setLinearPS(lines, pointSelected);
							}
							showPointSelected(pointSelected, 0);
						}
						break;
					case INSERTPAREA:
						areaPointsList.add(pointSelected);
						size = areaPointsList.size();
						if (!confirmButtonPressed) {
							if (startPoint) {
								lines = new Polyline();
								polygon = new Polygon();
								lines.startPath(pointSelected);
								startPoint = false;
							} else {
								setLinearPS(lines, pointSelected);
							}
							if (size > 3) {
								check = checkClosedFigure();
								if (check) {
									areaPointsList.removeLast(); // poichè la chiusura potrebbe non essere precisa si
																	// rimuove l'ultimo elemento
									areaPointsList.addLast(areaPointsList.getFirst()); // e si aggiunge in coda il primo
																						// elemento così da chiudere la
																						// figura in modo preciso
									pAreaGraphicsLayer.removeGraphic(lastPolygonLineMapID);
									pAreaGraphicsLayer.removeGraphic(lastPolygonPointMapID);
									setPSArea(areaPointsList, true);
									insertPollutedAreaPanel.setArea(polygon);
									insertPollutedAreaPanel.setVisible(true);
									confirmButtonPressed = true;
								}
							} else if (size == 3) {
								confirmButton.setEnabled(true);
							}
							if (!check) {
								showPointSelected(pointSelected, 0);
							}
						}
						break;
					}

					switch (analysisComboBox.getSelectedIndex()) {
					case ANALIZEDATA:
						analysisPanel.startSelection(getCoordinateFromMouseClicked(event));
						break;
					case SELECTAREA:
						if (areaSearchPanel.getListenMouseClick()) {
							areaPointsList.add(pointSelected);
							size = areaPointsList.size();
							if (!areaSearchPanel.confirmButtonPressed) {
								if (startPoint) {
									lines = new Polyline();
									polygon = new Polygon();
									lines.startPath(pointSelected);
									startPoint = false;
								} else {
									setLinearPS(lines, pointSelected);
								}
								if (size > 3) {
									check = checkClosedFigure();
									if (check) {
										areaPointsList.removeLast(); // poichè la chiusura potrebbe non essere precisa
																		// si rimuove l'ultimo elemento
										areaPointsList.addLast(areaPointsList.getFirst()); // e si aggiunge in coda il
																							// primo elemento così da
																							// chiudere la figura in
																							// modo preciso
										searchAreaGraphicsLayer.removeGraphic(lastPolygonLineMapID);
										searchAreaGraphicsLayer.removeGraphic(lastPolygonPointMapID);
										setPSArea(areaPointsList, true);
										areaSearchPanel.confirmButtonPressed = true;
									}
								} else if (size == 3) {
									areaSearchPanel.confirmButton.setEnabled(true);
								}
								if (!check) {
									showPointSelected(pointSelected, 0);
								}
							}
						}
						break;
					}
				}
				super.onMouseClicked(event);
			}
		});
	}

	protected boolean checkClosedFigure() {
		// Controlla se l'ultimo punto è vicino al primo (utile per la chiusura del
		// poligono di un'area inquinata)
		Polygon bufferStart = GeometryEngine.buffer(areaPointsList.getFirst(), mapSR, 200, mapUnit);
		Polygon bufferEnd = GeometryEngine.buffer(areaPointsList.getLast(), mapSR, 200, mapUnit);
		return GeometryEngine.intersects(bufferStart, bufferEnd, mapSR);
	}

	protected void showPointSelected(Point point, double radius) {
		Graphic pointGraphic;
		switch (insertComboBox.getSelectedIndex()) {
		case INSERTDIAGNOSIS:
			pointGraphic = new Graphic(point, DIAGNOSISICON);
			diagnosisGraphicsLayer.removeAll();
			diagnosisGraphicsLayer.addGraphic(pointGraphic);
			pointGraphic = new Graphic(point, DIAGNOSISICON2);
			diagnosisGraphicsLayer.addGraphic(pointGraphic);
			break;
		case INSERTPOINTPS:
			pointGraphic = getBuffer(point, POLYGONFILL, radius);
			pointPSGraphicsLayer.removeAll();
			pointPSGraphicsLayer.addGraphic(pointGraphic);
			pointGraphic = new Graphic(point, POINTICON);
			pointPSGraphicsLayer.addGraphic(pointGraphic);
			break;
		case INSERTPAREA:
			pointGraphic = new Graphic(point, POINTLINEICON);
			pAreaGraphicsLayer.addGraphic(pointGraphic);
			break;
		case INSERTLINEARPS:
			pointGraphic = new Graphic(point, POINTLINEICON);
			linearPSGraphicsLayer.addGraphic(pointGraphic);
			break;

		}

		switch (analysisComboBox.getSelectedIndex()) {
		case ANALIZEDATA:
			break;
		case SELECTAREA:
			pointGraphic = new Graphic(point, POINTLINEICON);
			searchAreaGraphicsLayer.addGraphic(pointGraphic);
			break;
		}
	}

	private Graphic getBuffer(Point point, SimpleFillSymbol fill, double distance) {
		return new Graphic(GeometryEngine.buffer(point, mapSR, distance, mapUnit), fill);
	}

	private void initGraphicLayers() {
		diagnosisGraphicsLayer = new GraphicsLayer();
		diagnosisGraphicsLayer.setSelectionColor(Color.GREEN);
		diagnosisGraphicsLayer.setName("DiagnosisGraphics");

		pointPSGraphicsLayer = new GraphicsLayer();
		pointPSGraphicsLayer.setSelectionColor(Color.GREEN);
		pointPSGraphicsLayer.setName("PPSGraphics");

		linearPSGraphicsLayer = new GraphicsLayer();
		linearPSGraphicsLayer.setName("LPSGraphics");
		linearPSGraphicsLayer.setSelectionColor(Color.GREEN);

		pAreaGraphicsLayer = new GraphicsLayer();
		pAreaGraphicsLayer.setName("PSAGraphics");
		pAreaGraphicsLayer.setSelectionColor(Color.GREEN);

		searchAreaGraphicsLayer = new GraphicsLayer();
		searchAreaGraphicsLayer.setName("SearchAreaGraphics");
		searchAreaGraphicsLayer.setSelectionColor(Color.GREEN);

		map.getLayers().add(searchAreaGraphicsLayer);
		map.getLayers().add(pAreaGraphicsLayer);
		map.getLayers().add(pointPSGraphicsLayer);
		map.getLayers().add(linearPSGraphicsLayer);
		map.getLayers().add(diagnosisGraphicsLayer);
	}

	protected void setDiagnosis(Point point) {
	}

	private void setPSArea(LinkedList<Point> points, boolean closed) {
		Point current;
		Point start;
		Graphic polygonGraphic;
		if (points.size() > 0) {
			start = points.removeFirst();
			polygon.startPath(start);
			while (points.size() > 0) {
				current = points.removeFirst();
				polygon.lineTo(current);
			}
			if (!closed) {
				polygon.lineTo(start);
			}
		}

		if (insertComboBox.getSelectedIndex() == INSERTPAREA) {
			polygonGraphic = new Graphic(polygon, POLYGONFILL);
			confirmButton.setEnabled(false);
			pAreaGraphicsLayer.addGraphic(polygonGraphic);
		} else {
			areaSearchPanel.confirmButton.setEnabled(false);
			areaSearchPanel.newFigureButton.setEnabled(true);
			areaSearchPanel.analizeButton.setEnabled(true);
			areaSearchFrame.setArea(polygon);
			polygonGraphic = new Graphic(polygon, POLYGONFILL2);
			searchAreaGraphicsLayer.addGraphic(polygonGraphic);
		}
	}

	private void setLinearPS(Polyline lines, Point point) {
		lines.lineTo(point);
		int selected = insertComboBox.getSelectedIndex();
		if (selected == INSERTLINEARPS) {
			Graphic lineGraphic = new Graphic(lines, LINESYMBOL);
			linearPSGraphicsLayer.addGraphic(lineGraphic);
		} else if (selected == INSERTPAREA) {
			Graphic polygonGraphic = new Graphic(lines, POLYGONOUTLINE);
			lastPolygonLineMapID = pAreaGraphicsLayer.addGraphic(polygonGraphic);
		} else {
			Graphic polygonGraphic = new Graphic(lines, POLYGONOUTLINE);
			lastPolygonLineMapID = searchAreaGraphicsLayer.addGraphic(polygonGraphic);
		}
	}

	protected Point getCoordinateFromMouseClicked(MouseEvent event) {
		java.awt.Point screenPoint = event.getPoint();
		return map.toMapPoint(screenPoint.x, screenPoint.y);
	}

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

	private void centeringMap() {
		map.centerAt(DEFAULTLATITUDE, DEFAULTLONGITUDE);
		map.zoom(0.002, DEFAULTCENTER);
	}

	public void callSetPSArea() {
		setPSArea(areaPointsList, false);
	}

	private void addPanelListener(JPanel menuPanel) {
		menuPanel.addMouseListener(new MouseListener() {

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
