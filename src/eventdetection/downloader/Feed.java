package eventdetection.downloader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import eventdetection.common.ID;
import eventdetection.common.IDAble;
import eventdetection.common.Source;

public interface Feed extends Downloader, IDAble {
	@Override
	public List<RawArticle> get();
	
	@Override
	public ID getID();
	
	public String getName();
	
	public String getLastSeen();
	
	public Source getSource();
	
	public static Feed loadFromJSON(Path file, Map<ID, Scraper> scrapers) throws IOException {
		return null; //TODO We can't implement this without a JSON library
	}
}
