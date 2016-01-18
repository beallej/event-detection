package eventdetection.validator.implementations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import eventdetection.common.Article;
import eventdetection.common.Query;
import eventdetection.validator.ValidationResult;
import eventdetection.validator.Validator;
import eventdetection.validator.ValidatorController;

public class SwoogleSemanticAnalysisValidator extends Validator {
	private static final String URL_PREFIX = "http://swoogle.umbc.edu/StsService/GetStsSim?operation=api";

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
		double queries = 0.0, sum = 0.0;
		for (Annotation paragraph : article.getAnnotatedText()) {
			List<CoreMap> sentences = paragraph.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				String sen = reconstructSentence(sentence);
				System.out.println(sen);
				String url = String.format("%s&phrase1=%s&phrase2=%s", URL_PREFIX, URLEncoder.encode(phrase1.toString(), StandardCharsets.UTF_8.name()),
						URLEncoder.encode(sen, StandardCharsets.UTF_8.name()));
				System.out.println(url);
				URLConnection connection = new URL(URL_PREFIX + url).openConnection();
				connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
				try (BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					sum += Double.parseDouble(response.readLine().trim());
					queries++;
				}
			}
		}
		return new ValidationResult(this.getID(), article.getID(), sum / queries);
	}
	
	private static String reconstructSentence(CoreMap sentence) {
		StringBuilder sb = new StringBuilder();
		// traversing the words in the current sentence
		// a CoreLabel is a CoreMap with additional token-specific methods
		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
		for (CoreLabel token : tokens)
			sb.append(token.word()).append(" ");
		return sb.toString().trim();
	}
}
