package ore.interfacing;

import ore.utilities.FilePathString;

public class DefaultReasonerDescriptionFactory implements ReasonerDescriptionFactory {
	
	public ReasonerDescription createReasonerDescription(FilePathString sourceFilePathString, String reasonerName, ReasonerInterfaceType reasonerInterfaceType, FilePathString startingScript, FilePathString workingDirectory, OntologyFormatType ontologyFormatType, ExpressivitySupport expressivitySupport, String outputPathString) {
		return new ReasonerDescription(sourceFilePathString,reasonerName,reasonerInterfaceType,startingScript,workingDirectory,ontologyFormatType,expressivitySupport,outputPathString);
	}

}
