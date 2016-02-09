package eventdetection.common;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;

import toberumono.json.JSONArray;
import toberumono.json.JSONData;
import toberumono.json.JSONNumber;
import toberumono.json.JSONObject;
import toberumono.json.JSONString;
import toberumono.json.JSONSystem;

public class Cluster {
	private final Collection<Article> articles, unmodifiableArticles;
	private final Collection<String> keywords, unmodifiableKeywords;
	private final int hashCode;
	
	public Cluster(JSONArray articles, JSONArray keywords, Map<Integer, Article> loadedArticles) {
		this(articles, keywords, loadedArticles::get);
	}
	
	public Cluster(JSONArray articles, JSONArray keywords, Function<Integer, Article> articleLoader) {
		this.articles = new LinkedHashSet<>();
		this.unmodifiableArticles = Collections.unmodifiableCollection(this.articles);
		this.keywords = new LinkedHashSet<>();
		this.unmodifiableKeywords = Collections.unmodifiableCollection(this.keywords);
		loader(articles, keywords, articleLoader);
		int hash = 17;
		hash += 31 * hash + this.articles.hashCode();
		hash += 31 * hash + this.keywords.hashCode();
		hashCode = hash;
	}
	
	@SuppressWarnings("unchecked")
	private void loader(JSONArray articles, JSONArray keywords, Function<Integer, Article> loader) {
		for (JSONData<?> data : articles)
			this.articles.add(loader.apply(((JSONNumber<Number>) data).value().intValue()));
		for (JSONData<?> data : keywords)
			this.keywords.add(((JSONString) data).value());
	}
	
	/**
	 * @return an <i>unmodifiable</i> view of the {@link Article Articles} in the {@link Cluster}
	 */
	public Collection<Article> getArticles() {
		return unmodifiableArticles;
	}
	
	/**
	 * @return an <i>unmodifiable</i> view of the keywords in the {@link Cluster}
	 */
	public Collection<String> getKeywords() {
		return unmodifiableKeywords;
	}
	
	public static Collection<Cluster> loadClusters(String articles, Function<Integer, Article> articleLoader) throws IOException, InterruptedException {
		Process p = SubprocessHelpers.executePythonProcess(Paths.get("./clusterer.py"), articles.split("(,\\s*|\\s+)"));
		p.waitFor();
		JSONArray clusters = (JSONArray) JSONSystem.readJSON(p.getInputStream());
		Collection<Cluster> out = new LinkedHashSet<>();
		for (JSONData<?> data : clusters)
			out.add(new Cluster((JSONArray) ((JSONObject) data).get("articles"), (JSONArray) ((JSONObject) data).get("keywords"), articleLoader));
		return out;
	}
	
	public static Collection<Cluster> loadClusters(Function<Integer, Article> articleLoader, String... articles) throws IOException, InterruptedException {
		Process p = SubprocessHelpers.executePythonProcess(Paths.get("./clusterer.py"), articles);
		p.waitFor();
		JSONArray clusters = (JSONArray) JSONSystem.readJSON(p.getInputStream());
		Collection<Cluster> out = new LinkedHashSet<>();
		for (JSONData<?> data : clusters)
			out.add(new Cluster((JSONArray) ((JSONObject) data).get("articles"), (JSONArray) ((JSONObject) data).get("keywords"), articleLoader));
		return out;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Cluster))
			return false;
		Cluster o = (Cluster) obj;
		return articles.equals(o.articles) && keywords.equals(o.keywords);
	}
	
	@Override
	public int hashCode() {
		return hashCode;
	}
}
