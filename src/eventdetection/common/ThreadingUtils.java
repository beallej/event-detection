package eventdetection.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple class that holds threading related constants and methods for the rest of the classes in this project.
 * 
 * @author Joshua Lipstone
 */
public class ThreadingUtils {
	/**
	 * A work stealing pool for use by all classes in this program.
	 */
	public static final ExecutorService pool = Executors.newWorkStealingPool();
	
	private ThreadingUtils() {/* This class should not be initialized */}
}
