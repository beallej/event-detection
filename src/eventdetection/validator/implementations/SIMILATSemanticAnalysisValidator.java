package eventdetection.validator.implementations;

import semilar.config.ConfigManager;
import semilar.data.Sentence;
import semilar.sentencemetrics.BLEUComparer;
import semilar.sentencemetrics.CorleyMihalceaComparer;
import semilar.sentencemetrics.DependencyComparer;
import semilar.sentencemetrics.GreedyComparer;
import semilar.sentencemetrics.LSAComparer;
import semilar.sentencemetrics.LexicalOverlapComparer;
import semilar.sentencemetrics.MeteorComparer;
import semilar.sentencemetrics.OptimumComparer;
import semilar.sentencemetrics.PairwiseComparer.NormalizeType;
import semilar.sentencemetrics.PairwiseComparer.WordWeightType;
import semilar.tools.preprocessing.SentencePreprocessor;
import semilar.tools.semantic.WordNetSimilarity;
import semilar.wordmetrics.LSAWordMetric;
import semilar.wordmetrics.WNWordMetric;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.text.*;

import toberumono.structures.collections.lists.SortedList;
import toberumono.structures.tuples.Pair;
import toberumono.structures.SortingMethods;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import eventdetection.common.Article;
import eventdetection.common.POSUtils;
import eventdetection.common.Query;
import eventdetection.common.Source;
import eventdetection.validator.ValidationResult;
import eventdetection.validator.Validator;
import eventdetection.validator.ValidatorController;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import toberumono.json.JSONArray;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;

/**
 * Examples using various sentence to sentence similarity methods. Please note that due to huge models of similarity
 * measures (wordnet files, LSA model, LDA models, ESA, PMI etc) and preprocessors - Standford/OpenNLP models, you may
 * not be able to run all the methods in a single pass. Also, for LDA based methods, we have to estimate the topic
 * distributions for the candidate pairs before calculating similarity. So, The examples for LDA based sentence to
 * sentence similarity and document level similarity are provided in the separate file as they have special
 * requirements.
 *
 * Some methods calculate the similarity of sentences directly or some use word to word similarity expanding to the
 * sentence level similarity. See the examples + documentation for more details.
 *
 * @author Rajendra
 */
public class SIMILATSemanticAnalysisValidator extends Validator {

    /* NOTE:
     * The greedy matching and Optimal matching methods rely on word to word similarity method.
     *(please see http://aclweb.org/anthology//W/W12/W12-2018.pdf for more details). So, based on the unique word to
     * word similarity measure, they have varying output (literally, many sentence to sentence similarity methods from
     * the combinations).
     */
    //greedy matching (see the available word 2 word similarity in the separate example file). Here I use some of them
    // for the illustration.
    GreedyComparer greedyComparerWNLin; //greedy matching, use wordnet LIN method for Word 2 Word similarity
    GreedyComparer greedyComparerWNLeskTanim;//greedy matching, use wordnet LESK-Tanim method for Word 2 Word similarity
    GreedyComparer greedyComparerLSATasa; // use LSA based word 2 word similarity (using TASA corpus LSA model).
    GreedyComparer greedyComparerLDATasa; // use LDA based word 2 word similarity (using TASA corpus LDA model).
    //Overall optimum matching method.. you may try all possible word to word similarity measures. Here I show some.
    OptimumComparer optimumComparerWNLin;
    OptimumComparer optimumComparerWNLeskTanim;
    OptimumComparer optimumComparerLSATasa;
    OptimumComparer optimumComparerLDATasa;
    //dependency based method.. we need to provide a word to word similarity metric. Here is just one example
    // using Wordnet Lesk Tanim.
    DependencyComparer dependencyComparerWnLeskTanim;
    //Please see paper Corley, C. and Mihalcea, R. (2005). Measuring the semantic similarity of texts.
    CorleyMihalceaComparer cmComparer;
    //METEOR method (introduced for machine translation evaluation): http://www.cs.cmu.edu/~alavie/METEOR/
    MeteorComparer meteorComparer;
    //BLEU (introduced for machine translation evaluation):http://acl.ldc.upenn.edu/P/P02/P02-1040.pdf 
    BLEUComparer bleuComparer;
    LSAComparer lsaComparer;
    LexicalOverlapComparer lexicalOverlapComparer; // Just see the lexical overlap.
    //For LDA based method.. see the separate example file. Its something different.
    public SIMILATSemanticAnalysisValidator(Integer algorithmID, Query query, Article article) {
		super(algorithmID, query, article);
	//}
//    public Sentence2SentenceSimilarityTest() {

        /* Word to word similarity expanded to sentence to sentence .. so we need word metrics */
//        boolean wnFirstSenseOnly = false; //applies for WN based methods only.
//        WNWordMetric wnMetricLin = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LIN, wnFirstSenseOnly);
//        WNWordMetric wnMetricLeskTanim = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LESK_TANIM, wnFirstSenseOnly);
        //provide the LSA model name you want to use.
        //LSAWordMetric lsaMetricTasa = new LSAWordMetric("LSA-MODEL-TASA-LEMMATIZED-DIM300");
        //provide the LDA model name you want to use.
        //LDAWordMetric ldaMetricTasa = new LDAWordMetric("LDA-MODEL-TASA-LEMMATIZED-TOPIC300");

        //greedyComparerWNLin = new GreedyComparer(wnMetricLin, 0.3f, false);
        //greedyComparerWNLeskTanim = new GreedyComparer(wnMetricLeskTanim, 0.3f, false);
        //greedyComparerLSATasa = new GreedyComparer(lsaMetricTasa, 0.3f, false);
        //greedyComparerLDATasa = new GreedyComparer(ldaMetricTasa, 0.3f, false);

        //optimumComparerWNLin = new OptimumComparer(wnMetricLin, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);
        //optimumComparerWNLeskTanim = new OptimumComparer(wnMetricLeskTanim, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);
//        optimumComparerLSATasa = new OptimumComparer(lsaMetricTasa, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);
        //optimumComparerLDATasa = new OptimumComparer(ldaMetricTasa, 0.3f, false, WordWeightType.NONE, NormalizeType.AVERAGE);

        //Use one of the many word metrics. The example below uses Wordnet Lesk Tanim. Similarly, try using other
        //word similarity metrics.
        //dependencyComparerWnLeskTanim = new DependencyComparer(wnMetricLeskTanim, 0.3f, true, "NONE", "AVERAGE");

        /* methods without using word metrics */
        //cmComparer = new CorleyMihalceaComparer(0.3f, false, "NONE", "par");
        //for METEOR, please provide the **Absolute** path to your project home folder (without / at the end), And the
        // semilar library jar file should be in your project home folder.
        //meteorComparer = new MeteorComparer("C:/Users/Rajendra/workspace/SemilarLib/");
        //bleuComparer = new BLEUComparer();

        //lsaComparer: This is different from lsaMetricTasa, as this method will
        // directly calculate sentence level similarity whereas  lsaMetricTasa
        // is a word 2 word similarity metric used with Optimum and Greedy methods.
        lsaComparer = new LSAComparer("LSA-MODEL-TASA-LEMMATIZED-DIM300");
        //lexicalOverlapComparer = new LexicalOverlapComparer(false);  // use base form of words? - No/false. 
        //for LDA based method.. please see the different example file.
    }

