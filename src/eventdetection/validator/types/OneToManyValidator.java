package eventdetection.validator.types;

import java.util.Collection;

import eventdetection.common.Article;
import eventdetection.common.Query;
import eventdetection.validator.ValidationResult;

/**
 * Base class for implementations of validation algorithms that take one {@link Query} and multiple {@link Article Articles}
 * that are callable by this library.
 * 
 * @author Joshua Lipstone
 */
public abstract class OneToManyValidator implements Validator {
	protected final Query query;
	protected final Collection<Article> articles;
	
	/**
	 * Constructs a new instance of the {@link Validator} for the given {@link Query} and {@link Article Articles}
	 * 
	 * @param query
	 *            the {@link Query} to validate
	 * @param articles
	 *            the {@link Article Articles} against which the {@link Query Queries} are to be validated
	 */
	public OneToManyValidator(Query query, Collection<Article> articles) {
		this.query = query;
		this.articles = articles;
	}
	
	/**
	 * Executes the algorithm that the {@link Validator} implements
	 * 
	 * @return a {@link ValidationResult} with the appropriate information
	 */
	@Override
	public abstract ValidationResult[] call() throws Exception;
}
