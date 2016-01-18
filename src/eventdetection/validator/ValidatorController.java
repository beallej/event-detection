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
							System.out.println(serialized);
							articles.add(article);
						}
					}
				}
			}
		}
		
		return articles;
	}
	
	public boolean addValidator(String algorithm, ValidatorConstructor constructor) throws SQLException {
		synchronized (connection) {
			if (validators.containsKey(algorithm))
				return false;
			validators.put(algorithm, new ValidatorWrapper(connection, algorithm, constructor));
			return true;
		}
	}
	
	public void executeValidators(Query query, List<Article> articles) throws SQLException {
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
					try (PreparedStatement stmt = connection.prepareStatement("insert into validation_results (query, algorithm, validates, invalidates) values (?, ?, ?, ?)")) {
						stmt.setInt(1, query.getId());
						stmt.setInt(2, res.getAlgorithmID());
						stmt.setFloat(3, res.getValidates().floatValue());
						if (res.getInvalidates() != null)
							stmt.setFloat(4, res.getInvalidates().floatValue());
						else
							stmt.setNull(4, Types.REAL);
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
