package org.semanticweb.ore.execution.events;

import org.semanticweb.ore.execution.ReasonerQueryExecutedCallback;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.threading.Event;

public class ReasonerProcessInitialiseEvent implements Event {
	
	private ReasonerDescription mReasonerDescription = null;
	private long mExecutionTimeout = 0;
	private Query mQuery = null;
	private ReasonerQueryExecutedCallback mCallback = null;
	private String mResponseDestinationString = null;
	private long mMemoryLimit = 0;
	

	public ReasonerDescription getReasonerDescription() {
		return mReasonerDescription;
	}
	
	
	public Query getQuery() {
		return mQuery;
	}
	
	public ReasonerQueryExecutedCallback getCallback() {
		return mCallback;
	}	
	
	public String getResponseDestinationString() {
		return mResponseDestinationString;
	}
	
	public long getExecutionTimeout() {
		return mExecutionTimeout;
	}

	public long getMemoryLimit() {
		return mMemoryLimit;
	}
	
	public ReasonerProcessInitialiseEvent(Query query, ReasonerDescription reasoner, String responseDestinationString, long executionTimeout, long memoryLimit, ReasonerQueryExecutedCallback callback) {
		mReasonerDescription = reasoner;
		mExecutionTimeout = executionTimeout;
		mQuery = query;
		mCallback = callback;
		mResponseDestinationString = responseDestinationString;
		mMemoryLimit = memoryLimit;
	}

}
