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

public class TagSimplifier {
	private final Map<String, String> mapping;
	private static final Pattern tagPattern = Pattern.compile("(\\S+?_)(\\S+?)");
	
	public TagSimplifier(Map<String, String> simplificationMap) {
		this.mapping = simplificationMap;
	}
	
	public TagSimplifier(List<String> simplifications) {
		this(simplifications.stream());
	}
	
	public TagSimplifier(Path simplificationsFile) throws IOException {
		this(Files.readAllLines(simplificationsFile));
	}
	
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
