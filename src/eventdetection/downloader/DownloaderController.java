package eventdetection.downloader;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedHashSet;

import toberumono.json.JSONArray;
import toberumono.json.JSONBoolean;
import toberumono.json.JSONData;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;

import eventdetection.common.ArticleManager;

/**
 * Main class of the downloader. Controls startup and and article management.
 * 
 * @author Joshua Lipstone
 */
public class DownloaderController {
	
	/**
	 * The main method.
	 * 
	 * @param args
	 *            command line arguments
	 * @throws SQLException
	 *             if an SQL error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException, SQLException {
		JSONObject config = (JSONObject) JSONSystem.loadJSON(Paths.get(args.length > 0 ? args[0] : "configuration.json"));
		Downloader.configureConnection((JSONObject) config.get("database"));
		try (DownloaderCollection dc = new DownloaderCollection()) {
			JSONObject paths = (JSONObject) config.get("paths");
			JSONObject articles = (JSONObject) config.get("articles");
			Downloader.loadSource(Downloader.getConnection(), "sources");
			for (JSONData<?> str : ((JSONArray) paths.get("sources")).value())
				Downloader.loadSource(Paths.get(str.toString()));
			FeedManager fm = new FeedManager();
			for (JSONData<?> str : ((JSONArray) paths.get("scrapers")).value())
				fm.addScraper(Paths.get(str.toString()));
			for (JSONData<?> str : ((JSONArray) paths.get("feeds")).value())
				fm.addFeed(Paths.get(str.toString()));
			fm.addFeed(Downloader.getConnection(), "feeds");
			dc.addDownloader(fm);
			ArticleManager am = new ArticleManager(dc.getConnection(), "articles",
					((JSONArray) paths.get("articles")).stream().collect(LinkedHashSet::new, (s, p) -> s.add(Paths.get(p.toString())), LinkedHashSet::addAll),
					((JSONBoolean) articles.get("enable-pos-tagging")).value());
			Calendar oldest = computeOldest((JSONObject) articles.get("deletion-delay"));
			Path active = Paths.get(System.getProperty("user.home"), ".event-detection-active");
			if (!Files.exists(active)) {
				Files.createDirectories(active.getParent());
				Files.createFile(active);
			}
			try (FileChannel chan = FileChannel.open(active, StandardOpenOption.CREATE, StandardOpenOption.WRITE); FileLock lock = chan.lock();) {
				am.removeArticlesBefore(oldest);
				for (RawArticle ra : dc.get()) {
					am.store(am.process(ra));
					System.out.println("Processed: " + ra.getTitle());
				}
			}
		}
	}
	
	private static Calendar computeOldest(JSONObject deletionDelay) {
		Calendar oldest = Calendar.getInstance();
		oldest.add(Calendar.YEAR, -((Number) deletionDelay.get("years").value()).intValue());
		oldest.add(Calendar.MONTH, -((Number) deletionDelay.get("months").value()).intValue());
		oldest.add(Calendar.WEEK_OF_MONTH, -((Number) deletionDelay.get("weeks").value()).intValue());
		oldest.add(Calendar.DAY_OF_MONTH, -((Number) deletionDelay.get("days").value()).intValue());
		oldest.add(Calendar.HOUR_OF_DAY, -((Number) deletionDelay.get("hours").value()).intValue());
		oldest.add(Calendar.MINUTE, -((Number) deletionDelay.get("minutes").value()).intValue());
		oldest.add(Calendar.SECOND, -((Number) deletionDelay.get("seconds").value()).intValue());
		return oldest;
	}
}
