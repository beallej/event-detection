package eventdetection.validator;

import eventdetection.common.Query;
import edu.stanford.nlp.util.CoreMap;
import toberumono.structures.collections.lists.SortedList;
import toberumono.structures.tuples.Pair;


public class SemilarComputations {

	private SortedList<Pair<Double, CoreMap>> topN;
	private Query query;
	private String rawQuery;
	private String articleTitle;
	private double titleScore;
	private double average;


	public SemilarComputations(SortedList<Pair<Double, CoreMap>> topN, Query query, String rawQuery, String articleTitle, double titleScore, double average) {
		this.topN = topN;
		this.query = query;
		this.rawQuery = rawQuery;
		this.articleTitle = articleTitle;
		this.titleScore = titleScore;
		this.average = average;
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

}