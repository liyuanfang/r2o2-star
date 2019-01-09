package org.semanticweb.ore.evaluation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.querying.QueryType;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.semanticweb.ore.verification.QueryResultVerificationCorrectnessType;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummarisationQueryResultEvaluator implements QueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(SummarisationQueryResultEvaluator.class);
	
	private Config mConfig = null;
	
	private String mSummaryFileString = null;
	
	private ArrayList<Query> mIncorrectSupportedQueriesList = new ArrayList<Query>();
	private ArrayList<Query> mParsingErrorSupportedQueriesList = new ArrayList<Query>();
	private ArrayList<Query> mExecutionErrorSupportedQueriesList = new ArrayList<Query>();
	private ArrayList<Query> mReportedErrorSupportedQueriesList = new ArrayList<Query>();
	
	private ArrayList<ResultDataWriter> mResultDataWriterList = new ArrayList<ResultDataWriter>(); 
	private ArrayList<ResultDataWriter> mSupportedResultDataWriterList = new ArrayList<ResultDataWriter>(); 
	
	
	private int mClassificationQueriesCount = 0;
	private int mRealisationQueriesCount = 0;
	private int mConsistencyQueriesCount = 0;
	private int mTotalQueriesCount = 0;
	private int mSupportedQueriesCount = 0;
	
	
	private long mTimeout = Long.MAX_VALUE;
	
	
	private class ResultDataWriter {
		private String mDataFileString = null;
		private String mDataValueSeparaterString = null;
		private OutputStreamWriter mOutputFileWriter = null;
		private Exception mWritingException = null;
		
		public ResultDataWriter(String dataFileString, String dataValueSeparaterString) {
			mDataFileString = dataFileString;
			mDataValueSeparaterString = dataValueSeparaterString;
			try {
				FileSystemHandler.ensurePathToFileExists(mDataFileString);
				mOutputFileWriter = new OutputStreamWriter(new FileOutputStream(new File(mDataFileString)));
				String headerString = 
						"QueryFile"+mDataValueSeparaterString+
						"QueryType"+mDataValueSeparaterString+
						"QueryName"+mDataValueSeparaterString+
						"Supported"+mDataValueSeparaterString+
						"ExecutionTime"+mDataValueSeparaterString+
						"ReasonerReportedTime"+mDataValueSeparaterString+
						"ProcessingTimedOut"+mDataValueSeparaterString+
						"ExecutionFailure"+mDataValueSeparaterString+
						"ReasonerOutputParsingFailure"+mDataValueSeparaterString+
						"ReasonerReportedError"+mDataValueSeparaterString+
						"MatchExpectedResult\n";
				mOutputFileWriter.write(headerString);
			} catch (IOException e) {
				mWritingException = e;
			}
		}
		
		public void writeResultData(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {		
			if (mWritingException == null) {
				try {
					boolean correctness = verificationReport.getCorrectnessType() == QueryResultVerificationCorrectnessType.QUERY_RESULT_CORRECT;
					String rowString = 
							query.getQuerySourceString()+mDataValueSeparaterString+
							query.getQueryType().getQueryTypeName().toLowerCase()+mDataValueSeparaterString+
							query.toString()+mDataValueSeparaterString+
							verificationReport.isSupported()+mDataValueSeparaterString+
							queryResponse.getExecutionTime()+mDataValueSeparaterString+
							queryResponse.getReasonerQueryProcessingTime()+mDataValueSeparaterString+
							queryResponse.hasTimedOut()+mDataValueSeparaterString+
							queryResponse.hasExecutionError()+mDataValueSeparaterString+
							queryResponse.getReasonerOutputParsingError()+mDataValueSeparaterString+
							queryResponse.getReasonerErrorsAvailable()+mDataValueSeparaterString+
							correctness+"\n";
					mOutputFileWriter.write(rowString);
					mOutputFileWriter.flush();
				} catch (IOException e) {
					mWritingException = e;
				}
			}
		}
		
		public void completeWriting() {
			if (mWritingException == null) {
				try {
					mOutputFileWriter.close();
				} catch (IOException e) {
					mWritingException = e;
				}
			}
			if (mWritingException != null) {
				mLogger.error("Writing result data to '{}' failed, got Exception '{}'",mDataFileString,mWritingException.getMessage());
			}
		}
	}
	
	
	private class ResultStatistics {
		int mCorrectResults = 0;
		int mIncorrectResults = 0;
		int mTimeouts = 0;
		
		long mTotalQueryProcessingTime = 0;
		long mTotalExecutionTime = 0;
		
		long mQueryCount = 0;
		long mFailedCount = 0;
		long mExecutedCount = 0;
		
		public void addStatistics(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {
			++mQueryCount;
			if (queryResponse.hasExecutionCompleted()) {
				++mExecutedCount;
			}
			if (queryResponse.hasExecutionError()) {
				++mFailedCount;
			}
			long queryProcessingTime = queryResponse.getReasonerQueryProcessingTime();
			long executionTime = queryResponse.getExecutionTime();
			if (queryProcessingTime <= 0) {
				if (queryResponse.getReasonerOutputParsingError()) {
					queryProcessingTime = executionTime;
				}
			}
			if (queryProcessingTime > mTimeout) {
				queryProcessingTime = mTimeout;
			}
			if (queryResponse.hasTimedOut()) {
				++mTimeouts;
				queryProcessingTime = mTimeout;				
			}
			mTotalExecutionTime += executionTime;
			mTotalQueryProcessingTime += queryProcessingTime;
			if (verificationReport.getCorrectnessType() == QueryResultVerificationCorrectnessType.QUERY_RESULT_CORRECT) {
				++mCorrectResults;
			} else if (verificationReport.getCorrectnessType() == QueryResultVerificationCorrectnessType.QUERY_RESULT_INCORRECT) {
				++mIncorrectResults;
			}
		}
		
		public String getStatisticsString(String queryTypesString) {
			double averageQueryProcessingTime = 0.;
			if (mQueryCount > 0) {				
				averageQueryProcessingTime = (double)mTotalQueryProcessingTime/(double)mQueryCount;
			}
			double averageExecutionTime = 0.;
			if (mQueryCount > 0) {				
				averageExecutionTime = (double)mTotalExecutionTime/(double)mQueryCount;
			}
			String performanceString = new String("Evaluation summary for all"+queryTypesString+"queries:\n"
					+ "\t- "+mExecutedCount+" of "+mQueryCount+queryTypesString+"queries completely processed;\n"
					+ "\t- Reasoner execution failed/crashed for "+mFailedCount+queryTypesString+"queries;\n"
					+ "\t- Timeout reached for "+mTimeouts+queryTypesString+"queries;\n"
					+ "\t- Average execution time: "+averageExecutionTime+" ms;\n"
					+ "\t- Average processing time reported by reasoner: "+averageQueryProcessingTime+" ms;\n"
					+ "\t- Correct/expected results: "+mCorrectResults+";\n"
					+ "\t- Incorrect/unexpected results: "+mIncorrectResults+".");
			return performanceString;
		}
	}
	
	private ResultStatistics mAllQueriesStats = new ResultStatistics();
	private ResultStatistics mSupportedQueriesStats = new ResultStatistics();
	
	
	
	public SummarisationQueryResultEvaluator(Config config, String summaryFileString, String resultDataBaseString) {
		mResultDataWriterList.add(new ResultDataWriter(resultDataBaseString+"-all.csv",","));
		mResultDataWriterList.add(new ResultDataWriter(resultDataBaseString+"-all.tsv","\t"));
		mSupportedResultDataWriterList.add(new ResultDataWriter(resultDataBaseString+"-supported.csv",","));
		mSupportedResultDataWriterList.add(new ResultDataWriter(resultDataBaseString+"-supported.tsv","\t"));
		mSummaryFileString = summaryFileString;
		mConfig = config;
		
		mTimeout = ConfigDataValueReader.getConfigDataValueInteger(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT);
	}


	@Override
	public void evaluateQueryResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {
		++mTotalQueriesCount;
		if (query.getQueryType() == QueryType.QUERY_TYPE_CLASSIFICATION) {
			++mClassificationQueriesCount;
		} else if (query.getQueryType() == QueryType.QUERY_TYPE_CONSISTENCY) {
			++mConsistencyQueriesCount;
		} if (query.getQueryType() == QueryType.QUERY_TYPE_REALISATION) {
			++mRealisationQueriesCount;
		}
		mAllQueriesStats.addStatistics(reasoner, query, queryResponse, verificationReport);
		if (verificationReport.isSupported()) {
			++mSupportedQueriesCount;
			if (verificationReport.getCorrectnessType() == QueryResultVerificationCorrectnessType.QUERY_RESULT_INCORRECT) {
				mIncorrectSupportedQueriesList.add(query);
			}
			if (queryResponse.getReasonerOutputParsingError()) {
				mParsingErrorSupportedQueriesList.add(query);
			}
			if (queryResponse.hasExecutionError()) {
				mExecutionErrorSupportedQueriesList.add(query);
			}
			if (queryResponse.getReasonerErrorsAvailable()) {
				mReportedErrorSupportedQueriesList.add(query);
			}
			mSupportedQueriesStats.addStatistics(reasoner, query, queryResponse, verificationReport);
			for (ResultDataWriter resultDataWriter : mSupportedResultDataWriterList) {
				resultDataWriter.writeResultData(reasoner, query, queryResponse, verificationReport);
			}			
		}
		for (ResultDataWriter resultDataWriter : mResultDataWriterList) {
			resultDataWriter.writeResultData(reasoner, query, queryResponse, verificationReport);
		}
	}
	
	
	
	public void generateEvaluationSummary() {		
		String queryTypesString = new String(mTotalQueriesCount+" queries overall, "+mClassificationQueriesCount+" classification queries, "+mConsistencyQueriesCount+" consistency queries, and "+mRealisationQueriesCount+" realisation queries.");
		mLogger.info(queryTypesString);
		String statisticsAllString = mAllQueriesStats.getStatisticsString(" ");
		mLogger.info(statisticsAllString);
		String statisticsString = mSupportedQueriesStats.getStatisticsString(" supported ");
		mLogger.info(statisticsString);
		boolean problems = false;
		String nothingSupportedString = "";
		if (mSupportedQueriesCount <= 0) {
			nothingSupportedString = new String("No query executed for configured reasoner, check reasoner configuration!");
			mLogger.warn(nothingSupportedString);
			problems = true;
		}		
		String executionFailsString = new String("Execution status: FINE - reasoner execution was successful for all supported queries.");
		if (!mExecutionErrorSupportedQueriesList.isEmpty()) {
			executionFailsString = new String("Execution status: PROBLEMATIC - reasoner execution failed for the following queries:");
			for (Query query : mExecutionErrorSupportedQueriesList) {
				executionFailsString = executionFailsString+"\n\t- "+query.toString();
			}
			executionFailsString = executionFailsString+".";
			problems = true;
			mLogger.warn(executionFailsString);
		} else {	
			mLogger.info(executionFailsString);
		}
		String reportedErrorsString = new String("Error status: FINE - no errors were reported by reasoner for all supported queries.");
		if (!mReportedErrorSupportedQueriesList.isEmpty()) {
			reportedErrorsString = new String("Error status: POSSIBLY PROBLEMATIC - reasoner reported errors for the following queries:");
			for (Query query : mReportedErrorSupportedQueriesList) {
				reportedErrorsString = reportedErrorsString+"\n\t- "+query.toString();
			}
			reportedErrorsString = reportedErrorsString+".";
			problems = true;
			mLogger.warn(reportedErrorsString);
		} else {	
			mLogger.info(reportedErrorsString);
		}
		String parsingProblemsString = new String("Parsing status: FINE - parsing of reasoner output succeeded for all supported queries.");
		if (!mParsingErrorSupportedQueriesList.isEmpty()) {
			parsingProblemsString = new String("Parsing status: PROBLEMATIC - parsing of reasoner output failed for the following queries:");
			for (Query query : mParsingErrorSupportedQueriesList) {
				parsingProblemsString = parsingProblemsString+"\n\t- "+query.toString();
			}
			parsingProblemsString = parsingProblemsString+".";
			problems = true;
			mLogger.warn(parsingProblemsString);
		} else {	
			mLogger.info(parsingProblemsString);
		}
		String incorrectResultsString = new String("Correctness status: FINE - results match expected results for all supported queries.");
		if (!mIncorrectSupportedQueriesList.isEmpty()) {
			incorrectResultsString = new String("Correctness status: PROBLEMATIC - unexpected results for the following queries:");
			for (Query query : mIncorrectSupportedQueriesList) {
				incorrectResultsString = incorrectResultsString+"\n\t- "+query.toString();
			}
			incorrectResultsString = incorrectResultsString+".";
			problems = true;
			mLogger.warn(incorrectResultsString);
		} else {	
			mLogger.info(incorrectResultsString);
		}
		String problemsSolveString = new String("Everything seems to be FINE!.");
		if (problems) {			
			problemsSolveString = new String("Problems found, check reasoner and configuration!");
			mLogger.warn(problemsSolveString);
		} else {			
			mLogger.info(problemsSolveString);
		}
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(mSummaryFileString)));
			outputStreamWriter.write(queryTypesString+"\n");
			outputStreamWriter.write(statisticsAllString+"\n");
			outputStreamWriter.write(statisticsString+"\n");
			outputStreamWriter.write(nothingSupportedString+"\n");
			outputStreamWriter.write(executionFailsString+"\n");
			outputStreamWriter.write(reportedErrorsString+"\n");
			outputStreamWriter.write(parsingProblemsString+"\n");
			outputStreamWriter.write(incorrectResultsString+"\n");
			outputStreamWriter.write(problemsSolveString+"\n");
			outputStreamWriter.close();
			mLogger.info("Wrote evaluation summary to file '{}'.",mSummaryFileString);
		} catch (IOException e) {			
			mLogger.warn("Writing of evaluation summary to file '{}' failed, got IOException '{}'.",mSummaryFileString,e.getMessage());
		}
		
		for (ResultDataWriter resultDataWriter : mResultDataWriterList) {
			resultDataWriter.completeWriting();
		}
		for (ResultDataWriter resultDataWriter : mSupportedResultDataWriterList) {
			resultDataWriter.completeWriting();
		}
		
	}
	

}
