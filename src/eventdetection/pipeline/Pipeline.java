package eventdetection.pipeline;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import eventdetection.voting.VotingController;

/**
 * Implements an easily-expanded pipeline system for the project.
 * 
 * @author Joshua Lipstone
 */
public class Pipeline implements PipelineComponent, Closeable {
	private static final Logger logger = LoggerFactory.getLogger("Pipeline");
	
	private final ArticleManager articleManager;
	private final List<PipelineComponent> components;
	private boolean closed;
	
	/**
	 * Creates a new {@link Pipeline} instance.
	 * 
	 * @param config
	 *            a {@link JSONObject} holding the configuration data for the {@link Pipeline} and its components
	 * @param queryIDs
	 *            the IDs of the {@link Query Queries} to use. This must be empty or {@code null} for the downloader to be
	 *            run
	 * @param articleIDs
	 *            the IDs of the {@link Article Articles} to use. This must be empty or {@code null} for the downloader to be
	 *            run
	 * @param addDefaultComponents
	 *            whether the default pipeline components should be added (Downloader, preprocessors, and Validator)
	 * @throws IOException
	 *             if an error occurs while initializing the Downloader
	 * @throws SQLException
	 *             if an error occurs while connecting to the database
	 */
	public Pipeline(JSONObject config, Collection<Integer> queryIDs, Collection<Integer> articleIDs, boolean addDefaultComponents) throws IOException, SQLException {
		final Collection<Integer> qIDs = queryIDs == null ? Collections.emptyList() : queryIDs, aIDs = articleIDs == null ? Collections.emptyList() : articleIDs;
		articleManager = new ArticleManager(config);
		
		components = new ArrayList<>();
		if (addDefaultComponents) {
			addComponent(inputs -> {
				ThreadingUtils.loadQueries(qIDs, inputs.getX());
				return inputs;
			});
			addComponent(inputs -> {
				ThreadingUtils.cleanUpArticles(articleManager);
				return inputs;
			});
			addComponent(inputs -> {
				ThreadingUtils.loadArticles(articleManager, aIDs, inputs.getY());
				return inputs;
			});
			if (articleIDs.size() == 0) { //Only run the Downloader if no articles are specified.
				addComponent(new DownloaderController(config));
				addComponent(inputs -> {
					Process p = SubprocessHelpers.executePythonProcess(Paths.get("./ArticleProcessorDaemon.py"), "--no-lock");
					try {
						p.waitFor();
					}
					catch (InterruptedException e) {}
					return inputs;
				});
				addComponent(inputs -> {
					Process p = SubprocessHelpers.executePythonProcess(Paths.get("./QueryProcessorDaemon.py"), "--no-lock");
					try {
						p.waitFor();
					}
					catch (InterruptedException e) {}
					return inputs;
				});
			}
			addComponent(new ValidatorController(config));
			addComponent(new VotingController(config));
		}
	}
	
	/**
	 * Main method for the {@link Pipeline} entry point.
	 * 
	 * @param args
	 *            the command-line arguments
	 * @throws IOException
	 *             if an error occurs while initializing the Downloader
	 * @throws SQLException
	 *             if an error occurs while connecting to the database
	 */
	public static void main(String[] args) throws IOException, SQLException {
		Path configPath = Paths.get("./configuration.json"); //The configuration file defaults to "./configuration.json", but can be changed with arguments
		int action = 0;
		boolean actionSet = false;
		final Collection<Integer> articleIDs = new LinkedHashSet<>(), queryIDs = new LinkedHashSet<>();
		for (String arg : args) {
			try {
				if (arg.equalsIgnoreCase("-c")) {
					action = 0;
					actionSet = true;
				}
				else if (arg.equalsIgnoreCase("-a")) {
					action = 1;
					actionSet = true;
				}
				else if (arg.equalsIgnoreCase("-q")) {
					action = 2;
					actionSet = true;
				}
				else if (action == 0)
					configPath = Paths.get(arg);
				else if (action == 1)
					articleIDs.add(Integer.parseInt(arg));
				else if (action == 2)
					queryIDs.add(Integer.parseInt(arg));
			}
			catch (NumberFormatException e) {
				logger.warn(arg + " is not an integer");
			}
			if (!actionSet)
				action++;
			if (action > 2)
				break;
		}
		JSONObject config = (JSONObject) JSONSystem.loadJSON(configPath);
		DBConnection.configureConnection((JSONObject) config.get("database"));
		
		try (Pipeline pipeline = new Pipeline(config, queryIDs, articleIDs, true)) {
			pipeline.execute();
		}
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
		return inputs;
	}
	
	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		articleManager.close();
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
