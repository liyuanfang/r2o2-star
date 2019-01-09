package org.semanticweb.ore.networking.messages;

import java.util.HashMap;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.semanticweb.ore.interfacing.ReasonerInterfaceType;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.verification.QueryResultVerificationCorrectnessType;
import org.semanticweb.ore.verification.QueryResultVerificationReport;

public class ProcessedEvaluationTaskMessage extends Message {
	

	
	public ProcessedEvaluationTaskMessage(HashMap<String,String> keyValueMap) {
		super(MessageType.MESSAGE_TYPE_PROCESSED_EVALUATION_TASK);
		mKeyValueMap.put("ReasonerName",keyValueMap.get("ReasonerName"));
		mKeyValueMap.put("QueryName",keyValueMap.get("QueryName"));
		mKeyValueMap.put("OutputName",keyValueMap.get("OutputName"));
		
		mKeyValueMap.put("UsedInterface",keyValueMap.get("UsedInterface"));
		mKeyValueMap.put("ReportFile",keyValueMap.get("ReportFile"));
		mKeyValueMap.put("ResultDataFile",keyValueMap.get("ResultDataFile"));
		mKeyValueMap.put("ErrorFile",keyValueMap.get("ErrorFile"));
		mKeyValueMap.put("LogFile",keyValueMap.get("LogFile"));
		
		mKeyValueMap.put("ExecutionTime",keyValueMap.get("ExecutionTime"));
		mKeyValueMap.put("TimedOut",keyValueMap.get("TimedOut"));
		mKeyValueMap.put("ExecutionError",keyValueMap.get("ExecutionError"));
		mKeyValueMap.put("ExecutionCompleted",keyValueMap.get("ExecutionCompleted"));
		mKeyValueMap.put("ReasonerQueryProcessingTime",keyValueMap.get("ReasonerQueryProcessingTime"));
		
		mKeyValueMap.put("ReasonerQueryStarted",keyValueMap.get("ReasonerQueryStarted"));
		mKeyValueMap.put("ReasonerQueryCompleted",keyValueMap.get("ReasonerQueryCompleted"));
		mKeyValueMap.put("ReasonerConsoleOutputAvailable",keyValueMap.get("ReasonerConsoleOutputAvailable"));
		mKeyValueMap.put("ReasonerErrorsAvailable",keyValueMap.get("ReasonerErrorsAvailable"));
		mKeyValueMap.put("ResultDataAvailable",keyValueMap.get("ResultDataAvailable"));
		mKeyValueMap.put("ReasonerOutputParsingError",keyValueMap.get("ReasonerOutputParsingError"));

		mKeyValueMap.put("ResultHashCode",keyValueMap.get("ResultHashCode"));
		mKeyValueMap.put("ReasonerSupport",keyValueMap.get("ReasonerSupport"));
		mKeyValueMap.put("ResultCorrectness",keyValueMap.get("ResultCorrectness"));
		
		mKeyValueMap.put("ExecutionStartTime",keyValueMap.get("ExecutionStartTime"));
		mKeyValueMap.put("ExecutionEndTime",keyValueMap.get("ExecutionEndTime"));
	}
	
