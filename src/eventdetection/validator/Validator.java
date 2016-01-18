package eventdetection.validator;

import java.util.concurrent.Callable;

import eventdetection.common.Article;
import eventdetection.common.Query;

/**
 * Base class for implementations of validation algorithms that are callable by this library.
 * 
 * @author Joshua Lipstone
 */
public abstract class Validator implements Callable<ValidationResult> {
	protected final Query query;
	protected final Article article;
	protected final Integer algorithmID;
	
	/**
	 * Constructs a new instance of the {@link Validator} for the given {@link Query} and {@link Article}
	 * 
	 * @param algorithmID
	 *            the ID of the implemented algorithm as determined by the {@link ValidatorController}
	 * @param query
	 *            the {@link Query} to validate
	 * @param article
	 *            the {@link Article} against which the {@link Query} is to be validated
	 */
	public Validator(Integer algorithmID, Query query, Article article) {
		this.query = query;
		this.article = article;
		this.algorithmID = algorithmID;
	}
	
	/**
	 * Executes the algorithm that the {@link Validator} implements
	 * 
	 * @return a {@link ValidationResult} with the appropriate information
	 */
	@Override
	public abstract ValidationResult call();
}
