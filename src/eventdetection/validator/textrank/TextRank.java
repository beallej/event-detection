package eventdetection.validator.textrank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import toberumono.structures.tuples.Pair;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import eventdetection.common.POSTagger;

/**
 * A static class that implements the TextRank algorithm for sentences
 * 
 * @author Joshua Lipstone
 */
public class TextRank {
	
	public static void main(String[] args) throws IOException {
		Path p = Paths.get("./testing_text.txt");
		StringBuilder sb = new StringBuilder();
		for (String line : Files.readAllLines(p))
			sb.append(line).append("\n");
		Annotation document = new Annotation(sb.toString());
		POSTagger.annotate(document);
		
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		List<CoreMap> ranked = getSortedRankedSentencesStream(document).map(a -> a.getX()).collect(Collectors.toList());
		
		sentences.retainAll(ranked.subList(0, ranked.size() >= 5 ? 5 : ranked.size()));
		System.out.println(POSTagger.untag(POSTagger.tag(sentences)));
	}
	
	private static TextRankGraph<CoreMap> generateGraph(Annotation document) {
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		TextRankGraph<CoreMap> g = new TextRankGraph<>(sentences.size(), 100, 0.0001, 0.8);
		for (CoreMap sentence : sentences)
			g.addNode(new TextRankNode<>(1.0, sentence));
		String[][] words = new String[sentences.size()][0];
		for (int i = 0; i < sentences.size(); i++) {
			if (words[i].length == 0)
				words[i] = getWords(sentences.get(i));
			for (int j = i + 1; j < sentences.size(); j++) {
				if (words[j].length == 0)
					words[j] = getWords(sentences.get(j));
				g.addEdge(i, similarity(words[i], words[j]), j);
			}
		}
		return g;
	}
	
	/**
	 * This method runs the TextRank algorithm before returning
	 * 
	 * @param document
	 *            a container holding the text to be ranked
	 * @return a {@link Stream} containing {@link Pair Pairs} of objects and their ranks
	 */
	public static Stream<Pair<CoreMap, Double>> getRankedSentencesStream(Annotation document) {
		return generateGraph(document).getRankedObjectsStream();
	}
	
	/**
	 * This method runs the TextRank algorithm before returning
	 * 
	 * @param document
	 *            a container holding the text to be ranked
	 * @return a {@link Stream} containing {@link Pair Pairs} of objects and their ranks sorted in <i>descending</i> order
	 */
	public static Stream<Pair<CoreMap, Double>> getSortedRankedSentencesStream(Annotation document) {
		return generateGraph(document).getSortedRankedObjectsStream();
	}
	
	/**
	 * This method runs the TextRank algorithm before returning
	 * 
	 * @param document
	 *            a container holding the text to be ranked
	 * @return a {@link List} of {@link Pair Pairs} of values and their ranks from the graph
	 */
	public static List<Pair<CoreMap, Double>> getRankedSentences(Annotation document) {
		return getRankedSentencesStream(document).collect(Collectors.toList());
	}
	
	/**
	 * This method runs the TextRank algorithm before returning
	 * 
	 * @param document
	 *            a container holding the text to be ranked
	 * @return a {@link List} of ranked objects sorted in <i>descending</i> order
	 */
	public static List<Pair<CoreMap, Double>> getSortedRankedSentences(Annotation document) {
		return getSortedRankedSentencesStream(document).collect(Collectors.toList());
	}
	
	private static String[] getWords(CoreMap sentence) {
		List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
		Set<String> out = new LinkedHashSet<>();
		for (int i = 0; i < tokens.size(); i++)
			out.add(tokens.get(i).get(TextAnnotation.class).toLowerCase());
		return out.toArray(new String[out.size()]);
	}
	
	private static double similarity(String[] s1, String[] s2) {
		double overlap = 0;
		for (int i = 0; i < s1.length; i++)
			for (int j = 0; j < s2.length; j++)
				if (s1[i].equals(s2[j])) {
					overlap++;
					break;
				}
		return overlap / (Math.log(s1.length) + Math.log(s2.length));
	}
}
