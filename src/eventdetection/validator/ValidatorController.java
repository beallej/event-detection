package eventdetection.validator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;
import toberumono.structures.tuples.Triple;

import eventdetection.common.Article;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;
import eventdetection.common.Query;
import eventdetection.validator.implementations.SwoogleSemanticAnalysisValidator;

/**
 * A class that manages multiple validation algorithms and allows them to run in parallel.
 * 
 * @author Joshua Lipstone
 */
public class ValidatorController {
	/**
	 * A work stealing pool for use by all classes in this program.
	 */
	public static final ExecutorService pool = Executors.newWorkStealingPool();
	
	private final Connection connection;
	private final Map<String, ValidatorWrapper> validators;
	private static final Logger logger = LoggerFactory.getLogger(ValidatorController.class);
	private final ArticleManager articleManager;
	
	/**
	 * Constructs a {@link ValidatorController} that uses the given {@link Connection} to connect to the database.
	 * 
	 * @param connection
	 *            a {@link Connection} to the database to be used
	 * @param config
	 *            the {@link JSONObject} holding the configuration data
	 */
	public ValidatorController(Connection connection, JSONObject config) {
		this.connection = connection;
		this.validators = new LinkedHashMap<>();
		JSONObject paths = (JSONObject) config.get("paths");
		JSONObject articles = (JSONObject) config.get("articles");
		this.articleManager = new ArticleManager(connection, "articles", paths, articles);
	}
	
	/**
	 * Main method of the validation program.
	 * 
	 * @param args
	 *            the command line arguments
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws SQLException
	 *             if an SQL error occurs
	 * @throws ClassNotFoundException
	 *             if there is an issue loading an {@link Article}
	 */
	public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
		Path configPath = null; //The configuration file must be provided
		int action = 0;
		Collection<Integer> articleIDs = new LinkedHashSet<>();
		Collection<Integer> queryIDs = new LinkedHashSet<>();
		for (String arg : args) {
			if (arg.equalsIgnoreCase("-c"))
				action = 0;
			else if (arg.equalsIgnoreCase("-q"))
				action = 1;
			else if (arg.equalsIgnoreCase("-a"))
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
		try (Connection connection = DBConnection.getConnection()) {
			ValidatorController vc = new ValidatorController(connection, config);
			//ADD VALIDATORS HERE
			vc.addValidator("Swoogle Semantic Analysis", SwoogleSemanticAnalysisValidator::new);
			//vc.addValidator("SEMILAR Semantic Analysis", SIMILATSemanticAnalysisValidator::new);
			vc.executeValidators(queryIDs, articleIDs);
		}
	}
	
	private List<Query> loadQueries(Collection<Integer> ids) throws SQLException {
		List<Query> queries = new ArrayList<>();
		try (ResultSet rs = connection.prepareStatement("select * from queries").executeQuery()) {
			int id = 0;
			while (rs.next()) {
				id = rs.getInt("id");
				if (ids.contains(id)) {
					ids.remove(id); //Prevents queries from being loaded more than once
					queries.add(new Query(rs));
				}
			}
		}
		if (ids.size() > 0)
			logger.warn("Did not find queries with ids matching " + ids.stream().reduce("", (a, b) -> a + ", " + b.toString(), (a, b) -> a + b).substring(2));
		return queries;
	}
	
	/**
	 * Adds the given algorithm to the {@link ValidatorController}.
	 * 
	 * @param algorithm
	 *            the name of the algorithm as it appears in the database
	 * @param constructor
	 *            the constructor of a {@link Validator} that implements the given algorithm
	 * @return {@code true} if the algorithm was successfully added to the {@link ValidatorController}, otherwise
	 *         {@code false}
	 * @throws SQLException
	 *             if an error occurs while connecting to the database
	 */
	public boolean addValidator(String algorithm, ValidatorConstructor constructor) throws SQLException {
		synchronized (connection) {
			if (validators.containsKey(algorithm))
				return false;
			validators.put(algorithm, new ValidatorWrapper(connection, algorithm, constructor));
			return true;
		}
	}
	
	/**
	 * Executes the {@link Validator Validators} registered with the {@link ValidatorController} on the given
	 * {@link Collection} of {@link Query} IDs and {@link Collection} of {@link Article} IDs after loading them from the
	 * database and writes the results to the database.
	 * 
	 * @param queryIDs
	 *            the IDs of the {@link Query Queries} to be validated
	 * @param articleIDs
	 *            the IDs of the {@link Article Articles} against which the {@link Query Queries} are to be validated
	 * @throws SQLException
	 *             if an error occurs while reading from or writing to the database
	 */
	public void executeValidators(Collection<Integer> queryIDs, Collection<Integer> articleIDs) throws SQLException {
		synchronized (connection) {
			Collection<Query> queries = loadQueries(queryIDs);
			Collection<Article> articles = articleManager.loadArticles(articleIDs);
			List<Triple<Integer, Integer, Future<ValidationResult[]>>> results = new ArrayList<>();
			try (PreparedStatement stmt = connection.prepareStatement("select * from validation_results as vr where vr.query = ? and vr.algorithm = ? and vr.article = ?")) {
				for (Query query : queries) {
					stmt.setInt(1, query.getId());
					for (Article article : articles) {
						stmt.setInt(3, article.getID());
						for (ValidatorWrapper vw : validators.values()) {
							stmt.setInt(2, vw.getID());
							try (ResultSet rs = stmt.executeQuery()) {
								if (rs.next()) //If we've already processed the current article with the current validator for the current query 
									continue;
							}
							results.add(new Triple<>(query.getId(), vw.getID(), pool.submit(vw.construct(query, article))));
						}
					}
				}
			}
			for (Triple<Integer, Integer, Future<ValidationResult[]>> result : results) {
				try {
					ValidationResult[] ress = result.getZ().get();
					for (ValidationResult res : ress) {
						String statement = "insert into validation_results as vr (query, algorithm, article, validates, invalidates) values (?, ?, ?, ?, ?) " +
								"ON CONFLICT (query, algorithm, article) DO UPDATE set (validates, invalidates) = (EXCLUDED.validates, EXCLUDED.invalidates)"; //This is valid as of PostgreSQL 9.5
						try (PreparedStatement stmt = connection.prepareStatement(statement)) {
							stmt.setInt(1, result.getX());
							stmt.setInt(2, result.getY());
							stmt.setInt(3, res.getArticleID());
							stmt.setFloat(4, res.getValidates().floatValue());
							if (res.getInvalidates() != null)
								stmt.setFloat(5, res.getInvalidates().floatValue());
							else
								stmt.setNull(5, Types.REAL);
							stmt.executeUpdate();
							logger.info("(" + result.getX() + ", " + result.getY() + ", " + res.getArticleID() + ") -> (" + res.getValidates() + ", " +
									(res.getInvalidates() == null ? "null" : res.getInvalidates()) + ")");
						}
					}
				}
				catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class ValidatorWrapper {
	private final int algorithmID;
	private final ValidatorConstructor constructor;
	
	public ValidatorWrapper(Connection connection, String algorithm, ValidatorConstructor constructor) throws SQLException {
		this.constructor = constructor;
		try (PreparedStatement stmt = connection.prepareStatement("select id from validation_algorithms as va where va.algorithm = ?")) {
			stmt.setString(1, algorithm);
			try (ResultSet rs = stmt.executeQuery()) {
				rs.next();
				algorithmID = rs.getInt("id");
			}
		}
	}
	
	public int getID() {
		return algorithmID;
	}
	
	public Validator construct(Query query, Article article) {
		return constructor.construct(algorithmID, query, article);
	}
}