    public void printSimilarities(Sentence sentenceA, Sentence sentenceB) {
        System.out.println("Sentence 1:" + sentenceA.getRawForm());
        System.out.println("Sentence 2:" + sentenceB.getRawForm());
        System.out.println("------------------------------");
        System.out.println("greedyComparerWNLin : " + greedyComparerWNLin.computeSimilarity(sentenceA, sentenceB));
        //System.out.println("greedyComparerWNLeskTanim : " + greedyComparerWNLeskTanim.computeSimilarity(sentenceA, sentenceB));
        //System.out.println("greedyComparerLSATasa : " + greedyComparerLSATasa.computeSimilarity(sentenceA, sentenceB));
        //System.out.println("greedyComparerLDATasa : " + greedyComparerLDATasa.computeSimilarity(sentenceA, sentenceB));
        //System.out.println("optimumComparerWNLin : " + optimumComparerWNLin.computeSimilarity(sentenceA, sentenceB));
        //System.out.println("optimumComparerWNLeskTanim : " + optimumComparerWNLeskTanim.computeSimilarity(sentenceA, sentenceB));
//        System.out.println("optimumComparerLSATasa : " + optimumComparerLSATasa.computeSimilarity(sentenceA, sentenceB));
        //System.out.println("optimumComparerLDATasa : " + optimumComparerLDATasa.computeSimilarity(sentenceA, sentenceB));
        System.out.println("dependencyComparerWnLeskTanim : " + dependencyComparerWnLeskTanim.computeSimilarity(sentenceA, sentenceB));
        System.out.println("cmComparer : " + cmComparer.computeSimilarity(sentenceA, sentenceB));
        //System.out.println("meteorComparer : " + meteorComparer.computeSimilarity(sentenceA, sentenceB));
        //System.out.println("bleuComparer : " + bleuComparer.computeSimilarity(sentenceA, sentenceB));
        System.out.println("lsaComparer : " + lsaComparer.computeSimilarity(sentenceA, sentenceB));
//        System.out.println("lexicalOverlapComparer : " + lexicalOverlapComparer.computeSimilarity(sentenceA, sentenceB));
        System.out.println("                              ");
    }
    public double similarityProb(Query query, String articleText) {
        
        StringBuilder phrase1 = new StringBuilder();
		phrase1.append(query.getSubject()).append(" ").append(query.getVerb());
		if (query.getDirectObject() != null && query.getDirectObject().length() > 0)
			phrase1.append(" ").append(query.getDirectObject());
		if (query.getIndirectObject() != null && query.getIndirectObject().length() > 0)
			phrase1.append(" ").append(query.getIndirectObject());
        if (query.getLocation() != null && query.getLocation().length() > 0)
			phrase1.append(" ").append(query.getLocation());
        
        SortedList<Pair<Double, String>> topFive = new SortedList<>((a, b) -> b.getX().compareTo(a.getX()));
        
        Sentence sentence1;
        Sentence sentence2;
        double temp;
        BreakIterator iterator = BreakIterator.getSentenceInstance();
iterator.setText(articleText);
int start = iterator.first();
SentencePreprocessor preprocessor = new SentencePreprocessor(SentencePreprocessor.TokenizerType.STANFORD, SentencePreprocessor.TaggerType.STANFORD, SentencePreprocessor.StemmerType.PORTER, SentencePreprocessor.ParserType.STANFORD);
        sentence1 = preprocessor.preprocessSentence(phrase1.toString());
        double max = 0.0;
for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
    //System.out.println(content.substring(start,end));
    String sentence = articleText.substring(start,end);
    sentence2 = preprocessor.preprocessSentence(sentence);
    temp = lsaComparer.computeSimilarity(sentence1, sentence2);
    if (temp > max){
        max = temp;
    }
}
//    topFive.add(new Pair<>(temp, sentence2));
//    if (topFive.size() > 5)
//         topFive.remove(topFive.size() - 1);
//}
//        double average = 0.0;
//        for (Pair<Double, String> p : topFive)
//            average += p.getX();
//        average /= (double) topFive.size();
        
