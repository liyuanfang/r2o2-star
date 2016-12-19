package ore.evaluation;

import ore.competition.Competition;
import ore.competition.CompetitionStatusUpdater;
import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.utilities.FileSystemHandler;
import ore.verification.QueryResultVerificationCorrectnessType;
import ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class QueryDifficultyOrderGenerationEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(QueryDifficultyOrderGenerationEvaluator.class);
	
	private Config mConfig = null;
	private String mOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;


	public QueryDifficultyOrderGenerationEvaluator(Config config, String outputString, CompetitionStatusUpdater statusUpdater) {
		mOutputString = outputString;
		mConfig = config;
		mStatusUpdater = statusUpdater;
		
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
		
		class QueryProcessingData implements Comparable<QueryProcessingData> {
			Query mQuery = null;			
			long mProcessingTime = 0;
			long mCorrectlyProcessedTime = 0;
			long mExecutionTime = 0;
			int mProccessedCount = 0;			
			
			public QueryProcessingData(Query query) {
				mQuery = query;
			}
			
			public void addData(long processingTime, long executionTime, int proccessedCount, long correctlyProcessedTime) {
				mProccessedCount += proccessedCount;
				mProcessingTime += processingTime;
				mExecutionTime += executionTime;
				mCorrectlyProcessedTime += correctlyProcessedTime;
			}

			@Override
			public int compareTo(QueryProcessingData qpd) {
				if (mExecutionTime < qpd.mExecutionTime) {
					return -1;
				} else if (mExecutionTime > qpd.mExecutionTime) {
					return 1;
				} 
				return 0;
			}
		}
		
		final HashMap<Query,QueryProcessingData> queryDataMap = new HashMap<Query,QueryProcessingData>();
		
		ArrayList<QueryProcessingData> queryDataList = new ArrayList<QueryProcessingData>();
		
		for (Query query : resultStorage.getStoredQueryCollection()) {
			QueryProcessingData queryData = new QueryProcessingData(query);
			queryDataMap.put(query, queryData);
			queryDataList.add(queryData);
		}
		
		for (Query query : resultStorage.getStoredQueryCollection()) {
			
			resultStorage.visitStoredResultsForQuery(query, new QueryResultStorageItemVisitor() {
				@Override
				public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item) {
					boolean resultCorrect = false;
					boolean resultValid = true;
					long processingTime = 0;
					long executionTime = 0;
					if (item != null) {
						QueryResponse queryResponse = item.getQueryResponse();
						if (queryResponse != null) {
							processingTime = queryResponse.getReasonerQueryProcessingTime();		
							executionTime = queryResponse.getExecutionTime();
							if (queryResponse.hasTimedOut() || queryResponse.hasExecutionError() || !queryResponse.hasExecutionCompleted() || queryResponse.getReasonerQueryProcessingTime() > mProcessingTimeout) {
								resultValid = false;
							}
						} else {
							resultValid = false;
						}
						QueryResultVerificationReport verificationReport = item.getVerificationReport();
						if (verificationReport != null && resultValid) {
							if (verificationReport.getCorrectnessType() == QueryResultVerificationCorrectnessType.QUERY_RESULT_CORRECT) {
								resultCorrect = true;
							} else if (verificationReport.getCorrectnessType() == QueryResultVerificationCorrectnessType.QUERY_RESULT_INCORRECT) {
								resultCorrect = false;
							} else {
								resultValid = false;
							}
						} else {
							resultValid = false;
						}
					} else {
						resultValid = false;
					}
					
					int correctProccessedCount = 0;
					long correctlyProccessedTime = 0;
					if (resultValid && resultCorrect) {
						correctProccessedCount = 1;
						correctlyProccessedTime = processingTime;
					}
					QueryProcessingData queryData = queryDataMap.get(query);
					queryData.addData(processingTime,executionTime,correctProccessedCount,correctlyProccessedTime);

				}			
			});
		}
		
		Collections.sort(queryDataList);
		
		try {
			FileSystemHandler.ensurePathToFileExists(mOutputString+"query-execution-time-sorted.txt");
			FileOutputStream outputStream = new FileOutputStream(new File(mOutputString+"query-execution-time-sorted.txt"));
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
			for (QueryProcessingData queryData : queryDataList) {
				String querySource = queryData.mQuery.getQuerySourceString().getAbsoluteFilePathString();
				if (queryData.mQuery.getQuerySourceString().isRelative()) {
					querySource = queryData.mQuery.getQuerySourceString().getRelativeFilePathString();
				}
				querySource = querySource.replace("\\","/");
				outputStreamWriter.write(querySource+"\r\n");		
			}
			outputStreamWriter.close();
		} catch (Exception e) {
			mLogger.error("Writing to file '{}' failed, got Exception '{}'.",mOutputString+"query-execution-time-sorted.txt",e.getMessage());
			
		}

		
	}

}
