package ore.parsing;

import ore.configuration.Config;
import ore.interfacing.ReasonerInterfaceType;
import ore.querying.QueryResponse;
import ore.querying.QueryResponseFactory;
import ore.utilities.FilePathString;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class QueryResponseTSVParser extends TSVParser {

	final private static Logger mLogger = LoggerFactory.getLogger(QueryResponseTSVParser.class);


	private QueryResponseFactory mQueryResponseFactory = null;
	private QueryResponse mQueryResponse = null;
	
	private ReasonerInterfaceType mInterfaceType = null;
	private FilePathString mReportFileString = null;
	private FilePathString mResultDataFileString = null;
	private FilePathString mErrorFileString = null;
	private FilePathString mLogFileString = null;
	private long mExecutionTime = 0;
	private boolean mTimedOut = false;
	private boolean mReasonerOutputParsingError = false;
	private boolean mExecutionError = false;
	private boolean mExecutionCompleted = false;
	private long mReasonerQueryProcessingTime = 0;
	private boolean mReasonerQueryStarted = false;
	private boolean mReasonerQueryCompleted = false;
	private boolean mResultDataAvailable = false;
	private boolean mReasonerConsoleOutputAvailable = false;
	private boolean mReasonerErrorsAvailable = false;
	private DateTime mExecutionStartTime = null;
	private DateTime mExecutionEndTime = null;
	

	@Override
	protected boolean handleParsedValues(String[] values) {
		boolean parsed = false;
		try {
			if (values.length >= 2) {
				String valueString1 = values[0].trim();
				String valueString2 = values[1].trim();
				if (valueString1.compareToIgnoreCase("InterfaceType") == 0) {
					if (valueString2.compareToIgnoreCase("OREv1") == 0) {
						mInterfaceType = ReasonerInterfaceType.REASONER_INTERFACE_ORE_V1;
						parsed = true;
					}
				} else if (valueString1.compareToIgnoreCase("ReportFile") == 0) {
					mReportFileString = parseTSVFilePathString(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ResultDataFile") == 0) {
					mResultDataFileString = parseTSVFilePathString(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ErrorFile") == 0) {
					mErrorFileString = parseTSVFilePathString(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("LogFile") == 0) {
					mLogFileString = parseTSVFilePathString(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ExecutionTime") == 0) {
					mExecutionTime = parseTSVLong(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ReasonerQueryProcessingTime") == 0) {
					mReasonerQueryProcessingTime = parseTSVLong(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ExecutionError") == 0) {
					mExecutionError = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ExecutionCompleted") == 0) {
					mExecutionCompleted = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ReasonerQueryStarted") == 0) {
					mReasonerQueryStarted = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ReasonerQueryCompleted") == 0) {
					mReasonerQueryCompleted = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ReasonerConsoleOutputAvailable") == 0) {
					mReasonerConsoleOutputAvailable = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ResultDataAvailable") == 0) {
					mResultDataAvailable = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ReasonerErrorsAvailable") == 0) {
					mReasonerErrorsAvailable = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("TimedOut") == 0) {
					mTimedOut = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ReasonerOutputParsingError") == 0) {
					mReasonerOutputParsingError = parseTSVBoolean(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ExecutionStartTime") == 0) {
					mExecutionStartTime = parseTSVDateTime(values);
					parsed = true;
				} else if (valueString1.compareToIgnoreCase("ExecutionEndTime") == 0) {
					mExecutionEndTime = parseTSVDateTime(values);
					parsed = true;
				}
			}
			if (!parsed) {
				mLogger.warn("Cannot parse values '{}' for query response.",getValuesString(values));
			}
		} catch (Exception e) {
			mLogger.warn("Cannot parse values '{}' for query response, got Exception '{}'.",getValuesString(values),e.getMessage());
		}
		return parsed;
	}
	


	@Override
	protected boolean handleStartParsing() {
		mQueryResponse = null;
		return true;
	}

	@Override
	protected boolean handleFinishParsing() {
		if (mQueryResponseFactory != null) {
			mQueryResponse = mQueryResponseFactory.createQueryResponse(mResultDataFileString, mReportFileString, mLogFileString, mErrorFileString, mInterfaceType);
			mQueryResponse.setExecutionCompleted(mExecutionCompleted);
			mQueryResponse.setExecutionError(mExecutionError);
			mQueryResponse.setExecutionTime(mExecutionTime);
			mQueryResponse.setReasonerConsoleOutputAvailable(mReasonerConsoleOutputAvailable);
			mQueryResponse.setReasonerErrorsAvailable(mReasonerErrorsAvailable);
			mQueryResponse.setReasonerQueryCompleted(mReasonerQueryCompleted);
			mQueryResponse.setReasonerQueryProcessingTime(mReasonerQueryProcessingTime);
			mQueryResponse.setReasonerQueryStarted(mReasonerQueryStarted);
			mQueryResponse.setResultDataAvailable(mResultDataAvailable);
			mQueryResponse.setTimedOut(mTimedOut);
			mQueryResponse.setReasonerOutputParsingError(mReasonerOutputParsingError);
			mQueryResponse.setExecutionStartDateTime(mExecutionStartTime);
			mQueryResponse.setExecutionEndDateTime(mExecutionEndTime);
		}
		return true;
	}
	
	
	public QueryResponse parseQueryResponse(InputStream inputStream) throws IOException {
		try {
			parse(inputStream);
		} catch (IOException e) {
			mLogger.warn("Parsing failed, got IOException {}.",e.getMessage());
			throw e;
		} 
		return mQueryResponse;
	}	
	

	public QueryResponse parseQueryResponse(String fileString) throws IOException {
		parse(fileString);
		return mQueryResponse;
	}		
	
	
	public QueryResponseTSVParser(QueryResponseFactory queryResponseFactory, Config config, FilePathString parsingSourceString) {
		super(config,parsingSourceString);
		mQueryResponseFactory = queryResponseFactory;
	}
	


}
