package eventdetection.validator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
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

import toberumono.json.JSONArray;
import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;

import eventdetection.common.Article;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;
import eventdetection.common.Query;
import eventdetection.validator.implementations.SIMILATSemanticAnalysisValidator;
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
	
	/**
	 * Constructs a {@link ValidatorController} that uses the given {@link Connection} to connect to the database.
	 * 
	 * @param connection
	 *            a {@link Connection} to the database to be used
	 */
	public ValidatorController(Connection connection) {
		this.connection = connection;
		this.validators = new LinkedHashMap<>();
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
		Path configPath = Paths.get(args[0]); //The configuration file must be provided
		JSONObject config = (JSONObject) JSONSystem.loadJSON(configPath);
		DBConnection.configureConnection((JSONObject) config.get("database"));
		Query query;
		try (Connection connection = DBConnection.getConnection()) {
			try (PreparedStatement stmt = connection.prepareStatement("select * from queries where queries.id = ?")) {
				stmt.setInt(1, Integer.parseInt(args[1]));
				try (ResultSet rs = stmt.executeQuery()) {
					if (!rs.next())
						return;
					query = new Query(rs);
				}
			}
			ValidatorController vc = new ValidatorController(connection);
			//ADD VALIDATORS HERE
			vc.addValidator("Swoogle Semantic Analysis", SwoogleSemanticAnalysisValidator::new);
            vc.addValidator("SEMILAR Semantic Analysis", SIMILATSemanticAnalysisValidator::new);
			List<Article> articles = loadArticles(connection, config, args);
			vc.executeValidators(query, articles);
		}
	}
	
	private static List<Article> loadArticles(Connection connection, JSONObject config, String[] args) throws SQLException, IOException, ClassNotFoundException {
		Collection<Path> storage =
				((JSONArray) ((JSONObject) config.get("paths")).get("articles")).stream().collect(LinkedHashSet::new, (s, p) -> s.add(Paths.get(p.toString())), LinkedHashSet::addAll);
		List<Article> articles = new ArrayList<>();
		try (PreparedStatement stmt = connection.prepareStatement("select * from articles where articles.id = ?")) {
			for (int i = 2; i < args.length; i++) {
				stmt.setInt(1, Integer.parseInt(args[i]));
				try (ResultSet rs = stmt.executeQuery()) {
					if (!rs.next())
						continue;
					for (Path store : storage) {
						if (!Files.exists(store))
							continue;
						String filename = rs.getString("filename");
						Path serialized = ArticleManager.toSerializedPath(store.resolve(filename));
						if (!Files.exists(serialized))
							continue;
						try (ObjectInputStream serialIn = new ObjectInputStream(new FileInputStream(serialized.toFile()))) {
							Article article = (Article) serialIn.readObject();
							articles.add(article);
						}
					}
				}
			}
		}
		return articles;
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
	 * Executes the {@link Validator Validators} registered with the {@link ValidatorController} on the given {@link Query}
	 * and {@link Collection} of {@link Article Articles} and writes the results to the database.
	 * 
	 * @param query
	 *            the {@link Query} to be validated
	 * @param articles
	 *            the {@link Article Articles} against which the {@link Query} is to be validated
	 * @throws SQLException
	 *             if an error occurs while reading from or writing to the database
	 */
	public void executeValidators(Query query, Collection<Article> articles) throws SQLException {
		synchronized (connection) {
			List<Future<ValidationResult>> results = new ArrayList<>();
			try (PreparedStatement stmt = connection.prepareStatement("select * from validation_results as vr where vr.query = ? and vr.algorithm = ? and vr.article = ?")) {
				stmt.setInt(1, query.getId());
				for (Article article : articles) {
					stmt.setInt(3, article.getID());
					for (ValidatorWrapper vw : validators.values()) {
						stmt.setInt(2, vw.getID());
						try (ResultSet rs = stmt.executeQuery()) {
							if (rs.next()) //If we've already processed the current article with the current validator for the current query 
								continue;
						}
						results.add(pool.submit(vw.construct(query, article)));
					}
				}
			}
			for (Future<ValidationResult> result : results) {
				try {
					ValidationResult res = result.get();
					System.out.println(res);
					try (PreparedStatement stmt = connection.prepareStatement("insert into validation_results (query, algorithm, article, validates, invalidates) values (?, ?, ?, ?, ?)")) {
						stmt.setInt(1, query.getId());
						stmt.setInt(2, res.getAlgorithmID());
						stmt.setInt(3, res.getArticleID());
						stmt.setFloat(4, res.getValidates().floatValue());
						if (res.getInvalidates() != null)
							stmt.setFloat(5, res.getInvalidates().floatValue());
						else
							stmt.setNull(5, Types.REAL);
						stmt.executeUpdate();
						System.out.println("(" + query.getId() + ", " + res.getAlgorithmID() + ", " + res.getArticleID() + ") -> (" + res.getValidates() + ", " +
								(res.getInvalidates() == null ? "null" : res.getInvalidates()) + ")");
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
