import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;

public class LoadComboBoxItems {

	private static Connection connection;
	protected static final HashSet<String> pathologies = new HashSet<String>();

	protected void setConnection(Connection conn) {

		connection = conn;
	}

	protected static void addPathologyItems(JComboBox<String> pathologyComboBox) {
		String string = "SELECT pathology from pathologies order by pathology";
		System.out.println(string);
		String pathology;
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(string);
				while (rs.next()) {
					pathology = rs.getString("pathology");
					pathologyComboBox.addItem(pathology);
					pathologies.add(pathology);
				}
				rs.close();
				stmt.close();
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(null, "Errore nel caricamento elenco patologie.\n" + ex.getMessage(),
						"Attenzione!", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	protected static void addPointPSTypeItems(JComboBox<String> ppsTypeComboBox) {
		String string = "SELECT sourcetype from pointpstypes order by sourcetype";
		System.out.println(string);
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(string);
				while (rs.next()) {
					ppsTypeComboBox.addItem(rs.getString("sourcetype"));
				}
				rs.close();
				stmt.close();
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(null,
						"Errore nel caricamento elenco tipi sorgenti puntiformi.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	protected static void addLinearPSTypeItems(JComboBox<String> lpsTypeComboBox) {
		String string = "SELECT sourcetype from linearpstypes order by sourcetype";
		System.out.println(string);
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(string);
				while (rs.next()) {
					lpsTypeComboBox.addItem(rs.getString("sourcetype"));
				}
				rs.close();
				stmt.close();
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(null,
						"Errore nel caricamento elenco tipi sorgenti lineari.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	protected static void addPATypeItems(JComboBox<String> paTypeComboBox) {
		String string = "SELECT sourcetype from pareatypes order by sourcetype";
		System.out.println(string);
		if (connection != null) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(string);
				while (rs.next()) {
					paTypeComboBox.addItem(rs.getString("sourcetype"));
				}
				rs.close();
				stmt.close();
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(null,
						"Errore nel caricamento elenco tipi aree inquinate.\n" + ex.getMessage(), "Attenzione!",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}
}
