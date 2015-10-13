package eventdetection.downloader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import eventdetection.common.ID;
import eventdetection.common.IDAble;
import eventdetection.common.Source;

/**
 * Represents an RSS feed and provides methods for getting the articles from it.
 * 
 * @author Joshua Lipstone
 */
public abstract class Feed implements Downloader, IDAble {
	private final ID id;
	private final Source source;
	private final URL url;
	private final Map<ID, Scraper> scrapers;
	
	/**
	 * Initializes a {@link Feed}
	 * 
	 * @param id
	 *            the ID of the {@link Feed}
	 * @param source
	 *            the {@link Source} of the {@link Feed}
	 * @param url
	 *            the specific {@link URL} of the {@link Feed}
	 * @param scrapers
	 *            the {@link Scraper Scrapers} available to the {@link Feed}
	 */
	public Feed(ID id, Source source, URL url, Map<ID, Scraper> scrapers) {
		this.id = id;
		this.source = source;
		this.url = url;
		this.scrapers = scrapers;
	}
	
	@Override
	public abstract List<RawArticle> get();
	
	@Override
	public ID getID() {
		return id;
	}
	
	/**
	 * @return the name of the last-seen article. Downloading proceeds from the article immediately after this one
	 */
	public abstract String getLastSeen();
	
	/**
	 * This is to account for news sites such as NYT and CNN having multiple feeds.
	 * 
	 * @return the {@link Source} that this {@link Feed} is from
	 */
	public Source getSource() {
		return source;
	}
	
	/**
	 * @return the {@link URL} of the {@link Feed}
	 */
	public URL getURL() {
		return url;
	}
	
	/**
	 * Loads a {@link Feed} from a JSON file
	 * 
	 * @param file
	 *            a {@link Path} to the JSON file
	 * @param scrapers
	 *            the available {@link Scraper Scrapers}
	 * @return the {@link Feed} described in the JSON file
	 * @throws IOException
	 *             an I/O error occurs
	 */
	public static Feed loadFromJSON(Path file, Map<ID, Scraper> scrapers) throws IOException {
		return null; //TODO We can't implement this without a JSON library
	}
}
