package eventdetection.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eventdetection.common.ID;
import eventdetection.common.IDAble;
import eventdetection.temporarylibraryplaceholders.Pair;

/**
 * A mechanism for scraping article text from online sources
 * 
 * @author Joshua Lipstone
 */
public class Scraper implements IDAble {
	private final List<Pair<Pattern, String>> sectioning;
	private final List<Pair<Pattern, String>> filtering;
	private final ID id;
	
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
	public Scraper(ID id, List<Pair<Pattern, String>> sectioning, List<Pair<Pattern, String>> filtering) {
		this.sectioning = sectioning;
		this.id = id;
		this.filtering = filtering;
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
		//This disables the delimiter and then uses the scanner to convert the stream from the URL into text
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
		try (@SuppressWarnings("resource")
		Scanner sc = new Scanner(is, "UTF-16").useDelimiter("\\A")) {
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
		return filter(separate(sb.toString(), sectioning), filtering);
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
			if (found)
				page = sb.toString();
			sb.delete(0, sb.length()); //Clear the buffer
		}
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
	 * @return the {@link ID}
	 */
	@Override
	public ID getID() {
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
		return null; //TODO We can't implement this without a JSON library
	}
}
