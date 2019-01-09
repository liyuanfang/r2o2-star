package org.semanticweb.ore.querying;

import org.joda.time.DateTime;
import org.semanticweb.ore.interfacing.ReasonerInterfaceType;
import org.semanticweb.ore.utilities.FilePathString;

public class QueryResponse {
	
	protected ReasonerInterfaceType mUsedInterface = null;
	protected FilePathString mResultDataFilePathString = null;
	protected FilePathString mReportFilePathString = null;
	protected FilePathString mLogFilePathString = null;
	protected FilePathString mErrorFilePathString = null;
	
	protected DateTime mExecutionStartDateTime = null;
	protected DateTime mExecutionEndDateTime = null;
	
	private boolean mReasonerConsoleOutputAvailable = false;
	private boolean mReasonerErrorsAvailable = false;
	private boolean mResultDataAvailable = false;
	
	
	private boolean mTimedOut = false;
	private boolean mExecutionError = false;
	private boolean mExecutionCompleted = false;	
	private long mExecutionTime = 0;
	
	private boolean mReasonerOutputParsingError = false;
	private long mReasonerQueryProcessingTime = 0;
	private boolean mReasonerQueryStarted = false;
	private boolean mReasonerQueryCompleted = false;
	
	
	
	public DateTime getExecutionStartDateTime() {
		return mExecutionStartDateTime;
	}
	
	public DateTime getExecutionEndDateTime() {
		return mExecutionEndDateTime;
	}
	
	public void setExecutionStartDateTime(DateTime dateTime) {
		mExecutionStartDateTime = dateTime;
	}	
	
	public void setExecutionEndDateTime(DateTime dateTime) {
		mExecutionEndDateTime = dateTime;
	}	
	
	public boolean getReasonerQueryStarted() {
		return mReasonerQueryStarted;
	}	
	
	public boolean getReasonerOutputParsingError() {
		return mReasonerOutputParsingError;
	}	
	
	public boolean getReasonerQueryCompleted() {
		return mReasonerQueryCompleted;
	}	
	
	public long getReasonerQueryProcessingTime() {
		return mReasonerQueryProcessingTime;
	}		
	
	public void setReasonerQueryProcessingTime(long time) {
		mReasonerQueryProcessingTime = time;
	}		
	
	public void setReasonerOutputParsingError(boolean parsingError) {
		mReasonerOutputParsingError = parsingError;
	}		
	
	public void setReasonerQueryCompleted(boolean completed) {
		mReasonerQueryCompleted = completed;
	}
	
	public void setReasonerQueryStarted(boolean started) {
		mReasonerQueryStarted = started;
	}		
	
	
	public void setResultDataAvailable(boolean available) {
		mResultDataAvailable = available;
	}		
	
	public void setReasonerErrorsAvailable(boolean available) {
		mReasonerErrorsAvailable = available;
	}	
	
	public void setReasonerConsoleOutputAvailable(boolean available) {
		mReasonerConsoleOutputAvailable = available;
	}		
		
	
	public boolean getResultDataAvailable() {
		return mResultDataAvailable;
	}	
	
	public boolean getReasonerErrorsAvailable() {
		return mReasonerErrorsAvailable;
	}
	
	public boolean getReasonerConsoleOutputAvailable() {
		return mReasonerConsoleOutputAvailable;
	}	
	
	
	
	public boolean hasTimedOut() {
		return mTimedOut;
	}	

	public long getExecutionTime() {
		return mExecutionTime;
	}
	
	public void setExecutionTime(long executionTime) {
		mExecutionTime = executionTime;
	}
	
	public void setTimedOut(boolean timedOut) {
		mTimedOut = timedOut;
	}
		
	
	public void setExecutionError(boolean executionError) {
		mExecutionError = executionError;
	}
		
	
	public boolean hasExecutionError() {
		return mExecutionError;
	}
	
	public boolean hasExecutionCompleted() {
		return mExecutionCompleted;
	}
	
	public void setExecutionCompleted(boolean executionCompleted) {
		mExecutionCompleted = executionCompleted;
	}
	
	
	
	public ReasonerInterfaceType getUsedInterface() {
		return mUsedInterface;
	}
	
	public FilePathString getResultDataFilePathString() {
		return mResultDataFilePathString;
	}
	
	public FilePathString getReportFilePathString() {
		return mReportFilePathString;
	}
	
	public FilePathString getLogFilePathString() {
		return mLogFilePathString;
	}	
	
	public FilePathString getErrorFilePathString() {
		return mErrorFilePathString;
	}	
	
	public QueryResponse(FilePathString resultDataFileString, FilePathString reportFileString, FilePathString logFileString, FilePathString errorFileString, ReasonerInterfaceType usedInterface) {
		mResultDataFilePathString = resultDataFileString;
		mReportFilePathString = reportFileString;
		mLogFilePathString = logFileString;
		mErrorFilePathString = errorFileString;
		mUsedInterface = usedInterface;
	}

}
