package eventdetection.downloader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DownloaderCollection implements Supplier<List<RawArticle>> {
	private final List<Supplier<List<RawArticle>>> downloaders;
	
	public DownloaderCollection() {
		downloaders = new ArrayList<>();
	}
	
	public DownloaderCollection(List<Supplier<List<RawArticle>>> downloaders) {
		this();
		this.getDownloaders().addAll(downloaders);
	}
	
	public void addDownloader(Supplier<List<RawArticle>> downloader) {
		this.getDownloaders().add(downloader);
	}
	
	@Override
	public List<RawArticle> get() {
		List<RawArticle> out = new ArrayList<>();
		for (Supplier<List<RawArticle>> downloader : getDownloaders())
			out.addAll(downloader.get());
		return out;
	}
	
	/**
	 * @return the downloaders that this {@link DownloaderCollection} forwards to
	 */
	public List<Supplier<List<RawArticle>>> getDownloaders() {
		return downloaders;
	}
}
