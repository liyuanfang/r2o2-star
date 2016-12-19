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

import java.util.*;

public class CorrectAverageRankingQueryResultEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CorrectAverageRankingQueryResultEvaluator.class);
	
	private Config mConfig = null;
	private String mResultOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	protected EvaluationChartPrintingHandler mChartPrintingHandler = null;
	
	public CorrectAverageRankingQueryResultEvaluator(Config config, String expectionsOutputString, CompetitionStatusUpdater statusUpdater) {
		mResultOutputString = expectionsOutputString;
		mConfig = config;
		mStatusUpdater = statusUpdater;
	
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);
		
		mChartPrintingHandler = new EvaluationChartPrintingHandler(mConfig,mResultOutputString,statusUpdater);
	}
	
	enum EvaluationType {
		ET_REASONER,
		ET_RANK,
		ET_CORRECTLY_PROCESSED_COUNT,
		ET_CORRECTLY_PROCESSED_ACCUMULATED_TIME,
		ET_CORRECTLY_PROCESSED_AVERAGE_TIME,
		
		ET_TOTAL_EXECUTION_COUNT,
		ET_TOTAL_EXECUTION_ACCUMULATED_TIME,
		ET_TOTAL_EXECUTION_AVERAGE_TIME,
		
		ET_TIMEOUT_COUNT,
		ET_EXECUTION_ERROR_COUNT,
		ET_REPORTED_ERROR_COUNT,
		ET_UNEXPECTED_COUNT,
		ET_NOT_PROCESSED_COUNT;
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
		
		class ReasonerData implements Comparable<ReasonerData> {
			ReasonerDescription mReasoner = null;
			long mCorrectlyProcessedTime = 0;
			int mCorrectlyProccessedCount = 0;
			int mRank = 0;
			int mTotalProcessedCount = 0;
			long mTotalExecutionTime = 0;	
			int mTimeOutCount = 0;
			int mExecutionErrorCount = 0;
			int mReportedErrorCount = 0;
			int mUnexpectedCount = 0;
			int mNotProcessedCount = 0;
			
			ReasonerData(ReasonerDescription reasoner) {
				mReasoner = reasoner;
			}
			
			void addData(long correctlyProcessingTime, int correctlyProcessedCount, int totalProcessedCount, long totalExecutionTime, int timeoutCount, int executionErrorCount, int reportedErrorCount, int unexpectedCount, int notProcessedCount) {
				mCorrectlyProcessedTime += correctlyProcessingTime;
				mCorrectlyProccessedCount += correctlyProcessedCount;
				
				mTotalProcessedCount += totalProcessedCount;
				mTotalExecutionTime += totalExecutionTime;
				
				mTimeOutCount += timeoutCount;
				mExecutionErrorCount += executionErrorCount;
				mReportedErrorCount += reportedErrorCount;
				mNotProcessedCount += notProcessedCount;
				mUnexpectedCount += unexpectedCount;
				
			}
			
			
			public boolean hasSameRank(ReasonerData data) {
				if (mCorrectlyProccessedCount > data.mCorrectlyProccessedCount) {
					return false;
				} else if (mCorrectlyProccessedCount < data.mCorrectlyProccessedCount) {
					return false;
				} else {
					if (mCorrectlyProcessedTime < data.mCorrectlyProcessedTime) {
						return false;
					} else if (mCorrectlyProcessedTime > data.mCorrectlyProcessedTime) {
						return false;
					} else {
						return true;
					}
				}
			}
			

			@Override
			public int compareTo(ReasonerData data) {
				if (mCorrectlyProccessedCount > data.mCorrectlyProccessedCount) {
					return -1;
				} else if (mCorrectlyProccessedCount < data.mCorrectlyProccessedCount) {
					return 1;
				} else {
					if (mCorrectlyProcessedTime < data.mCorrectlyProcessedTime) {
						return -1;
					} else if (mCorrectlyProcessedTime > data.mCorrectlyProcessedTime) {
						return 1;
					} else {
						return mReasoner.getReasonerName().toUpperCase().compareTo(data.mReasoner.getReasonerName().toUpperCase());
					}
				}
			}
		}
		
		final HashMap<ReasonerDescription,ReasonerData> reasonerDataHash = new HashMap<ReasonerDescription,ReasonerData>();
		ArrayList<ReasonerData> reasonerDataList = new ArrayList<ReasonerData>();
		
		for (ReasonerDescription reasoner : resultStorage.getStoredReasonerCollection()) {
			ReasonerData reasonerData = new ReasonerData(reasoner);
			reasonerDataHash.put(reasoner, reasonerData);
			reasonerDataList.add(reasonerData);
		}
		
		for (Query query : resultStorage.getStoredQueryCollection()) {

			
			resultStorage.visitStoredResultsForQuery(query, new QueryResultStorageItemVisitor() {
				
				@Override
				public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item) {
					boolean resultCorrect = false;
					boolean resultValid = true;
					long processingTime = 0;
					long executionTime = 0;
					
					int correctlyProcessingCount = 0;
					long correctlyProcessingTime = 0;
					int totalProcessedCount = 0;
					int executionErrorCount = 0;
					int reportedErrorCount = 0;
					int unexpectedCount = 0;
					int timeoutCount = 0;
					int notProcessedCount = 0;
					
					
					if (item != null) {
						++totalProcessedCount;
						QueryResponse queryResponse = item.getQueryResponse();
						if (queryResponse != null) {
							executionTime = queryResponse.getExecutionTime();
							if (queryResponse.hasTimedOut() || queryResponse.getReasonerQueryProcessingTime() > mProcessingTimeout) {
								resultValid = false;
								++timeoutCount;
							} else if (queryResponse.hasExecutionError() || !queryResponse.hasExecutionCompleted()) {
								resultValid = false;
								++executionErrorCount;
							} else {
								processingTime = queryResponse.getReasonerQueryProcessingTime();								
							}
							if (queryResponse.getReasonerErrorsAvailable()) {
								++reportedErrorCount;
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
								++unexpectedCount;
							} else {
								resultValid = false;
							}
						} else {
							resultValid = false;
						}
					} else {
						++notProcessedCount;
						resultValid = false;
					}

					
					if (resultValid && resultCorrect) {		
						++correctlyProcessingCount;
						correctlyProcessingTime += processingTime;
					}
					ReasonerData reasonerData = reasonerDataHash.get(reasoner);
					reasonerData.addData(correctlyProcessingTime, correctlyProcessingCount, totalProcessedCount, executionTime, timeoutCount, executionErrorCount, reportedErrorCount, unexpectedCount, notProcessedCount);
					
				}				
			});
				
		}
		
		

		ArrayList<ReasonerDescription> reasonerList = new ArrayList<ReasonerDescription>();
		Collections.sort(reasonerDataList);
		int nextRank = 1;
		for (ReasonerData reasonerData : reasonerDataList) {
			reasonerList.add(reasonerData.mReasoner);
			reasonerData.mRank = nextRank;
			++nextRank;
		}
		
		ListIterator<ReasonerData> reasonerDataIt = reasonerDataList.listIterator();
		while (reasonerDataIt.hasNext()) {
			ReasonerData reasonerData = reasonerDataIt.next();
			ListIterator<ReasonerData> reasonerDataIt2 = reasonerDataList.listIterator(reasonerDataIt.nextIndex());
			boolean stillIdentical = true;
			int identicalCount = 0;
			while (reasonerDataIt2.hasNext() && stillIdentical) {
				ReasonerData reasonerData2 = reasonerDataIt2.next();
				if (reasonerData.hasSameRank(reasonerData2)) {
					identicalCount++;
				} else {
					stillIdentical = false;
				}
			}
			reasonerData.mRank += identicalCount;
		}				
	
		
		
		
		EvaluationDataTable<ReasonerDescription,EvaluationType,String> reasonerRankProcessedTimeTable = new EvaluationDataTable<ReasonerDescription,EvaluationType,String>();
		reasonerRankProcessedTimeTable.initTable(reasonerList, Arrays.asList(EvaluationType.values()));
		reasonerRankProcessedTimeTable.initColumnHeaders(Arrays.asList(new String[]{
				"Reasoner",
				"Rank",
				"Correctly-Processed-Count",
				"Correctly-Processed-Accumulated-Time",
				"Correctly-Processed-Average-Time",
				"Total-Processed-Count",
				"Total-Execution-Accumulated-Time",
				"Total-Execution-Average-Time",
				"Timeout-Count",
				"Execution-Error-Count",
				"Reported-Error-Count",
				"Unexpected-Count",
				"Not-Processed-Count"
			}));
		

		for (ReasonerData reasonerData : reasonerDataList) {	
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_REASONER, reasonerData.mReasoner.getReasonerName());
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_RANK, String.valueOf(reasonerData.mRank));
			
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_CORRECTLY_PROCESSED_COUNT, String.valueOf(reasonerData.mCorrectlyProccessedCount));
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_CORRECTLY_PROCESSED_ACCUMULATED_TIME, String.valueOf(reasonerData.mCorrectlyProcessedTime));
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_CORRECTLY_PROCESSED_AVERAGE_TIME, String.valueOf((double)reasonerData.mCorrectlyProcessedTime/(double)reasonerData.mCorrectlyProccessedCount));
			
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_TOTAL_EXECUTION_COUNT, String.valueOf(reasonerData.mTotalProcessedCount));
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_TOTAL_EXECUTION_ACCUMULATED_TIME, String.valueOf(reasonerData.mTotalExecutionTime));
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_TOTAL_EXECUTION_AVERAGE_TIME, String.valueOf((double)reasonerData.mTotalExecutionTime/(double)reasonerData.mTotalProcessedCount));
			
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_TIMEOUT_COUNT, String.valueOf(reasonerData.mTimeOutCount));
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_EXECUTION_ERROR_COUNT, String.valueOf(reasonerData.mExecutionErrorCount));
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_REPORTED_ERROR_COUNT, String.valueOf(reasonerData.mReportedErrorCount));
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_UNEXPECTED_COUNT, String.valueOf(reasonerData.mUnexpectedCount));
			reasonerRankProcessedTimeTable.setData(reasonerData.mReasoner, EvaluationType.ET_NOT_PROCESSED_COUNT, String.valueOf(reasonerData.mNotProcessedCount));
		}
		
		reasonerRankProcessedTimeTable.writeCSVTable(mResultOutputString+"Correct-Average-Ranking-Statistics.csv");
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Correct-Average-Ranking-Statistics",mResultOutputString+"Correct-Average-Ranking-Statistics.csv", CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV,new DateTime(DateTimeZone.UTC)));
		}
		
		reasonerRankProcessedTimeTable.writeTSVTable(mResultOutputString+"Correct-Average-Ranking-Statistics.tsv");
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Correct-Average-Ranking-Statistics",mResultOutputString+"Correct-Average-Ranking-Statistics.tsv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV,new DateTime(DateTimeZone.UTC)));
		}
		
		mChartPrintingHandler.printBarChart(competition, "Correctly-Processed-Count", "Comparison of Correctly Solved Problems",reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_CORRECTLY_PROCESSED_COUNT),reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_REASONER),"Number of correctly solved problems","Reasoner","Number of correctly solved problems");
		mChartPrintingHandler.printBarChart(competition, "Correctly-Processed-Time", "Comparison of Cumulative Reasoning Time for Correctly Solved Problems",reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_CORRECTLY_PROCESSED_ACCUMULATED_TIME),reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_REASONER),"Reasoning time for correctly solved problems","Reasoner","Reasoning time (in milliseconds)");
		mChartPrintingHandler.printBarChart(competition, "Timeout-Count", "Comparison of Timeouts",reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_TIMEOUT_COUNT),reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_REASONER),"Number of timeouts","Reasoner","Number of timeouts");
		mChartPrintingHandler.printBarChart(competition, "Execution-Error-Count", "Comparison of Execution Errors",reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_EXECUTION_ERROR_COUNT),reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_REASONER),"Number of execution errors","Reasoner","Number of execution errors");
		mChartPrintingHandler.printBarChart(competition, "Reported-Error-Count", "Comparison of Reported Errors",reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_REPORTED_ERROR_COUNT),reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_REASONER),"Number of reported errors","Reasoner","Number of reported errors");
		mChartPrintingHandler.printBarChart(competition, "Unexpected-Count", "Comparison of Unexpected/Incorrect Results",reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_UNEXPECTED_COUNT),reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_REASONER),"Number of unexpected/incorrect results","Reasoner","Number of unexpected/incorrect results");		
		mChartPrintingHandler.printBarChart(competition, "Execution-Time", "Comparison of Cumulative Execution Time",reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_TOTAL_EXECUTION_ACCUMULATED_TIME),reasonerRankProcessedTimeTable.getColumnValueList(EvaluationType.ET_REASONER),"Total execution time","Reasoner","Execution time (in milliseconds)");		
	}

}
