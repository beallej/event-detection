package eventdetection.validator;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toberumono.json.JSONArray;
import toberumono.json.JSONBoolean;
import toberumono.json.JSONObject;
import toberumono.json.JSONString;
import toberumono.json.JSONSystem;
import toberumono.structures.tuples.Triple;

import eventdetection.common.Article;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;
import eventdetection.common.Query;
import eventdetection.validator.types.Validator;
import eventdetection.validator.types.ValidatorType;

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
		this.articleManager = new ArticleManager(connection, ((JSONObject) config.get("tables")).get("articles").value().toString(), paths, articles);
		((JSONArray) paths.get("validators")).value().stream().map(a -> ((JSONString) a).value()).forEach(s -> {
			try {
				loadValidators(((JSONObject) config.get("tables")).get("validators").value().toString(), Paths.get(s));
			}
			catch (ClassNotFoundException | NoSuchMethodException | SecurityException | SQLException | IOException e) {
				logger.warn("Unabile to initialize a validator described in " + s, e);
			}
		});
	}
	
	private void loadValidators(String table, Path path) throws ClassNotFoundException, NoSuchMethodException, SecurityException, SQLException, IOException {
		if (Files.isRegularFile(path)) {
			if (path.getFileName().toString().endsWith(".json")) {
				JSONObject json = (JSONObject) JSONSystem.loadJSON(path);
				if (!json.containsKey("enabled") || ((JSONBoolean) json.get("enabled")).value()) {
					ValidatorWrapper vw = new ValidatorWrapper(connection, table, getClass().getClassLoader(), json);
					validators.put(vw.getName(), vw);
				}
			}
		}
		else {
			ClassLoader classloader = new URLClassLoader(new URL[]{path.toUri().toURL()});
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, p -> p.getFileName().toString().endsWith(".json"))) {
				for (Path p : stream) {
					JSONObject json = (JSONObject) JSONSystem.loadJSON(p);
					if (!json.containsKey("enabled") || ((JSONBoolean) json.get("enabled")).value()) {
						ValidatorWrapper vw = new ValidatorWrapper(connection, table, classloader, json);
						validators.put(vw.getName(), vw);
					}
				}
			}
		}
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
		try (Connection connection = DBConnection.getConnection()) {
			ValidatorController vc = new ValidatorController(connection, config);
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
							try {
								results.add(new Triple<>(query.getId(), vw.getID(), pool.submit(vw.construct(query, article))));
							}
							catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								logger.error("Unable to initialize the " + vw.getName() + " for query " + query.getId() + " and article " + article.getID(), e);
							}
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
	private static final Logger logger = LoggerFactory.getLogger(ValidatorWrapper.class);
	
	private final int algorithmID;
	private final String name;
	private final Class<? extends Validator> clazz;
	private final ValidatorType type;
	private final Constructor<? extends Validator> constructor;
	
	@SuppressWarnings("unchecked")
	public ValidatorWrapper(Connection connection, String table, ClassLoader classloader, JSONObject validator) throws SQLException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		clazz = (Class<? extends Validator>) classloader.loadClass(validator.get("class").value().toString());
		name = validator.get("id").value().toString();
		type = ValidatorType.valueOf(validator.get("type").value().toString());
		
		constructor = clazz.getConstructor(type.getConstructorArgTypes());
		constructor.setAccessible(true);
		
		try (PreparedStatement stmt = connection.prepareStatement("select id from " + table + " as va where va.algorithm = ?")) {
			stmt.setString(1, name);
			try (ResultSet rs = stmt.executeQuery()) {
				rs.next();
				algorithmID = rs.getInt("id");
			}
		}
		if (validator.containsKey("properties"))
			loadStaticProperties((JSONObject) validator.get("properties"));
	}
	
	private void loadStaticProperties(JSONObject properties) {
		try {
			Method staticInit = clazz.getMethod("loadStaticProperties", JSONObject.class);
			staticInit.setAccessible(true);
			try {
				staticInit.invoke(null, properties);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				logger.error("Failed to invoke the found static property initialization method for " + name, e);
			}
		}
		catch (NoSuchMethodException | SecurityException e) {
			logger.warn("Unable to find the static property initialization method for " + name, e);
		}
	}
	
	public int getID() {
		return algorithmID;
	}
	
	public String getName() {
		return name;
	}
	
	public ValidatorType getType() {
		return type;
	}
	
	public Validator construct(Object... args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return constructor.newInstance(args);
	}
}
