package eventdetection.common;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Query {
	private final int id;
	private final String subject, verb, directObject, indirectObject, location;
	private final boolean processed;
	
	public Query(ResultSet rs) throws SQLException {
		id = rs.getInt("id");
		subject = rs.getString("subject");
		verb = rs.getString("verb");
		directObject = rs.getString("direct_obj");
		indirectObject = rs.getString("indirect_obj");
		location = rs.getString("loc");
		processed = rs.getBoolean("processed");
	}
	
	public Query(int id, String subject, String verb, String directObject, String indirectObject, String location, Boolean processed) {
		this.id = id;
		this.subject = subject;
		this.verb = verb;
		this.directObject = directObject;
		this.indirectObject = indirectObject;
		this.location = location;
		this.processed = processed;
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}
	
	/**
	 * @return the verb
	 */
	public String getVerb() {
		return verb;
	}
	
	/**
	 * @return the direct object
	 */
	public String getDirectObject() {
		return directObject;
	}
	
	/**
	 * @return the indirect object
	 */
	public String getIndirectObject() {
		return indirectObject;
	}
	
	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}
	
	/**
	 * @return whether the {@link Query} has been processed
	 */
	public boolean isProcessed() {
		return processed;
	}
}
