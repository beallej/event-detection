package eventdetection.temporarylibraryplaceholders;

import java.io.IOException;

/**
 * A sub-interface of {@link ExceptedBiFunction} specifically for {@link IOException IOExceptions}.
 * 
 * @author Toberumono
 * @param <T>
 *            the type of the first argument
 * @param <U>
 *            the type of the second argument
 * @param <R>
 *            the type of the returned value
 */
@FunctionalInterface
public interface IOExceptedBiFunction<T, U, R> extends ExceptedBiFunction<T, U, R> {
	
	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t
	 *            the first argument
	 * @param u
	 *            the second argument
	 * @return the function result
	 * @throws IOException
	 *             if something goes wrong
	 */
	@Override
	public R apply(T t, U u) throws IOException;
}
