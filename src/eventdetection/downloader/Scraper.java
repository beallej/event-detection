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

public abstract class Scraper implements IDAble {
	private final List<Pair<Pattern, String>> sectioning;
	private final List<Pair<Pattern, String>> filtering;
	private final ID id;
	
	public Scraper(ID id, List<Pair<Pattern, String>> sectioning, List<Pair<Pattern, String>> filtering) {
		this.sectioning = sectioning;
		this.id = id;
		this.filtering = filtering;
	}
	
	public String scrape(String url) throws IOException {
		return scrape(new URL(url));
	}
	
	public String scrape(URL url) throws IOException {
		StringBuilder sb = new StringBuilder();
		//This disables the delimiter and then uses the scanner to convert the stream from the URL into text
		try (InputStream is = url.openStream(); Scanner sc = new Scanner(is, "UTF-16").useDelimiter("\\A")) {
			while (sc.hasNext())
				sb.append(sc.next());
		}
		return filter(separate(sb.toString(), sectioning), filtering);
	}
	
	public static String separate(String text, List<Pair<Pattern, String>> rules) {
		StringBuffer sb = new StringBuffer();
		for (Pair<Pattern, String> rule : rules) {
			int offset = 0, lastEnd = 0;
			boolean found = false;
			Matcher m = rule.getX().matcher(text);
			while (m.find()) {
				found = true;
				m.appendReplacement(sb, rule.getY());
				sb.delete(offset, offset + m.start() - lastEnd);
				offset = sb.length();
				lastEnd = m.end();
			}
			if (found)
				text = sb.toString();
			sb.delete(0, sb.length()); //Clear the buffer
		}
		return text;
	}
	
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
	
	public static Scraper loadFromJSON(Path json) throws IOException {
		return null; //TODO We can't implement this without a JSON library
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
}
