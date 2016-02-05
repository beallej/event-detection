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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toberumono.json.JSONArray;
import toberumono.json.JSONBoolean;
import toberumono.json.JSONObject;
import toberumono.json.JSONString;
import toberumono.json.JSONSystem;
import toberumono.structures.tuples.Triple;

import static eventdetection.common.ThreadingUtils.pool;

import eventdetection.common.Article;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;
import eventdetection.common.Query;
import eventdetection.common.ThreadingUtils;
import eventdetection.validator.types.Validator;
import eventdetection.validator.types.ValidatorType;

/**
 * A class that manages multiple validation algorithms and allows them to run in parallel.
 * 
 * @author Joshua Lipstone
 */
public class ValidatorController {
	private final Connection connection;
	private final Map<ValidatorType, Map<String, ValidatorWrapper>> validators;
	private static final Logger logger = LoggerFactory.getLogger("ValidatorController");
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
		this.validators = new EnumMap<>(ValidatorType.class);
		for (ValidatorType vt : ValidatorType.values())
			validators.put(vt, new LinkedHashMap<>());
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
					validators.get(vw.getType()).put(vw.getName(), vw);
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
						validators.get(vw.getType()).put(vw.getName(), vw);
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
			if (ids.size() > 0) {
				int id = 0;
				while (ids.size() > 0 && rs.next()) {
					id = rs.getInt("id");
					if (ids.contains(id)) {
						ids.remove(id); //Prevents queries from being loaded more than once
						queries.add(new Query(rs));
					}
				}
			}
			else {
				while (rs.next())
					queries.add(new Query(rs));
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
	 * @throws IOException
	 *             if an error occurs while securing access to the serialized {@link Article Articles}
	 */
	public void executeValidators(Collection<Integer> queryIDs, Collection<Integer> articleIDs) throws SQLException, IOException {
		synchronized (connection) {
			List<Article> articles = null;
			try {
				ThreadingUtils.acquireLock();
				articles = articleManager.loadArticles(articleIDs);
			}
			finally {
				ThreadingUtils.releaseLock();
			}
			executeValidatorsUsingObjects(loadQueries(queryIDs), articles);
		}
	}
	
	/**
	 * Executes the {@link Validator Validators} registered with the {@link ValidatorController} on the given
	 * {@link Collection} of {@link Query} IDs and {@link Collection} of {@link Article} IDs after loading them from the
	 * database and writes the results to the database.
	 * 
	 * @param queries
	 *            the IDs of the {@link Query Queries} to be validated
	 * @param articles
	 *            the IDs of the {@link Article Articles} against which the {@link Query Queries} are to be validated
	 * @throws SQLException
	 *             if an error occurs while reading from or writing to the database
	 */
	public void executeValidatorsUsingObjects(Collection<Query> queries, Collection<Article> articles) throws SQLException {
		Collection<Integer> queryIDs = null, articleIDs = null;
		synchronized (connection) {
			List<Triple<Integer, Integer, Future<ValidationResult[]>>> results = new ArrayList<>();
			for (ValidatorWrapper vw : validators.get(ValidatorType.ManyToMany).values()) {
				try {
					results.add(new Triple<>(null, vw.getID(), pool.submit(vw.construct(queries.toArray(new Query[0]), articles.toArray(new Article[0])))));
				}
				catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					logger.error("Unable to initialize the validator, " + vw.getName() + ", for queries " +
							(queryIDs == null ? (queryIDs = queries.stream().collect(ArrayList::new, (a, b) -> a.add(b.getID()), ArrayList::addAll)) : queryIDs).toString() + " and articles " +
							(articleIDs == null ? (articleIDs = articles.stream().collect(ArrayList::new, (a, b) -> a.add(b.getID()), ArrayList::addAll)) : articleIDs).toString(), e);
				}
			}
			for (ValidatorWrapper vw : validators.get(ValidatorType.OneToMany).values()) {
				for (Query query : queries) {
					try {
						results.add(new Triple<>(query.getID(), vw.getID(), pool.submit(vw.construct(query, articles.toArray(new Article[0])))));
					}
					catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						logger.error("Unable to initialize the validator, " + vw.getName() + ", for query " + query.getID() + " and articles " +
								(articleIDs == null ? (articleIDs = articles.stream().collect(ArrayList::new, (a, b) -> a.add(b.getID()), ArrayList::addAll)) : articleIDs).toString(), e);
					}
				}
			}
			for (ValidatorWrapper vw : validators.get(ValidatorType.ManyToOne).values()) {
				for (Article article : articles) {
					try {
						results.add(new Triple<>(null, vw.getID(), pool.submit(vw.construct(queries.toArray(new Query[0]), article))));
					}
					catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						logger.error("Unable to initialize the validator, " + vw.getName() + ", for queries " +
								(articleIDs == null ? (articleIDs = articles.stream().collect(ArrayList::new, (a, b) -> a.add(b.getID()), ArrayList::addAll)) : articleIDs).toString() +
								" and article " + article.getID(), e);
					}
				}
			}
			//Unfortunately, we can only perform existence checks for one-to-one validation algorithms
			try (PreparedStatement stmt = connection.prepareStatement("select * from validation_results as vr where vr.query = ? and vr.algorithm = ? and vr.article = ?")) {
				for (Query query : queries) {
					stmt.setInt(1, query.getID());
					for (Article article : articles) {
						stmt.setInt(3, article.getID());
						for (ValidatorWrapper vw : validators.get(ValidatorType.OneToOne).values()) {
							stmt.setInt(2, vw.getID());
							try (ResultSet rs = stmt.executeQuery()) {
								if (rs.next()) //If we've already processed the current article with the current validator for the current query 
									continue;
							}
							try {
								results.add(new Triple<>(query.getID(), vw.getID(), pool.submit(vw.construct(query, article))));
							}
							catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								logger.error("Unable to initialize the validator, " + vw.getName() + ", for query " + query.getID() + " and article " + article.getID(), e);
							}
						}
					}
				}
			}
			String statement = "insert into validation_results as vr (query, algorithm, article, validates, invalidates) values (?, ?, ?, ?, ?) " +
					"ON CONFLICT (query, algorithm, article) DO UPDATE set (validates, invalidates) = (EXCLUDED.validates, EXCLUDED.invalidates)"; //This is valid as of PostgreSQL 9.5
			try (PreparedStatement stmt = connection.prepareStatement(statement)) {
				for (Triple<Integer, Integer, Future<ValidationResult[]>> result : results) {
					try {
						ValidationResult[] ress = result.getZ().get();
						for (ValidationResult res : ress) {
							String stringVer = "(" + result.getX() + ", " + result.getY() + ", " + res.getArticleID() + ") -> (" + res.getValidates() + ", " +
									(res.getInvalidates() == null ? "null" : res.getInvalidates()) + ")";
							if (res.getValidates().isNaN() || (res.getInvalidates() != null && res.getInvalidates().isNaN())) {
								logger.error("Cannot add " + stringVer + " to the database because it has NaN values.");
								continue;
							}
							stmt.setInt(1, res.getQueryID() == null ? result.getX() : res.getQueryID());
							stmt.setInt(2, result.getY());
							stmt.setInt(3, res.getArticleID());
							stmt.setFloat(4, res.getValidates().floatValue());
							if (res.getInvalidates() != null)
								stmt.setFloat(5, res.getInvalidates().floatValue());
							else
								stmt.setNull(5, Types.REAL);
							stmt.executeUpdate();
							logger.info("Added " + stringVer + " to the database.");
						}
					}
					catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

class ValidatorWrapper {
	private static final Logger logger = LoggerFactory.getLogger("ValidatorWrapper");
	
	private final int algorithmID;
	private final String name;
	private final Class<? extends Validator> clazz;
	private final ValidatorType type;
	private final Constructor<? extends Validator> constructor;
	private final JSONObject instanceParameters;
	private final boolean useInstanceParameters;
	
	@SuppressWarnings("unchecked")
	public ValidatorWrapper(Connection connection, String table, ClassLoader classloader, JSONObject validator) throws SQLException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		clazz = (Class<? extends Validator>) classloader.loadClass(validator.get("class").value().toString());
		name = validator.get("id").value().toString();
		type = ValidatorType.valueOf(validator.get("type").value().toString());
		
		JSONObject parameters = (JSONObject) validator.get("parameters");
		instanceParameters = parameters != null ? (JSONObject) parameters.get("instance") : null;
		
		Class<?>[][] constructorTypes = type.getConstructorArgTypes();
		Constructor<? extends Validator> temp = null;
		if (instanceParameters != null)
			try {
				temp = clazz.getConstructor(constructorTypes[0]);
			}
			catch (NoSuchMethodException e) {
				if (instanceParameters != null)
					logger.warn("Validator " + name + " has declared instance parameters but no constructor for them.");
				temp = clazz.getConstructor(constructorTypes[1]);
			}
		else
			temp = clazz.getConstructor(constructorTypes[1]);
		constructor = temp;
		constructor.setAccessible(true);
		useInstanceParameters = constructor.getParameterCount() > 2;
		
		try (PreparedStatement stmt = connection.prepareStatement("select id from " + table + " as va where va.algorithm = ?")) {
			stmt.setString(1, name);
			try (ResultSet rs = stmt.executeQuery()) {
				rs.next();
				algorithmID = rs.getInt("id");
			}
		}
		if (parameters.containsKey("static"))
			loadStaticProperties(((JSONObject) parameters.get("static")));
	}
	
	private void loadStaticProperties(JSONObject properties) {
		try {
			Method staticInit = clazz.getMethod("loadStaticParameters", JSONObject.class);
			staticInit.setAccessible(true);
			try {
				staticInit.invoke(null, properties);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				logger.error("Failed to invoke the found static property initialization method for " + name, e);
			}
		}
		catch (NoSuchMethodException | SecurityException e) {
			logger.warn("Validator " + name + " has declared static parameters but no static parameter initialization method for them.");
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
		Object[] params;
		if (useInstanceParameters)
			params = new Object[]{instanceParameters, args[0], args[1]};
		else
			params = args;
		return constructor.newInstance(params);
	}
}
