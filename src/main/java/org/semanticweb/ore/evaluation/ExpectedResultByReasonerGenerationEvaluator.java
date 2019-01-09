package org.semanticweb.ore.evaluation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.ore.competition.Competition;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.evaluation.ReasonerQueryResultSimilarityListEvaluator.QueryReasonerData;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.semanticweb.ore.verification.QueryResultVerificationCorrectnessType;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpectedResultByReasonerGenerationEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ExpectedResultByReasonerGenerationEvaluator.class);
	
	private Config mConfig = null;
	private String mExpectionsOutputString = null;
	private boolean mConsiderReportedError = false;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;

	public ExpectedResultByReasonerGenerationEvaluator(Config config, String expectionsOutputString, boolean considerReportedErrors) {
		mExpectionsOutputString = expectionsOutputString;
		mConfig = config;
		mConsiderReportedError = considerReportedErrors;
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);		
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);		
	}
	
	public void evaluateCompetitionResults(QueryResultStorage resultStorage, Competition competition) {
		mProcessingTimeout = competition.getProcessingTimeout();
		mExecutionTimeout = competition.getExecutionTimeout();
		if (mExecutionTimeout <= 0) {
			mExecutionTimeout = mDefaultExecutionTimeout;
		}
		if (mProcessingTimeout <= 0) {
			mProcessingTimeout = mDefaultProcessingTimeout;
		}
		if (mProcessingTimeout <= 0) {
			mProcessingTimeout = mExecutionTimeout;
		}
			
		for (Query query : resultStorage.getStoredQueryCollection()) {
			
	
			resultStorage.visitStoredResultsForQuery(query, new QueryResultStorageItemVisitor() {
				
				@Override
				public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item) {
					int hashCode = 0;
					boolean hashValid = true;
					boolean reportedError = false;
					if (item != null) {
						QueryResponse queryResponse = item.getQueryResponse();
						if (queryResponse != null) {
							reportedError = queryResponse.getReasonerErrorsAvailable();
							if (queryResponse.hasTimedOut() || queryResponse.hasExecutionError() || !queryResponse.hasExecutionCompleted() || queryResponse.getReasonerQueryProcessingTime() > mProcessingTimeout) {
								hashValid = false;
							}
						} else {
							hashValid = false;
						}
						QueryResultVerificationReport verificationReport = item.getVerificationReport();
						if (verificationReport == null) {
							hashValid = false;
						} else {
							if (mConsiderReportedError && reportedError) {
								hashValid = false;
							} else {
								hashCode = verificationReport.getResultHashCode();
							}
						}
					} else {
						hashValid = false;
					}
					
					if (hashValid) {						

						
						FilePathString queryFilePathString = query.getQuerySourceString();
						String expectionOutputFileString = null;
						
						String relativeQueryFilePathString = FileSystemHandler.relativeAbsoluteResolvedFileString(queryFilePathString);
						expectionOutputFileString = mExpectionsOutputString + reasoner.getReasonerName() + File.separator + relativeQueryFilePathString + File.separator + "query-result-data.owl.hash";
						
						if (expectionOutputFileString != null) {
							try {
								FileSystemHandler.ensurePathToFileExists(expectionOutputFileString);
								FileOutputStream outputStream = new FileOutputStream(new File(expectionOutputFileString));
								OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
								outputStreamWriter.write(new Integer(hashCode).toString());
								outputStreamWriter.close();
							} catch (IOException e) {
								mLogger.error("Saving expection hash code '{}' to '{}' failed, got IOException '{}'.",new Object[]{hashCode,expectionOutputFileString,e.getMessage()});
							}
						}

						
					}
				}				
			});
			
	
		}
		
	}

}
