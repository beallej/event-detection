package eventdetection.downloader;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import eventdetection.common.ID;
import eventdetection.common.IDAble;
import eventdetection.temporarylibraryplaceholders.IOExceptedFunction;

/**
 * A system for loading and managing {@link Feed Feeds} and {@link Scraper Scrapers}.
 * 
 * @author Joshua Lipstone
 */
public class FeedManager extends Downloader {
	private final Map<ID, Scraper> scrapers;
	private final Map<ID, Feed> feeds;
	
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
		scrapers = new LinkedHashMap<>(); //We might want to keep the order consistent...
		feeds = new LinkedHashMap<>(); //We might want to keep the order consistent...
		loadItemsFromFile(Scraper::loadFromJSON, p -> p.endsWith(".json"), scraperFolder, scrapers::put);
		loadItemsFromFile(p -> Feed.loadFromJSON(p, scrapers), p -> p.endsWith(".json"), feedFolder, feeds::put);
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
	 * @return a {@link List} of the IDs of the loaded objects
	 * @throws IOException
	 *             if an error occurs while loading from files
	 */
	public static <T extends IDAble> List<ID> loadItemsFromFile(IOExceptedFunction<Path, T> loader, Filter<Path> filter, Path path, BiFunction<ID, T, T> store) throws IOException {
		List<ID> ids = new ArrayList<>();
		if (Files.isRegularFile(path)) {
			if (filter.accept(path)) {
				T t = loader.apply(path);
				store.apply(t.getID(), t);
				ids.add(t.getID());
			}
			return ids;
		}
		for (Path p : Files.newDirectoryStream(path, filter)) {
			try {
				T t = loader.apply(p);
				store.apply(t.getID(), t);
				ids.add(t.getID());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ids;
	}
	
	/**
	 * Adds a {@link Scraper} or a folder of {@link Scraper Scrapers} to this {@link FeedManager}.
	 * 
	 * @param path
	 *            a {@link Path} to a JSON file defining a {@link Scraper} or a folder of {@link Scraper Scrapers}
	 * @return the IDs of the added {@link Scraper Scrapers}
	 * @throws IOException
	 *             if an error occurs while loading the JSON files
	 */
	public List<ID> addScraper(Path path) throws IOException {
		return loadItemsFromFile(Scraper::loadFromJSON, p -> p.endsWith(".json"), path, scrapers::put);
	}
	
	/**
	 * Adds a {@link Feed} or a folder of {@link Feed Feeds} to this {@link FeedManager}.
	 * 
	 * @param path
	 *            a {@link Path} to a JSON file defining a {@link Feed} or a folder of {@link Feed Feeds}
	 * @return the IDs of the added {@link Feed Feeds}
	 * @throws IOException
	 *             if an error occurs while loading the JSON files
	 */
	public List<ID> addFeed(Path path) throws IOException {
		return loadItemsFromFile(p -> Feed.loadFromJSON(p, scrapers), p -> p.endsWith(".json"), path, feeds::put);
	}
	
	/**
	 * Removes the {@link Scraper} with the given ID.
	 * 
	 * @param id
	 *            the ID of the {@link Scraper} to remove
	 * @return the removed {@link Scraper} or {@code null}
	 */
	public Scraper removeScraper(ID id) {
		return scrapers.remove(id);
	}
	
	/**
	 * Removes the {@link Feed} with the given ID.
	 * 
	 * @param id
	 *            the ID of the {@link Feed} to remove
	 * @return the removed {@link Feed} or {@code null}
	 */
	public Feed removeFeed(ID id) {
		return feeds.remove(id);
	}
	
	@Override
	public List<RawArticle> get() {
		List<RawArticle> out = new ArrayList<>();
		for (Downloader downloader : feeds.values())
			out.addAll(downloader.get());
		return out;
	}
}
