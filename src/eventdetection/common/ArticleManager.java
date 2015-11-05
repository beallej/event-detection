package eventdetection.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;

/**
 * A mechanism for managing articles.
 * 
 * @author Joshua Lipstone
 */
public class ArticleManager {
	private final Connection connection;
	private final String table;
	private final Collection<Path> storage;
	
	/**
	 * Initializes an {@link ArticleManager}.
	 * 
	 * @param connection
	 *            a {@link Connection} to the database in use
	 * @param table
	 *            the name of the table holding the articles
	 * @param storage
	 *            the places where articles are stored
	 */
	public ArticleManager(Connection connection, String table, Collection<Path> storage) {
		this.connection = connection;
		this.table = table;
		this.storage = storage;
	}
	
	/**
	 * Removes all articles with a file creation date earlier than <tt>oldest</tt>.
	 * 
	 * @param oldest
	 *            a {@link Calendar} containing the oldest date from which {@link Article Articles} should be kept
	 * @throws SQLException
	 *             if an SQL error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void cleanUpArticles(Calendar oldest) throws SQLException, IOException {
		cleanUpArticles(oldest.toInstant());
	}
	
	/**
	 * Removes all articles with a file creation date earlier than <tt>oldest</tt>.
	 * 
	 * @param oldest
	 *            an {@link Instant} containing the oldest date from which {@link Article Articles} should be kept
	 * @throws SQLException
	 *             if an SQL error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void cleanUpArticles(Instant oldest) throws SQLException, IOException {
		String statement = "select * from " + table;
		try (PreparedStatement stmt = connection.prepareStatement(statement)) {
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) //Set the pointer to the first row and test if it is not valid
				return;
			do {
				boolean deleted = false;
				for (Path store : storage) {
					if (!Files.exists(store))
						continue;
					Path path = store.resolve(rs.getString("filename"));
					if (!Files.exists(path))
						continue;
					BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
					if (attrs.creationTime().toInstant().compareTo(oldest) >= 0)
						continue;
					Files.delete(path);
					deleted = true;
				}
				if (deleted) {
					try (Statement stm = connection.createStatement()) {
						stm.executeUpdate("delete from " + table + " where id = " + rs.getLong("id"));
					}
				}
			} while (rs.next()); //While the next row is valid
		}
	}
	
	/**
	 * Stores the given {@link Article} in the first path in the {@link Collection} of storage {@link Path Paths} as defined
	 * by its iterator.
	 * 
	 * @param article
	 *            the {@link Article} to store
	 * @return the {@link Path} that points to the file in which the article was stored
	 * @throws SQLException
	 *             if an issue with the SQL server occurs
	 */
	public Path store(Article article) throws SQLException {
		String statement = "insert into " + table + " (title, url, source) values (?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(statement)) {
			stmt.setString(1, article.getTitle());
			stmt.setString(2, article.getUrl().toString());
			stmt.setString(3, article.getSource().getID());
			stmt.executeUpdate();
			String sql = "select * from " + table + " as arts group by arts.id having arts.id >= all (select a.id from " + table + " as a)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ResultSet rs = ps.executeQuery();
				if (!rs.next())
					return null;
				String filename = makeFilename(rs.getInt("id"), article.getSource(), article.getTitle());
				try (PreparedStatement stm = connection.prepareStatement("update " + table + " set filename = ? where id = ?")) {
					stm.setString(1, filename);
					stm.setLong(2, rs.getLong("id"));
					stm.executeUpdate();
				}
				Path path = storage.iterator().next().resolve(filename);
				try {
					if (!Files.exists(path.getParent()))
						Files.createDirectories(path.getParent());
					Files.write(path, article.getTaggedText().getBytes());
				}
				catch (IOException e) {
					try (Statement stm = connection.createStatement()) {
						stm.executeUpdate("delete from " + table + " where id = " + rs.getLong("id"));
					}
					return null;
				}
				return path;
			}
		}
	}
	
	/**
	 * Constructs the file name for an {@link Article}.
	 * 
	 * @param id
	 *            the {@link Article Article's} id in the database
	 * @param source
	 *            the {@link Source} of the {@link Article}
	 * @param title
	 *            the title of the {@link Article}
	 * @return the file name as a {@link String}
	 */
	public String makeFilename(int id, Source source, String title) {
		return makeFilename(id, source.getID(), title);
	}
	
	/**
	 * Constructs the file name for an {@link Article}.
	 * 
	 * @param id
	 *            the {@link Article Article's} id in the database
	 * @param source
	 *            the id of the {@link Source} of the {@link Article} as a {@link String}
	 * @param title
	 *            the title of the {@link Article}
	 * @return the file name as a {@link String}
	 */
	public String makeFilename(int id, String source, String title) {
		return id + "_" + source + "_" + title.replaceAll("[:/\\s]", "_") + ".txt";
	}
	
	/**
	 * @return the {@link Connection}
	 */
	public Connection getConnection() {
		return connection;
	}
	
	/**
	 * @return the name of the SQL table
	 */
	public String getTable() {
		return table;
	}
}
