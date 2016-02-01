package eventdetection.downloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;

import toberumono.json.JSONArray;
import toberumono.json.JSONObject;
import toberumono.json.JSONString;
import toberumono.json.JSONSystem;

/**
 * An extension of {@link Scraper} that is designed for invoking Python 3 scripts.
 * 
 * @author Joshua Lipstone
 */
public class PythonScraper extends Scraper {
	/**
	 * The path required to run the system's bash executable.
	 */
	public static final String bashPath = getBashPath();
	/**
	 * The path required to run the system's Python 3 executable.
	 */
	public static final String pythonPath = getPythonPath();
	
	protected final Path json;
	protected final JSONObject scripts, parameters;
	
	/**
	 * Creates a {@link PythonScraper} using the given configuration data.
	 * 
	 * @param json
	 *            the {@link Path} to the JSON file that describes the {@link PythonScraper}
	 * @param config
	 *            a {@link JSONObject} containing the configuration data for the {@link PythonScraper}
	 */
	public PythonScraper(Path json, JSONObject config) {
		super(json, config);
		this.json = json;
		JSONSystem.transferField("python", new JSONObject(), config);
		JSONObject python = (JSONObject) config.get("python");
		JSONSystem.transferField("scripts", new JSONObject(), python);
		JSONSystem.transferField("parameters", new JSONObject(), python);
		this.scripts = (JSONObject) python.get("scripts");
		this.parameters = (JSONObject) python.get("parameters");
	}

	@Override
	public String scrape(URL url) throws IOException {
		String link = url.toString();
		JSONObject variableParameters = new JSONObject();
		variableParameters.put("url", new JSONString(link));
		variableParameters.put("textId", new JSONString(parameters.get("textId").toString()));
		String sectioned = callScript("sectioning", variableParameters);
		return sectioned.trim();
	}
	
	private static final String getBashPath() {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command("which", "bash");
		try {
			Process p = pb.start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				p.waitFor();
				return reader.readLine().trim();
			}
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return "/bin/bash";
	}
	
	private static final String getPythonPath() {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(bashPath, "-l", "-c", "[ \"$(python --version 2>&1 | grep 'Python 3')\" != \"\" ] && echo \"$(which python)\" || echo \"$(which python3)\"");
		try {
			Process p = pb.start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				p.waitFor();
				return reader.readLine().trim();
			}
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return "python3";
	}
	
	/**
	 * This method calls a Python 3 script based on data in the "python" object {@link PythonScraper Scraper's} JSON file.
	 * 
	 * @param scriptName
	 *            the name of the script to call as it appears in the "python.scripts" section of the {@link PythonScraper
	 *            Scraper's} JSON file
	 * @param variableParameters
	 *            any parameters that should be passed to the script that aren't enumerated in the {@link PythonScraper
	 *            Scraper's} JSON file
	 * @return a {@link String} containing the contents of the scripts {@code stdout} stream
	 * @throws IOException
	 *             if an error occurs while invoking the script
	 */
	public String callScript(String scriptName, JSONObject variableParameters) throws IOException {
		String[] comm = ((JSONArray) scripts.get(scriptName)).stream().collect(ArrayList::new, (a, b) -> a.add((String) b.value()), ArrayList::addAll).toArray(new String[0]);
		Path scriptPath = json.getParent().resolve(comm[0]);
		String[] command = {bashPath, "-l", "-c", ""};
		StringBuilder cmd = new StringBuilder(pythonPath.length() * 2);
		cmd.append(pythonPath).append(" ").append(scriptPath.normalize().toString());
		for (String c : comm)
			cmd.append(" ").append(c);
		command[3] = cmd.toString();
		JSONObject parameters = new JSONObject();
		JSONObject scriptParameters = (JSONObject) parameters.get(scriptName);
		JSONObject globalParameters = (JSONObject) parameters.get("global");
		if (globalParameters != null)
			parameters.putAll(globalParameters);
		if (scriptParameters != null)
			parameters.putAll(scriptParameters);
		if (variableParameters != null)
			parameters.putAll(variableParameters);
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(scriptPath.getParent().toFile());
		Process p = pb.start();
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()))) {
			JSONSystem.writeJSON(parameters, bw);
		}
		try {
			p.waitFor();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				StringBuilder sb = new StringBuilder();
				br.lines().forEach(l -> sb.append(l).append("\n"));
				return sb.toString().trim();
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			return "";
		}
	}
}
