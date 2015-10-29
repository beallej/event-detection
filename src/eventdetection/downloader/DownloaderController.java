package eventdetection.downloader;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map.Entry;

import toberumono.json.JSONArray;
import toberumono.json.JSONData;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;

public class DownloaderController {
	
	public static void main(String[] args) throws IOException {
		JSONObject config = null;
		if (args.length > 0) {
			config = (JSONObject) JSONSystem.loadJSON(Paths.get(args[0]));
			for (Entry<String, JSONData<?>> e : ((JSONObject) config.get("database")).entrySet()) {
				String key = "db." + e.getKey();
				if (System.getProperty(key) != null)
					continue;
				Object val = e.getValue().value();
				if (val == null)
					continue;
				System.setProperty(key, val.toString());
			}
		}
		try (DownloaderCollection dc = new DownloaderCollection()) {
			if (config != null) {
				JSONObject paths = (JSONObject) config.get("paths");
				for (JSONData<?> str : ((JSONArray) paths.get("sources")).value())
					Downloader.loadSource(Paths.get(str.toString()));
			}
			FeedManager fm = new FeedManager(Paths.get("./Feeds/"), Paths.get("./Scrapers/"));
			dc.addDownloader(fm);
			NLPFunction nlpf = new NLPFunction();
			for (RawArticle ra : dc.get())
				nlpf.apply(ra).toString();
		}
	}
}
