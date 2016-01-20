package eventdetection.validator;

import eventdetection.common.Article;
import eventdetection.common.Query;

/**
 * Container for the result of a {@link Validator Validator's} algorithm.
 * 
 * @author Joshua Lipstone
 */
public class ValidationResult {
	private final Integer algorithmID;
	private final Integer articleID;
	private final Double validates, invalidates;
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article} with a
	 * {@code null} {@code invalidates} value.
	 * 
	 * @param algorithm
	 *            the {@link Validator} that was used
	 * @param article
	 *            the {@link Article} on which the algorithm was run
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 */
	public ValidationResult(Validator algorithm, Article article, Double validates) {
		this(algorithm, article, validates, null);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article}.
	 * 
	 * @param algorithm
	 *            the {@link Validator} that was used
	 * @param article
	 *            the {@link Article} on which the algorithm was run
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 * @param invalidates
	 *            the probability from [0.0, 1.0] that the {@link Article} invalidates the query
	 *            ({@code validates + invalidates} need not equal 1)
	 */
	public ValidationResult(Validator algorithm, Article article, Double validates, Double invalidates) {
		this(algorithm.getID(), article.getID(), validates, invalidates);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article} with a
	 * {@code null} {@code invalidates} value.
	 * 
	 * @param algorithmID
	 *            the ID of the {@link Validator} that was used as it appears in the database
	 * @param articleID
	 *            the ID {@link Article} on which the algorithm was run as it appears in the database
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 */
	public ValidationResult(Integer algorithmID, Integer articleID, Double validates) {
		this(algorithmID, articleID, validates, null);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article}.
	 * 
	 * @param algorithmID
	 *            the ID of the {@link Validator} that was used as it appears in the database
	 * @param articleID
	 *            the ID {@link Article} on which the algorithm was run as it appears in the database
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 * @param invalidates
	 *            the probability from [0.0, 1.0] that the {@link Article} invalidates the query
	 *            ({@code validates + invalidates} need not equal 1)
	 */
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
