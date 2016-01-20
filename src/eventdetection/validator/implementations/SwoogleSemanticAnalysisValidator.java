package eventdetection.validator.implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import toberumono.structures.collections.lists.SortedList;
import toberumono.structures.tuples.Pair;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import eventdetection.common.Article;
import eventdetection.common.POSUtils;
import eventdetection.common.Query;
import eventdetection.validator.ValidationResult;
import eventdetection.validator.Validator;
import eventdetection.validator.ValidatorController;

/**
 * A relatively simple validator that uses the <a href="http://swoogle.umbc.edu/SimService/api.html">Swoogle semantic
 * analysis web API</a> to get the semantic matching between the query and all sentences in the article. It returns the
 * average of the five sentences with the highest match-score.
 * 
 * @author Joshua Lipstone
 */
public class SwoogleSemanticAnalysisValidator extends Validator {
	private static final String URL_PREFIX = "http://swoogle.umbc.edu/StsService/GetStsSim?operation=api";
	private static final int MAX_SENTENCES = 5;
	
	/**
	 * Constructs a new instance of the {@link Validator} for the given {@code ID}, {@link Query}, and {@link Article}
	 * 
	 * @param algorithmID
	 *            the {@code ID} of the implemented algorithm as determined by the {@link ValidatorController}
	 * @param query
	 *            the {@link Query} to validate
	 * @param article
	 *            the {@link Article} against which the {@link Query} is to be validated
	 */
	public SwoogleSemanticAnalysisValidator(Integer algorithmID, Query query, Article article) {
		super(algorithmID, query, article);
	}
	
	@Override
	public ValidationResult call() throws IOException {
		StringBuilder phrase1 = new StringBuilder();
		phrase1.append(query.getSubject()).append(" ").append(query.getVerb());
		if (query.getDirectObject() != null && query.getDirectObject().length() > 0)
			phrase1.append(" ").append(query.getDirectObject());
		if (query.getIndirectObject() != null && query.getIndirectObject().length() > 0)
			phrase1.append(" ").append(query.getIndirectObject());
		SortedList<Pair<Double, String>> topN = new SortedList<>((a, b) -> b.getX().compareTo(a.getX()));
		for (Annotation paragraph : article.getAnnotatedText()) {
			List<CoreMap> sentences = paragraph.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				String sen = POSUtils.reconstructSentence(sentence);
				String url = String.format("%s&phrase1=%s&phrase2=%s", URL_PREFIX, URLEncoder.encode(phrase1.toString(), StandardCharsets.UTF_8.name()),
						URLEncoder.encode(sen, StandardCharsets.UTF_8.name()));
				URLConnection connection = new URL(url).openConnection();
				connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
				try (BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					topN.add(new Pair<>(Double.parseDouble(response.readLine().trim()), sen));
					if (topN.size() > MAX_SENTENCES)
						topN.remove(topN.size() - 1);
				}
			}
		}
		double average = 0.0;
		for (Pair<Double, String> p : topN)
			average += p.getX();
		average /= (double) topN.size();
		return new ValidationResult(this.getID(), article.getID(), average);
	}
}
