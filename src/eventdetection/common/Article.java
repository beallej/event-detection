package eventdetection.common;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import edu.stanford.nlp.pipeline.Annotation;
import eventdetection.downloader.POSTagger;

/**
 * Represents an {@link Article} that has been processed. All {@link Annotation} processing and PoS tagging is done lazily. A
 * call to {@link #process()} will immediately perform all processing tasks.
 * 
 * @author Joshua Lipstone
 */
public class Article implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String[] titles, texts;
	private Annotation title;
	private Annotation[] text;
	private final URL url;
	private final Source source;
	
	/**
	 * Initializes an {@link Article}
	 * 
	 * @param title
	 *            the title of the article as untagged text
	 * @param text
	 *            the text of the article as untagged text
	 * @param url
	 *            the URL of the full article as a {@link String}
	 * @param source
	 *            the {@link Source} that the article is from
	 * @throws MalformedURLException
	 *             if the given <tt>url</tt> is incorrectly formatted
	 */
	public Article(String title, String text, String url, Source source) throws MalformedURLException {
		this(title, text, new URL(url), source);
	}
	
	/**
	 * Initializes an {@link Article}
	 * 
	 * @param title
	 *            the title of the article as untagged text
	 * @param text
	 *            the text of the article as untagged text
	 * @param url
	 *            the {@link URL} of the full article
	 * @param source
	 *            the {@link Source} that the article is from
	 */
	public Article(String title, String text, URL url, Source source) {
		this.titles = new String[]{title, null};
		this.texts = new String[]{text, null};
		this.url = url;
		this.source = source;
		this.title = null;
		this.text = null;
	}
	
	/**
	 * Immediately performs all of the lazily evaluations that remain for the {@link Article Article's} fields. Call this
	 * prior to serialization.
	 */
	public final void process() {
		getAnnotatedTitle();
		getAnnotatedText();
		getTaggedTitle();
		getTaggedText();
	}
	
	/**
	 * @return the untagged title of the {@link Article}
	 */
	public final String getUntaggedTitle() {
		return titles[0];
	}
	
	/**
	 * @return the untagged text of the {@link Article}
	 */
	public final String getUntaggedText() {
		return texts[0];
	}
	
	/**
	 * @return the PoS tagged title of the {@link Article}
	 */
	public final String getTaggedTitle() {
		if (titles[1] == null)
			titles[1] = POSTagger.tag(getAnnotatedTitle());
		return titles[1];
	}
	
	/**
	 * @return the PoS tagged text of the {@link Article}
	 */
	public final String getTaggedText() {
		if (texts[1] == null)
			texts[1] = POSTagger.tagParagraphs(getAnnotatedText());
		return texts[1];
	}
	
	/**
	 * @return the {@link Annotation Annotated} title of the {@link Article}
	 */
	public final Annotation getAnnotatedTitle() {
		if (title == null)
			title = POSTagger.annotate(getUntaggedTitle());
		return title;
	}
	
	/**
	 * @return the {@link Annotation Annotated} text of the {@link Article}
	 */
	public final Annotation[] getAnnotatedText() {
		if (text == null)
			text = POSTagger.annotateParagraphs(getUntaggedText());
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
	
	@Override
	public String toString() {
		String out = "Title: " + getUntaggedTitle();
		out += "\nURL: " + getURL();
		out += "\n" + getUntaggedText();
		return out;
	}
}
