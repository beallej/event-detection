package eventdetection.pipeline;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import toberumono.structures.tuples.Pair;

import eventdetection.common.Article;
import eventdetection.common.Query;

/**
 * This describes the methods that must be implemented by components that can be added to the {@link Pipeline}.
 * 
 * @author Joshua Lipstone
 */
@FunctionalInterface
public interface PipelineComponent {
	
	/**
	 * Executes the {@link Pipeline} component's step with an empty query, article pair
	 * 
	 * @return the query, article pair with which the step was executed
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws SQLException
	 *             if an SQL error occurs
	 */
	public default Pair<Map<Integer, Query>, Map<Integer, Article>> execute() throws IOException, SQLException {
		return execute(new Pair<>(new LinkedHashMap<>(), new LinkedHashMap<>()));
	}
	
	/**
	 * Executes the {@link Pipeline} component's step with the given query, article pair
	 * 
	 * @param inputs
	 *            the query, article pair with which the step is to be executed
	 * @return the query, article pair with which the step was executed
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws SQLException
	 *             if an SQL error occurs
	 */
	public Pair<Map<Integer, Query>, Map<Integer, Article>> execute(Pair<Map<Integer, Query>, Map<Integer, Article>> inputs) throws IOException, SQLException;
}
