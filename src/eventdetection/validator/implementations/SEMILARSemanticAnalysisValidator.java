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
import java.util.HashMap;
import java.util.HashSet;
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
    private double MIN_WORD_TO_WORD_THRESHOLD = 0.70;
    private static Pattern STOPWORD_RELN_REGEX = Pattern.compile("det|mark|cc|aux|punct|auxpass|cop|expl|goeswith|dep");
    private static Pattern USEFUL_RELN_REGEX = Pattern.compile("nmod|dobj|iobj|nsubj|nsubjpass|appos");

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
        
	long startSP = System.nanoTime();
        SentencePreprocessor preprocessor = new SentencePreprocessor(SentencePreprocessor.TokenizerType.STANFORD, SentencePreprocessor.TaggerType.STANFORD, SentencePreprocessor.StemmerType.PORTER, SentencePreprocessor.ParserType.STANFORD);
	long endSP = System.nanoTime();
	long spElapsedMillis = (endSP - startSP) / 1000000;
        
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
        	long startPPSentence = System.nanoTime();
                articleSentence = preprocessor.preprocessSentence(sen);
		long endPPSentence = System.nanoTime();
		long ppSentenceElapsedMillis = (endPPSentence - startPPSentence) / 1000000;
		//System.out.println("TIMING2: " + ppSentenceElapsedMillis + " milliseconds to preprocess sentence"); 
		long startComputeSimilarity = System.nanoTime();
                temp = (double) optimumComparerWNLin.computeSimilarity(querySentence, articleSentence);
		long endComputeSimilarity = System.nanoTime();
		long computeSimilarityElapsedMillis = (endComputeSimilarity - startComputeSimilarity) / 1000000;
		//System.out.println("TIMING3: " + computeSimilarityElapsedMillis + " milliseconds to compute similarity between query and article sentence");
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

        String subject, dirObject, indirObject;
        subject = query.getSubject();
        dirObject = "";
        indirObject = "";
        if (query.getDirectObject() != null && query.getDirectObject().length() > 0)
            dirObject = query.getDirectObject();
        if (query.getIndirectObject() != null && query.getIndirectObject().length() > 0)
            indirObject = query.getIndirectObject();

 
        HashMap<String, String> keywordNouns = new HashMap<String, String>();
        //Annotation taggedQuery = POSTagger.annotate(rawQuery);
        // Annotation taggedQuery = POSTagger.tag(annotatedQuery);
        Annotation taggedQuery = POSTagger.annotate(rawQuery);

        for (CoreLabel token: taggedQuery.get(TokensAnnotation.class)){
                String pos = token.get(PartOfSpeechAnnotation.class);
                System.out.println("Query tag: "+token + pos);
                if (pos.length() > 1 && pos.substring(0,2).equals("NN")){
                    if (subject.contains(token.get(LemmaAnnotation.class))){
                        keywordNouns.put(token.get(LemmaAnnotation.class), "SUBJECT");
                    }
                    if (dirObject.contains(token.get(LemmaAnnotation.class))){
                        keywordNouns.put(token.get(LemmaAnnotation.class), "OBJECT");
                    }
                    if (indirObject.contains(token.get(LemmaAnnotation.class))){
                        keywordNouns.put(token.get(LemmaAnnotation.class), "OBJECT"); //We will treat DirObj and IndirObj the same
                    }
                }
            }

        double articleMatchScore = 0.0;
        int matchedPerSentence = 0;
        HashSet<String> dependencyMatches = new HashSet<String>();

        // Count how many elements in Query
        // HashMatch = {Subject: False, Verb:..., Object, Location:}
        // SentenceMatchPattern: {{SVO}:1,....}
        // double totalScore = 0;
        /*
        For each sentence
            return the set
            for each element in the set, add to HashMatch
            add the set to SentenceMatchPattern HashMap

        for each key of SentenceMatchPattern:
            totalScore = calSentenceScore(each sentence)

        totalScore = calSentenceScore(title)

        if totalScore > threshold {
        ...
        }

    public double calSentenceScore(){
        if S_Pronoun or O_PRONOUN is in:
            check if Subject/ Object in HashMatch is true. Add more point if yes. less point if no
        else:
            calculate point (more elements -> more point!)
            3/3, 4/4: a lot a lot point
            3/4, 2/3, 2/2: good point
            2/4: less point
    }

        */

        // ARTICLE CONTENT SCORE
        for (Pair<Double, CoreMap> p : topN){ //for each sentence
            dependencyMatches= validationScore(query, p.getY(), keywordNouns);
            System.out.println("DEPENDENCY MATCHES::");
            System.out.println(dependencyMatches.toString()); 

            if (dependencyMatches.size() > 1){
                System.out.println("Sentence matched a lot!!!"+p.getY());
                articleMatchScore += 1.5;
            }else if (dependencyMatches.size() == 1){
                System.out.println("Sentence matched only subject or object!!!" + p.getY());
                articleMatchScore += 0.5;
            }  
        }

        Annotation annotatedTitle = POSTagger.annotate(articleTitle);
        CoreMap taggedTitle = annotatedTitle.get(SentencesAnnotation.class).get(0);
        
        //TITLE SCORE
        dependencyMatches = validationScore(query, taggedTitle, keywordNouns);
        System.out.println("DEPENDENCY MATCHES OF TITLE::");
        System.out.println(dependencyMatches.toString());
        if (dependencyMatches.size() > 1){
            System.out.println("Title matched a lot!!!" + taggedTitle);
            articleMatchScore += 3;
        } else if (dependencyMatches.size() == 1){
            System.out.println("Sentence matched only subject or object!!!" + taggedTitle);
            articleMatchScore += 1;
        }  

        System.out.println("MATCH SCORE: " + articleMatchScore);
        System.out.println("die-die: " + wnMetricLin.computeWordSimilarityNoPos("pass away", "death"));
        System.out.println("die-pass away: "  + wnMetricLin.computeWordSimilarityNoPos("run into", "encounter"));
        System.out.println("die-pass away: "  + wnMetricLin.computeWordSimilarityNoPos("throw away", "dispose of"));




        if (articleMatchScore > 3){
            return 1.0;

        } 
        return 0.0;

    }

    public HashSet<String> validationScore(Query query, CoreMap sentence, HashMap<String, String> keywordNouns){
        int matchedPerSentence = 0;
        // matchedTokens contains HashMap<SVO string, set of words that match that SVO>
        HashMap<String, HashSet<CoreLabel>> matchedTokens = new HashMap<String, HashSet<CoreLabel>>();
        for (CoreLabel token: sentence.get(TokensAnnotation.class)){ //each word in sentence
            String pos = token.get(PartOfSpeechAnnotation.class);
            String lemma = token.get(LemmaAnnotation.class);
            // WE NEED Better Lemmatization
            ///
            if (pos.length() > 1 && pos.substring(0,2).equals("NN")){
                System.out.println(article.getID() +"POS tag for word : " + token + " tag: " + pos + " lemma " + lemma);
                for (String imptNoun:keywordNouns.keySet()){
                    double matched = 0.0;
                    if (pos.equals("NNP")){
                        if (lemma.toLowerCase().equals(imptNoun.toLowerCase())){
                            matched = 1;
                        }
                    } else {
                        matched = wnMetricLin.computeWordSimilarityNoPos(lemma.toLowerCase(), imptNoun.toLowerCase());

                        // TODO::: HACK for now to account for the stupid library which does not know what mosque is!
                        // This only fixes identical word. i need LEMMATISATION here!
                        if (lemma.toLowerCase().equals(imptNoun.toLowerCase())) {
                            matched = 1;
                        }

                    }
                    if (matched > MIN_WORD_TO_WORD_THRESHOLD){
                        matchedPerSentence += 1;
                        if (!matchedTokens.containsKey(keywordNouns.get(imptNoun))) {
                            HashSet<CoreLabel> newTokenSet = new HashSet<CoreLabel>();
                            matchedTokens.put(keywordNouns.get(imptNoun), newTokenSet);
                        }
                        matchedTokens.get(keywordNouns.get(imptNoun)).add(token);
                    }           
                }
            }
        }
        if (matchedPerSentence > 0) {
            if (matchedTokens.containsKey("SUBJECT")) {
                return matchSVO(query, sentence, matchedTokens, "SUBJECT");
            }else if (matchedTokens.containsKey("OBJECT")) {
                return matchSVO(query, sentence, matchedTokens, "OBJECT");
            }
        }       
        return new HashSet<String>(); //String could only be SUBJECT, VERB, OBJECT, S_PRONOUN, O_PRONOUN, LOCATION
    }


    public HashSet<String> matchSVO(Query query, CoreMap sentence, HashMap<String, HashSet<CoreLabel>> matchedTokens, String tokenType) {
        SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
        String verb = query.getVerb();// TODO .get(LemmaAnnotation.class);
        HashSet<String> dependencyMatches = new HashSet<String>();
        for (CoreLabel token : matchedTokens.get(tokenType)) {
            if (token.tag().equals("WP") || token.tag().equals("WDT") || token.tag().equals("PRP")) {
                if (tokenType.equals("SUBJECT")) {
                    dependencyMatches.add("S_PRONOUN");
                }
                else if (tokenType.equals("OBJECT")) {
                    dependencyMatches.add("O_PRONOUN");
                }
            }
            else {
                dependencyMatches.add(tokenType);
            }
            System.out.println("Dependecy Tree: " + dependencies);
            List<IndexedWord> verbNodes = getVerbNodes(token, verb, dependencies);
            for (IndexedWord verbNode : verbNodes){
                // TODO: Not only verb to verb, but also verb to adj (e.g) die == was dead
                System.out.println("VERB FETCHED from: "+token+" is "+verbNode);
                if (verbNode != null) {
                    if (wnMetricLin.computeWordSimilarityNoPos(verbNode.lemma(), verb) > MIN_WORD_TO_WORD_THRESHOLD) {
                        dependencyMatches.add("VERB");
                    }
                    if (!dependencyMatches.contains("OBJECT") && matchedTokens.containsKey("OBJECT")){
                        System.out.println("Calling recursive search from matchSVO!! " + verbNode);

                        dependencyMatches = recursiveSearchKeyword(verbNode, dependencies, tokenType, dependencyMatches, matchedTokens);                      
                    }
                }
            }
        }
        // Search sentence for location match as well
        // Use String Regex method (NOTE: CASE sensitive to avoid unfortunate matching like us - US)
        String queryLocation = "";
        if (query.getLocation() != null && query.getLocation().length() > 0) {    
            queryLocation = query.getLocation();
            String potentialPrep = queryLocation.substring(0,3);
            if (potentialPrep.equals("in ") || potentialPrep.equals("on ") || potentialPrep.equals("at ")){
                queryLocation = queryLocation.substring(3);
            }
            System.out.println("CLEANED QUERY: " + queryLocation);
        }    

        Pattern isLocationMatch = Pattern.compile("^|[\\-\"' \t]"+queryLocation+"[$\\.!?\\-,;\"' \t]");
        if (!queryLocation.equals("") && isLocationMatch.matcher(sentence.toString()).find()) {
            System.out.println("MATCHED LOCATION: " + queryLocation);
            dependencyMatches.add("LOCATION");
        }
        return dependencyMatches;
    } 

    public HashSet<String> recursiveSearchKeyword(IndexedWord headNode, SemanticGraph dependencies, String tokenType, HashSet<String> dependencyMatches, HashMap<String, HashSet<CoreLabel>> matchedTokens){

        for (IndexedWord childNode : dependencies.getChildren(headNode)) {
            if (!STOPWORD_RELN_REGEX.matcher(dependencies.reln(headNode, childNode).getShortName()).find()){
                if (tokenType.equals("SUBJECT")) {
                    if (childNode.tag().equals("WP") || childNode.tag().equals("WDT") || childNode.tag().equals("PRP") || childNode.tag().equals("WP$")) {
                            dependencyMatches.add("O_PRONOUN");
                    }
                    else {
                        for (CoreLabel objectToken : matchedTokens.get("OBJECT")) { 
                            if (wnMetricLin.computeWordSimilarityNoPos(childNode.lemma(), objectToken.get(LemmaAnnotation.class)) > MIN_WORD_TO_WORD_THRESHOLD) {
                                dependencyMatches.add("OBJECT");
                                break;
                            } else {
                                if (USEFUL_RELN_REGEX.matcher(dependencies.reln(headNode, childNode).getShortName()).find()){
                                    dependencyMatches = recursiveSearchKeyword(childNode, dependencies, tokenType, dependencyMatches, matchedTokens);
                                }
                            }
                        }
                    }
                } else if (tokenType.equals("OBJECT")) {
                    if (childNode.tag().equals("WP") || childNode.tag().equals("WDT") || childNode.tag().equals("PRP") || childNode.tag().equals("WP$")) {
                            dependencyMatches.add("S_PRONOUN");
                    }                           
                }
            }
        }
        return dependencyMatches;
    }


    public List<IndexedWord> getVerbNodes(CoreLabel token, String verb, SemanticGraph dependencies) {
        //Find verb of the matched subject (or object)

        List<IndexedWord> nounNodes = dependencies.getAllNodesByWordPattern(token.get(LemmaAnnotation.class));
        List<IndexedWord> verbNodes = new ArrayList<IndexedWord>();

        for (IndexedWord nounNode : nounNodes) {
            IndexedWord parent = dependencies.getParent(nounNode);
            while (parent != null && !parent.tag().substring(0,1).equals("V")) {
                if (parent == dependencies.getParent(parent)){
                    break;
                }
                parent = dependencies.getParent(parent);
            }
            verbNodes.add(parent);
        }
        return verbNodes;
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
