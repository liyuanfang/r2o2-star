package org.semanticweb.ore.execution;

import java.util.ArrayList;
import java.util.Collection;

import org.joda.time.DateTime;

public class ReasonerQueryExecutionReport {
	
	private boolean mTimedOut = false;
	private boolean mExecutionError = false;
	private boolean mExecutionCompleted = false;
	private long mExecutionTime = 0;
	private ArrayList<String> mErrorStringList = new ArrayList<String>();
	private DateTime mExecutionEndDateTime = null;
	private DateTime mExecutionStartDateTime = null;
	
	
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
	
	public void addErrorMessage(String errorMessageString) {
		mExecutionError = true;
		mErrorStringList.add(errorMessageString);
	}
	
	public String getErrorString() {
		if (mErrorStringList.isEmpty()) {
			return "";
		} else {
			String errorString = null;
			for (String errorMessage : mErrorStringList) {
				if (errorString == null) {
					errorString = errorMessage;
				} else {
					errorString = errorString+", "+errorMessage;
				}
			}
			if (errorString == null) {
				errorString = "";
			}
			return errorString;			
		}
	}
	
	public void setExecutionError(boolean executionError) {
		if (executionError) {
			mExecutionError = true;
		}
	}	
	
	public Collection<String> getErrorStringList() {
		return mErrorStringList;
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
	
	public ReasonerQueryExecutionReport() {		
	}

	public DateTime getExecutionEndDataTime() {
		return mExecutionEndDateTime;
	}	

	public DateTime getExecutionStartDataTime() {
		return mExecutionStartDateTime;
	}	
	

	public void setExecutionEndDataTime(DateTime dateTime) {
		mExecutionEndDateTime = dateTime;
	}
	

	public void setExecutionStartDataTime(DateTime dateTime) {
		mExecutionStartDateTime = dateTime;
	}		
}
