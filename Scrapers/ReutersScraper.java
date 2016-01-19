import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.charset.Charset;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import toberumono.structures.tuples.Pair;

import eventdetection.downloader.Scraper;

class ReutersScraper extends Scraper {

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
	 * Creates a {@link Scraper} with the given id and patterns.
	 *
	 * @param id
	 *            the ID of the {@link Scraper}
	 * @param sectioning
	 *            the pattern/replacement combinations used to extract article text from an article
	 * @param filtering
	 *            the pattern/replacement combinations used to clean the extracted text
	 */
	public ReutersScraper(String id, List<Pair<Pattern, String>> sectioning, List<Pair<Pattern, String>> filtering) {
		super(id, sectioning, filtering);
        System.out.println("ReutersScraper constructor");
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
        Pattern body = Pattern.compile("<body.*?>(.*?)</body>", Pattern.DOTALL);
        Matcher m = body.matcher(page);
        m.find();
        page = m.group(0);

        Pattern scriptRemover = Pattern.compile("<script.*?>.*?</script>", Pattern.DOTALL);
        Matcher sm = scriptRemover.matcher(page);
        page = sm.replaceAll("");

        Pattern inputRemover = Pattern.compile("<input.*?>", Pattern.DOTALL);
        Matcher im = inputRemover.matcher(page);
        page = im.replaceAll("");

        // Pattern formRemover = Pattern.compile("<form.*?>.*?</form>", Pattern.DOTALL);
        // Matcher fm = formRemover.matcher(page);
        // page = fm.replaceAll("");
        System.out.println(page);
        try (InputStream is = new ByteArrayInputStream(page.getBytes(Charset.defaultCharset()))) {
            Document doc = db.parse(is);
            //do stuff here
            Element text = doc.getElementById("articleText"); //Should be a good starting point
            System.out.println(text);
        } catch (IOException e) {
            System.err.println(e);
        }  catch (SAXException e) {
            System.err.println(e);
        }

        return null;
    }
}
