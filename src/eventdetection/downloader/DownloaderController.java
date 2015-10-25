package eventdetection.downloader;

import java.io.IOException;
import java.nio.file.Paths;

public class DownloaderController {

	public static void main(String[] args) throws IOException {
		Downloader.loadSource(Paths.get("./Sources/"));
		try (DownloaderCollection dc = new DownloaderCollection()) {
			FeedManager fm = new FeedManager(Paths.get("./Feeds/"), Paths.get("./Scrapers/"));
			dc.addDownloader(fm);
			NLPFunction nlpf = new NLPFunction();
			for (RawArticle ra : dc.get())
				System.out.println(nlpf.apply(ra).toString());
		}
	}
}
