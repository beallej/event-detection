package eventdetection.validator.implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import toberumono.json.JSONObject;
import toberumono.structures.tuples.Pair;

import edu.stanford.nlp.util.CoreMap;
import eventdetection.common.Article;
import eventdetection.common.POSTagger;
import eventdetection.common.Query;
import eventdetection.textrank.TextRank;
import eventdetection.validator.ValidationResult;
import eventdetection.validator.types.OneToOneValidator;
import eventdetection.validator.types.Validator;

/**
 * A relatively simple validator that uses the <a href="http://swoogle.umbc.edu/SimService/api.html">Swoogle semantic
 * analysis web API</a> to get the semantic matching between the query and all sentences in the article. It then uses
 * {@link TextRank} to compute the weighted average of the rankings of the sentences in the article.
 * 
 * @author Joshua Lipstone
 */
public class TextRankSwoogleSemanticAnalysisValidator extends OneToOneValidator {
	private static String URL_PREFIX = "http://swoogle.umbc.edu/StsService/GetStsSim?operation=api";
	
	/**
	 * Constructs a new instance of the {@link Validator} for the given {@code ID}, {@link Query}, and {@link Article}
	 * 
	 * @param query
	 *            the {@link Query} to validate
	 * @param article
	 *            the {@link Article} against which the {@link Query} is to be validated
	 */
	public TextRankSwoogleSemanticAnalysisValidator(Query query, Article article) {
		super(query, article);
	}
	
	@Override
	public ValidationResult[] call() throws IOException {
		StringBuilder phrase1 = new StringBuilder();
		phrase1.append(query.getSubject()).append(" ").append(query.getVerb());
		if (query.getDirectObject() != null && query.getDirectObject().length() > 0)
			phrase1.append(" ").append(query.getDirectObject());
		if (query.getIndirectObject() != null && query.getIndirectObject().length() > 0)
			phrase1.append(" ").append(query.getIndirectObject());
		double average = 0.0, divisor = 0.0;
		List<Pair<CoreMap, Double>> sentences = TextRank.getRankedSentences(article.getAnnotatedText());
		for (Pair<CoreMap, Double> sentence : sentences) {
			String sen = POSTagger.reconstructSentence(sentence.getX());
			String url = String.format("%s&phrase1=%s&phrase2=%s", URL_PREFIX, URLEncoder.encode(phrase1.toString(), StandardCharsets.UTF_8.name()),
					URLEncoder.encode(sen, StandardCharsets.UTF_8.name()));
			URLConnection connection = new URL(url).openConnection();
			connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
			try (BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				average += Double.parseDouble(response.readLine().trim()) * sentence.getY();
				divisor += sentence.getY();
			}
		}
		return new ValidationResult[]{new ValidationResult(article.getID(), average / divisor)};
	}
	
	/**
	 * Hook for loading properties from the Validator's JSON data
	 * 
	 * @param properties
	 *            a {@link JSONObject} holding the validator's static properties
	 */
	public static void loadStaticProperties(JSONObject properties) {
		URL_PREFIX = (String) properties.get("url-prefix").value();
	}
}
