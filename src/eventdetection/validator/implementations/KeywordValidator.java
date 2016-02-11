package eventdetection.validator.implementations;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import toberumono.json.JSONObject;

import eventdetection.common.Article;
import eventdetection.common.Query;
import eventdetection.common.SubprocessHelpers;
import eventdetection.validator.ValidationResult;
import eventdetection.validator.types.OneToOneValidator;

/**
 * A simple wrapper that forwards to the Python KeywordValidator that was implemented by Josie, Laura and Julia.
 * 
 * @author Joshua Lipstone
 */
public class KeywordValidator extends OneToOneValidator {
	private static Path SCRIPT_PATH = null;
	
	/**
	 * Constructs a new {@link KeywordValidator} for the given {@link Query} and {@link Article}
	 * 
	 * @param query
	 *            the {@link Query} to validate
	 * @param article
	 *            the {@link Article} with which to validate the {@link Query}
	 */
	public KeywordValidator(Query query, Article article) {
		super(query, article);
	}
	
	@Override
	public ValidationResult[] call() throws Exception {
		Process p = SubprocessHelpers.executePythonProcess(SCRIPT_PATH, query.getID().toString(), article.getID().toString());
		p.waitFor();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			return new ValidationResult[]{new ValidationResult(query, article, Double.parseDouble(br.readLine()))};
		}
	}
	
	/**
	 * Hook for loading parameters from the Validator's JSON data
	 * 
	 * @param parameters
	 *            a {@link JSONObject} holding the validator's static parameters
	 */
	public static void loadStaticParameters(JSONObject parameters) {
		SCRIPT_PATH = Paths.get((String) parameters.get("script-path").value());
	}
}
