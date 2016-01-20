package eventdetection.common;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * A static class containing helper methods for algorithms that interact with the CoreNLP library.
 * 
 * @author Joshua Lipstone
 */
public class POSUtils {
	
	private POSUtils() {/* This is a static class */}

	/**
	 * Reconstructs a paragraph (without PoS tags) from a {@link Annotation} from the CoreNLP library.
	 * 
	 * @param paragraph
	 *            the {@link Annotation} representing the paragraph to reconstruct
	 * @return the reconstructed sentence
	 */
	public static String reconstructParagraph(Annotation paragraph) {
		StringBuilder sb = new StringBuilder();
		for (CoreMap sentence : paragraph.get(SentencesAnnotation.class))
			reconstructSentence(sentence, sb).append(". ");
		return sb.toString().replaceAll("\\s+([!,.;:'\"?%])", "$1").trim();
	}
	
	/**
	 * Reconstructs a sentence (without PoS tags) from a {@link CoreMap} from the CoreNLP library.
	 * 
	 * @param sentence
	 *            the {@link CoreMap} representing the sentence to reconstruct
	 * @return the reconstructed sentence
	 */
	public static String reconstructSentence(CoreMap sentence) {
		return reconstructSentence(sentence, new StringBuilder()).toString().replaceAll("\\s+([!,\\.;:'\"?%])", "$1").trim();
	}
	
	/**
	 * Reconstructs a sentence (without PoS tags) from a {@link CoreMap} from the CoreNLP library in the given
	 * {@link StringBuilder}.
	 * 
	 * @param sentence
	 *            the {@link CoreMap} representing the sentence to reconstruct
	 * @param sb
	 *            the {@link StringBuilder} into which the sentence should be reconstructed
	 * @return the {@link StringBuilder}
	 */
	public static StringBuilder reconstructSentence(CoreMap sentence, StringBuilder sb) {
		// traversing the words in the current sentence
		// a CoreLabel is a CoreMap with additional token-specific methods
		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
		for (CoreLabel token : tokens)
			sb.append(token.word()).append(" ");
		return sb;
	}
}
