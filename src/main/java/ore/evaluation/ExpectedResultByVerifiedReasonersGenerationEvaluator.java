package ore.evaluation;

import ore.competition.Competition;
import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.utilities.FilePathString;
import ore.utilities.FileSystemHandler;
import ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ExpectedResultByVerifiedReasonersGenerationEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ExpectedResultByVerifiedReasonersGenerationEvaluator.class);
	
	private Set<ReasonerDescription> mVerifiedReasoners = new HashSet<ReasonerDescription>();
	
	private Config mConfig = null;
	private String mExpectionsOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	

	public ExpectedResultByVerifiedReasonersGenerationEvaluator(Collection<ReasonerDescription> verifiedReasoners, Config config, String expectionsOutputString) {
		mExpectionsOutputString = expectionsOutputString;
		mConfig = config;
		for (ReasonerDescription reasoner : verifiedReasoners) {
			mVerifiedReasoners.add(reasoner);
		}
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);
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
		
		int verifyReasonerCount = mVerifiedReasoners.size();
		
		for (Query query : resultStorage.getStoredQueryCollection()) {
			
			class IdenticalHashValueCounter {
				public int identicalHashCode = 0; 
				public int validCount = 0;
				public int identicalHashCount = 0;
				public int differentHashCount = 0;
			};
			final IdenticalHashValueCounter identicalHashValueCounter = new IdenticalHashValueCounter();
			
			resultStorage.visitStoredResultsForQuery(query, new QueryResultStorageItemVisitor() {
				@Override
				public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item) {
					if (mVerifiedReasoners.contains(reasoner)) {
						int hashCode = 0;
						boolean hashValid = true;
						if (item != null) {
							QueryResponse queryResponse = item.getQueryResponse();
							if (queryResponse != null) {
								if (queryResponse.hasTimedOut() || queryResponse.hasExecutionError() || !queryResponse.hasExecutionCompleted() || queryResponse.getReasonerErrorsAvailable() || queryResponse.getReasonerQueryProcessingTime() > mProcessingTimeout) {
									hashValid = false;
								}
							} else {
								hashValid = false;
							}
							QueryResultVerificationReport verificationReport = item.getVerificationReport();
							if (verificationReport == null) {
								hashValid = false;
							} else {
								hashCode = verificationReport.getResultHashCode();
							}
						} else {
							hashValid = false;
						}
						
						if (hashValid) {
							if (identicalHashValueCounter.validCount == 0) {
								identicalHashValueCounter.identicalHashCode = hashCode;								
								identicalHashValueCounter.identicalHashCount = 1;
							} else {
								if (hashCode == identicalHashValueCounter.identicalHashCode) {
									identicalHashValueCounter.identicalHashCount++;
								} else {
									identicalHashValueCounter.differentHashCount++;
								}
							}
							identicalHashValueCounter.validCount += 1;
						}
						
					}
				}				
			});
			
			if (identicalHashValueCounter.identicalHashCount >= verifyReasonerCount) {
				FilePathString queryFilePathString = query.getQuerySourceString();
				
				String expectionOutputFileString = null;
				
				String relativeQueryFilePathString = FileSystemHandler.relativeAbsoluteResolvedFileString(queryFilePathString);
				expectionOutputFileString = mExpectionsOutputString + relativeQueryFilePathString + File.separator + "query-result-data.owl.hash";
				
				if (expectionOutputFileString != null) {
					try {
						FileSystemHandler.ensurePathToFileExists(expectionOutputFileString);
						FileOutputStream outputStream = new FileOutputStream(new File(expectionOutputFileString));
						OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
						outputStreamWriter.write(new Integer(identicalHashValueCounter.identicalHashCode).toString());
						outputStreamWriter.close();
					} catch (IOException e) {
						mLogger.error("Saving expection hash code '{}' to '{}' failed, got IOException '{}'.",new Object[]{identicalHashValueCounter.identicalHashCode,expectionOutputFileString,e.getMessage()});
					}
				}
				
			}
		}
		
	}

}
