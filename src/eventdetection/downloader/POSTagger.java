package eventdetection.downloader;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * A wrapper for the CoreNLP API.
 * 
 * @author Joshua Lipstone
 */
public class POSTagger {
	private static final String delimiter = "_";
	private static final StanfordCoreNLP pipeline;
	private static final Pattern newline = Pattern.compile("\n", Pattern.LITERAL);
	private static final Pattern untagger = Pattern.compile("([^_\\s]+)_([^_\\s]+)");
	
	static {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		pipeline = System.getProperty("enable.pos", "true").toLowerCase().charAt(0) == 't' ? new StanfordCoreNLP(props) : null;
	}
	
	/**
	 * Tags the given text. This keeps paragraphs.
	 * 
	 * @param text
	 *            the text to tag.
	 * @return the tagged text
	 */
	public static String tag(String text) {
		if (pipeline == null)
			return text;
		StringBuilder sb = new StringBuilder((int) (text.length() * 1.5));
		for (String paragraph : newline.split(text)) { //This allows the annotated text to retain paragraph breaks.
			// create an empty Annotation just with the given text
			Annotation document = new Annotation(paragraph);
			
			// run all Annotators on this text
			pipeline.annotate(document);
			
			// these are all the sentences in this document
			// a CoreMap is essentially a Map that uses class objects as keys and
			// has values with custom types
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			
			for (CoreMap sentence : sentences) {
				// traversing the words in the current sentence
				// a CoreLabel is a CoreMap with additional token-specific methods
				List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
				for (CoreLabel token : tokens)
					sb.append(token.word()).append(delimiter).append(token.get(PartOfSpeechAnnotation.class)).append(" ");
			}
			sb.append("\n");
		}
		return sb.toString().trim();
	}
	
	/**
	 * Untags the given text. This process is comprised of using regex to remove the _tag suffixes from each word.
	 * 
	 * @param text
	 *            the text to untag
	 * @return the untagged text
	 */
	public static String untag(String text) {
		return untagger.matcher(text).replaceAll("$1");
	}
}
