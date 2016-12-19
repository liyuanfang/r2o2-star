package ore.interfacing;

import ore.conversion.OntologyFormatRedirector;
import ore.querying.Query;
import ore.querying.QueryType;
import ore.querying.SatisfiabilityQuery;
import ore.utilities.FilePathString;
import ore.utilities.RelativeFilePathStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReasonerAdaptorOREv1 extends ReasonerAdaptorORE {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerAdaptorOREv1.class);
	
	final public static String mResponseFileSuffixString = "query-response.dat";
	final public static String mResultOutputFileSuffixString = "query-result-data.owl";
	final public static String mConsoleOutputFileSuffixString = "reasoner-console-output.txt";
	final public static String mErrorOutputFileSuffixString = "reasoner-error-output.txt";
	
	
	public ReasonerAdaptorOREv1(ReasonerDescription reasoner, Query query, String responseDestinationString, OntologyFormatRedirector formatRedirector) {
		mQuery = query;
		mReasoner = reasoner;
		
		
		mResponseOutputFilePathString = new FilePathString(responseDestinationString,mResponseFileSuffixString, RelativeFilePathStringType.RELATIVE_TO_SOURCE_DIRECTORY);
		mResultOutputFileString = new FilePathString(responseDestinationString,mResultOutputFileSuffixString,RelativeFilePathStringType.RELATIVE_TO_SOURCE_DIRECTORY);
		mConsoleOutputFilePathString = new FilePathString(responseDestinationString,mConsoleOutputFileSuffixString,RelativeFilePathStringType.RELATIVE_TO_SOURCE_DIRECTORY);
		mErrorOutputFileString = new FilePathString(responseDestinationString,mErrorOutputFileSuffixString,RelativeFilePathStringType.RELATIVE_TO_SOURCE_DIRECTORY);
		
		String queryTypeArgument = null;
		if (mQuery.getQueryType() == QueryType.QUERY_TYPE_CLASSIFICATION) {
			queryTypeArgument = "classification";
		} else if (mQuery.getQueryType() == QueryType.QUERY_TYPE_CONSISTENCY) {
			queryTypeArgument = "consistency";
		} else if (mQuery.getQueryType() == QueryType.QUERY_TYPE_SATISFIABILITY) {
			queryTypeArgument = "sat";
		} else {
			mLogger.error("Query '{}' not supported by reasoner '{}'.",mQuery.toString(),mReasoner.toString());
			mQueryInitialisationFailed = true;
		}
		mExecutionArgumentsList.add(queryTypeArgument);		
		
		FilePathString redirectedOntologySource = formatRedirector.getOntologySourceStringForFormat(mQuery.getOntologySourceString(), mReasoner.getOntologyFormatType());
		
		mExecutionArgumentsList.add(redirectedOntologySource.getAbsoluteFilePathString());
		mExecutionArgumentsList.add(mResultOutputFileString.getAbsoluteFilePathString());
		
		if (mQuery.getQueryType() == QueryType.QUERY_TYPE_SATISFIABILITY) {
			SatisfiabilityQuery satQuery = (SatisfiabilityQuery)mQuery;
			mExecutionArgumentsList.add(satQuery.getClassString());
			
		}
	}




}
