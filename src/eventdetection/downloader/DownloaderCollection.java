package eventdetection.downloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implements a {@link Downloader} that queries a list of other {@link Downloader Downloaders} and returns the
 * {@link RawArticle RawArticles} that they find.
 * 
 * @author Joshua Lipstone
 */
public class DownloaderCollection implements Downloader {
	private final Collection<Downloader> downloaders;
	
	/**
	 * Creates an empty {@link DownloaderCollection}
	 */
	public DownloaderCollection() {
		downloaders = new ArrayList<>();
	}
	
	/**
	 * Creates a {@link DownloaderCollection} that contains the {@link Downloader Downloaders} within the given
	 * {@link Collection}
	 * 
	 * @param downloaders
	 *            the {@link Collection} of {@link Downloader Downloaders}
	 */
	public DownloaderCollection(Collection<Downloader> downloaders) {
		this();
		getDownloaders().addAll(downloaders);
	}
	
	/**
	 * Adds the given {@link Downloader} to this {@link DownloaderCollection}
	 * 
	 * @param downloader
	 *            the {@link Downloader} to add
	 */
	public void addDownloader(Downloader downloader) {
		getDownloaders().add(downloader);
	}
	
	@Override
	public List<RawArticle> get() {
		List<RawArticle> out = new ArrayList<>();
		for (Downloader downloader : getDownloaders())
			out.addAll(downloader.get());
		return out;
	}
	
	/**
	 * @return the {@link Downloader Downloaders} to which this {@link DownloaderCollection} forwards
	 */
	public Collection<Downloader> getDownloaders() {
		return downloaders;
	}
}
