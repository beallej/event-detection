package eventdetection.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import toberumono.json.JSONArray;
import toberumono.json.JSONBoolean;
import toberumono.json.JSONObject;
import toberumono.json.JSONString;

import eventdetection.downloader.POSTagger;
import eventdetection.downloader.RawArticle;

/**
 * A mechanism for managing articles.
 * 
 * @author Joshua Lipstone
 */
public class ArticleManager {
	private final Connection connection;
	private final String table;
	private final Collection<Path> storage;
	private final boolean posTaggingEnabled, posTagSimplificationEnabled;
	private final TagSimplifier tagSimplifier;
	
	/**
	 * Initializes an {@link ArticleManager} from JSON configuration data.
	 * 
	 * @param connection
	 *            a {@link Connection} to the database in use
	 * @param articleTable
	 *            the name of the table holding the {@link Article Articles}
	 * @param paths
	 *            the "paths" section of the configuration file
	 * @param articles
	 *            the "articles" section of the configuration file
	 */
	public ArticleManager(Connection connection, String articleTable, JSONObject paths, JSONObject articles) {
		this.connection = connection;
		this.table = articleTable;
		this.storage = ((JSONArray) paths.get("articles")).stream().collect(LinkedHashSet::new, (s, p) -> s.add(Paths.get(p.toString())), LinkedHashSet::addAll);
		JSONObject posTagging = (JSONObject) articles.get("pos-tagging");
		this.posTaggingEnabled = ((JSONBoolean) posTagging.get("enable-pos-tagging")).value();
		this.posTagSimplificationEnabled = ((JSONBoolean) posTagging.get("enable-tag-simplification")).value();
		this.tagSimplifier = new TagSimplifier(((JSONArray) posTagging.get("tag-simplification-maps")).stream().collect(ArrayList::new, (a, b) -> { //What?
			try {
				a.addAll(Files.readAllLines(Paths.get(((JSONString) b).value())));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}, ArrayList::addAll));
	}
	
	/**
	 * Initializes an {@link ArticleManager}.
	 * 
	 * @param connection
	 *            a {@link Connection} to the database in use
	 * @param articleTable
	 *            the name of the table holding the {@link Article Articles}
	 * @param articleStorage
	 *            the places where {@link Article Articles} are stored
	 * @param posTaggingEnabled
	 *            whether the {@link Article Articles} should POS tagged
	 */
	public ArticleManager(Connection connection, String articleTable, Collection<Path> articleStorage, boolean posTaggingEnabled) {
		this.connection = connection;
		this.table = articleTable;
		this.storage = articleStorage;
		this.posTaggingEnabled = posTaggingEnabled;
		this.posTagSimplificationEnabled = false;
		this.tagSimplifier = null;
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
	public void removeArticlesBefore(Calendar oldest) throws SQLException, IOException {
		removeArticlesBefore(oldest.toInstant());
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
	public void removeArticlesBefore(Instant oldest) throws SQLException, IOException {
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
	 * by its {@link Iterator}.
	 * 
	 * @param article
	 *            the {@link Article} to store
	 * @return the {@link Path} that points to the file in which the article was stored
	 * @throws SQLException
	 *             if an issue with the SQL server occurs
	 * @throws IOException
	 *             if the storage directory does not exist and cannot be created or the article file cannot be written to
	 *             disk
	 */
	public Path store(Article article) throws SQLException, IOException {
		Path storagePath = storage.iterator().next();
		if (!Files.exists(storagePath))
			Files.createDirectories(storagePath);
		String statement = "insert into " + table + " (title, url, source) values (?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(statement)) {
			String untaggedTitle = article.getUntagged().getTitle();
			stmt.setString(1, untaggedTitle);
			stmt.setString(2, article.getURL().toString());
			stmt.setInt(3, article.getSource().getID());
			stmt.executeUpdate();
			String sql = "select * from " + table + " as arts group by arts.id having arts.id >= all (select a.id from " + table + " as a)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ResultSet rs = ps.executeQuery();
				if (!rs.next())
					return null;
				String filename = makeFilename(rs.getInt("id"), article.getSource(), untaggedTitle);
				try (PreparedStatement stm = connection.prepareStatement("update " + table + " set filename = ? where id = ?")) {
					stm.setString(1, filename);
					stm.setLong(2, rs.getLong("id"));
					stm.executeUpdate();
				}
				Path filePath = storagePath.resolve(filename);
				try {
					StringBuilder fileText = new StringBuilder(article.getTitle().length() + article.getText().length() + 14); //14 is the length of the section dividers
					fileText.append("TITLE:\n").append(article.getTitle()).append("\nTEXT:\n").append(article.getText());
					Files.write(filePath, fileText.toString().getBytes());
				}
				catch (IOException e) {
					try (Statement stm = connection.createStatement()) {
						stm.executeUpdate("delete from " + table + " where id = " + rs.getLong("id"));
					}
					throw e;
				}
				return filePath;
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
	public String makeFilename(int id, int source, String title) {
		return id + "_" + source + "_" + title.replaceAll("[:/\\s]", "_") + ".txt";
	}
	
	/**
	 * Converts a {@link RawArticle} into an {@link Article}. This applies pos tagging to both the title and text.
	 * 
	 * @param ra
	 *            the {@link RawArticle} to process
	 * @return the processed {@link RawArticle} as an {@link Article}
	 */
	public Article process(RawArticle ra) {
		String title = ra.getTitle(), text = ra.getText();
		if (posTaggingEnabled) {
			title = POSTagger.tag(title).replaceAll("(\\w+?)_(\\w+)\\s+(\\w*?'\\w*?)_(\\w+)", "$1$3_$2");
			text = POSTagger.tag(text).replaceAll("(\\w+?)_(\\w+)\\s+(\\w*?'\\w*?)_(\\w+)", "$1$3_$2");
			if (posTagSimplificationEnabled) {
				title = tagSimplifier.simplifyTags(title);
				text = tagSimplifier.simplifyTags(text);
			}
		}
		return new Article(title, text, ra.getURL(), ra.getSource(), isPOSTaggingEnabled());
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
	
	/**
	 * @return {@code true} if POS tagging is enabled
	 */
	public boolean isPOSTaggingEnabled() {
		return posTaggingEnabled;
	}
}
