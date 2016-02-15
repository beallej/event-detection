package eventdetection.downloader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Map;

import toberumono.json.JSONArray;
import toberumono.json.JSONBoolean;
import toberumono.json.JSONData;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;
import toberumono.structures.tuples.Pair;

import eventdetection.common.Article;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;
import eventdetection.common.Query;
import eventdetection.common.ThreadingUtils;
import eventdetection.pipeline.Pipeline;
import eventdetection.pipeline.PipelineComponent;

/**
 * Main class of the downloader. Controls startup and and article management.
 * 
 * @author Joshua Lipstone
 */
public class DownloaderController extends DownloaderCollection implements PipelineComponent {
	private final ArticleManager am;
	private final Instant oldest;
	private boolean closed;
	
	/**
	 * Constructs a new {@link DownloaderController} with the given configuration data. This is for use with
	 * {@link Pipeline}.
	 * 
	 * @param config
	 *            the configuration data
	 * @throws SQLException
	 *             if an error occurs while connecting to the database
	 * @throws IOException
	 *             if an error occurs while loading data from the file system
	 */
	public DownloaderController(JSONObject config) throws SQLException, IOException {
		super();
		updateJSONConfiguration(config);
		Connection connection = DBConnection.getConnection();
		JSONObject paths = (JSONObject) config.get("paths");
		JSONObject articles = (JSONObject) config.get("articles");
		JSONObject tables = (JSONObject) config.get("tables");
		am = new ArticleManager(connection, tables.get("articles").value().toString(), paths, articles);
		oldest = computeOldest((JSONObject) articles.get("deletion-delay")).toInstant();
		Downloader.loadSource(connection, tables.get("sources").value().toString());
		for (JSONData<?> str : ((JSONArray) paths.get("sources")).value())
			Downloader.loadSource(Paths.get(str.toString()));
		
		FeedManager fm = new FeedManager(connection);
		for (JSONData<?> str : ((JSONArray) paths.get("scrapers")).value())
			fm.addScraper(Paths.get(str.toString()));
		for (JSONData<?> str : ((JSONArray) paths.get("feeds")).value())
			fm.addFeed(Paths.get(str.toString()));
		fm.addFeed(connection, tables.get("feeds").value().toString());
		addDownloader(fm);
	}
	
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
		try (DownloaderController dc = new DownloaderController(config)) {
			dc.execute();
		}
	}
	
	@Override
	public Pair<Map<Integer, Query>, Map<Integer, Article>> execute(Pair<Map<Integer, Query>, Map<Integer, Article>> inputs) throws IOException, SQLException {
		Map<Integer, Article> articles = inputs.getY();
		ThreadingUtils.executeTask(() -> {
			am.removeArticlesBefore(oldest);
			for (Article article : get()) {
				article = am.store(article);
				articles.put(article.getID(), article);
			}
		});
		return inputs;
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
	
	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		am.close();
		super.close();
	}
}
