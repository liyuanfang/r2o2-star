package org.semanticweb.ore.evaluation;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.semanticweb.ore.competition.Competition;
import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.competition.CompetitionEvaluationType;
import org.semanticweb.ore.competition.CompetitionStatusUpdater;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.semanticweb.ore.verification.QueryResultVerificationCorrectnessType;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

	
	
	protected enum EvaluationType {
		ET_QUERY("Query"),
		ET_AVERAGE_EXECUTION_TIME("Average-Execution-Time");
		
		String mShortString = null;
		
		private EvaluationType(String shortString) {
			mShortString = shortString;
		}
	}
	

	
	

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
		
		
		Collection<Query> queryCollection = resultStorage.getStoredQueryCollection();
		Collection<ReasonerDescription> reasonerCollection = resultStorage.getStoredReasonerCollection();
		
		ArrayList<String> queryNameList = new ArrayList<String>();
		ArrayList<String> evalNameList = new ArrayList<String>();


		for (Query query : queryCollection) {		
			queryNameList.add(query.getOntologySourceString().getPreferedRelativeFilePathString());
		}
		for (EvaluationType evalType : EvaluationType.values()) {	
			evalNameList.add(evalType.mShortString);
		}
		
		


		
		for (Query query : queryCollection) {
			QueryProcessingData queryData = new QueryProcessingData(query);
			queryDataMap.put(query, queryData);
			queryDataList.add(queryData);
		}		
		
		for (Query query : queryCollection) {
			
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
		
		
		EvaluationDataTable<Query,EvaluationType,String> queryEvalTable = new EvaluationDataTable<Query,EvaluationType,String>();
		queryEvalTable.initTable(queryCollection, Arrays.asList(EvaluationType.values()));
		queryEvalTable.initColumnHeaders(evalNameList);		
		
		for (QueryProcessingData queryData : queryDataList) {
			queryEvalTable.setData(queryData.mQuery, EvaluationType.ET_QUERY, queryData.mQuery.getOntologySourceString().getPreferedRelativeFilePathString());
			queryEvalTable.setData(queryData.mQuery, EvaluationType.ET_AVERAGE_EXECUTION_TIME, String.valueOf(queryData.mExecutionTime));
		}
		
		
		queryEvalTable.writeCSVTable(mOutputString+"Query-Sorted-Average-Execution-Time-Difficulty.csv");
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Query-Sorted-Average-Execution-Time-Difficulty",mOutputString+"Query-Sorted-Average-Execution-Time-Difficulty.csv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV,new DateTime(DateTimeZone.UTC)));
		}
		queryEvalTable.writeTSVTable(mOutputString+"Query-Sorted-Average-Execution-Time-Difficulty.tsv");
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Query-Sorted-Average-Execution-Time-Difficulty",mOutputString+"Query-Sorted-Average-Execution-Time-Difficulty.tsv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV,new DateTime(DateTimeZone.UTC)));
		}
		
		
		
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
