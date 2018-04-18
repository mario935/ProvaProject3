import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class ConnectionDBFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private JPanel contentPane;
	private JPanel panel;
	private JTextField urlTextField;
	private JTextField dbTextField;
	private JTextField userTextField;
	private JPasswordField passwordTextField;
	private JFormattedTextField portFormattedTextField;
	private JButton connectButton;

	private static final String DEFAULTURL = "127.0.0.1";
	private static final String DEFAULTUSER = "postgres";
	private static final String DEFAULTPWD = "postgres";
	private static final String DEFAULTDB = "dbprova";
	private static final String DEFAULTPORT = "5432";

	private Connection connection;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					ConnectionDBFrame frame = new ConnectionDBFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ConnectionDBFrame() {
		initComponent();
		initListeners();
	}

	protected void connect() {
		String url = urlTextField.getText();
		String port = portFormattedTextField.getText();
		String database = dbTextField.getText();
		String user = userTextField.getText();
		String password = new String(passwordTextField.getPassword());
		String connectionString;
		if (checkFieldsEmpty()) {
			JOptionPane.showMessageDialog(getParent(), "Nessun campo può essere vuoto.", "Attenzione!",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			JOptionPane.showMessageDialog(getParent(), "JDBC Driver per PostgreSQL non trovati.", "Attenzione!",
					JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
			return;
		}

		try {
			connectionString = "jdbc:postgresql://" + url + ":" + port + "/" + database;
			System.out.println(connectionString);
			connection = DriverManager.getConnection(connectionString, user, password);
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(getParent(), "Connessione Fallita.\nMessaggio Console:\n" + e.getMessage(),
					"Attenzione!", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (connection != null) {
			new MainWindow(connection);
			setVisible(false);
		} else {
			JOptionPane.showMessageDialog(getParent(), "(2) Connessione Fallita.", "Attenzione!",
					JOptionPane.ERROR_MESSAGE);
			System.out.println("Failed to make connection!");
		}
		return;
	}

	private boolean checkFieldsEmpty() {
		String url = urlTextField.getText();
		String port = portFormattedTextField.getText();
		String database = dbTextField.getText();
		String user = userTextField.getText();
		if (url.equals("") || port.equals("") || database.equals("") || user.equals(""))
			return true;
		return false;
	}

	protected void cleanFields() {
		urlTextField.setText("");
		portFormattedTextField.setValue(null);
		dbTextField.setText("");
		userTextField.setText("");
		passwordTextField.setText("");
	}

	private void initComponent() {
		setTitle("S&IGIS");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setBounds(100, 100, 227, 308);
		contentPane = new JPanel();
		contentPane.setBackground(Color.WHITE);
		contentPane.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Connessione al Database",
				TitledBorder.LEADING, TitledBorder.TOP, null, SystemColor.activeCaption));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		panel = new JPanel();
		panel.setBounds(getContentPane().getBounds());
		contentPane.add(panel);

		connectButton = new JButton("Connetti");
		contentPane.add(connectButton);
		connectButton.setBounds(new Rectangle(130, 237, 81, 26));

		urlTextField = new JTextField(DEFAULTURL);
		urlTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		urlTextField.setBorder(new TitledBorder(null, "URL", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		urlTextField.setBounds(10, 28, 115, 40);
		contentPane.add(urlTextField);
		urlTextField.setColumns(10);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMaximumIntegerDigits(6);
		nf.setMaximumFractionDigits(0);
		portFormattedTextField = new JFormattedTextField(nf);
		portFormattedTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		portFormattedTextField
				.setBorder(new TitledBorder(null, "Porta", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		portFormattedTextField.setBounds(141, 28, 70, 40);
		portFormattedTextField.setText(DEFAULTPORT);
		contentPane.add(portFormattedTextField);

		dbTextField = new JTextField(DEFAULTDB);
		dbTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		dbTextField.setBorder(new TitledBorder(null, "Database", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		dbTextField.setBounds(10, 80, 201, 40);
		contentPane.add(dbTextField);
		dbTextField.setColumns(10);

		userTextField = new JTextField(DEFAULTUSER);
		userTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		userTextField.setBorder(new TitledBorder(null, "Utente", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		userTextField.setBounds(10, 132, 201, 40);
		contentPane.add(userTextField);
		userTextField.setColumns(10);

		passwordTextField = new JPasswordField(DEFAULTPWD);
		passwordTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		passwordTextField
				.setBorder(new TitledBorder(null, "Password", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		passwordTextField.setBounds(11, 184, 200, 40);
		contentPane.add(passwordTextField);
		passwordTextField.setColumns(10);
	}

	private void initListeners() {
		MouseListener mouseListener = new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				JTextField tf = (JTextField) e.getSource();
				if(tf.getText().equals(DEFAULTURL) || tf.getText().equals(DEFAULTUSER) || tf.getText().equals(DEFAULTDB))
					tf.setText("");
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

		};

		urlTextField.addMouseListener(mouseListener);
		userTextField.addMouseListener(mouseListener);
		dbTextField.addMouseListener(mouseListener);

		passwordTextField.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				passwordTextField.setText("");
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

		portFormattedTextField.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if(portFormattedTextField.getText().equals(DEFAULTPORT))
					portFormattedTextField.setText("");
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

		connectButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				connect();

			}
		});

		portFormattedTextField.addKeyListener(new KeyListener() {

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

		portFormattedTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				if (portFormattedTextField.getText().equals("")) {
					portFormattedTextField.setValue(null);
				}
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});
	}
}
