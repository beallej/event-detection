package eventdetection.validator.implementations;

import semilar.config.ConfigManager;
import semilar.data.Sentence;

import semilar.sentencemetrics.LexicalOverlapComparer;
import semilar.sentencemetrics.OptimumComparer;
import semilar.tools.preprocessing.SentencePreprocessor;
import semilar.tools.semantic.WordNetSimilarity;
import semilar.sentencemetrics.PairwiseComparer.NormalizeType;
import semilar.sentencemetrics.PairwiseComparer.WordWeightType;
import semilar.wordmetrics.LSAWordMetric;
import semilar.wordmetrics.WNWordMetric;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.text.*;
import edu.sussex.nlp.jws.*;
import java.util.Properties;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;


import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import eventdetection.common.Article;
import eventdetection.common.POSTagger;
import eventdetection.common.Query;
import eventdetection.common.Source;
import eventdetection.validator.ValidationResult;
import eventdetection.validator.ValidatorController;
import eventdetection.validator.types.OneToOneValidator;
import eventdetection.validator.types.Validator;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import toberumono.json.JSONArray;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;
import toberumono.structures.collections.lists.SortedList;
import toberumono.structures.tuples.Pair;
import toberumono.structures.SortingMethods;

/**
 * A SEMILAR validator using prebuilt library from http://deeptutor2.memphis.edu/
 * With some post-run modification
 *
 * @author Anmol and Phuong
 */
public class SEMILARSemanticAnalysisValidator extends OneToOneValidator {

    private static int MAX_SENTENCES = 5;

    OptimumComparer optimumComparerWNLin;
    OptimumComparer optimumComparerLSATasa;
    LexicalOverlapComparer lexicalOverlapComparer; // Just see the lexical overlap.
    WNWordMetric wnMetricLin;
    
	/**
	 * Constructs a new instance of the {@link Validator} for the given {@code ID}, {@link Query}, and {@link Article}
	 * 
	 * @param query
	 *            the {@link Query} to validate
	 * @param article
	 *            the {@link Article} against which the {@link Query} is to be validated
	 */
    public SEMILARSemanticAnalysisValidator(Query query, Article article) {
		super(query, article);

        /* Word to word similarity expanded to sentence to sentence .. so we need word metrics */
        boolean wnFirstSenseOnly = false; //applies for WN based methods only.
        wnMetricLin = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LIN, wnFirstSenseOnly);

        optimumComparerWNLin = new OptimumComparer(wnMetricLin, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);
        //optimumComparerLSATasa = new OptimumComparer(lsaMetricTasa, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);

