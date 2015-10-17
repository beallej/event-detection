package eventdetection.downloader;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import toberumono.utils.functions.IOExceptedFunction;

import eventdetection.common.ID;
import eventdetection.common.IDAble;
import eventdetection.common.Source;

/**
 * This represents an object that can download information from news sources.
 * 
 * @author Joshua Lipstone
 */
public abstract class Downloader implements Supplier<List<RawArticle>> {
	/**
	 * The available news {@link Source Sources}
	 */
	public static final Map<ID, Source> sources = new LinkedHashMap<>();
	
	/**
	 * Adds a {@link Source} or a folder of {@link Source Sources} to {@link #sources}.
	 * 
	 * @param path
	 *            a {@link Path} to a JSON file defining a {@link Source} or a folder of files defining {@link Source
	 *            Sources}
	 * @return the IDs of the added {@link Source Sources}
	 * @throws IOException
	 *             if an error occurs while loading the JSON files
	 */
	public static List<ID> loadSource(Path path) throws IOException {
		return loadItemsFromFile(Source::loadFromJSON, p -> p.endsWith(".json"), path, sources::put);
	}
	
	/**
	 * Removes the {@link Source} with the given ID.
	 * 
	 * @param id
	 *            the ID of the {@link Source} to remove
	 * @return the removed {@link Source} or {@code null}
	 */
	public static Source removeSource(ID id) {
		return sources.remove(id);
	}
	
	/**
	 * A helper method for loading {@link IDAble} objects from files.
	 * 
	 * @param loader
	 *            the method used to load the {@link IDAble} object from a file
	 * @param filter
	 *            the method used to determine if the file has the correct extension
	 * @param path
	 *            a {@link Path} to a file or folder of files that define instances of the {@link IDAble} object
	 * @param store
	 *            the method used to store the constructed {@link IDAble} objects
	 * @param <T>
	 *            the type of the item to load. This will be implicitly set when this method is used correctly
	 * @return a {@link List} of the IDs of the loaded objects
	 * @throws IOException
	 *             if an error occurs while loading from files
	 */
	public static <T extends IDAble> List<ID> loadItemsFromFile(IOExceptedFunction<Path, T> loader, Filter<Path> filter, Path path, BiFunction<ID, T, T> store) throws IOException {
		List<ID> ids = new ArrayList<>();
		if (Files.isRegularFile(path)) {
			if (filter.accept(path)) {
				T t = loader.apply(path);
				store.apply(t.getID(), t);
				ids.add(t.getID());
			}
			return ids;
		}
		for (Path p : Files.newDirectoryStream(path, filter)) {
			try {
				T t = loader.apply(p);
				store.apply(t.getID(), t);
				ids.add(t.getID());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ids;
	}
}
