package eventdetection.pipeline;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import toberumono.json.JSONObject;
import toberumono.json.JSONSystem;
import toberumono.structures.tuples.Pair;

import eventdetection.common.Article;
import eventdetection.common.ArticleManager;
import eventdetection.common.DBConnection;
import eventdetection.common.Query;
import eventdetection.common.SubprocessHelpers;
import eventdetection.common.ThreadingUtils;
import eventdetection.downloader.DownloaderController;
import eventdetection.validator.ValidatorController;

public class Pipeline implements PipelineComponent, Closeable {
	private final ArticleManager articleManager;
	private final List<PipelineComponent> components;
	private boolean closed;
	
	public Pipeline(JSONObject config, Collection<Integer> queryIDs, Collection<Integer> articleIDs, boolean addDefaultComponents) throws IOException, SQLException {
		Connection connection = DBConnection.getConnection();
		JSONObject paths = (JSONObject) config.get("paths");
		JSONObject articles = (JSONObject) config.get("articles");
		JSONObject tables = (JSONObject) config.get("tables");
		articleManager = new ArticleManager(connection, tables.get("articles").value().toString(), paths, articles);
		
		components = new ArrayList<>();
		if (addDefaultComponents) {
			addComponent(inputs -> Pipeline.loadQueries(queryIDs, inputs));
			addComponent(inputs -> Pipeline.loadArticles(articleManager, articleIDs, inputs));
			if (articleIDs.size() == 0) { //Only run the downloader if no articles are specified.
				addComponent(new DownloaderController(config));
				addComponent(inputs -> {
					SubprocessHelpers.executePythonProcess(Paths.get("./ArticleProcessorDaemon.py"), "--no-lock");
					return inputs;
				});
				addComponent(inputs -> {
					SubprocessHelpers.executePythonProcess(Paths.get("./QueryProcessorDaemon.py"), "--no-lock");
					return inputs;
				});
			}
			addComponent(new ValidatorController(config));
		}
	}
	
	public static void main(String[] args) throws IOException, SQLException {
		Path configPath = Paths.get("./configuration.json"); //The configuration file defaults to "./configuration.json", but can be changed with arguments
		int action = 0;
		final Collection<Integer> articleIDs = new LinkedHashSet<>(), queryIDs = new LinkedHashSet<>();
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
		
		try (Pipeline pipeline = new Pipeline(config, queryIDs, articleIDs, true)) {
			pipeline.execute();
		}
	}
	
	private static Pair<Map<Integer, Query>, Map<Integer, Article>> loadArticles(ArticleManager am, Collection<Integer> ids, Pair<Map<Integer, Query>, Map<Integer, Article>> inputs)
			throws IOException, SQLException {
		Map<Integer, Article> articles = inputs.getY();
		for (Article a : ThreadingUtils.executeTask(() -> am.loadArticles(ids)))
			articles.put(a.getID(), a);
		return inputs;
	}
	
	private static Pair<Map<Integer, Query>, Map<Integer, Article>> loadQueries(Collection<Integer> ids, Pair<Map<Integer, Query>, Map<Integer, Article>> inputs)
			throws SQLException {
		final Logger logger = LoggerFactory.getLogger("QueryLoader");
		Map<Integer, Query> queries = inputs.getX();
		try (ResultSet rs = DBConnection.getConnection().prepareStatement("select * from queries").executeQuery()) {
			if (ids.size() > 0) {
				int id = 0;
				while (ids.size() > 0 && rs.next()) {
					id = rs.getInt("id");
					if (ids.contains(id)) {
						ids.remove(id); //Prevents queries from being loaded more than once
						queries.put(id, new Query(rs));
					}
				}
			}
			else {
				while (rs.next())
					queries.put(rs.getInt("id"), new Query(rs));
			}
		}
		if (ids.size() > 0)
			logger.warn("Did not find queries with ids matching " + ids.stream().reduce("", (a, b) -> a + ", " + b.toString(), (a, b) -> a + b).substring(2));
		return inputs;
	}
	
	/**
	 * Adds a {@link PipelineComponent} to the {@link Pipeline}
	 * 
	 * @param component
	 *            the {@link PipelineComponent} to add
	 * @return the {@link Pipeline} for chaining purposes
	 */
	public Pipeline addComponent(PipelineComponent component) {
		components.add(component);
		return this;
	}
	
	@Override
	public Pair<Map<Integer, Query>, Map<Integer, Article>> execute(Pair<Map<Integer, Query>, Map<Integer, Article>> inputs) throws IOException, SQLException {
		for (PipelineComponent pc : components)
			inputs = pc.execute(inputs);
		return null;
	}
	
	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		IOException except = null;
		for (PipelineComponent comp : components) {
			try {
				if (comp instanceof Closeable)
					((Closeable) comp).close();
			}
			catch (IOException e) {
				except = e;
			}
		}
		if (except != null)
			throw except;
	}
}
