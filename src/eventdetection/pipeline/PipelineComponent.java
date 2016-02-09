package eventdetection.pipeline;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import toberumono.structures.tuples.Pair;

import eventdetection.common.Article;
import eventdetection.common.Query;

@FunctionalInterface
public interface PipelineComponent {

	public default Pair<Map<Integer, Query>, Map<Integer, Article>> execute() throws IOException, SQLException {
		return execute(new Pair<>(new LinkedHashMap<>(), new LinkedHashMap<>()));
	}
	
	public Pair<Map<Integer, Query>, Map<Integer, Article>> execute(Pair<Map<Integer, Query>, Map<Integer, Article>> inputs) throws IOException, SQLException;
}
