package eventdetection.validator;

import eventdetection.common.Article;
import eventdetection.common.Query;

@FunctionalInterface
public interface ValidatorConstructor {
	
	public Validator construct(Integer algorithmID, Query query, Article article);
}
