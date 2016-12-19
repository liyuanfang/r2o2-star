package ore.conversion;

import ore.interfacing.OntologyFormatType;
import ore.utilities.FilePathString;

public interface OntologyFormatRedirector {
	
	public FilePathString getOntologySourceStringForFormat(FilePathString ontologySource, OntologyFormatType ontologyFormat);

}
