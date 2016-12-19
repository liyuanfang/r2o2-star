package ore.conversion;

import ore.interfacing.OntologyFormatType;
import ore.utilities.FilePathString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyFormatNoRedictionRedirector implements OntologyFormatRedirector {
	
	@SuppressWarnings("unused")
	final private static Logger mLogger = LoggerFactory.getLogger(OntologyFormatNoRedictionRedirector.class);


	@Override
	public FilePathString getOntologySourceStringForFormat(FilePathString ontologySource, OntologyFormatType ontologyFormat) {		
		return ontologySource;
	}

}
