package eventdetection.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import toberumono.json.JSONArray;
import toberumono.json.JSONData;
import toberumono.json.JSONObject;
import toberumono.json.JSONRepresentable;
import toberumono.json.JSONString;
import toberumono.json.JSONSystem;
import toberumono.structures.tuples.Pair;

import eventdetection.common.IDAble;

/**
 * A mechanism for scraping article text from online sources
 * 
 * @author Joshua Lipstone
 */
public class Scraper implements IDAble, JSONRepresentable {
	private final List<Pair<Pattern, String>> sectioning;
	private final List<Pair<Pattern, String>> filtering;
	private final String id;
	private final JSONObject json;
	private static final Function<Pair<Pattern, String>, JSONData<?>> filterToJSON = e -> {
		JSONArray arr = new JSONArray();
		arr.add(new JSONString(e.getX().toString()));
		arr.add(new JSONString(e.getY()));
		return arr;
	};
	
	/**
	 * Creates a {@link Scraper} with the given id and patterns.
	 * 
	 * @param id
	 *            the ID of the {@link Scraper}
	 * @param sectioning
	 *            the pattern/replacement combinations used to extract article text from an article
	 * @param filtering
	 *            the pattern/replacement combinations used to clean the extracted text
	 */
	public Scraper(String id, List<Pair<Pattern, String>> sectioning, List<Pair<Pattern, String>> filtering) {
		this(id, sectioning, filtering, null);
	}
	
	/**
	 * Creates a {@link Scraper} with the given id and patterns.
	 * 
	 * @param id
	 *            the ID of the {@link Scraper}
	 * @param sectioning
	 *            the pattern/replacement combinations used to extract article text from an article
	 * @param filtering
	 *            the pattern/replacement combinations used to clean the extracted text
	 * @param json
	 *            the {@link JSONObject} on which the {@link Scraper} is based
	 */
	public Scraper(String id, List<Pair<Pattern, String>> sectioning, List<Pair<Pattern, String>> filtering, JSONObject json) {
		this.sectioning = sectioning;
		this.id = id;
		this.filtering = filtering;
		if (json == null) {
			json = new JSONObject();
			json.put("id", new JSONString(getID()));
			json.put("sectioning", JSONArray.wrap(sectioning, filterToJSON));
			json.put("filtering", JSONArray.wrap(filtering, filterToJSON));
		}
		this.json = json;
	}
	
	/**
	 * Scrapes the text from the page at the given URL.
	 * 
	 * @param url
	 *            the URL of the page as a {@link String}
	 * @return the scraped text
	 * @throws IOException
	 *             if the text cannot be read from the URL
	 */
	public String scrape(String url) throws IOException {
		return scrape(new URL(url));
	}
	
	/**
	 * Scrapes the text from the page at the given {@link URL}.
	 * 
	 * @param url
	 *            the {@link URL} of the page
	 * @return the scraped text
	 * @throws IOException
	 *             if the text cannot be read from the {@link URL}
	 */
	public String scrape(URL url) throws IOException {
		try (InputStream is = url.openStream()) {
			return scrape(is);
		}
	}
	
	/**
	 * Scrapes the text in the given {@link InputStream}.
	 * 
	 * @param is
	 *            the {@link InputStream} containing the text to scrape
	 * @return the scraped text
	 */
	public String scrape(InputStream is) {
		//This disables the delimiter and then uses the scanner to convert the stream from the URL into text
		try (@SuppressWarnings("resource")
		Scanner sc = new Scanner(is).useDelimiter("\\A")) {
			return scrape(sc);
		}
	}
	
	/**
	 * Scrapes the text in the given {@link Scanner}.
	 * 
	 * @param sc
	 *            the {@link Scanner} containing the text to scrape
	 * @return the scraped text
	 */
	public String scrape(Scanner sc) {
		StringBuilder sb = new StringBuilder();
		while (sc.hasNext())
			sb.append(sc.next());
		String separated = separate(sb.toString(), sectioning);
		if (separated == null)
			return null;
		return filter(separated, filtering);
	}
	
	/**
	 * Extracts the text that composes an article from the given page
	 * 
	 * @param page
	 *            the page from which to extract the text as a {@link String}
	 * @param rules
	 *            the rules to use to extract the text
	 * @return the extracted text
	 */
	public static String separate(String page, List<Pair<Pattern, String>> rules) {
		StringBuffer sb = new StringBuffer();
		boolean didFind = false;
		for (Pair<Pattern, String> rule : rules) {
			int offset = 0, lastEnd = 0;
			boolean found = false;
			Matcher m = rule.getX().matcher(page);
			while (m.find()) {
				found = true;
				m.appendReplacement(sb, rule.getY());
				sb.delete(offset, offset + m.start() - lastEnd);
				offset = sb.length();
				lastEnd = m.end();
			}
			if (found) {
				didFind = true;
				page = sb.toString();
			}
			sb.delete(0, sb.length()); //Clear the buffer
		}
		if (!didFind)
			return null;
		return page;
	}
	
	/**
	 * Filters already scraped text.
	 * 
	 * @param text
	 *            the text to filter
	 * @param rules
	 *            the rules to use to filter the text
	 * @return the filtered text
	 */
	public static String filter(String text, List<Pair<Pattern, String>> rules) {
		for (Pair<Pattern, String> rule : rules)
			text = rule.getX().matcher(text).replaceAll(rule.getY());
		return text;
	}
	
	/**
	 * @return the ID
	 */
	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Scraper))
			return false;
		Scraper s = (Scraper) o;
		return getID().equals(s.getID()) && sectioning.equals(s.sectioning) && filtering.equals(s.filtering);
	}
	
	@Override
	public int hashCode() {
		return getID().hashCode();
	}
	
	/**
	 * Loads a {@link Scraper} from a JSON file
	 * 
	 * @param json
	 *            a {@link Path} to the JSON file
	 * @return the {@link Scraper} described in the JSON file
	 * @throws IOException
	 *             an I/O error occurs
	 */
	public static Scraper loadFromJSON(Path json) throws IOException {
		JSONObject config = (JSONObject) JSONSystem.loadJSON(json);
		String id = (String) config.get("id").value();
		List<Pair<Pattern, String>> sectioning = new ArrayList<>();
		for (JSONData<?> r : (JSONArray) config.get("sectioning").value()) {
			JSONArray rule = (JSONArray) r.value();
			sectioning.add(new Pair<>(Pattern.compile(rule.get(0).value().toString()), rule.get(1).value().toString()));
		}
		List<Pair<Pattern, String>> filtering = new ArrayList<>();
		for (JSONData<?> r : (JSONArray) config.get("filtering").value()) {
			JSONArray rule = (JSONArray) r.value();
			filtering.add(new Pair<>(Pattern.compile(rule.get(0).value().toString()), rule.get(1).value().toString()));
		}
		return new Scraper(id, sectioning, filtering);
	}
	
	@Override
	public JSONObject toJSONObject() {
		return json;
	}
}
