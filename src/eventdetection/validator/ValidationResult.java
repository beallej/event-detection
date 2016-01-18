package eventdetection.validator;

import eventdetection.common.Article;
import eventdetection.common.Query;

public class ValidationResult {
	private final Integer algorithmID;
	private final Integer articleID;
	private final Double validates, invalidates;
	
	public ValidationResult(Validator algorithm, Article article, Double validates) {
		this(algorithm, article, validates, null);
	}
	
	public ValidationResult(Validator algorithm, Article article, Double validates, Double invalidates) {
		this(algorithm.getID(), article.getID(), validates, invalidates);
	}
	
	public ValidationResult(Integer algorithmID, Integer articleID, Double validates) {
		this(algorithmID, articleID, validates, null);
	}
	
	public ValidationResult(Integer algorithmID, Integer articleID, Double validates, Double invalidates) {
		this.algorithmID = algorithmID;
		this.articleID = articleID;
		this.validates = validates;
		this.invalidates = invalidates;
	}
	
	/**
	 * @return the {@code ID} of the algorithm that produced the {@link ValidationResult} as it appears in the database
	 */
	public Integer getAlgorithmID() {
		return algorithmID;
	}
	
	/**
	 * @return the {@code ID} of the {@link Article} that produced the {@link ValidationResult} references as it appears in
	 *         the database
	 */
	public Integer getArticleID() {
		return articleID;
	}
	
	/**
	 * @return the probability that the {@link Article} validates the {@link Query}
	 */
	public Double getValidates() {
		return validates;
	}
	
	/**
	 * @return the probability that the {@link Article} invalidates the {@link Query}
	 */
	public Double getInvalidates() {
		return invalidates;
	}
	
	@Override
	public String toString() {
		return "(" + getAlgorithmID() + ", " + getArticleID() + ", " + getValidates() + ", " + (getInvalidates() == null ? "null" : getInvalidates()) + ")";
	}
}
