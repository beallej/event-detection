import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import toberumono.json.JSONObject;
import toberumono.json.JSONString;
import toberumono.structures.tuples.Pair;

import eventdetection.downloader.PythonScraper;
import eventdetection.downloader.Scraper;

class ReutersScraper extends PythonScraper {

	private static final DocumentBuilderFactory dbf;
	private static final DocumentBuilder db;
	static {
		dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder dbtemp = null;
		try {
			dbtemp = dbf.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		db = dbtemp;
	}

	/**
	 * Creates a {@link Scraper} using the given configuration data.
	 *
	 * @param json
	 *            the {@link Path} to the JSON file that describes the {@link PythonScraper}
	 * @param config
	 *            a {@link JSONObject} containing the configuration data for the {@link PythonScraper}
	 */
    public ReutersScraper(Path json, JSONObject config) {
		super(json, config);
        System.out.println("ReutersScraper constructor");
	}

	@Override
	public String scrape(URL url) throws IOException {
		String link = url.toString();
		JSONObject variableParameters = new JSONObject();
		variableParameters.put("url", new JSONString(link));
		String sectioned = callScript("sectioning", variableParameters);
		System.out.println(sectioned);
		return sectioned.trim();
	}

	/**
	 * Extracts the text that composes an article from the given page
	 *
	 * @param page
	 *            the page from which to extract the text as a {@link String}
	 * @param rules
	 *            the rules to use to extract the text
	 * @return the extracted text
	 */
	public String separate(String page, List<Pair<Pattern, String>> rules) {
		// System.out.println(page);
		// Pattern body = Pattern.compile("<body.*?>(.*?)</body>", Pattern.DOTALL);
		// Matcher m = body.matcher(page);
		// m.find();
		// page = m.group(0);
		//
		// Pattern scriptRemover = Pattern.compile("<script.*?>.*?</script>", Pattern.DOTALL);
		// Matcher sm = scriptRemover.matcher(page);
		// page = sm.replaceAll("");
		//
		// Pattern inputRemover = Pattern.compile("<input.*?>", Pattern.DOTALL);
		// Matcher im = inputRemover.matcher(page);
		// page = im.replaceAll("");
		//
		// // Pattern formRemover = Pattern.compile("<form.*?>.*?</form>", Pattern.DOTALL);
		// // Matcher fm = formRemover.matcher(page);
		// // page = fm.replaceAll("");
		// System.out.println(page);
		// try (InputStream is = new ByteArrayInputStream(page.getBytes(Charset.defaultCharset()))) {
		// 	Document doc = db.parse(is);
		// 	//do stuff here
		// 	Element text = doc.getElementById("articleText"); //Should be a good starting point
		// 	System.out.println(text);
		// }
		// catch (IOException e) {
		// 	System.err.println(e);
		// }
		// catch (SAXException e) {
		// 	System.err.println(e);
		// }

		return null;
	}
}