        return max;
    }
    
	@Override
	public ValidationResult call() throws IOException {
//		StringBuilder phrase1 = new StringBuilder();
//		phrase1.append(query.getSubject()).append(" ").append(query.getVerb());
//		if (query.getDirectObject() != null && query.getDirectObject().length() > 0)
//			phrase1.append(" ").append(query.getDirectObject());
//		if (query.getIndirectObject() != null && query.getIndirectObject().length() > 0)
//			phrase1.append(" ").append(query.getIndirectObject());
//		SortedList<Pair<Double, String>> topN = new SortedList<>((a, b) -> b.getX().compareTo(a.getX()));
//		for (Annotation paragraph : article.getAnnotatedText()) {
//			List<CoreMap> sentences = paragraph.get(SentencesAnnotation.class);
//			for (CoreMap sentence : sentences) {
//				String sen = POSUtils.reconstructSentence(sentence);
//				String url = String.format("%s&phrase1=%s&phrase2=%s", URL_PREFIX, URLEncoder.encode(phrase1.toString(), StandardCharsets.UTF_8.name()),
//						URLEncoder.encode(sen, StandardCharsets.UTF_8.name()));
//				URLConnection connection = new URL(url).openConnection();
//				connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
//				try (BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
//					topN.add(new Pair<>(Double.parseDouble(response.readLine().trim()), sen));
//					if (topN.size() > MAX_SENTENCES)
//						topN.remove(topN.size() - 1);
//				}
//			}
//		}
//		double average = 0.0;
//		for (Pair<Double, String> p : topN)
//			average += p.getX();
//		average /= (double) topN.size();
//		return new ValidationResult(this.getID(), article.getID(), average);
	return null;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // first of all set the semilar data folder path (ending with /).
        ConfigManager.setSemilarDataRootFolder("../../semilar-data/");

       




        
		Query query1;
        Article article1;
        query1 = new Query(58, "Obama", "meets", "mayor", "", "at White House", false);
        Query query2 = new Query(57, "astronomers", "finds", "supernova", "", "", false);
        String title = "Obama meets with Flint mayor; EPA says reviewing its role ";
        String text = "Obama meets with Flint mayor. Flint Mayor Karen Weaver met with President Barack Obama at the White House Tuesday to discuss the Michigan city's water crisis, the same day the Environmental Protection Agency said it was reviewing its own response to lead contamination there. Weaver, in Washington to attend a meeting of mayors, had said previously Tuesday that Obama needed to hear directly about the ongoing health calamity facing her city, and advocated for higher levels of federal support. Obama, who will visit Detroit on Wednesday, is not expected to visit Flint while he's in Michigan. The goal of his trip, the White House says, is to highlight a resurgent American auto industry. Flint, the birthplace of General Motors that once employed 80,000 autoworkers, but which now faces widespread poverty after auto jobs largely left the city, has been reeling from the discovery that its water contains dangerously high levels of lead. The revelation has led to accusations of government negligence and political cover-ups. Focus has honed in on Michigan Governor Rick Snyder, a Republican, who opponents claim took too long to respond when tests indicated high levels of lead in Flint's drinking water.";
        Source source = null;
        URL url = null;
        article1 = new Article( title,  text,  url,  source);
        SIMILATSemanticAnalysisValidator s2sSimilarityMeasurer = new SIMILATSemanticAnalysisValidator(3 , query1, article1);
        //s2sSimilarityMeasurer.printSimilarities(sentence1, sentence2);
        double result1 = s2sSimilarityMeasurer.similarityProb(query1, text);
        double result2 = s2sSimilarityMeasurer.similarityProb(query2, text);
        System.out.println("");
        System.out.println("Testing on the same article 'Obama meets with Flint mayor.'");
        System.out.println("LSA similarity for matched query (Obama meets mayor at White House): " + result1);
        System.out.println("LSA similarity for unmatched query (Astronomer finds supernova): " + result2);
        
        System.out.println("\nDone!");
    }
}
