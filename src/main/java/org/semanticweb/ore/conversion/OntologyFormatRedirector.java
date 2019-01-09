package org.semanticweb.ore.conversion;

import org.semanticweb.ore.interfacing.OntologyFormatType;
import org.semanticweb.ore.utilities.FilePathString;

public interface OntologyFormatRedirector {
	
	public FilePathString getOntologySourceStringForFormat(FilePathString ontologySource, OntologyFormatType ontologyFormat);

}
