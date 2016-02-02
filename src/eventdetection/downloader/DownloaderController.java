package eventdetection.downloader;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import toberumono.json.JSONArray;
import toberumono.json.JSONBoolean;
import toberumono.json.JSONData;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;

import eventdetection.common.Article;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;

/**
 * Main class of the downloader. Controls startup and and article management.
 * 
 * @author Joshua Lipstone
 */
public class DownloaderController {
	/**
	 * A work stealing pool for use by all classes in this program.
	 */
	public static final ExecutorService pool = Executors.newWorkStealingPool();
	
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
		Path configPath = Paths.get(args.length > 0 ? args[0] : "configuration.json");
		JSONObject config = (JSONObject) JSONSystem.loadJSON(configPath);
		updateJSONConfiguration(config);
		if (config.isModified())
			JSONSystem.writeJSON(config, configPath);
		DBConnection.configureConnection((JSONObject) config.get("database"));
		try (DownloaderCollection dc = new DownloaderCollection()) {
			Connection connection = DBConnection.getConnection();
			JSONObject paths = (JSONObject) config.get("paths");
			JSONObject articles = (JSONObject) config.get("articles");
			JSONObject tables = (JSONObject) config.get("tables");
			Downloader.loadSource(connection, tables.get("sources").value().toString());
			for (JSONData<?> str : ((JSONArray) paths.get("sources")).value())
				Downloader.loadSource(Paths.get(str.toString()));
				
			FeedManager fm = new FeedManager(connection);
			for (JSONData<?> str : ((JSONArray) paths.get("scrapers")).value())
				fm.addScraper(Paths.get(str.toString()));
			for (JSONData<?> str : ((JSONArray) paths.get("feeds")).value())
				fm.addFeed(Paths.get(str.toString()));
			fm.addFeed(connection, tables.get("feeds").value().toString());
			
			dc.addDownloader(fm);
			ArticleManager am = new ArticleManager(connection, tables.get("articles").value().toString(), paths, articles);
			
			Path active = Paths.get(System.getProperty("user.home"), ".event-detection-active");
			if (!Files.exists(active)) {
				Files.createDirectories(active.getParent());
				Files.createFile(active);
			}
			
			try (FileChannel chan = FileChannel.open(active, StandardOpenOption.CREATE, StandardOpenOption.WRITE); FileLock lock = chan.lock();) {
				Calendar oldest = computeOldest((JSONObject) articles.get("deletion-delay"));
				am.removeArticlesBefore(oldest);
				List<Article> processed = new ArrayList<>();
				for (Article article : dc.get())
					processed.add(am.store(article));
				for (Article done : processed)
					System.out.println("Finished Processing: " + done.getUntaggedTitle());
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
	
	private static void updateJSONConfiguration(JSONObject config) {
		JSONObject articles = (JSONObject) config.get("articles");
		JSONSystem.transferField("enable-pos-tagging", new JSONBoolean(true), articles, (JSONObject) articles.get("pos-tagging"));
		JSONSystem.transferField("enable-tag-simplification", new JSONBoolean(false), (JSONObject) articles.get("pos-tagging"));
	}
}
