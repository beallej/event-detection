package eventdetection.validator.types;

import eventdetection.common.Article;
import eventdetection.common.Query;
import eventdetection.validator.ValidationResult;

/**
 * Base class for implementations of validation algorithms that take one {@link Query} and one {@link Article} that are
 * callable by this library.
 * 
 * @author Joshua Lipstone
 */
public abstract class OneToOneValidator implements Validator {
	protected final Query query;
	protected final Article article;
	
	/**
	 * Constructs a new instance of the {@link Validator} for the given {@link Query} and {@link Article}
	 * 
	 * @param query
	 *            the {@link Query} to validate
	 * @param article
	 *            the {@link Article} against which the {@link Query} is to be validated
	 */
	public OneToOneValidator(Query query, Article article) {
		this.query = query;
		this.article = article;
	}
	
	/**
	 * Executes the algorithm that the {@link Validator} implements
	 * 
	 * @return a {@link ValidationResult} with the appropriate information
	 */
	@Override
	public abstract ValidationResult[] call() throws Exception;
}
