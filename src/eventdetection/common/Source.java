package eventdetection.common;

import java.io.IOException;
import java.nio.file.Path;

import eventdetection.downloader.Scraper;

/**
 * Represents a source of news information.
 * 
 * @author Joshua Lipstone
 */
public class Source implements IDAble {
	private final String id;
	private final double reliability;
	
	/**
	 * Creates a {@link Source} with the given ID and reliability coefficient
	 * 
	 * @param id
	 *            the ID of the {@link Source}
	 * @param reliability
	 *            the reliability coefficient of the {@link Source}
	 */
	public Source(String id, double reliability) {
		this.id = id;
		this.reliability = reliability;
	}
	
	/**
	 * @return a coefficient used to denote how much weight we give to this {@link Source}
	 */
	public double getReliability() {
		return reliability;
	}
	
	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public int hashCode() {
		return getID().hashCode();
	}
	
	/**
	 * Loads a {@link Source} from a JSON file
	 * 
	 * @param json
	 *            a {@link Path} to the JSON file
	 * @return the {@link Scraper} described in the JSON file
	 * @throws IOException
	 *             an I/O error occurs
	 */
	public static Source loadFromJSON(Path json) throws IOException {
		return null; //TODO Placeholder.  We should probably implement this
	}
}
