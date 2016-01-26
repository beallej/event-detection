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
 * A SEMILAR validator using prebuilt library from http://deeptutor2.memphis.edu/
 * With some post-run modification
 *
 * @author Anmol and Phuong
 */
public class SIMILATSemanticAnalysisValidator extends Validator {

    private static final int MAX_SENTENCES = 5;
    
        

    //BELOW ARE SOME OF THE LIBRARY ALGORITH!
    //NEED TO TEST AND DECIDE WHICH ONE WE WANT TO USE
    // ----------------------------------------------
    
    // TESTING SO FAR: 
    // cmComparer works but is not very good.
    // BLEU is BAD
    // LSA is much better, but loading + training time is much longer, AND it's not working right now due to not be able to find the file
    
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
    public SIMILATSemanticAnalysisValidator(Integer algorithmID, Query query, Article article) {
        
		super(algorithmID, query, article);
	//}
//    public articleSentenceSentenceSimilarityTest() {

        /* Word to word similarity expanded to sentence to sentence .. so we need word metrics */
//        boolean wnFirstSenseOnly = false; //applies for WN based methods only.
//        WNWordMetric wnMetricLin = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LIN, wnFirstSenseOnly);
//        WNWordMetric wnMetricLeskTanim = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LESK_TANIM, wnFirstSenseOnly);
        //provide the LSA model name you want to use.
        LSAWordMetric lsaMetricTasa = new LSAWordMetric("LSA-MODEL-TASA-LEMMATIZED-DIM300");
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
        cmComparer = new CorleyMihalceaComparer(0.3f, false, "NONE", "par");
        //for METEOR, please provide the **Absolute** path to your project home folder (without / at the end), And the
        // semilar library jar file should be in your project home folder.
        //meteorComparer = new MeteorComparer("C:/Users/Rajendra/workspace/SemilarLib/");
//        bleuComparer = new BLEUComparer();

        //lsaComparer: This is different from lsaMetricTasa, as this method will
        // directly calculate sentence level similarity whereas  lsaMetricTasa
        // is a word 2 word similarity metric used with Optimum and Greedy methods.
        lsaComparer = new LSAComparer("LSA-MODEL-TASA-LEMMATIZED-DIM300");
        //lexicalOverlapComparer = new LexicalOverlapComparer(false);  // use base form of words? - No/false. 
        //for LDA based method.. please see the different example file.
    }


    
	@Override
	public ValidationResult call() throws IOException {
        //ConfigManager.setSemilarDataRootFolder("../semilar-data/");
        
        Sentence querySentence;
        Sentence articleSentence;
        
        SentencePreprocessor preprocessor = new SentencePreprocessor(SentencePreprocessor.TokenizerType.STANFORD, SentencePreprocessor.TaggerType.STANFORD, SentencePreprocessor.StemmerType.PORTER, SentencePreprocessor.ParserType.STANFORD);
        
        SortedList<Pair<Double, String>> topN = new SortedList<>((a, b) -> b.getX().compareTo(a.getX()));
        
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

        
        for (Annotation paragraph : article.getAnnotatedText()) {
			List<CoreMap> sentences = paragraph.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
                String sen = POSUtils.reconstructSentence(sentence);
                articleSentence = preprocessor.preprocessSentence(sen);
                temp = (double) lsaComparer.computeSimilarity(querySentence, articleSentence);
                if (temp.equals(Double.NaN))
                    continue;
                topN.add(new Pair<>(temp, sen));
                if (topN.size() > MAX_SENTENCES)
                    topN.remove(topN.size() - 1);
            }
        }
        
  
  		double average = 0.0;
		for (Pair<Double, String> p : topN){
			average += p.getX();
            System.out.println(p.getY() + p.getX());
        }
		average /= (double) topN.size(); 
        //System.out.println("AVERGARE OF SEMILAR = " + average);
        return new ValidationResult(this.getID(), article.getID(), average);
    }
    
   
}
