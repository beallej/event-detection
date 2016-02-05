package eventdetection.common;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterprocessSynchronizationHandler {
	private static final Logger logger = LoggerFactory.getLogger("InterprocessSynchronizationHandler");
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
	
	public static void acquireLock() throws IOException {
		lock.lock();
		if (chan != null)
			return;
		chan = FileChannel.open(active, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		fsLock = chan.lock();
	}
	
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
	
	public static <T> T executeTask(Supplier<T> task) throws IOException {
		try {
			InterprocessSynchronizationHandler.acquireLock();
			return task.get();
		}
		finally {
			InterprocessSynchronizationHandler.releaseLock();
		}
	}
	
	public static void executeTask(Runnable task) throws IOException {
		try {
			InterprocessSynchronizationHandler.acquireLock();
			task.run();
		}
		finally {
			InterprocessSynchronizationHandler.releaseLock();
		}
	}
}
