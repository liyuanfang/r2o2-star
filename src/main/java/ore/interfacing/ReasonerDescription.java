package ore.interfacing;

import ore.utilities.FilePathString;

public class ReasonerDescription {
	
	protected FilePathString mStarterScript = null;
	protected String mReasonerName = null;
	protected FilePathString mWorkingDirectory = null;
	protected ReasonerInterfaceType mReasonerInterface = null;
	protected OntologyFormatType mOntologyFormat = OntologyFormatType.ONTOLOGY_FORMAT_ALL;
	protected ExpressivitySupport mExpressivitySupport = null;
	protected String mOutputPathString = null;
	protected FilePathString mSourceFilePathString = null;
	
	public FilePathString getSourceFilePathString() {
		return mSourceFilePathString;
	}
	
	public String getReasonerName() {
		return mReasonerName;
	}
	
	public String getOutputPathString() {
		return mOutputPathString;
	}
	
	public FilePathString getStarterScript() {
		return mStarterScript;
	}	
	
	public ReasonerInterfaceType getReasonerInterface() {
		return mReasonerInterface;
	}	
	
	public OntologyFormatType getOntologyFormatType() {
		return mOntologyFormat;
	}
	
	public ExpressivitySupport getExpressivitySupport() {
		return mExpressivitySupport;
	}
	
	public ReasonerDescription(FilePathString sourceFilePathString, String reasonerName, ReasonerInterfaceType reasonerInterfaceType, FilePathString starterScript, FilePathString workingDirectory, OntologyFormatType ontologyFormatType, ExpressivitySupport expressivitySupport, String outputPathString) {
		mSourceFilePathString = sourceFilePathString;
		mStarterScript = starterScript;		
		mReasonerName = reasonerName;
		mReasonerInterface = reasonerInterfaceType;
		mWorkingDirectory = workingDirectory;
		mOntologyFormat = ontologyFormatType;
		mExpressivitySupport = expressivitySupport;
		mOutputPathString = outputPathString;
	}
	
	public String toString() {
		return mReasonerName;		
	}

	public FilePathString getWorkingDirectory() {
		return mWorkingDirectory;
	}

}
