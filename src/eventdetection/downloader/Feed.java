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

import toberumono.json.JSONArray;
import toberumono.json.JSONObject;
import toberumono.json.JSONRepresentable;
import toberumono.json.JSONString;
import toberumono.json.JSONSystem;

import eventdetection.common.IDAble;
import eventdetection.common.Source;

/**
 * Represents an RSS feed and provides methods for getting the articles from it.
 * 
 * @author Joshua Lipstone
 */
public class Feed extends Downloader implements IDAble, JSONRepresentable {
	private final String id;
	private final List<String> scraperIDs;
	private String lastSeen;
	private final Source source;
	private final URL url;
	private final Map<String, Scraper> scrapers;
	private final JSONObject json;
	private final Path file;
	private boolean closed;
	
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
		this(id, source, scraperIDs, lastSeen, url, scrapers, null, null);
	}
	
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
	 * @param json
	 *            the {@link JSONObject} on which the {@link Feed} is based
	 * @param file
	 *            the {@link Path} to the file from which the {@link Feed} was loaded
	 */
	public Feed(String id, Source source, List<String> scraperIDs, String lastSeen, URL url, Map<String, Scraper> scrapers, JSONObject json, Path file) {
		this.id = id;
		this.source = source;
		this.url = url;
		this.lastSeen = lastSeen;
		this.scraperIDs = scraperIDs;
		this.scrapers = scrapers;
		if (json == null) {
			json = new JSONObject();
			json.put("id", new JSONString(getID()));
			json.put("source", new JSONString(source.getID()));
			json.put("url", new JSONString(getURL().toString()));
			json.put("scraperIDs", JSONArray.wrap(scraperIDs));
			json.put("lastSeen", new JSONString(lastSeen));
		}
		this.json = json;
		this.file = file;
		closed = false;
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
				System.out.println(e.getTitle());
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
	@SuppressWarnings("unchecked")
	public static Feed loadFromJSON(Path file, Map<String, Scraper> scrapers) throws IOException {
		JSONObject json = (JSONObject) JSONSystem.loadJSON(file);
		List<String> scraperIDs = new ArrayList<>();
		for (JSONString s : (List<JSONString>) json.get("scraperIDs").value())
			scraperIDs.add(s.value());
		URL url = new URL((String) json.get("url").value());
		Source source = Downloader.sources.get(json.get("source"));
		String lastSeen = json.containsKey("lastSeen") ? (String) json.get("lastSeen").value() : null;
		return new Feed((String) json.get("id").value(), source, scraperIDs, lastSeen, url, scrapers);
	}
	
	@Override
	public JSONObject toJSONObject() {
		return json;
	}
	
	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		if (lastSeen != null)
			json.put("lastSeen", new JSONString(lastSeen));
		if (file != null)
			JSONSystem.writeJSON(toJSONObject(), file);
	}
}
