package eventdetection.downloader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import toberumono.utils.functions.IOExceptedFunction;

import eventdetection.common.IDAble;
import eventdetection.common.SQLExceptedFunction;
import eventdetection.common.Source;

/**
 * This represents an object that can download information from news sources.
 * 
 * @author Joshua Lipstone
 */
public abstract class Downloader implements Supplier<List<RawArticle>>, Closeable {
	/**
	 * The available news {@link Source Sources}
	 */
	public static final Map<Integer, Source> sources = new LinkedHashMap<>();
	private static Connection connection = null;
	
	/**
	 * Adds a {@link Source} or a folder of {@link Source Sources} to {@link #sources}.
	 * 
	 * @param path
	 *            a {@link Path} to a JSON file defining a {@link Source} or a folder of files defining {@link Source
	 *            Sources}
	 * @return the IDs of the added {@link Source Sources}
	 * @throws IOException
	 *             if an error occurs while loading the JSON files
	 */
	public static List<Integer> loadSource(Path path) throws IOException {
		return loadItemsFromFile(Source::loadFromJSON, p -> p.toString().endsWith(".json"), path, sources::put);
	}
	
	/**
	 * Adds the given {@link Source}.
	 * 
	 * @param source
	 *            the {@link Source} to add
	 * @return the ID of the added {@link Source}
	 */
	public static Integer addSource(Source source) {
		sources.put(source.getID(), source);
		return source.getID();
	}
	
	/**
	 * Removes the {@link Source} with the given ID.
	 * 
	 * @param id
	 *            the ID of the {@link Source} to remove
	 * @return the removed {@link Source} or {@code null}
	 */
	public static Source removeSource(Integer id) {
		return sources.remove(id);
	}
	
	/**
	 * A helper method for loading {@link IDAble} objects from files.
	 * 
	 * @param loader
	 *            the method used to load the {@link IDAble} object from a file
	 * @param filter
	 *            the method used to determine if the file has the correct extension
	 * @param path
	 *            a {@link Path} to a file or folder of files that define instances of the {@link IDAble} object
	 * @param store
	 *            the method used to store the constructed {@link IDAble} objects
	 * @param <T>
	 *            the type of the item to load
	 * @param <V>
	 *            the type of the ID of the item to load
	 * @return a {@link List} of the IDs of the loaded objects
	 * @throws IOException
	 *             if an error occurs while loading from files
	 */
	public static <T extends IDAble<V>, V> List<V> loadItemsFromFile(IOExceptedFunction<Path, T> loader, Filter<Path> filter, Path path, BiFunction<V, T, T> store) throws IOException {
		List<V> ids = new ArrayList<>();
		if (Files.isRegularFile(path)) {
			if (filter.accept(path)) {
				T t = loader.apply(path);
				store.apply(t.getID(), t);
				ids.add(t.getID());
			}
			return ids;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, filter)) {
			for (Path p : stream) {
				try {
					T t = loader.apply(p);
					store.apply(t.getID(), t);
					ids.add(t.getID());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ids;
	}
	
	/**
	 * A helper method for loading {@link IDAble} objects from files.
	 * 
	 * @param table
	 *            the name of the table from which to load the items
	 * @param loader
	 *            the method used to load the {@link IDAble} object from a file
	 * @param store
	 *            the method used to store the constructed {@link IDAble} objects
	 * @param <T>
	 *            the type of the item to load. This will be implicitly set when this method is used correctly
	 * @return a {@link List} of the IDs of the loaded objects
	 * @throws SQLException
	 *             if an error occurs while loading
	 * @throws IOException
	 *             if an error occurs while loading
	 */
	public static <T extends IDAble<V>, V> List<V> loadItemsFromSQL(String table, SQLExceptedFunction<ResultSet, T> loader, BiFunction<V, T, T> store)
			throws SQLException, IOException {
		return loadItemsFromSQL(table, getConnection(), loader, store);
	}
	
	/**
	 * A helper method for loading {@link IDAble} objects from files.
	 * 
	 * @param table
	 *            the name of the table from which to load the items
	 * @param connection
	 *            a {@link Connection} to the database to use
	 * @param loader
	 *            the method used to load the {@link IDAble} object from a file
	 * @param store
	 *            the method used to store the constructed {@link IDAble} objects
	 * @param <T>
	 *            the type of the item to load. This will be implicitly set when this method is used correctly
	 * @return a {@link List} of the IDs of the loaded objects
	 * @throws SQLException
	 *             if an error occurs while loading
	 * @throws IOException
	 *             if an error occurs while loading
	 */
	public static <T extends IDAble<V>, V> List<V> loadItemsFromSQL(String table, Connection connection, SQLExceptedFunction<ResultSet, T> loader, BiFunction<V, T, T> store)
			throws SQLException, IOException {
		List<V> ids = new ArrayList<>();
		Connection con = getConnection();
		String statement = "select * from " + table;
		try (PreparedStatement stmt = con.prepareStatement(statement)) {
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) //Set the pointer to the first row and test if it is not valid
				return ids;
			do {
				T t = loader.apply(rs);
				store.apply(t.getID(), t);
				ids.add(t.getID());
			} while (rs.next()); //While the next row is valid
		}
		return ids;
	}
	
	/**
	 * Generates a connection using JDBC using arguments from the command line.<br>
	 * Currently works for:
	 * <ul>
	 * <li>MySQL</li>
	 * </ul>
	 * 
	 * @return a {@link Connection} based on the command line arguments
	 * @throws SQLException
	 *             if something goes wrong while creating the {@link Connection}
	 */
	public static Connection getConnection() throws SQLException {
		if (connection != null)
			return connection;
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
		return Downloader.connection = DriverManager.getConnection(connection, connectionProps);
	}
}
