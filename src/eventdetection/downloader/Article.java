package eventdetection.downloader;


import java.net.MalformedURLException;
import java.net.URL;

import eventdetection.common.Source;

public class Article {
	private final String title, text;
	private final URL url;
	private final Source source;
	
	/**
	 * Initializes a {@link RawArticle}
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
	public RawArticle(String title, String text, String url, Source source) throws MalformedURLException {
		this(title, text, new URL(url), source);
	}
	
	/**
	 * Initializes a {@link RawArticle}
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
	public RawArticle(String title, String text, URL url, Source source) {
		this.title = title;
		this.text = text;
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
	
	@Override
	public String toString() {
		String out = "Title: " + getTitle();
		out += "\nURL: " + getUrl();
		out += "\n" + getText();
		return out;
	}
}