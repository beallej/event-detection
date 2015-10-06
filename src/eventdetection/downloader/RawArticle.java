package eventdetection.downloader;

import java.net.URL;

import eventdetection.common.Source;

public class RawArticle {
	private final String title, text;
	private final URL url;
	private final Source source;
	
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
	 * @return the {@link URL} of the original article
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
	
}
