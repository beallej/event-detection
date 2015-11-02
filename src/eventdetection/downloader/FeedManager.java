package eventdetection.downloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A system for loading and managing {@link Feed Feeds} and {@link Scraper Scrapers}.
 * 
 * @author Joshua Lipstone
 */
public class FeedManager extends Downloader {
	private final Map<String, Scraper> scrapers;
	private final Map<String, Feed> feeds;
	private boolean closed;
	
	/**
	 * Initializes a {@link FeedManager} without any {@link Feed Feeds} or {@link Scraper Scrapers}.
	 */
	public FeedManager() {
		scrapers = new LinkedHashMap<>(); //We might want to keep the order consistent...
		feeds = new LinkedHashMap<>(); //We might want to keep the order consistent...
		closed = false;
	}
	
	/**
	 * Initializes a {@link FeedManager} with {@link Feed Feeds} and {@link Scraper Scrapers} from the given folders
	 * 
	 * @param feedFolder
	 *            a {@link Path} to the folder containing JSON files describing the feeds in use
	 * @param scraperFolder
	 *            a {@link Path} to the folder containing JSON files describing the sources in use
	 * @throws IOException
	 *             if an error occurs while loading the JSON files
	 */
	public FeedManager(Path feedFolder, Path scraperFolder) throws IOException {
		this();
		if (Files.exists(scraperFolder))
			loadItemsFromFile(Scraper::loadFromJSON, p -> p.toString().endsWith(".json"), scraperFolder, scrapers::put);
		if (Files.exists(feedFolder))
			loadItemsFromFile(p -> Feed.loadFromJSON(p, scrapers), p -> p.toString().endsWith(".json"), feedFolder, feeds::put);
	}
	
	public FeedManager(Connection sqlConnection, String feedTable, String scraperTable) throws SQLException, IOException {
		this();
		if (scraperTable != null)
			addScraper(sqlConnection, scraperTable);
		if (feedTable != null)
			addFeed(sqlConnection, feedTable);
	}
	
	/**
	 * Adds a {@link Scraper} or a folder of {@link Scraper Scrapers} to this {@link FeedManager}.
	 * 
	 * @param path
	 *            a {@link Path} to a JSON file defining a {@link Scraper} or a folder of files defining {@link Scraper
	 *            Scrapers}
	 * @return the IDs of the added {@link Scraper Scrapers}
	 * @throws IOException
	 *             if an error occurs while loading the JSON files
	 */
	public List<String> addScraper(Path path) throws IOException {
		return loadItemsFromFile(Scraper::loadFromJSON, p -> p.toString().endsWith(".json"), path, scrapers::put);
	}
	
	public List<String> addScraper(Connection connection, String table) throws SQLException, IOException {
		return loadItemsFromSQL(table, connection, Scraper::loadFromSQL, scrapers::put);
	}
	
	/**
	 * Adds a {@link Feed} or a folder of {@link Feed Feeds} to this {@link FeedManager}.
	 * 
	 * @param path
	 *            a {@link Path} to a JSON file defining a {@link Feed} or a folder of files defining {@link Feed Feeds}
	 * @return the IDs of the added {@link Feed Feeds}
	 * @throws IOException
	 *             if an error occurs while loading the JSON files
	 */
	public List<String> addFeed(Path path) throws IOException {
		return loadItemsFromFile(p -> Feed.loadFromJSON(p, scrapers), p -> p.toString().endsWith(".json"), path, feeds::put);
	}
	
	public List<String> addFeed(Connection connection, String table) throws SQLException, IOException {
		return loadItemsFromSQL(table, connection, rs -> Feed.loadFromSQL(rs, scrapers), feeds::put);
	}
	
	/**
	 * Removes the {@link Scraper} with the given ID.
	 * 
	 * @param id
	 *            the ID of the {@link Scraper} to remove
	 * @return the removed {@link Scraper} or {@code null}
	 */
	public Scraper removeScraper(String id) {
		return scrapers.remove(id);
	}
	
	/**
	 * Removes the {@link Feed} with the given ID.
	 * 
	 * @param id
	 *            the ID of the {@link Feed} to remove
	 * @return the removed {@link Feed} or {@code null}
	 */
	public Feed removeFeed(String id) {
		return feeds.remove(id);
	}
	
	@Override
	public List<RawArticle> get() {
		List<RawArticle> out = new ArrayList<>();
		for (Downloader downloader : feeds.values())
			out.addAll(downloader.get());
		return out;
	}
	
	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		for (Feed f : feeds.values())
			f.close();
	}
}