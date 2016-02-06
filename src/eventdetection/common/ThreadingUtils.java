package eventdetection.common;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private static final Logger logger = LoggerFactory.getLogger("ThreadingUtils");
	private static final ReentrantLock lock = new ReentrantLock();
	private static final Path active = Paths.get(System.getProperty("user.home"), ".event-detection-active");
	private static FileLock fsLock = null;
	private static FileChannel chan = null;
	
	static {
		if (!Files.exists(active))
			try {
				Files.createDirectories(active.getParent());
				Files.createFile(active);
			}
			catch (IOException e) {
				logger.error("Unable to create the file for the filesystem lock", e);
			}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (fsLock != null) {
					try {
						fsLock.close();
					}
					catch (IOException e) {
						logger.error("Unable to release the filesystem lock.", e);
					}
					try {
						chan.close();
					}
					catch (IOException e) {
						logger.error("Unable to close the filesystem lock's filechannel.", e);
					}
				}
			}
		});
	}
	
	private ThreadingUtils() {/* This class should not be initialized */}
	
	/**
	 * Provides a means of acquiring a combination intraprocess and interprocess lock.
	 * 
	 * @throws IOException
	 *             if an error occurs while acquiring the interprocess lock
	 */
	public static void acquireLock() throws IOException {
		lock.lock();
		try {
			if (chan != null)
				return;
			chan = FileChannel.open(active, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			fsLock = chan.lock();
		}
		catch (IOException e) {
			if (fsLock != null)
				fsLock.close();
			if (chan != null)
				chan.close();
			fsLock = null;
			chan = null;
			lock.unlock();
			logger.error("Failed to acquire the interprocess lock.", e);
			throw e;
		}
	}
	
	/**
	 * Provides a means of releasing a combination intraprocess and interprocess lock.
	 * 
	 * @throws IOException
	 *             if an error occurs while releasing the interprocess lock
	 */
	public static void releaseLock() throws IOException {
		if (!lock.isHeldByCurrentThread()) {
			logger.warn("A thread that does not own the filesystem lock attempted to release it.");
			return;
		}
		try {
			fsLock.close();
			chan.close();
			fsLock = null;
			chan = null;
		}
		finally {
			lock.unlock();
		}
	}
	
	/**
	 * Executes the given task synchronously with regard to the locks provided by this class.
	 * 
	 * @param task
	 *            the task to execute
	 * @return the result of executing the task
	 * @throws IOException
	 *             if an error occurs while interacting with the interprocess lock or an IO error occurs within the function
	 * @throws SQLException
	 *             if an SQL error occurs within the function
	 */
	public static <T> T executeTask(IOSQLExceptedSupplier<T> task) throws IOException, SQLException {
		try {
			acquireLock();
			return task.get();
		}
		finally {
			releaseLock();
		}
	}
	
	/**
	 * Executes the given task synchronously with regard to the locks provided by this class.
	 * 
	 * @param task
	 *            the task to execute
	 * @throws IOException
	 *             if an error occurs while interacting with the interprocess lock or an IO error occurs within the function
	 * @throws SQLException 
	 *             if an SQL error occurs within the function
	 */
	public static void executeTask(IOSQLExceptedRunnable task) throws IOException, SQLException {
		try {
			acquireLock();
			task.run();
		}
		finally {
			releaseLock();
		}
	}
}
