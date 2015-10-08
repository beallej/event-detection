package eventdetection.downloader;

import java.util.List;
import java.util.function.Supplier;

/**
 * This is purely for readability purposes.
 * 
 * @author Joshua Lipstone
 */
@FunctionalInterface
public interface Downloader extends Supplier<List<RawArticle>> {

}
