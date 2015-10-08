package eventdetection.downloader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DownloaderCollection implements Downloader {
	private final List<Downloader> downloaders;
	
	public DownloaderCollection() {
		downloaders = new ArrayList<>();
	}
	
	public DownloaderCollection(List<Downloader> downloaders) {
		this();
		this.getDownloaders().addAll(downloaders);
	}
	
	public void addDownloader(Downloader downloader) {
		this.getDownloaders().add(downloader);
	}
	
	@Override
	public List<RawArticle> get() {
		List<RawArticle> out = new ArrayList<>();
		for (Downloader downloader : getDownloaders())
			out.addAll(downloader.get());
		return out;
	}
	
	/**
	 * @return the downloaders that this {@link DownloaderCollection} forwards to
	 */
	public List<Downloader> getDownloaders() {
		return downloaders;
	}
}
