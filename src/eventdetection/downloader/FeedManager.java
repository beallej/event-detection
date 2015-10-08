package eventdetection.downloader;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import eventdetection.common.ID;
import eventdetection.common.IDAble;
import eventdetection.temporarylibraryplaceholders.IOExceptedFunction;

public class FeedManager implements Downloader {
	private final Map<ID, Scraper> scrapers;
	private final Map<ID, Feed> feeds;
	
	public FeedManager(Path feedFolder, Path scraperFolder) throws IOException {
		scrapers = new LinkedHashMap<>(); //We might want to keep the order consistent...
		feeds = new LinkedHashMap<>(); //We might want to keep the order consistent...
		loadItemsFromFile(p -> Feed.loadFromJSON(p, scrapers), p -> p.endsWith(".json"), feedFolder, feeds::put);
		loadItemsFromFile(Scraper::loadFromJSON, p -> p.endsWith(".json"), scraperFolder, scrapers::put);
	}
	
	//What?  So it's a bit long...
	public static <T extends IDAble> void loadItemsFromFile(IOExceptedFunction<Path, T> loader, Filter<Path> filter, Path folder, BiFunction<ID, T, T> store) throws IOException {
		for (Path p : Files.newDirectoryStream(folder, filter)) {
			try {
				T t = loader.apply(p);
				store.apply(t.getID(), t);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public ID addScraper(Path file) throws IOException {
		Scraper s = Scraper.loadFromJSON(file);
		scrapers.put(s.getID(), s);
		return s.getID();
	}
	
	public ID addFeed(Path file) throws IOException {
		Feed f = Feed.loadFromJSON(file, scrapers);
		feeds.put(f.getID(), f);
		return f.getID();
	}
	
	public Scraper removeScraper(ID id) {
		return scrapers.remove(id);
	}
	
	public Feed removeFeed(ID id) {
		return feeds.remove(id);
	}
	
	@Override
	public List<RawArticle> get() {
		List<RawArticle> out = new ArrayList<>();
		for (Downloader downloader : feeds.values())
			out.addAll(downloader.get());
		return out;
	}
}
