package eventdetection.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Map.Entry;

import toberumono.json.JSONData;
import toberumono.json.JSONObject;

/**
 * A static class that the methods used to connect to the database.
 * 
 * @author Joshua Lipstone
 */
public class DBConnection {
	
	private DBConnection() {/* This is a static class */}
	
	/**
	 * Reads the {@link JSONObject} describing the database into the appropriate System property keys. The {@link JSONObject}
	 * must have the following keys:
	 * <ul>
	 * <li>server : string</li>
	 * <li>port : integer</li>
	 * <li>type : string</li>
	 * <li>name : string</li>
	 * <li>user : string</li>
	 * <li>pass : string</li>
	 * </ul>
	 * All fields other than name and type can be {@code null} - default values will be used instead.
	 * 
	 * @param database
	 *            the {@link JSONObject} describing the connection
	 */
	public static void configureConnection(JSONObject database) {
		for (Entry<String, JSONData<?>> e : database.entrySet()) {
			String key = "db." + e.getKey();
			if (System.getProperty(key) != null)
				continue;
			Object val = e.getValue().value();
			if (val == null)
				continue;
			System.setProperty(key, val.toString().toLowerCase());
		}
	}
	
	/**
	 * Generates a JDBC {@link Connection} using arguments from the command line.<br>
	 * Currently works for:
	 * <ul>
	 * <li>PostgreSQL</li>
	 * </ul>
	 * 
	 * @return a {@link Connection} based on the command line arguments
	 * @throws SQLException
	 *             if something goes wrong while creating the {@link Connection}
	 */
	public static Connection getConnection() throws SQLException {
		String dbtype = System.getProperty("db.type");
		if (dbtype == null)
			return null;
		Properties connectionProps = new Properties();
		String connection = "jdbc:";
		switch (dbtype) {
			case "postgresql":
				connection += dbtype + "://";
				if (System.getProperty("db.server") != null)
					connection += System.getProperty("db.server");
				if (System.getProperty("db.port") != null)
					connection += ":" + System.getProperty("db.port");
				connection += "/" + System.getProperty("db.name", "event_detection");
				break;
			default:
				return null;
		}
		connectionProps.put("user", System.getProperty("db.user", "root"));
		connectionProps.put("password", System.getProperty("db.password", ""));
		return DriverManager.getConnection(connection, connectionProps);
	}
}
