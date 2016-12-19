package ore.evaluation;

import ore.competition.Competition;
import ore.competition.CompetitionEvaluationStatus;
import ore.competition.CompetitionEvaluationType;
import ore.competition.CompetitionStatusUpdater;
import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.verification.QueryResultVerificationCorrectnessType;
import ore.verification.QueryResultVerificationReport;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class ReasonerQueryResultListEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerQueryResultListEvaluator.class);
	
	private Config mConfig = null;
	private String mResultOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	public ReasonerQueryResultListEvaluator(Config config, String expectionsOutputString, CompetitionStatusUpdater statusUpdater) {
		mResultOutputString = expectionsOutputString;
		mConfig = config;
		mStatusUpdater = statusUpdater;
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);		
	}
	
	protected enum EvaluationType {
		ET_PROCESSED("Processed"),
		ET_EXECUTION_TIME("Execution-Time"),
		ET_REPORTED_PROCESSING_TIME("Reported-Processing-Time"),
		ET_CORRECTLY_PROCESSED("Correctly-Processed"),
		ET_CORRECTLY_PROCESSED_TIME("Correctly-Processed-Time"),
		ET_TIMEOUT("Timeout"),
		ET_EXECUTION_ERROR("Execution-Error"),
		ET_REPORTED_ERROR("Reported-Error"),
		ET_UNEXPECTED("Unexpected"),
		ET_NOT_PROCESSED("Not-Processed");
		
		String mShortString = null;
		
		private EvaluationType(String shortString) {
			mShortString = shortString;
		}
	}
	

	
	
	protected class QueryReasonerData {	
		ReasonerDescription mReasoner = null;
		Query mQuery = null;
		long mCorrectlyProcessedTime = 0;
		long mReportedProcessedTime = 0;
		boolean mCorrectlyProccessed = false;
		boolean mProcessed = false;
		long mExecutionTime = 0;	
		boolean mTimeOut = false;
		boolean mExecutionError = false;
		boolean mUnexpected = false;
		boolean mNotProcessed = false;
		boolean mReportedError = false;
		
		String getValueString(EvaluationType evalType) {
			String string = null;
			if (evalType == EvaluationType.ET_PROCESSED) {
				string = String.valueOf(mProcessed);
			} else if (evalType == EvaluationType.ET_EXECUTION_TIME) {
				string = String.valueOf(mExecutionTime);
			} else if (evalType == EvaluationType.ET_REPORTED_PROCESSING_TIME) {
				string = String.valueOf(mReportedProcessedTime);
			} else if (evalType == EvaluationType.ET_CORRECTLY_PROCESSED) {
				string = String.valueOf(mCorrectlyProccessed);
			} else if (evalType == EvaluationType.ET_CORRECTLY_PROCESSED_TIME) {
				string = String.valueOf(mCorrectlyProcessedTime);
			} else if (evalType == EvaluationType.ET_TIMEOUT) {
				string = String.valueOf(mTimeOut);
			} else if (evalType == EvaluationType.ET_EXECUTION_ERROR) {
				string = String.valueOf(mExecutionError);
			} else if (evalType == EvaluationType.ET_REPORTED_ERROR) {
				string = String.valueOf(mReportedError);
			} else if (evalType == EvaluationType.ET_UNEXPECTED) {
				string = String.valueOf(mUnexpected);
			} else if (evalType == EvaluationType.ET_NOT_PROCESSED) {
				string = String.valueOf(mNotProcessed);
			}
			return string;
		}
		
		QueryReasonerData(ReasonerDescription reasoner, Query query) {
			mReasoner = reasoner;
			mQuery = query;
		}
		
		void setData(long reportedProcessingTime, long correctlyProcessingTime, boolean correctlyProcessedCount, boolean totalProcessedCount, long totalExecutionTime, boolean timeoutCount, boolean executionErrorCount, boolean reportedErrorCount, boolean unexpectedCount, boolean notProcessedCount) {
			mReportedProcessedTime = reportedProcessingTime;
			mCorrectlyProcessedTime = correctlyProcessingTime;
			mCorrectlyProccessed = correctlyProcessedCount;
			
			mProcessed = totalProcessedCount;
			mExecutionTime = totalExecutionTime;
			
			mTimeOut = timeoutCount;
			mExecutionError = executionErrorCount;
			mReportedError = reportedErrorCount;
			mNotProcessed = notProcessedCount;
			mUnexpected = unexpectedCount;
		}
	}	
	
	
	
	protected HashMap<Query,HashMap<ReasonerDescription,QueryReasonerData>> mQueryReasonerDataHash = new HashMap<Query,HashMap<ReasonerDescription,QueryReasonerData>>();
	protected HashMap<ReasonerDescription,HashMap<Query,QueryReasonerData>> mReasonerQueryDataHash = new HashMap<ReasonerDescription,HashMap<Query,QueryReasonerData>>();


	protected QueryReasonerData getQueryReasonerData(Query query, ReasonerDescription reasoner) {
		HashMap<ReasonerDescription,QueryReasonerData> reasonerDataMap = mQueryReasonerDataHash.get(query);
		if (reasonerDataMap == null) {
			reasonerDataMap = new HashMap<ReasonerDescription,QueryReasonerData>();
			mQueryReasonerDataHash.put(query, reasonerDataMap);
		}
		QueryReasonerData data = reasonerDataMap.get(reasoner);
		if (data == null) {
			data = new QueryReasonerData(reasoner,query);
			reasonerDataMap.put(reasoner, data);
		}
		
		HashMap<Query,QueryReasonerData> queryDataMap = mReasonerQueryDataHash.get(reasoner);
		if (queryDataMap == null) {
			queryDataMap = new HashMap<Query,QueryReasonerData>();
			mReasonerQueryDataHash.put(reasoner, queryDataMap);
		}
		queryDataMap.put(query, data);
		return data;
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
					boolean resultCorrect = false;
					boolean resultValid = true;
					long processingTime = 0;
					long executionTime = 0;
					
					boolean correctlyProcessing = false;
					long correctlyProcessingTime = 0;
					boolean totalProcessed = true;
					boolean executionError = false;
					boolean reportedError = false;
					boolean unexpected = false;
					boolean timeout = false;
					boolean notProcessed = false;
					
					if (item != null) {
						QueryResponse queryResponse = item.getQueryResponse();
						if (queryResponse != null) {
							executionTime = queryResponse.getExecutionTime();
							processingTime = queryResponse.getReasonerQueryProcessingTime();
							if (queryResponse.hasTimedOut() || queryResponse.getReasonerQueryProcessingTime() > mProcessingTimeout) {
								resultValid = false;
								timeout = true;
							} else if (queryResponse.hasExecutionError() || !queryResponse.hasExecutionCompleted()) {
								resultValid = false;
								executionError = true;
							}
							if (queryResponse.getReasonerErrorsAvailable()) {
								reportedError = true;
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
								unexpected = true;
							} else {
								resultValid = false;
							}
						} else {
							resultValid = false;
						}
					} else {
						notProcessed = true;
						resultValid = false;
					}

					
					if (resultValid && resultCorrect) {		
						correctlyProcessing = true;
						correctlyProcessingTime = processingTime;
						if (correctlyProcessingTime > mProcessingTimeout) {
							correctlyProcessingTime = mProcessingTimeout;
						}
					}
					QueryReasonerData queryReasonerData = getQueryReasonerData(query, reasoner);
					queryReasonerData.setData(processingTime, correctlyProcessingTime, correctlyProcessing, totalProcessed, executionTime, timeout, executionError, reportedError, unexpected, notProcessed);
					
				}				
			});
				
		}
		
		
		Collection<Query> queryCollection = resultStorage.getStoredQueryCollection();
		Collection<ReasonerDescription> reasonerCollection = resultStorage.getStoredReasonerCollection();
		
		ArrayList<String> reasonerNameList = new ArrayList<String>();
		ArrayList<String> queryNameList = new ArrayList<String>();
		ArrayList<String> evalNameList = new ArrayList<String>();

		for (ReasonerDescription reasoner : reasonerCollection) {		
			reasonerNameList.add(reasoner.getReasonerName());
		}
		for (Query query : queryCollection) {		
			queryNameList.add(query.getOntologySourceString().getPreferedRelativeFilePathString());
		}
		for (EvaluationType evalType : EvaluationType.values()) {	
			evalNameList.add(evalType.mShortString);
		}

		for (ReasonerDescription reasoner : reasonerCollection) {	
			try {
				EvaluationDataTable<Query,EvaluationType,String> reasonerTable = new EvaluationDataTable<Query,EvaluationType,String>();
				reasonerTable.initTable(queryCollection, Arrays.asList(EvaluationType.values()));
				reasonerTable.initColumnHeaders(evalNameList);
				reasonerTable.initRowHeaders(queryNameList);	
				
				HashMap<Query,QueryReasonerData> queryDataMap = mReasonerQueryDataHash.get(reasoner);
				for (Query query : queryCollection) {	
					QueryReasonerData queryReasonerData = queryDataMap.get(query);
					reasonerTable.setData(query, EvaluationType.ET_PROCESSED, String.valueOf(queryReasonerData.mProcessed));
					reasonerTable.setData(query, EvaluationType.ET_EXECUTION_TIME, String.valueOf(queryReasonerData.mExecutionTime));
					reasonerTable.setData(query, EvaluationType.ET_REPORTED_PROCESSING_TIME, String.valueOf(queryReasonerData.mReportedProcessedTime));
					reasonerTable.setData(query, EvaluationType.ET_CORRECTLY_PROCESSED_TIME, String.valueOf(queryReasonerData.mCorrectlyProcessedTime));
					reasonerTable.setData(query, EvaluationType.ET_CORRECTLY_PROCESSED, String.valueOf(queryReasonerData.mCorrectlyProccessed));
					reasonerTable.setData(query, EvaluationType.ET_TIMEOUT, String.valueOf(queryReasonerData.mTimeOut));
					reasonerTable.setData(query, EvaluationType.ET_EXECUTION_ERROR, String.valueOf(queryReasonerData.mExecutionError));
					reasonerTable.setData(query, EvaluationType.ET_REPORTED_ERROR, String.valueOf(queryReasonerData.mReportedError));
					reasonerTable.setData(query, EvaluationType.ET_UNEXPECTED, String.valueOf(queryReasonerData.mUnexpected));
					reasonerTable.setData(query, EvaluationType.ET_NOT_PROCESSED, String.valueOf(queryReasonerData.mNotProcessed));
				}
				
				reasonerTable.writeCSVTable(mResultOutputString+"Reasoner-Results-"+reasoner.getReasonerName()+".csv");
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Reasoner-Results-"+reasoner.getReasonerName(),mResultOutputString+"Reasoner-Results-"+reasoner.getReasonerName()+".csv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV,new DateTime(DateTimeZone.UTC)));
				}
				reasonerTable.writeTSVTable(mResultOutputString+"Reasoner-Results-"+reasoner.getReasonerName()+".tsv");
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Reasoner-Results-"+reasoner.getReasonerName(),mResultOutputString+"Reasoner-Results-"+reasoner.getReasonerName()+".tsv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV,new DateTime(DateTimeZone.UTC)));
				}
			} catch (Exception e) {
				mLogger.error("Failed to create result table for reasoner '{}', got Exception '{}'",reasoner,e.getMessage());
			}
			
		}
		
		
		
		for (EvaluationType evalType : EvaluationType.values()) {	
			try {
				EvaluationDataTable<Query,ReasonerDescription,String> queryEvalTable = new EvaluationDataTable<Query,ReasonerDescription,String>();
				queryEvalTable.initTable(queryCollection, reasonerCollection);
				queryEvalTable.initColumnHeaders(reasonerNameList);
				queryEvalTable.initRowHeaders(queryNameList);
				

				
				for (Query query : queryCollection) {
					HashMap<ReasonerDescription,QueryReasonerData> reasonerDataMap = mQueryReasonerDataHash.get(query);
					for (ReasonerDescription reasoner : reasonerCollection) {
						
						QueryReasonerData queryReasonerData = reasonerDataMap.get(reasoner);
						queryEvalTable.setData(query, reasoner, queryReasonerData.getValueString(evalType));
					}
				}
				
				queryEvalTable.writeCSVTable(mResultOutputString+"Query-Results-"+evalType.mShortString+".csv");
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Query-Results-"+evalType.mShortString,mResultOutputString+"Query-Results-"+evalType.mShortString+".csv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV,new DateTime(DateTimeZone.UTC)));
				}
				queryEvalTable.writeTSVTable(mResultOutputString+"Query-Results-"+evalType.mShortString+".tsv");
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Query-Results-"+evalType.mShortString,mResultOutputString+"Query-Results-"+evalType.mShortString+".tsv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV,new DateTime(DateTimeZone.UTC)));
				}
			} catch (Exception e) {
				mLogger.error("Failed to create result table for evaluation type '{}', got Exception '{}'",evalType.mShortString,e.getMessage());
			}
		}
		
		
		
		
		
				
	}

}
