package org.semanticweb.ore.conversion;

import org.semanticweb.ore.interfacing.OntologyFormatType;
import org.semanticweb.ore.utilities.FilePathString;
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
