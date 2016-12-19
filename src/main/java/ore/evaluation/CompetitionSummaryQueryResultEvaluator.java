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

import java.util.Arrays;

public class CompetitionSummaryQueryResultEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionSummaryQueryResultEvaluator.class);
	
	private Config mConfig = null;
	private String mResultOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	protected EvaluationChartPrintingHandler mChartPrintingHandler = null;
	
	public CompetitionSummaryQueryResultEvaluator(Config config, String expectionsOutputString, CompetitionStatusUpdater statusUpdater) {
		mResultOutputString = expectionsOutputString;
		mConfig = config;
		mStatusUpdater = statusUpdater;
	
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);
		
		mChartPrintingHandler = new EvaluationChartPrintingHandler(mConfig,mResultOutputString,statusUpdater);
	}
	
	enum EvaluationType {
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
	
	enum ValueType {
		VT_NAME,
		VT_VALUE;
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
		
		class SummaryData {	
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
			
		}
		
		final SummaryData summaryData = new SummaryData();
		
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
					summaryData.addData(correctlyProcessingTime, correctlyProcessingCount, totalProcessedCount, executionTime, timeoutCount, executionErrorCount, reportedErrorCount, unexpectedCount, notProcessedCount);
					
				}				
			});
				
		}
		
		

		
		
		EvaluationDataTable<EvaluationType,ValueType,String> reasonerRankProcessedTimeTable = new EvaluationDataTable<EvaluationType,ValueType,String>();
		reasonerRankProcessedTimeTable.initTable(Arrays.asList(EvaluationType.values()), Arrays.asList(ValueType.values()));
		reasonerRankProcessedTimeTable.initColumnHeaders(Arrays.asList(new String[]{
				"Type",
				"Value"
			}));

				

		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_CORRECTLY_PROCESSED_COUNT, ValueType.VT_NAME, "Correctly-Processed-Count");
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_CORRECTLY_PROCESSED_ACCUMULATED_TIME, ValueType.VT_NAME, "Correctly-Processed-Accumulated-Time");
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_CORRECTLY_PROCESSED_AVERAGE_TIME, ValueType.VT_NAME, "Correctly-Processed-Average-Time");
		
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_TOTAL_EXECUTION_COUNT, ValueType.VT_NAME, "Total-Processed-Count");
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_TOTAL_EXECUTION_ACCUMULATED_TIME, ValueType.VT_NAME, "Total-Execution-Accumulated-Time");
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_TOTAL_EXECUTION_AVERAGE_TIME, ValueType.VT_NAME, "Total-Execution-Average-Time");
		
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_TIMEOUT_COUNT, ValueType.VT_NAME, "Timeout-Count");
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_EXECUTION_ERROR_COUNT, ValueType.VT_NAME, "Execution-Error-Count");
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_REPORTED_ERROR_COUNT, ValueType.VT_NAME, "Reported-Error-Count");
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_UNEXPECTED_COUNT, ValueType.VT_NAME, "Unexpected-Count");
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_NOT_PROCESSED_COUNT, ValueType.VT_NAME, "Not-Processed-Count");

		
			
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_CORRECTLY_PROCESSED_COUNT, ValueType.VT_VALUE, String.valueOf(summaryData.mCorrectlyProccessedCount));
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_CORRECTLY_PROCESSED_ACCUMULATED_TIME, ValueType.VT_VALUE, String.valueOf(summaryData.mCorrectlyProcessedTime));
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_CORRECTLY_PROCESSED_AVERAGE_TIME, ValueType.VT_VALUE, String.valueOf((double)summaryData.mCorrectlyProcessedTime/(double)summaryData.mCorrectlyProccessedCount));
		
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_TOTAL_EXECUTION_COUNT, ValueType.VT_VALUE, String.valueOf(summaryData.mTotalProcessedCount));
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_TOTAL_EXECUTION_ACCUMULATED_TIME, ValueType.VT_VALUE, String.valueOf(summaryData.mTotalExecutionTime));
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_TOTAL_EXECUTION_AVERAGE_TIME, ValueType.VT_VALUE, String.valueOf((double)summaryData.mTotalExecutionTime/(double)summaryData.mTotalProcessedCount));
		
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_TIMEOUT_COUNT, ValueType.VT_VALUE, String.valueOf(summaryData.mTimeOutCount));
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_EXECUTION_ERROR_COUNT, ValueType.VT_VALUE, String.valueOf(summaryData.mExecutionErrorCount));
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_REPORTED_ERROR_COUNT, ValueType.VT_VALUE, String.valueOf(summaryData.mReportedErrorCount));
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_UNEXPECTED_COUNT, ValueType.VT_VALUE, String.valueOf(summaryData.mUnexpectedCount));
		reasonerRankProcessedTimeTable.setData(EvaluationType.ET_NOT_PROCESSED_COUNT, ValueType.VT_VALUE, String.valueOf(summaryData.mNotProcessedCount));

		
		reasonerRankProcessedTimeTable.writeCSVTable(mResultOutputString+"Competition-Summary-Statistics.csv");
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Competition-Summary-Statistics",mResultOutputString+"Competition-Summary-Statistics.csv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV,new DateTime(DateTimeZone.UTC)));
		}
		
		reasonerRankProcessedTimeTable.writeTSVTable(mResultOutputString+"Competition-Summary-Statistics.tsv");
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Competition-Summary-Statistics",mResultOutputString+"Competition-Summary-Statistics.tsv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV,new DateTime(DateTimeZone.UTC)));
		}
		
	}

}
