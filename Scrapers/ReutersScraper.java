import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.net.URL;

import toberumono.json.JSONObject;
import toberumono.json.JSONString;

import eventdetection.downloader.PythonScraper;
import eventdetection.downloader.Scraper;

class ReutersScraper extends PythonScraper {

	/**
	 * Creates a {@link PythonScraper} using the given configuration data.
	 *
	 * @param json
	 *            the {@link Path} to the JSON file that describes the {@link PythonScraper}
	 * @param config
	 *            a {@link JSONObject} containing the configuration data for the {@link PythonScraper}
	 */
    public ReutersScraper(Path json, JSONObject config) {
		super(json, config);
	}

	@Override
	public String scrape(URL url) throws IOException {
		String link = url.toString();
		JSONObject variableParameters = new JSONObject();
		variableParameters.put("url", new JSONString(link));
		String sectioned = callScript("sectioning", variableParameters);
		return sectioned.trim();
	}
}
