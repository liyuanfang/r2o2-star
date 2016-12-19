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
import java.util.HashMap;
import java.util.Map.Entry;

public class ExpectedResultByMajorityVotingGenerationEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ExpectedResultByMajorityVotingGenerationEvaluator.class);
	
	private Config mConfig = null;
	private String mExpectionsOutputString = null;
	private int mMajorSupportDiffCount = 1;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;

	public ExpectedResultByMajorityVotingGenerationEvaluator(Config config, String expectionsOutputString) {
		mExpectionsOutputString = expectionsOutputString;
		mConfig = config;
		
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
			
			final HashMap<Integer,Integer> hashCodeIdenticalHash = new HashMap<Integer,Integer>();
			
			resultStorage.visitStoredResultsForQuery(query, new QueryResultStorageItemVisitor() {
				
				@Override
				public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item) {
					int hashCode = 0;
					boolean hashValid = true;
					if (item != null) {
						QueryResponse queryResponse = item.getQueryResponse();
						if (queryResponse != null) {
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
							hashCode = verificationReport.getResultHashCode();
						}
					} else {
						hashValid = false;
					}
					
					if (hashValid) {
						if (!hashCodeIdenticalHash.containsKey(hashCode)) {
							hashCodeIdenticalHash.put(hashCode,1);
						} else {
							int identicalHashCodeCount = hashCodeIdenticalHash.get(hashCode)+1;
							hashCodeIdenticalHash.put(hashCode,identicalHashCodeCount);
						}
					}
				}				
			});
			
			int firstHashCode = 0;
			int firstHashCodeIdenticalCount = 0;
			int secondHashCode = 0;
			int secondHashCodeIdenticalCount = 0;
			
			for (Entry<Integer,Integer> entry : hashCodeIdenticalHash.entrySet()) {
				int hashCode = entry.getKey();
				int hashCodeIdenticalCount = entry.getValue();				
				if (firstHashCodeIdenticalCount == 0 || hashCodeIdenticalCount > firstHashCodeIdenticalCount) {
					firstHashCodeIdenticalCount = hashCodeIdenticalCount;
					firstHashCode = hashCode; 
				}
			}

			for (Entry<Integer,Integer> entry : hashCodeIdenticalHash.entrySet()) {
				int hashCode = entry.getKey();
				int hashCodeIdenticalCount = entry.getValue();
				if (hashCode != firstHashCode) {
					if (secondHashCodeIdenticalCount == 0 || hashCodeIdenticalCount > firstHashCodeIdenticalCount) {
						secondHashCodeIdenticalCount = hashCodeIdenticalCount;
						secondHashCode = hashCode;
					}
				}
			}
						
			if (firstHashCodeIdenticalCount >= secondHashCodeIdenticalCount + mMajorSupportDiffCount) {
				FilePathString queryFilePathString = query.getQuerySourceString();
				String expectionOutputFileString = null;
				
				String relativeQueryFilePathString = FileSystemHandler.relativeAbsoluteResolvedFileString(queryFilePathString);
				expectionOutputFileString = mExpectionsOutputString + relativeQueryFilePathString + File.separator + "query-result-data.owl.hash";
				
				if (expectionOutputFileString != null) {
					try {
						FileSystemHandler.ensurePathToFileExists(expectionOutputFileString);
						FileOutputStream outputStream = new FileOutputStream(new File(expectionOutputFileString));
						OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
						outputStreamWriter.write(new Integer(firstHashCode).toString());
						outputStreamWriter.close();
					} catch (IOException e) {
						mLogger.error("Saving expection hash code '{}' to '{}' failed, got IOException '{}'.",new Object[]{firstHashCode,expectionOutputFileString,e.getMessage()});
					}
				}
				
			}
		}
		
	}

}
