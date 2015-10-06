package eventdetection.common;

import java.nio.file.Path;

public interface Source extends IDAble {
	
	public double getReliability();
	
	@Override
	public ID getID();
	
	/**
	 * @return the name
	 */
	public String getName();
	
	public static Source loadFromJSON(Path file) {
		return null; //TODO Placeholder.  We should probably implement this
	}
}
