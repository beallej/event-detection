import eventdetection.common.Query;
import edu.stanford.nlp.util.CoreMap;
import toberumono.structures.collections.lists.SortedList;


public class SemilarComputations {
	
	private SortedList<Pair<Double, CoreMap>> topN;
	private Query query;
	private String rawQuery;
	private String articleTitle;
	private double titleScore;


	public SemilarComputations(SortedList<Pair<Double, CoreMap>> topN, Query query, String rawQuery, String articleTitle, double titleScore) {
		this.topN = topN;
		this.query = query;
		this.rawQuery = rawQuery;
		this.articleTitle = articleTitle;
		this.titleScore = titleScore;
	}

	public SortedList<Pair<Double, CoreMap>> getTopN(){
		return this.topN;
	}

	public Query getQuery(){
		return this.query;
	}

	public String getRawQuery(){
		return this.rawQuery;
	}
	public String getArticleTitle(){
		return this.articleTitle;
	}


	public double getTitleScore(){
		return this.titleScore;
	}

}