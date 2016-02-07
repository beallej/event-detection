package eventdetection.validator.types;

import java.util.Collection;

import eventdetection.common.Article;
import eventdetection.common.Query;
import eventdetection.validator.ValidationResult;

/**
 * Base class for implementations of validation algorithms that take multiple {@link Query Queries} and multiple
 * {@link Article Articles} that are callable by this library.
 * 
 * @author Joshua Lipstone
 */
public abstract class ManyToManyValidator implements Validator {
	protected final Collection<Query> queries;
	protected final Collection<Article> articles;
	
	/**
	 * Constructs a new instance of the {@link Validator} for the given {@link Query Queries} and {@link Article Articles}
	 * 
	 * @param queries
	 *            the {@link Query Queries} to validate
	 * @param articles
	 *            the {@link Article Articles} against which the {@link Query Queries} are to be validated
	 */
	public ManyToManyValidator(Collection<Query> queries, Collection<Article> articles) {
		this.queries = queries;
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
