package eventdetection.validator;

import eventdetection.common.Article;
import eventdetection.common.Query;

/**
 * A quick functional interface that represents the constructor of a {@link Validator}
 * 
 * @author Joshua Lipstone
 */
@FunctionalInterface
public interface ValidatorConstructor {
	
	/**
	 * Constructs a new instance of a particular {@link Validator} with the given parameters.
	 * 
	 * @param algorithmID
	 *            the ID of the algorithm that the {@link Validator} implements as it appears in the database
	 * @param query
	 *            the {@link Query} to be validated
	 * @param article
	 *            the {@link Article} against which the {@link Query} is to be validated
	 * @return a {@link Validator} that implements the algorithm and is set to validate the given {@link Query} against the
	 *         given {@link Article}
	 */
	public Validator construct(Integer algorithmID, Query query, Article article);
}