        //lexicalOverlapComparer = new LexicalOverlapComparer(false);  // use base form of words? - No/false. 
    }


    
	@Override
	public ValidationResult[] call() throws IOException {
        
        Sentence querySentence;
        Sentence articleSentence;
        
        SentencePreprocessor preprocessor = new SentencePreprocessor(SentencePreprocessor.TokenizerType.STANFORD, SentencePreprocessor.TaggerType.STANFORD, SentencePreprocessor.StemmerType.PORTER, SentencePreprocessor.ParserType.STANFORD);
        
        SortedList<Pair<Double, CoreMap>> topN = new SortedList<>((a, b) -> b.getX().compareTo(a.getX()));
        
        StringBuilder phrase1 = new StringBuilder();
		phrase1.append(query.getSubject()).append(" ").append(query.getVerb());
		if (query.getDirectObject() != null && query.getDirectObject().length() > 0)
			phrase1.append(" ").append(query.getDirectObject());
		if (query.getIndirectObject() != null && query.getIndirectObject().length() > 0)
			phrase1.append(" ").append(query.getIndirectObject());
        if (query.getLocation() != null && query.getLocation().length() > 0)
			phrase1.append(" ").append(query.getLocation());       

        
        querySentence = preprocessor.preprocessSentence(phrase1.toString());
        
        Double temp;

        String title = article.getAnnotatedTitle().toString();
        Sentence articleTitle = preprocessor.preprocessSentence(title);
        Double tempTitle = (double) optimumComparerWNLin.computeSimilarity(querySentence, articleTitle);

        for (Annotation paragraph : article.getAnnotatedText()) {
			List<CoreMap> sentences = paragraph.get(SentencesAnnotation.class);

			for (CoreMap sentence : sentences) {
                String sen = POSTagger.reconstructSentence(sentence);
                articleSentence = preprocessor.preprocessSentence(sen);
                temp = (double) optimumComparerWNLin.computeSimilarity(querySentence, articleSentence);
                if (temp.equals(Double.NaN))
                    continue;
                topN.add(new Pair<>(temp, sentence));
                if (topN.size() > MAX_SENTENCES)
                    topN.remove(topN.size() - 1);
            }
        }


  		double average = 0.0;
		for (Pair<Double, CoreMap> p : topN){
			average += p.getX();
            System.out.println(p.getY().toString() + p.getX());
        }
        average /= (double) topN.size(); 
        double validation = 0.0;
        if (average > 0.15 || tempTitle > 0.15) {
            validation = postProcess(topN, query,phrase1.toString(), title, tempTitle);
        }
        System.out.println("Annotated title: "+ article.getAnnotatedTitle());
        System.out.println("ARTICLE  ID " + article.getID() + " average: "+average + " title: "+tempTitle);
        return new ValidationResult[]{new ValidationResult(article.getID(), validation)};
    }
    
    public double postProcess(SortedList<Pair<Double, CoreMap>> topN, Query query, String rawQuery, String articleTitle, double titleScore){

	// Julia's dependencies experimentation
	// Note: for IndexedWord, value = word = ex "asking", lemma = "ask", tag = "VBG"
	/*
	for (Annotation paragraph : article.getAnnotatedText()) {
		List<CoreMap> sentences = paragraph.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			System.out.println("\"" + sentence + "\"");
			// Get 'first' (usually only) root
			IndexedWord root = dependencies.getFirstRoot();
			// Get a node's children
			Set<IndexedWord> children = dependencies.getChildren(root);
			System.out.println("Root: " + root + "\nChildren: " + children);
			for (IndexedWord child : children) {
				// Get a node's parent
				IndexedWord parent = dependencies.getParent(child);
				System.out.println("Child " + child + "\tParent " + parent);
			}
			// Prints out the dependency graph for every sentence
			System.out.println("Dependency graph:\n" + dependencies);
			// Note: Tried getting words' DependentsAnnotation.class but it returns null
			
			// Use POSList to get nsubj. We can do dobj, etc as well
			String posList = dependencies.toPOSList();
			String nsubjPattern = "nsubj[^\n]*\n";
			//String nsubjPattern = "nsubj(\([^)]*\))";
			Pattern nsubjRegex = Pattern.compile(nsubjPattern);
			Matcher nsubjMatcher = nsubjRegex.matcher(posList);
			if (nsubjMatcher.find()) {
				System.out.println("FOUND NSUBJ: " + nsubjMatcher.group(0));
			}
			else {
				System.out.println("NO NSUBJ FOUND");
			}
		}
	}
	*/

        String subject, dirObject, indirObject;
        subject = query.getSubject();
        dirObject = "";
        if (query.getDirectObject() != null && query.getDirectObject().length() > 0)
            dirObject = query.getDirectObject();
        if (query.getIndirectObject() != null && query.getIndirectObject().length() > 0)
            indirObject = query.getIndirectObject();
        // if (query.getLocation() != null && query.getLocation().length() > 0)
        //     phrase1.append(" ").append(query.getLocation()); 
        // if (query.getLocation() != null && query.getLocation().length() > 0)
        //     query.getLocation();  
        ArrayList<String> keywordNouns = new ArrayList<>();
        //Annotation taggedQuery = POSTagger.annotate(rawQuery);
        // Annotation taggedQuery = POSTagger.tag(annotatedQuery);
        Annotation taggedQuery = POSTagger.annotate(rawQuery);

        for (CoreLabel token: taggedQuery.get(TokensAnnotation.class)){
                String pos = token.get(PartOfSpeechAnnotation.class);
                System.out.println("Query tag: "+token + pos);
                if (pos.length() > 1 && pos.substring(0,2).equals("NN")){
                    if (subject.contains(token.get(LemmaAnnotation.class))){
                        keywordNouns.add(token.get(LemmaAnnotation.class));
                    }
                    if (dirObject.contains(token.get(LemmaAnnotation.class))){
                        keywordNouns.add(token.get(LemmaAnnotation.class));
                    }
                    //Can add ind Obj later
            }
            }

        int articleMatchScore = 0;
        int matchedPerSentence = 0;
        for (Pair<Double, CoreMap> p : topN){ //for each sentence
            matchedPerSentence= validationScore(p.getY(), keywordNouns);
            if (matchedPerSentence > 0){
                System.out.println("SEntence matched "+p.getY());
                articleMatchScore += 1;
            }            
        }

        Annotation annotatedTitle = POSTagger.annotate(articleTitle);
        CoreMap taggedTitle = annotatedTitle.get(SentencesAnnotation.class).get(0);
        
        matchedPerSentence = validationScore(taggedTitle, keywordNouns);
        // if (matchedPerSentence > 0 || titleScore>0.20){
        //     articleMatchScore += 2;
        // }
        System.out.println("MATCH SCORE: " + articleMatchScore);



        //add function here
        if (articleMatchScore > 2){ // at least (title + 1/5 sentences) or (3 sentences) or both
            return 1.0;
        } 
        return 0.0;

    }

    public int validationScore(CoreMap sentence, ArrayList<String> keywordNouns){
        int matchedPerSentence = 0;
        for (CoreLabel token: sentence.get(TokensAnnotation.class)){ //each word in sentence
            String pos = token.get(PartOfSpeechAnnotation.class);
            String lemma = token.get(LemmaAnnotation.class);
            // WE NEED Better Lemmatization
            ///
            if (pos.length() > 1 && pos.substring(0,2).equals("NN")){
                System.out.println(article.getID() +"POS tag for word : " + token + " tag: " + pos + " lemma " + lemma);
                for (String imptNoun:keywordNouns){
                    double matched = 0.0;
                    if (pos.equals("NNP")){
                        if (lemma.toLowerCase().equals(imptNoun.toLowerCase())){
                            matched = 1;
                        }
                    } else {
                        matched = wnMetricLin.computeWordSimilarityNoPos(lemma, imptNoun);
                    System.out.println("2 words that matched: "+lemma+ " matched with query "+imptNoun+" result: "+matched);
                    }
                    if (matched>0.65){
                        matchedPerSentence += 1;
                    }

                    // NOUN is matched

                    //We would want to look at Verb + adj depending on the matched noun and see if any of them match our query
                    // Check dependecy Graph
                }
            }
        }       
        return matchedPerSentence;
    }
	/**
	 * Hook for loading properties from the Validator's JSON data
	 * 
	 * @param properties
	 *            a {@link JSONObject} holding the validator's static properties
	 */
	public static void loadStaticProperties(JSONObject properties) {
		MAX_SENTENCES = (Integer) properties.get("max-sentences").value();
        System.out.println("MAX SENTENCE " + MAX_SENTENCES);

	}
}