	public ProcessedEvaluationTaskMessage(String reasonerName, String queryName, String outputName, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {
		super(MessageType.MESSAGE_TYPE_PROCESSED_EVALUATION_TASK);
		mKeyValueMap.put("ReasonerName",reasonerName);
		mKeyValueMap.put("QueryName",queryName);
		mKeyValueMap.put("OutputName",outputName);
		
		mKeyValueMap.put("UsedInterface",queryResponse.getUsedInterface().toString());
		mKeyValueMap.put("ReportFile",queryResponse.getReportFilePathString().getRelativeFilePathString());
		mKeyValueMap.put("ResultDataFile",queryResponse.getResultDataFilePathString().getRelativeFilePathString());
		mKeyValueMap.put("ErrorFile",queryResponse.getErrorFilePathString().getRelativeFilePathString());
		mKeyValueMap.put("LogFile",queryResponse.getLogFilePathString().getRelativeFilePathString());
	

		mKeyValueMap.put("ExecutionTime",String.valueOf(queryResponse.getExecutionTime()));
		mKeyValueMap.put("TimedOut",String.valueOf(queryResponse.hasTimedOut()));
		mKeyValueMap.put("ExecutionError",String.valueOf(queryResponse.hasExecutionError()));
		mKeyValueMap.put("ExecutionCompleted",String.valueOf(queryResponse.hasExecutionCompleted()));
		mKeyValueMap.put("ReasonerQueryProcessingTime",String.valueOf(queryResponse.getReasonerQueryProcessingTime()));
		
		mKeyValueMap.put("ReasonerQueryStarted",String.valueOf(queryResponse.getReasonerQueryStarted()));
		mKeyValueMap.put("ReasonerQueryCompleted",String.valueOf(queryResponse.getReasonerQueryCompleted()));
		mKeyValueMap.put("ReasonerConsoleOutputAvailable",String.valueOf(queryResponse.getReasonerConsoleOutputAvailable()));
		mKeyValueMap.put("ReasonerErrorsAvailable",String.valueOf(queryResponse.getReasonerErrorsAvailable()));
		mKeyValueMap.put("ResultDataAvailable",String.valueOf(queryResponse.getResultDataAvailable()));
		mKeyValueMap.put("ReasonerOutputParsingError",String.valueOf(queryResponse.getReasonerOutputParsingError()));
		
		mKeyValueMap.put("ResultHashCode",String.valueOf(verificationReport.getResultHashCode()));
		mKeyValueMap.put("ReasonerSupport",String.valueOf(verificationReport.isSupported()));
		mKeyValueMap.put("ResultCorrectness",verificationReport.getCorrectnessType().toString());

		mKeyValueMap.put("ExecutionStartTime",queryResponse.getExecutionStartDateTime().toString());
		mKeyValueMap.put("ExecutionEndTime",queryResponse.getExecutionEndDateTime().toString());
		
	}
	
	

	public DateTime getExecutionStartTime() {
		DateTime date = null;
		String dateString = mKeyValueMap.get("ExecutionStartTime");
		if (dateString.compareTo("null") != 0) {
			try {
				date = ISODateTimeFormat.dateTimeParser().parseDateTime(dateString);
			} catch (Exception e) {				
			}
		}
		return date;
	}

	public DateTime getExecutionEndTime() {
		DateTime date = null;
		String dateString = mKeyValueMap.get("ExecutionEndTime");
		if (dateString.compareTo("null") != 0) {
			try {
				date = ISODateTimeFormat.dateTimeParser().parseDateTime(dateString);
			} catch (Exception e) {				
			}
		}
		return date;
	}
	
	public String getReasonerString() {
		return mKeyValueMap.get("ReasonerName");
	}
	
	public String getQueryString() {
		return mKeyValueMap.get("QueryName");
	}
	
	public String getOutputString() {
		return mKeyValueMap.get("OutputName");
	}
	
	public ReasonerInterfaceType getInterface() {
		return ReasonerInterfaceType.valueOf(mKeyValueMap.get("UsedInterface"));
	}	
	

	public String getReportFileString() {
		return mKeyValueMap.get("ReportFile");
	}		

	public String getResultDataFileString() {
		return mKeyValueMap.get("ResultDataFile");
	}	

	public String getErrorFileString() {
		return mKeyValueMap.get("ErrorFile");
	}	

	public String getLogFileString() {
		return mKeyValueMap.get("LogFile");
	}	
	

	public long getExecutionTime() {
		return Long.valueOf(mKeyValueMap.get("ExecutionTime"));
	}	

	public boolean getTimedOut() {
		return Boolean.valueOf(mKeyValueMap.get("TimedOut"));
	}		

	public boolean getExecutionError() {
		return Boolean.valueOf(mKeyValueMap.get("ExecutionError"));
	}

	public boolean getExecutionCompleted() {
		return Boolean.valueOf(mKeyValueMap.get("ExecutionCompleted"));
	}	


	public long getReasonerQueryProcessingTime() {
		return Long.valueOf(mKeyValueMap.get("ReasonerQueryProcessingTime"));
	}		

	public boolean getReasonerQueryStarted() {
		return Boolean.valueOf(mKeyValueMap.get("ReasonerQueryStarted"));
	}		

	public boolean getReasonerQueryCompleted() {
		return Boolean.valueOf(mKeyValueMap.get("ReasonerQueryCompleted"));
	}	
	

	public boolean getReasonerConsoleOutputAvailable() {
		return Boolean.valueOf(mKeyValueMap.get("ReasonerConsoleOutputAvailable"));
	}		

	public boolean getReasonerErrorsAvailable() {
		return Boolean.valueOf(mKeyValueMap.get("ReasonerErrorsAvailable"));
	}		
	

	public boolean getResultDataAvailable() {
		return Boolean.valueOf(mKeyValueMap.get("ResultDataAvailable"));
	}	
	

	public boolean getReasonerOutputParsingError() {
		return Boolean.valueOf(mKeyValueMap.get("ReasonerOutputParsingError"));
	}	
	

	public boolean getReasonerSupport() {
		return Boolean.valueOf(mKeyValueMap.get("ReasonerSupport"));
	}	
	

	public int getResultHashCode() {
		return Integer.valueOf(mKeyValueMap.get("ResultHashCode"));
	}		

	public QueryResultVerificationCorrectnessType getResultCorrectness() {
		return QueryResultVerificationCorrectnessType.valueOf(mKeyValueMap.get("ResultCorrectness"));
	}		
	
}
