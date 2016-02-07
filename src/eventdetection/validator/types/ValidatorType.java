package eventdetection.validator.types;

import java.util.List;

import toberumono.json.JSONObject;

import eventdetection.common.Article;
import eventdetection.common.Query;

/**
 * Enumerates the types of {@link Validator} implementations.
 * 
 * @author Joshua Lipstone
 */
public enum ValidatorType {
	/**
	 * A {@link Validator} that takes one {@link Query} and one {@link Article}
	 */
	OneToOne {
		@Override
		public Class<?>[][] getConstructorArgTypes() {
			return new Class<?>[][]{{JSONObject.class, Query.class, Article.class}, {Query.class, Article.class}};
		}
	},
	/**
	 * A {@link Validator} that takes one {@link Query} and multiple {@link Article Articles}
	 */
	OneToMany {
		@Override
		public Class<?>[][] getConstructorArgTypes() {
			return new Class<?>[][]{{JSONObject.class, Query.class, List.class}, {Query.class, List.class}};
		}
	},
	/**
	 * A {@link Validator} that takes multiple {@link Query Queries} and one {@link Article}
	 */
	ManyToOne {
		@Override
		public Class<?>[][] getConstructorArgTypes() {
			return new Class<?>[][]{{JSONObject.class, List.class, Article.class}, {List.class, Article.class}};
		}
	},
	/**
	 * A {@link Validator} that takes multiple {@link Query Queries} and multiple {@link Article Articles}
	 */
	ManyToMany {
		@Override
		public Class<?>[][] getConstructorArgTypes() {
			return new Class<?>[][]{{JSONObject.class, List.class, List.class}, {List.class, List.class}};
		}
	};
	
	/**
	 * @return the {@link Class} objects representing the types for the {@link Validator Validator's} constructor
	 */
	public abstract Class<?>[][] getConstructorArgTypes();
}
