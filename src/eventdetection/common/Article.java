package eventdetection.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import eventdetection.downloader.Scraper;

public class Article {
	private final String title, text;
	private final URL url;
	private final Source source;
	private List<String> keywords;
	private Map<String, String> tags;
	
	/**
	 * Initializes a {@link Article}
	 * 
	 * @param title
	 *            the title of the article
	 * @param text
	 *            the plain text of the article (from a {@link Scraper})
	 * @param url
	 *            the URL of the full article as a {@link String}
	 * @param source
	 *            the {@link Source} that the article is from
	 * @throws MalformedURLException
	 *             if the given <tt>url</tt> is incorrectly formatted
	 */
	public Article(String title, String text, String url, Source source, List<String> keywords) throws MalformedURLException {
		this(title, text, new URL(url), source, keywords);
	}
	
	/**
	 * Initializes a {@link Article}
	 * 
	 * @param title
	 *            the title of the article
	 * @param text
	 *            the plain text of the article (from a {@link Scraper})
	 * @param url
	 *            the {@link URL} of the full article
	 * @param source
	 *            the {@link Source} that the article is from
	 */
	public Article(String title, String text, URL url, Source source, List<String> keywords) {
		this.title = title;
		this.text = text;
		this.url = url;
		this.source = source;
		this.keywords = keywords;
	}
	
	/**
	 * @return the title
	 */
	public final String getTitle() {
		return title;
	}
	
	/**
	 * @return the scraped text
	 */
	public final String getText() {
		return text;
	}
	
	/**
	 * @return the {@link URL} of the full article
	 */
	public final URL getUrl() {
		return url;
	}
	
	/**
	 * @return the {@link Source}
	 */
	public final Source getSource() {
		return source;
	}
	
	/**
	 * @return the keywords associated with the article
	 */
	
	public final List<String> getKeywords() {
		return keywords;
	}
	
	@Override
	public String toString() {
		String out = "Title: " + getTitle();
		out += "\nURL: " + getUrl();
		out += "\n" + getText();
		return out;
	}
}