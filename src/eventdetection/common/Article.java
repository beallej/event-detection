package eventdetection.common;

import java.net.MalformedURLException;
import java.net.URL;

import eventdetection.downloader.Scraper;

public class Article {
	private final String title, taggedText, rawText;
	private final URL url;
	private final Source source;
	
	/**
	 * Initializes a {@link Article}
	 * 
	 * @param title
	 *            the title of the article
	 * @param taggedText
	 *            the PoS tagged text of the article
	 * @param rawText
	 *            the plain text of the article (from a {@link Scraper})
	 * @param url
	 *            the URL of the full article as a {@link String}
	 * @param source
	 *            the {@link Source} that the article is from
	 * @throws MalformedURLException
	 *             if the given <tt>url</tt> is incorrectly formatted
	 */
	public Article(String title, String taggedText, String rawText, String url, Source source) throws MalformedURLException {
		this(title, taggedText, rawText, new URL(url), source);
	}
	
	/**
	 * Initializes a {@link Article}
	 * 
	 * @param title
	 *            the title of the article
	 * @param taggedText
	 *            the PoS tagged text of the article
	 * @param rawText
	 *            the plain text of the article (from a {@link Scraper})
	 * @param url
	 *            the {@link URL} of the full article
	 * @param source
	 *            the {@link Source} that the article is from
	 */
	public Article(String title, String taggedText, String rawText, URL url, Source source) {
		this.title = title;
		this.taggedText = taggedText;
		this.rawText = rawText;
		this.url = url;
		this.source = source;
	}
	
	/**
	 * @return the title
	 */
	public final String getTitle() {
		return title;
	}
	
	/**
	 * @return the PoS tagged text
	 */
	public final String getTaggedText() {
		return taggedText;
	}
	
	/**
	 * @return the scraped text
	 */
	public final String getRawText() {
		return rawText;
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
	
	@Override
	public String toString() {
		String out = "Title: " + getTitle();
		out += "\nURL: " + getUrl();
		out += "\n" + getTaggedText();
		return out;
	}
}