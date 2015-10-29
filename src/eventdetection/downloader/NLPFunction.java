package eventdetection.downloader;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eventdetection.common.Article;

public class NLPFunction implements Function<RawArticle, Article> {
	private static final String delimiter = "_";
	private static final StanfordCoreNLP pipeline;
	
	static {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		pipeline = new StanfordCoreNLP(props);
	}

	@Override
	public Article apply(RawArticle t) {

		// read some text in the text variable
		String text = t.getText();

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		StringBuilder sb = new StringBuilder((int) (t.getText().length() * 1.5));
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token : sentence.get(TokensAnnotation.class))
				sb.append(token.word()).append(delimiter).append(token.get(PartOfSpeechAnnotation.class)).append(" ");

			// this is the parse tree of the current sentence
			// Tree tree = sentence.get(TreeAnnotation.class);

			// System.out.println(tree.toString());
		}
		return new Article(t.getTitle(), sb.toString().trim(), t.getText(), t.getUrl(), t.getSource());
	}
}
