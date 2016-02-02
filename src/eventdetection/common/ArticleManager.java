package eventdetection.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toberumono.json.JSONArray;
import toberumono.json.JSONBoolean;
import toberumono.json.JSONObject;

/**
 * A mechanism for managing articles.
 * 
 * @author Joshua Lipstone
 */
public class ArticleManager {
	private final Connection connection;
	private final String table;
	private final Collection<Path> storage;
	private final boolean posTaggingEnabled;
	private static final ReadWriteLock fsLock = new ReentrantReadWriteLock();
	private final Logger logger;
	
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
		this.logger = LoggerFactory.getLogger(getClass());
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
		this.logger = LoggerFactory.getLogger(getClass());
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
					String filename = rs.getString("filename");
					Path path = store.resolve(filename);
					if (!Files.exists(path))
						continue;
					BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
					if (attrs.creationTime().toInstant().compareTo(oldest) >= 0)
						continue;
					Files.delete(path);
					
					Path serialized = toSerializedPath(path);
					if (Files.exists(serialized))
						Files.delete(serialized);
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
	public synchronized Article store(Article article) throws SQLException, IOException {
		synchronized (fsLock.writeLock()) {
			Path storagePath = storage.iterator().next(), serializedPath = storagePath.resolve("serialized");
			if (!Files.exists(serializedPath))
				Files.createDirectories(serializedPath);
			String statement = "insert into " + table + " (title, url, source) values (?, ?, ?)";
			try (PreparedStatement stmt = connection.prepareStatement(statement)) {
				String untaggedTitle = article.getUntaggedTitle();
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
					Path filePath = storagePath.resolve(filename), serialPath = serializedPath.resolve(toSerializedName(filename));
					logger.info("Started Processing: " + article.getUntaggedTitle());
					article = article.copyWithID(rs.getInt("id"));
					try {
						StringBuilder fileText = new StringBuilder(article.getTaggedTitle().length() + article.getTaggedText().length() + 14); //14 is the length of the section dividers
						fileText.append("TITLE:\n").append(article.getTaggedTitle()).append("\nTEXT:\n").append(article.getTaggedText());
						Files.write(filePath, fileText.toString().getBytes());
						try (ObjectOutputStream serialOut = new ObjectOutputStream(new FileOutputStream(serialPath.toFile()))) {
							article.process();
							serialOut.writeObject(article);
						}
						catch (Throwable t) {
							throw t;
						}
						try (ObjectInputStream serialIn = new ObjectInputStream(new FileInputStream(serialPath.toFile()))) {
							serialIn.readObject(); //Test to be sure that the serialization worked
						}
						catch (Throwable t) {
							throw new IOException("Serialization failed", t); //If anything goes wrong with reading the Article
						}
					}
					catch (IOException e) {
						try (Statement stm = DBConnection.getConnection().createStatement()) {
							stm.executeUpdate("delete from " + table + " where id = " + rs.getLong("id"));
						}
						if (Files.exists(filePath))
							Files.delete(filePath);
						if (Files.exists(serialPath))
							Files.delete(serialPath);
						throw e;
					}
					return article;
				}
			}
		}
	}
	
	/**
	 * Loads the {@link Article} with the given {@code id} from disk using the information in the SQL database
	 * 
	 * @param id
	 *            the ID of the {@link Article} to load
	 * @return the {@link Article} if one was found, otherwise null
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public Article load(int id) throws SQLException, ClassNotFoundException, IOException {
		synchronized (fsLock.readLock()) {
			try (PreparedStatement stmt = connection.prepareStatement("select * from articles where articles.id = ?")) {
				stmt.setInt(1, id);
				try (ResultSet rs = stmt.executeQuery()) {
					if (!rs.next()) {
						throw new IOException("Unable to locate an article with id = " + id + " in the SQL database.");
					}
					for (Path store : storage) {
						if (!Files.exists(store))
							continue;
						String filename = rs.getString("filename");
						Path serialized = ArticleManager.toSerializedPath(store.resolve(filename));
						if (!Files.exists(serialized))
							continue;
						try (ObjectInputStream serialIn = new ObjectInputStream(new FileInputStream(serialized.toFile()))) {
							Article article = (Article) serialIn.readObject();
							return article;
						}
					}
				}
			}
			throw new IOException("Unable to locate an article with id = " + id + " in the filesystem.");
		}
	}
	
	/**
	 * Loads the {@link Article Articles} with the given {@code ids} from disk using the information in the SQL database.
	 * 
	 * @param ids
	 *            the IDs of the {@link Article} to load as a {@link Collection}
	 * @return the {@link Article} if one was found, otherwise null
	 * @throws SQLException
	 */
	public List<Article> loadArticles(Collection<Integer> ids) throws SQLException {
		synchronized (fsLock.readLock()) {
			List<Article> articles = new ArrayList<>();
			try (ResultSet rs = connection.prepareStatement("select * from articles").executeQuery()) {
				logger.info("Starting to deserialize articles");
				Article article = null;
				if (ids.size() > 0) {
					int id = 0;
					while (rs.next()) {
						id = rs.getInt("id");
						if (ids.contains(id)) {
							article = loadArticle(rs);
							if (article != null) {
								articles.add(article);
								ids.remove(id); //Prevents articles from being loaded more than once
							}
						}
					}
				}
				else {
					while (rs.next()) {
						article = loadArticle(rs);
						if (article != null)
							articles.add(article);
					}
				}
				logger.info("Done deserializing articles");
			}
			if (ids.size() > 0)
				logger.warn("Did not find articles with ids matching " + ids.stream().reduce("", (a, b) -> a + ", " + b.toString(), (a, b) -> a + b).substring(2));
			return articles;
		}
	}
	
	private Article loadArticle(ResultSet rs) throws SQLException {
		Article article = null;
		for (Path store : storage) {
			if (!Files.exists(store))
				continue;
			String filename = rs.getString("filename");
			Path serialized = ArticleManager.toSerializedPath(store.resolve(filename));
			if (!Files.exists(serialized))
				continue;
			try (ObjectInputStream serialIn = new ObjectInputStream(new FileInputStream(serialized.toFile()))) {
				article = (Article) serialIn.readObject();
			}
			catch (ClassNotFoundException | IOException e) {
				logger.debug("Error while deserializing data for " + rs.getString("title"), e);
				article = null;
			}
		}
		if (article == null)
			logger.warn("Unable to find the serialized data for " + rs.getString("title") + ".  Skipping.");
		return article;
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
	public static String makeFilename(int id, Source source, String title) {
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
	public static String makeFilename(int id, int source, String title) {
		return id + "_" + source + "_" + title.replaceAll("[:/\\s]", "_") + ".txt";
	}
	
	/**
	 * Converts an {@link Article Article's} filename from the .txt ending to a name ending in .data
	 * 
	 * @param filename
	 *            the filename to convert
	 * @return the converted filename
	 */
	public static String toSerializedName(String filename) {
		return filename.substring(0, filename.length() - 4) + ".data";
	}
	
	/**
	 * Converts the {@link Path} to the saved text of an article to the {@link Path} to the serialized form of the
	 * corresponding {@link Article} object
	 * 
	 * @param textPath
	 *            the {@link Path} to the saved text of an article
	 * @return the {@link Path} to the serialized form of the corresponding {@link Article} object
	 */
	public static Path toSerializedPath(Path textPath) {
		String filename = toSerializedName(textPath.getFileName().toString());
		return textPath.getParent().resolve("serialized").resolve(filename);
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
