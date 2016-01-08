package eventdetection.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A basic system for simplifying tags in PoS tagged text.
 * 
 * @author Toberumono
 */
public class TagSimplifier {
	private final Map<String, String> mapping;
	private static final Pattern tagPattern = Pattern.compile("(\\S+?_)(\\S+?)");
	
	/**
	 * Constructs a new {@link TagSimplifier} with the given simplification mapping.
	 * 
	 * @param simplificationMap
	 *            a tag to tag map where the key is the tag to be replaced and the value is the tag with which it should be
	 *            replaced
	 */
	public TagSimplifier(Map<String, String> simplificationMap) {
		this.mapping = simplificationMap;
	}
	
	/**
	 * Constructs a new {@link TagSimplifier} with the given {@link List} of simplifications.
	 * 
	 * @param simplifications
	 *            a {@link List} of {@link String Strings} of the form: comma-separated list of tags -&gt; replacement tag
	 */
	public TagSimplifier(List<String> simplifications) {
		this(simplifications.stream());
	}
	
	/**
	 * Constructs a new {@link TagSimplifier} with the given {@link Path} to a file containing a list of simplifications.
	 * 
	 * @param simplificationsFile
	 *            a {@link Path} to a file with lines of the form: comma-separated list of tags -&gt; replacement tag
	 * @throws IOException
	 *             if an error occurs while reading the file
	 */
	public TagSimplifier(Path simplificationsFile) throws IOException {
		this(Files.readAllLines(simplificationsFile));
	}
	
	/**
	 * Constructs a new {@link TagSimplifier} with the given {@link Stream} of simplifications.
	 * 
	 * @param simplifications
	 *            a {@link Stream} of {@link String Strings} of the form: comma-separated list of tags -&gt; replacement tag
	 */
	public TagSimplifier(Stream<String> simplifications) {
		mapping = new LinkedHashMap<>();
		simplifications.forEach(map -> {
			map = map.trim();
			if (map.indexOf('#') > -1)
				map = map.substring(0, map.indexOf('#'));
			if (map.length() == 0)
				return;
			String[] mapping = map.split("(=|->|\u2192)");
			if (mapping.length != 2)
				return;
			String[] simplifiableTags = mapping[0].split(",");
			mapping[1] = mapping[1].trim();
			for (String tag : simplifiableTags)
				this.mapping.put(tag.trim(), mapping[1]);
			this.mapping.put(mapping[1], mapping[1]);
		});
	}
	
	/**
	 * Simplifies the PoS tags in the given PoS tagged text using the mappings loaded into the {@link TagSimplifier}.
	 * 
	 * @param taggedText
	 *            the PoS tagged text to simplify
	 * @return the tagged text with simplified tags
	 */
	public String simplifyTags(String taggedText) {
		Matcher m = tagPattern.matcher(taggedText);
		StringBuffer sb = new StringBuffer(taggedText.length());
		while (m.find()) {
			String tag = m.group(2);
			if (tag.indexOf('|') < 0) //If the tag does not contain |
				tag = mapping.containsKey(tag) ? mapping.get(tag) : tag;
			else {
				String[] tags = tag.split("\\|");
				tag = "";
				for (String t : tags)
					tag += "|" + (mapping.containsKey(t) ? mapping.get(t) : t);
			}
			m.appendReplacement(sb, "$1" + tag);
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
