package eventdetection.validator;

import eventdetection.common.Query;
import edu.stanford.nlp.util.CoreMap;
import toberumono.structures.collections.lists.SortedList;
import toberumono.structures.tuples.Pair;

import java.util.HashMap;
import java.util.HashSet;


public class SemilarComputations {

	private SortedList<Pair<Double, CoreMap>> topN;
	private Query query;
	private String rawQuery;
	private String articleTitle;
	private double titleScore;
	private double average;
	private HashMap<HashSet<String>, Integer> svolMatchCombinations;
	private HashSet<String> svolMatches;
	private HashSet<String> dependencyMatches;


	public SemilarComputations(SortedList<Pair<Double, CoreMap>> topN, Query query, String rawQuery, String articleTitle, double titleScore, double average) {
		this.topN = topN;
		this.query = query;
		this.rawQuery = rawQuery;
		this.articleTitle = articleTitle;
		this.titleScore = titleScore;
		this.average = average;
		// this.svolMatchCombinations = new HashMap<HashSet<String>, Integer>();
		// this.svolMatches = new HashSet<String>();
		// this.dependencyMatches = new HashSet<String>();
	}

	public SortedList<Pair<Double, CoreMap>> getTopN(){
		return topN;
	}

	public Query getQuery(){
		return query;
	}

	public String getRawQuery(){
		return rawQuery;
	}
	public String getArticleTitle(){
		return articleTitle;
	}

	public double getTitleScore(){
		return titleScore;
	}

	public double getAverage(){
		return average;
	}

	public HashMap<HashSet<String>, Integer> getSvolMatchCombinations() {
		return svolMatchCombinations;
	}

	public HashSet<String> getSvolMatches() {
		return svolMatches;
	}

	public HashSet<String> getDependencyMatches() {
		return dependencyMatches;
	}

	public void addMatches(HashMap<HashSet<String>, Integer> svolMatchCombinations, HashSet<String> svolMatches, HashSet<String> dependencyMatches) {
		this.svolMatchCombinations = svolMatchCombinations;
		this.svolMatches = svolMatches;
		this.dependencyMatches = dependencyMatches;
	}

	public boolean hasMatches() {
		if (svolMatchCombinations == null) {
			return false;
		}
		return true;
	}

}