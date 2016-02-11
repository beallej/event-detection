package eventdetection.voting;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toberumono.json.JSONNumber;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;
import toberumono.structures.tuples.Pair;

import eventdetection.common.Article;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;
import eventdetection.common.IOSQLExceptedRunnable;
import eventdetection.common.Query;
import eventdetection.common.ThreadingUtils;
import eventdetection.pipeline.PipelineComponent;
import eventdetection.validator.ValidatorController;

/**
 * A class that manages the voting algorithm that combines the results from all of the validation methods to finally
 * determine whether a {@link Query} has happened.
 * 
 * @author Joshua Lipstone
 */
public class VotingController implements PipelineComponent, Closeable {
	private final Connection connection;
	private static final Logger logger = LoggerFactory.getLogger("VotingController");
	private final PreparedStatement query;
	private final double globalThreshold;
	private boolean closed;
	
	/**
	 * Constructs a {@link ValidatorController} with the given configuration data.
	 * 
	 * @param config
	 *            the {@link JSONObject} holding the configuration data
	 * @throws SQLException
	 *             if an error occurs while connecting to the database
	 */
	public VotingController(JSONObject config) throws SQLException {
		connection = DBConnection.getConnection();
		query = constructQuery((JSONObject) config.get("tables"));
		globalThreshold = ((JSONNumber<?>) ((JSONObject) config.get("voting")).get("global-threshold")).value().doubleValue();
	}
	
	private PreparedStatement constructQuery(JSONObject tables) throws SQLException {
		String statement =
				"select vr.query, vr.algorithm, vr.article, vr.validates, vr.invalidates, va.threshold from " + tables.get("results").value() + " vr inner join " + tables.get("validators").value() +
						" va on vr.algorithm = va.id group by vr.query";
		return connection.prepareStatement(statement);
	}
	
	@Override
	public Pair<Map<Integer, Query>, Map<Integer, Article>> execute(Pair<Map<Integer, Query>, Map<Integer, Article>> inputs) throws IOException, SQLException {
		Map<Integer, Query> queries = inputs.getX();
		List<Query> notValidated = executeVoting(queries, inputs.getY(), false);
		for (Query nv : notValidated)
			queries.remove(nv.getID());
		return inputs;
	}
	
	/**
	 * Runs the voting algorithm using the given {@link Query Queries} and {@link Article Articles}.<br>
	 * This method is thread-safe and uses the interprocess lock from
	 * {@link ThreadingUtils#executeTask(IOSQLExceptedRunnable)}.
	 * 
	 * @param queries
	 *            the {@link Query Queries} to validate
	 * @param articles
	 *            the {@link Article Articles} on which to validate them
	 * @return a {@link List} containing the {@link Query Queries} that were validated
	 * @throws IOException
	 *             if an error occurs while interacting with the interprocess lock
	 * @throws SQLException
	 *             if an SQL error occurs while reading the {@link Query Queries} from the database
	 */
	public List<Query> executeVoting(Map<Integer, Query> queries, Map<Integer, Article> articles) throws IOException, SQLException {
		return executeVoting(queries, articles, true);
	}
	
	/**
	 * Runs the voting algorithm using the given {@link Query Queries} and {@link Article Articles}.<br>
	 * This method is thread-safe and uses the interprocess lock from
	 * {@link ThreadingUtils#executeTask(IOSQLExceptedRunnable)}.
	 * 
	 * @param queries
	 *            the {@link Query Queries} to validate
	 * @param articles
	 *            the {@link Article Articles} on which to validate them
	 * @param returnValidated
	 *            if {@code true}, this method will return the {@link Query Queries} that were validated. If {@code false},
	 *            this method will return the {@link Query Queries} that were not validated
	 * @return a {@link List} containing the {@link Query Queries} that were either validated or not validated depending on the value of {@code returnValidated}
	 * @throws IOException
	 *             if an error occurs while interacting with the interprocess lock
	 * @throws SQLException
	 *             if an SQL error occurs while reading the {@link Query Queries} from the database
	 */
	public List<Query> executeVoting(Map<Integer, Query> queries, Map<Integer, Article> articles, boolean returnValidated) throws IOException, SQLException {
		Map<Integer, Double> sum = new HashMap<>(), count = new HashMap<>();
		for (Integer id : queries.keySet()) {
			sum.put(id, 0.0);
			count.put(id, 0.0);
		}
		ThreadingUtils.executeTask(() -> {
			try (ResultSet rs = query.executeQuery()) {
				int query, article;
				while (rs.next()) {
					if (!queries.containsKey(query = rs.getInt("vr.query")) || !articles.containsKey(article = rs.getInt("vr.article")))
						continue;
					if (rs.getFloat("vr.validates") >= rs.getFloat("va.threshold")) {
						sum.put(query, sum.get(query) + 1);
						logger.info("Article " + article + " validates query " + query);
					}
					count.put(query, count.get(count) + 1);
				}
			}
		});
		Predicate<Integer> filter = returnValidated ? id -> (sum.get(id) / count.get(id) >= globalThreshold) : id -> (sum.get(id) / count.get(id) < globalThreshold);
		return sum.keySet().stream().filter(filter).map(id -> queries.get(id)).collect(Collectors.toList());
	}
	
	/**
	 * Main method of the voting program.
	 * 
	 * @param args
	 *            the command line arguments
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws SQLException
	 *             if an SQL error occurs
	 */
	public static void main(String[] args) throws IOException, SQLException {
		Path configPath = Paths.get("./configuration.json"); //The configuration file defaults to "./configuration.json", but can be changed with arguments
		int action = 0;
		Collection<Integer> articleIDs = new LinkedHashSet<>();
		Collection<Integer> queryIDs = new LinkedHashSet<>();
		for (String arg : args) {
			if (arg.equalsIgnoreCase("-c"))
				action = 0;
			else if (arg.equalsIgnoreCase("-a"))
				action = 1;
			else if (arg.equalsIgnoreCase("-q"))
				action = 2;
			else if (action == 0)
				configPath = Paths.get(arg);
			else if (action == 1)
				articleIDs.add(Integer.parseInt(arg));
			else if (action == 2)
				queryIDs.add(Integer.parseInt(arg));
		}
		JSONObject config = (JSONObject) JSONSystem.loadJSON(configPath);
		DBConnection.configureConnection((JSONObject) config.get("database"));
		try (Connection connection = DBConnection.getConnection(); VotingController vc = new VotingController(config); ArticleManager articleManager = new ArticleManager(connection, config)) {
			vc.executeVoting(ThreadingUtils.loadQueries(queryIDs), ThreadingUtils.loadArticles(articleManager, articleIDs));
		}
	}
	
	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		try {
			connection.close();
		}
		catch (SQLException e) {
			logger.error("An SQL error occured while closing a VotingController's Connection.", e);
		}
	}
}
