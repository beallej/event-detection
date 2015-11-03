package eventdetection.downloader;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import toberumono.json.JSONArray;
import toberumono.json.JSONData;
import toberumono.json.JSONNumber;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;

import eventdetection.common.ArticleManager;

public class DownloaderController {
	
	public static void main(String[] args) throws IOException, SQLException {
		JSONObject config = (JSONObject) JSONSystem.loadJSON(Paths.get(args.length > 0 ? args[0] : "configuration.json"));
		for (Entry<String, JSONData<?>> e : ((JSONObject) config.get("database")).entrySet()) {
			String key = "db." + e.getKey();
			if (System.getProperty(key) != null)
				continue;
			Object val = e.getValue().value();
			if (val == null)
				continue;
			System.setProperty(key, val.toString().toLowerCase());
		}
		try (DownloaderCollection dc = new DownloaderCollection()) {
			JSONObject paths = (JSONObject) config.get("paths");
			for (JSONData<?> str : ((JSONArray) paths.get("sources")).value())
				Downloader.loadSource(Paths.get(str.toString()));
			ArticleManager am = new ArticleManager(dc.getConnection(), "articles",
					((JSONArray) paths.get("articles")).stream().collect(LinkedHashSet::new, (s, p) -> s.add(Paths.get(p.toString())), LinkedHashSet::addAll));
			Calendar oldest = Calendar.getInstance();
			JSONObject deletionDelay = (JSONObject) ((JSONObject) config.get("articles")).get("deletion-delay");
			oldest.add(Calendar.YEAR, -((Number) deletionDelay.get("years").value()).intValue());
			oldest.add(Calendar.MONTH, -((Number) deletionDelay.get("months").value()).intValue());
			oldest.add(Calendar.WEEK_OF_MONTH, -((Number) deletionDelay.get("weeks").value()).intValue());
			oldest.add(Calendar.DAY_OF_MONTH, -((Number) deletionDelay.get("days").value()).intValue());
			oldest.add(Calendar.HOUR_OF_DAY, -((Number) deletionDelay.get("hours").value()).intValue());
			oldest.add(Calendar.MINUTE, -((Number) deletionDelay.get("minutes").value()).intValue());
			oldest.add(Calendar.SECOND, -((Number) deletionDelay.get("seconds").value()).intValue());
			am.cleanUpArticles(oldest);
			FeedManager fm = new FeedManager();
			for (JSONData<?> str : ((JSONArray) paths.get("scrapers")).value())
				fm.addScraper(Paths.get(str.toString()));
			for (JSONData<?> str : ((JSONArray) paths.get("feeds")).value())
				fm.addFeed(Paths.get(str.toString()));
			fm.addFeed(Downloader.getConnection(), "feeds");
			dc.addDownloader(fm);
			NLPFunction nlpf = new NLPFunction();
			for (RawArticle ra : dc.get())
				am.store(nlpf.apply(ra));
		}
	}
}
