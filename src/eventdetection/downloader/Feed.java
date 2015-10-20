package eventdetection.downloader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import eventdetection.common.IDAble;
import eventdetection.common.Source;

/**
 * Represents an RSS feed and provides methods for getting the articles from it.
 * 
 * @author Joshua Lipstone
 */
public abstract class Feed extends Downloader implements IDAble {
	private final String id;
	private final List<String> scraperIDs;
	private String lastSeen;
	private final Source source;
	private final URL url;
	private final Map<String, Scraper> scrapers;
	
	/**
	 * Initializes a {@link Feed}
	 * 
	 * @param id
	 *            the ID of the {@link Feed}
	 * @param source
	 *            the {@link Source} of the {@link Feed}
	 * @param lastSeen
	 *            the name of the last-seen article
	 * @param scraperIDs
	 *            the IDs of the {@link Scraper Scrapers} that the {@link Feed} can use
	 * @param url
	 *            the specific {@link URL} of the {@link Feed}
	 * @param scrapers
	 *            the {@link Scraper Scrapers} available to the {@link Feed}
	 */
	public Feed(String id, Source source, List<String> scraperIDs, String lastSeen, URL url, Map<String, Scraper> scrapers) {
		this.id = id;
		this.scraperIDs = scraperIDs;
		this.lastSeen = lastSeen;
		this.source = source;
		this.url = url;
		this.scrapers = scrapers;
	}
	
	@Override
	public List<RawArticle> get() {
		List<RawArticle> out = new ArrayList<>();
		Scraper s = getScraper();
		if (s == null)
			return out;
		SyndFeedInput input = new SyndFeedInput();
		try {
			SyndFeed feed = input.build(new XmlReader(url));
			for (SyndEntry e : feed.getEntries()) {
				
			}
		}
		catch (IllegalArgumentException | FeedException | IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	@Override
	public String getID() {
		return id;
	}
	
	/**
	 * @return the name of the last-seen article. Downloading proceeds from the article immediately after this one
	 */
	public String getLastSeen() {
		return lastSeen;
	}
	
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
	 * @return the {@link Scraper} to use or {@code null} if one is not found
	 */
	private Scraper getScraper() {
		Scraper out = null;
		for (String id : scraperIDs)
			if ((out = scrapers.get(id)) != null)
				return out;
		return out;
	}
	
	@Override
	public int hashCode() {
		return getID().hashCode();
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
