package eventdetection.validator;

import eventdetection.common.Article;
import eventdetection.common.Query;
import eventdetection.validator.types.Validator;

/**
 * Container for the result of a {@link Validator Validator's} algorithm.
 * 
 * @author Joshua Lipstone
 */
public class ValidationResult {
	private final Integer queryID;
	private final Integer articleID;
	private final Double validates, invalidates;
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article} with a
	 * {@code null} {@code invalidates} value.
	 * 
	 * @param article
	 *            the {@link Article} on which the algorithm was run
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 */
	public ValidationResult(Article article, Double validates) {
		this(article, validates, null);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article}.
	 * 
	 * @param article
	 *            the {@link Article} on which the algorithm was run
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 * @param invalidates
	 *            the probability from [0.0, 1.0] that the {@link Article} invalidates the query
	 *            ({@code validates + invalidates} need not equal 1)
	 */
	public ValidationResult(Article article, Double validates, Double invalidates) {
		this(article.getID(), validates, invalidates);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article} with a
	 * {@code null} {@code invalidates} value.
	 * 
	 * @param articleID
	 *            the ID {@link Article} on which the algorithm was run as it appears in the database
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 */
	public ValidationResult(Integer articleID, Double validates) {
		this(articleID, validates, null);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article}.
	 * 
	 * @param articleID
	 *            the ID {@link Article} on which the algorithm was run as it appears in the database
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 * @param invalidates
	 *            the probability from [0.0, 1.0] that the {@link Article} invalidates the query
	 *            ({@code validates + invalidates} need not equal 1)
	 */
	public ValidationResult(Integer articleID, Double validates, Double invalidates) {
		this.queryID = null;
		this.articleID = articleID;
		this.validates = validates;
		this.invalidates = invalidates;
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article} with a
	 * {@code null} {@code invalidates} value.
	 * 
	 * @param query
	 *            the {@link Query} on which the algorithm was run
	 * @param article
	 *            the {@link Article} on which the algorithm was run
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 */
	public ValidationResult(Query query, Article article, Double validates) {
		this(query, article, validates, null);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article}.
	 * 
	 * @param query
	 *            the {@link Query} on which the algorithm was run
	 * @param article
	 *            the {@link Article} on which the algorithm was run
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 * @param invalidates
	 *            the probability from [0.0, 1.0] that the {@link Article} invalidates the query
	 *            ({@code validates + invalidates} need not equal 1)
	 */
	public ValidationResult(Query query, Article article, Double validates, Double invalidates) {
		this(query.getId(), article.getID(), validates, invalidates);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article} with a
	 * {@code null} {@code invalidates} value.
	 * 
	 * @param queryID
	 *            the ID of the {@link Query} on which the algorithm was run as it appears in the database
	 * @param articleID
	 *            the ID {@link Article} on which the algorithm was run as it appears in the database
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 */
	public ValidationResult(Integer queryID, Integer articleID, Double validates) {
		this(queryID, articleID, validates, null);
	}
	
	/**
	 * Constructs a {@link ValidationResult} for the given {@link Validator algorithm} and {@link Article}.
	 * 
	 * @param queryID
	 *            the ID of the {@link Query} on which the algorithm was run as it appears in the database
	 * @param articleID
	 *            the ID {@link Article} on which the algorithm was run as it appears in the database
	 * @param validates
	 *            the probability from [0.0, 1.0] that the {@link Article} validates the query
	 * @param invalidates
	 *            the probability from [0.0, 1.0] that the {@link Article} invalidates the query
	 *            ({@code validates + invalidates} need not equal 1)
	 */
	public ValidationResult(Integer queryID, Integer articleID, Double validates, Double invalidates) {
		this.queryID = queryID;
		this.articleID = articleID;
		this.validates = validates;
		this.invalidates = invalidates;
	}
	
	/**
	 * @return the {@code ID} of the {@link Article} that produced the {@link ValidationResult} references as it appears in
	 *         the database
	 */
	public Integer getArticleID() {
		return articleID;
	}
	
	/**
	 * @return the {@code ID} of the {@link Query} that produced the {@link ValidationResult} references as it appears in the
	 *         database
	 */
	public Integer getQueryID() {
		return queryID;
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
		return "(" + getArticleID() + ", " + getValidates() + ", " + (getInvalidates() == null ? "null" : getInvalidates()) + ")";
	}
}
