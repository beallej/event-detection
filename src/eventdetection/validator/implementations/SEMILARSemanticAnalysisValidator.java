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
import toberumono.json.JSONNumber;
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

    // Test the following variables:
    private double HIGH_VALIDATION_THRESHOLD = 10; //THRESHOLD to accept validation return P = 1.0
                                                    // The max score depending on the HIGH_MATCH_SCORE, MEDIUM_MATCH_SCORE, TITLE_MULTIPLIER...
                                                    // 10 might be plenty for high HIGH_MATCH_SCORE, MEDIUM_MATCH_SCORE, TITLE_MULTIPLIER...
                                                    // but too low for low HIGH_MATCH_SCORE, MEDIUM_MATCH_SCORE, TITLE_MULTIPLIER...
                                                    // Sorrym but these variables are correlate
    private double MEDIUM_VALIDATION_THRESHOLD = 0.0; // Considering zone P ranging 0.0 to 0.9
    private double FIRST_ROUND_CONTENT_THRESHOLD = 0.10;
    private double FIRST_ROUND_TITLE_THRESHOLD = 0.15;
    private double TITLE_MULTIPLIER = 2;
    private double PRONOUN_SCORE = 0.5;
    private double HIGH_MATCH_SCORE = 4;
    private double MEDIUM_MATCH_SCORE = 2;
    private double LOW_MATCH_SCORE = 1;
    private double MIN_WORD_TO_WORD_THRESHOLD = 0.70;
    private double RELIABLE_TITLE_THRESHOLD = 0.4;


    // No need to change variable
    private int MAX_SENTENCES = 10;
    private static Pattern STOPWORD_RELN_REGEX = Pattern.compile("det|mark|cc|aux|punct|auxpass|cop|expl|goeswith|dep");
    private static Pattern USEFUL_RELN_REGEX = Pattern.compile("nmod|dobj|iobj|nsubj|nsubjpass|appos|conj|xcomp|ccomp");

    OptimumComparer optimumComparerWNLin;
    
    WNWordMetric wnMetricLin;
    SentencePreprocessor preprocessor;
    
	/**
	 * Constructs a new instance of the {@link Validator} for the given {@code ID}, {@link Query}, and {@link Article}
	 * @param config the configuration data
	 */
    public SEMILARSemanticAnalysisValidator(JSONObject config) {
		//MAX_SENTENCES = ((JSONNumber<?>) config.get("max-sentences")).value().intValue();
        HIGH_VALIDATION_THRESHOLD = ((JSONNumber<?>) config.get("HIGH_VALIDATION_THRESHOLD")).value().doubleValue();
        MEDIUM_VALIDATION_THRESHOLD = ((JSONNumber<?>) config.get("MEDIUM_VALIDATION_THRESHOLD")).value().doubleValue();
        FIRST_ROUND_CONTENT_THRESHOLD = ((JSONNumber<?>) config.get("FIRST_ROUND_CONTENT_THRESHOLD")).value().doubleValue();
        FIRST_ROUND_TITLE_THRESHOLD = ((JSONNumber<?>) config.get("FIRST_ROUND_TITLE_THRESHOLD")).value().doubleValue();
        TITLE_MULTIPLIER = ((JSONNumber<?>) config.get("TITLE_MULTIPLIER")).value().doubleValue();
        PRONOUN_SCORE = ((JSONNumber<?>) config.get("PRONOUN_SCORE")).value().doubleValue();
        HIGH_MATCH_SCORE = ((JSONNumber<?>) config.get("HIGH_MATCH_SCORE")).value().doubleValue();
        MEDIUM_MATCH_SCORE = ((JSONNumber<?>) config.get("MEDIUM_MATCH_SCORE")).value().doubleValue();
        LOW_MATCH_SCORE = ((JSONNumber<?>) config.get("LOW_MATCH_SCORE")).value().doubleValue();
        MIN_WORD_TO_WORD_THRESHOLD = ((JSONNumber<?>) config.get("MIN_WORD_TO_WORD_THRESHOLD")).value().doubleValue();
        RELIABLE_TITLE_THRESHOLD = ((JSONNumber<?>) config.get("RELIABLE_TITLE_THRESHOLD")).value().doubleValue();
        
        /* Word to word similarity expanded to sentence to sentence .. so we need word metrics */
        wnMetricLin = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LIN, false);
        optimumComparerWNLin = new OptimumComparer(wnMetricLin, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);
        //wnMetricWup = new WNWordMetric(WordNetSimilarity.WNSimMeasure.WUP, false);

        
        preprocessor = new SentencePreprocessor(SentencePreprocessor.TokenizerType.STANFORD, SentencePreprocessor.TaggerType.STANFORD, SentencePreprocessor.StemmerType.PORTER, SentencePreprocessor.ParserType.STANFORD);

    }


    
	@Override
	public ValidationResult[] call(Query query, Article article) throws IOException {

        Sentence querySentence;
        Sentence articleSentence;
        
	   //long startSP = System.nanoTime();
        
	   //long endSP = System.nanoTime();
	   //long spElapsedMillis = (endSP - startSP) / 1000000;
        
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
		////System.out.println("TIMING2: " + ppSentenceElapsedMillis + " milliseconds to preprocess sentence"); 
		long startComputeSimilarity = System.nanoTime();
                temp = (double) optimumComparerWNLin.computeSimilarity(querySentence, articleSentence);
		long endComputeSimilarity = System.nanoTime();
		long computeSimilarityElapsedMillis = (endComputeSimilarity - startComputeSimilarity) / 1000000;
                if (temp.equals(Double.NaN))
                    continue;
                topN.add(new Pair<>(temp, sentence));
                if (topN.size() > MAX_SENTENCES)
                    topN.remove(topN.size() - 1);
            }
        }

        // Average of top 5 similar sentences
        double average = 0.0;
        int count = 0;
		for (Pair<Double, CoreMap> p : topN){
            count += 1;
            if (count > 5) {
                break;
            }
			average += p.getX();
        }

        average /= (double) count; 
        double validation = 0.0;
        if (average > FIRST_ROUND_CONTENT_THRESHOLD || tempTitle > FIRST_ROUND_TITLE_THRESHOLD) {
            validation = postProcess(topN, query,phrase1.toString(), title, tempTitle);
        }
        //System.out.println("Annotated title: "+ article.getAnnotatedTitle());
        //System.out.println("ARTICLE  ID " + article.getID() + " average: "+average + " title: "+tempTitle);
        return new ValidationResult[]{new ValidationResult(article.getID(), validation)};
    }
    

    /* @author Phuong Dinh & Julia Kroll
    */
    public double postProcess(SortedList<Pair<Double, CoreMap>> topN, Query query, String rawQuery, String articleTitle, double titleScore){

        String subject, dirObject, indirObject, location;
        HashSet<String> userQueryParts = new HashSet<String>(); // because subject and verbs are compulsory
        userQueryParts.add("SUBJECT");
        userQueryParts.add("VERB");
        subject = query.getSubject();
        dirObject = "";
        indirObject = "";
        location = "";
        if (query.getDirectObject() != null && query.getDirectObject().length() > 0) {
            dirObject = query.getDirectObject();
            userQueryParts.add("OBJECT");
        }
        if (query.getIndirectObject() != null && query.getIndirectObject().length() > 0) {
            indirObject = query.getIndirectObject();
            userQueryParts.add("OBJECT");
        }
        if (query.getLocation() != null && query.getLocation().length() > 0) {
            location = query.getLocation();
            userQueryParts.add("LOCATION");
        }

 
        HashMap<String, String> keywordNouns = new HashMap<String, String>();
        //Annotation taggedQuery = POSTagger.annotate(rawQuery);
        // Annotation taggedQuery = POSTagger.tag(annotatedQuery);
        Annotation taggedQuery = POSTagger.annotate(rawQuery);

        for (CoreLabel token: taggedQuery.get(TokensAnnotation.class)){
                String pos = token.get(PartOfSpeechAnnotation.class);
                //System.out.println("Query tag: "+token + pos);
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

        HashSet<String> dependencyMatches = new HashSet<String>();
        HashSet<String> svolMatches = new HashSet<String>();
        HashMap<HashSet<String>, Integer> svolMatchCombinations = new HashMap<HashSet<String>, Integer>();
        double totalScore = 0;

        // ARTICLE CONTENT SCORE
        for (Pair<Double, CoreMap> p : topN){ //for each sentence
            dependencyMatches= validationScore(query, p.getY(), keywordNouns);
            //System.out.println("DEPENDENCY MATCHES::");
            //System.out.println(dependencyMatches.toString()); 
            for (String matchPart:dependencyMatches) {
                if (!svolMatches.contains(matchPart)) {
                    svolMatches.add(matchPart);
                }
            }
            if (!svolMatchCombinations.containsKey(dependencyMatches)) {
                svolMatchCombinations.put(dependencyMatches, 1);
            }
            else {
                svolMatchCombinations.put(dependencyMatches, svolMatchCombinations.get(dependencyMatches) + 1);
            }
            //System.out.println("SVOLCOMBI: "+svolMatchCombinations);
        }

        for (HashSet<String> combi : svolMatchCombinations.keySet()) {
            int count = svolMatchCombinations.get(combi);
            totalScore = totalScore + calSentenceScore(combi, count, svolMatches, userQueryParts);
        }

        //System.out.println("TOTAL SCORE: " + totalScore);

        Annotation annotatedTitle = POSTagger.annotate(articleTitle);
        CoreMap taggedTitle = annotatedTitle.get(SentencesAnnotation.class).get(0);
        
        //TITLE SCORE
        dependencyMatches = validationScore(query, taggedTitle, keywordNouns);
        //System.out.println("DEPENDENCY MATCHES OF TITLE::");
        //System.out.println(dependencyMatches.toString());
        double creditToSEMILARTitleScore = 0.0;
        if (titleScore > RELIABLE_TITLE_THRESHOLD) {
            creditToSEMILARTitleScore = TITLE_MULTIPLIER * MEDIUM_MATCH_SCORE;
        }
        totalScore += Math.max(creditToSEMILARTitleScore, calSentenceScore(dependencyMatches, 1, svolMatches, userQueryParts) * TITLE_MULTIPLIER);

        ////System.out.println("MATCH TOTAL SCORE: " + totalScore);


        if (totalScore > HIGH_VALIDATION_THRESHOLD){
            return 1.0;
        } else if (totalScore > MEDIUM_VALIDATION_THRESHOLD) {
            return totalScore/10;
        }
        return 0.0;

    }

    public double calSentenceScore(HashSet<String> combi, int count, HashSet<String> svolMatches, HashSet<String> userQueryParts){
        double totalScore = 0.0;
        if (combi.contains("S_PRONOUN")) {
            if (svolMatches.contains("SUBJECT")) { //If we decide that the number of missing sub in the other sentences are high
                totalScore += PRONOUN_SCORE; // half score of what we give other things
            }
            combi.remove("S_PRONOUN");
        }
        if (combi.contains("O_PRONOUN")) {
            if (svolMatches.contains("OBJECT")) {
                totalScore += PRONOUN_SCORE; // half score of what we give other things
            }
            combi.remove("O_PRONOUN");
        }
        //Calculate point as normal now
        if ((userQueryParts.size() == 4 && combi.size() == 4) || (userQueryParts.size() == 3 && combi.size() == 3)) {
            totalScore += HIGH_MATCH_SCORE;
        } else if ((userQueryParts.size() == 4 && combi.size() == 3) || (userQueryParts.size() == 3 && combi.size() == 2) 
            || (userQueryParts.size() == 2 && combi.size() == 2)) {
            totalScore += MEDIUM_MATCH_SCORE;
        } else if ((userQueryParts.size() == 4 && combi.size() == 2) || (userQueryParts.size() == 2 && combi.size() == 1 && svolMatches.size() == 2)) {
            totalScore += LOW_MATCH_SCORE;
        }
        return totalScore*count;
    }

    public HashSet<String> validationScore(Query query, CoreMap sentence, HashMap<String, String> keywordNouns){
        int matchedPerSentence = 0;
        // matchedTokens contains HashMap<SVO string, set of words that match that SVO>
        HashMap<String, HashSet<CoreLabel>> matchedTokens = new HashMap<String, HashSet<CoreLabel>>();
        for (CoreLabel token: sentence.get(TokensAnnotation.class)){ //each word in sentence
            String pos = token.get(PartOfSpeechAnnotation.class);
            String lemma = token.get(LemmaAnnotation.class);

            if (pos.length() > 1 && pos.substring(0,2).equals("NN")){
                for (String imptNoun:keywordNouns.keySet()){
                    double matched = 0.0;
                    if (pos.equals("NNP")){
                        if (lemma.toLowerCase().equals(imptNoun.toLowerCase())){
                            matched = 1;
                        }
                    } else {
                        matched = wnMetricLin.computeWordSimilarityNoPos(lemma.toLowerCase(), imptNoun.toLowerCase());

                        // This only fixes identical word. i need stem here!
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
            //System.out.println("Dependecy Tree: " + dependencies);
            List<IndexedWord> verbNodes = getVerbNodes(token, verb, dependencies);
            ////System.out.println("VERB found for "+token+" is "+verbNodes);

            for (IndexedWord verbNode : verbNodes){
                // TODO: Not only verb to verb, but also verb to adj (e.g) die == was dead
                if (verbNode != null) {
                    if (wnMetricLin.computeWordSimilarityNoPos(verbNode.lemma(), verb) > MIN_WORD_TO_WORD_THRESHOLD) {
                        dependencyMatches.add("VERB");
                    }
                    if (!dependencyMatches.contains("OBJECT") && matchedTokens.containsKey("OBJECT")){
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
            if (queryLocation.length() > 3) {
	            String potentialPrep = queryLocation.substring(0,3);
	            if (potentialPrep.equals("in ") || potentialPrep.equals("on ") || potentialPrep.equals("at ")){
	                queryLocation = queryLocation.substring(3);
	            }
	        }
        }    
        //System.out.println("LOCATION is: "+queryLocation + " sentence " + sentence.toString());

        Pattern isLocationMatch = Pattern.compile("(^|[\\-\"' \t])"+queryLocation+"[$\\.!?\\-,;\"' \t]");
        if (!queryLocation.equals("") && isLocationMatch.matcher(sentence.toString()).find()) {
            dependencyMatches.add("LOCATION");
        }

        //Clean up if sentence has both O_PRONOUN & OBJECT or S_PRONOUN & SUBJECT
        if (dependencyMatches.contains("O_PRONOUN") && dependencyMatches.contains("OBJECT")){
            dependencyMatches.remove("O_PRONOUN");
        }
        if (dependencyMatches.contains("S_PRONOUN") && dependencyMatches.contains("SUBJECT")){
            dependencyMatches.remove("S_PRONOUN");
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

                            if (wnMetricLin.computeWordSimilarityNoPos(childNode.lemma(), objectToken.get(LemmaAnnotation.class)) > MIN_WORD_TO_WORD_THRESHOLD
                                || childNode.lemma().toLowerCase().equals(objectToken.get(LemmaAnnotation.class).toLowerCase())) {
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

        List<IndexedWord> nounNodes = dependencies.getAllNodesByWordPattern(token.toString().split("-")[0]);
        List<IndexedWord> verbNodes = new ArrayList<IndexedWord>();
        for (IndexedWord nounNode : nounNodes) {
            IndexedWord parent = dependencies.getParent(nounNode);
            while (parent != null && dependencies.getParent(parent) != null && !parent.tag().substring(0,1).equals("V")) {
                if (parent == dependencies.getParent(parent)){
                    break;
                }
                parent = dependencies.getParent(parent);
            }
            verbNodes.add(parent);
        }
        return verbNodes;
    }
}
