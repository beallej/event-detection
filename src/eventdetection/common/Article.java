package eventdetection.common;

import java.net.MalformedURLException;
import java.net.URL;

import eventdetection.downloader.POSTagger;

/**
 * Represents an {@link Article} that has been processed.
 * 
 * @author Joshua Lipstone
 */
public class Article {
	private final String title, text;
	private final URL url;
	private final Source source;
	private final boolean isTagged;
	private Article alternate;
	
	/**
	 * Initializes a {@link Article}
	 * 
	 * @param title
	 *            the title of the article, which can be PoS tagged
	 * @param text
	 *            the text of the article, which can be PoS tagged
	 * @param url
	 *            the URL of the full article as a {@link String}
	 * @param source
	 *            the {@link Source} that the article is from
	 * @param isTagged
	 *            whether the {@link Article} have tagged text
	 * @throws MalformedURLException
	 *             if the given <tt>url</tt> is incorrectly formatted
	 */
	public Article(String title, String text, String url, Source source, boolean isTagged) throws MalformedURLException {
		this(title, text, new URL(url), source, isTagged);
	}
	
	/**
	 * Initializes a {@link Article}
	 * 
	 * @param title
	 *            the title of the article, which can be PoS tagged
	 * @param text
	 *            the text of the article, which can be PoS tagged
	 * @param url
	 *            the {@link URL} of the full article
	 * @param source
	 *            the {@link Source} that the article is from
	 * @param isTagged
	 *            whether the {@link Article} have tagged text
	 */
	public Article(String title, String text, URL url, Source source, boolean isTagged) {
		this.title = title;
		this.text = text;
		this.url = url;
		this.source = source;
		this.isTagged = isTagged;
	}
	
	/**
	 * Initializes a {@link Article}
	 * 
	 * @param title
	 *            the title of the article, which can be PoS tagged
	 * @param text
	 *            the text of the article, which can be PoS tagged
	 * @param url
	 *            the {@link URL} of the full article
	 * @param source
	 *            the {@link Source} that the article is from
	 * @param isTagged
	 *            whether the {@link Article} have tagged text
	 * @param alternate
	 *            an {@link Article} that has the same data as this one but whose text is either tagged if this one is not
	 *            tagged or not tagged if this one is tagged
	 */
	public Article(String title, String text, URL url, Source source, boolean isTagged, Article alternate) {
		this(title, text, url, source, isTagged);
		if (alternate.isTagged != this.isTagged)
			this.alternate = alternate;
	}
	
	/**
	 * @return the title of the {@link Article}
	 */
	public final String getTitle() {
		return title;
	}
	
	/**
	 * @return the text of the {@link Article}, which can be PoS tagged
	 */
	public final String getText() {
		return text;
	}
	
	/**
	 * @return the {@link URL} of the full article
	 */
	public final URL getURL() {
		return url;
	}
	
	/**
	 * @return the {@link Source} of the {@link Article}
	 */
	public final Source getSource() {
		return source;
	}
	
	/**
	 * @return {@code true} iff the {@link Article Article's} text is POS-tagged
	 */
	public boolean isTagged() {
		return isTagged;
	}
	
	/**
	 * @return an {@link Article} with the untagged version of the text. If the {@link Article} already has untagged text, it
	 *         returns itself.
	 */
	public Article getUntagged() {
		if (!isTagged)
			return this;
		if (alternate != null)
			return alternate;
		return alternate = new Article(POSTagger.untag(getTitle()), POSTagger.untag(getText()), getURL(), getSource(), false);
	}
	
	/**
	 * @return an {@link Article} with the tagged version of the text. If the {@link Article} already has tagged text, it
	 *         returns itself.
	 */
	public Article getTagged() {
		if (isTagged)
			return this;
		if (alternate != null)
			return alternate;
		return alternate = new Article(POSTagger.tag(getTitle()), POSTagger.tag(getText()), getURL(), getSource(), true);
	}
	
	@Override
	public String toString() {
		String out = "Title: " + getTitle();
		out += "\nURL: " + getURL();
		out += "\n" + getText();
		return out;
	}
}