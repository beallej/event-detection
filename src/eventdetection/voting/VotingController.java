package eventdetection.voting;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toberumono.json.JSONNumber;
import toberumono.json.JSONObject;
import toberumono.structures.tuples.Pair;

import eventdetection.common.Article;
import eventdetection.common.DBConnection;
import eventdetection.common.Query;
import eventdetection.pipeline.PipelineComponent;
import eventdetection.validator.ValidatorController;

/**
 * A class that manages the voting algorithm that combines the results from all of the validation methods to finally
 * determine whether a {@link Query} has happened.
 * 
 * @author Joshua Lipstone
 */
public class VotingController implements PipelineComponent {
	private final Connection connection;
	private static final Logger logger = LoggerFactory.getLogger("VotingController");
	private final PreparedStatement query;
	private final double globalThreshold;
	
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
		Map<Integer, Double> sum = new HashMap<>(), count = new HashMap<>();
		Map<Integer, Query> queries = inputs.getX();
		Map<Integer, Article> articles = inputs.getY();
		for (Integer id : inputs.getX().keySet()) {
			sum.put(id, 0.0);
			count.put(id, 0.0);
		}
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
		List<Query> notValidated = sum.keySet().stream().filter(id -> (sum.get(id) / count.get(id) < globalThreshold)).map(id -> queries.get(id)).collect(Collectors.toList());
		for (Query nv : notValidated)
			queries.remove(nv.getID());
		return inputs;
	}
}
