package eventdetection.common;

import java.nio.file.Path;

public abstract class Source implements IDAble {
	
	public abstract double getReliability();
	
	@Override
	public abstract ID getID();
	
	public static Source loadFromJSON(Path file) {
		return null; //TODO Placeholder.  We should probably implement this
	}
}
