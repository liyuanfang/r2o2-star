package org.semanticweb.ore.interfacing;

import org.semanticweb.ore.utilities.FilePathString;

public interface ReasonerDescriptionFactory {
	
	public ReasonerDescription createReasonerDescription(FilePathString sourceFilePathString, String reasonerName, ReasonerInterfaceType reasonerInterfaceType, FilePathString startingScript, FilePathString workingDirectory, OntologyFormatType ontologyFormatType, ExpressivitySupport expressivitySupport, String outputPathString);
	
}
