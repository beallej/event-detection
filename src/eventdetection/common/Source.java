package eventdetection.common;

import java.nio.file.Path;

public class Source implements IDAble {
	private final ID id;
	private final double reliability;
	
	/**
	 * Creates a {@link Source} with the given ID and reliability coefficient
	 * 
	 * @param id
	 *            the ID of the {@link Source}
	 * @param reliability
	 *            the reliability coefficient of the {@link Source}
	 */
	public Source(ID id, double reliability) {
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
	public ID getID() {
		return id;
	}
	
	public static Source loadFromJSON(Path file) {
		return null; //TODO Placeholder.  We should probably implement this
	}
}
