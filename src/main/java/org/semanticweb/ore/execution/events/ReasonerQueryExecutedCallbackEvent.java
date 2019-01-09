package org.semanticweb.ore.execution.events;

import org.semanticweb.ore.execution.ReasonerQueryExecutedCallback;
import org.semanticweb.ore.networking.ClientExecutionTask;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.threading.Event;
import org.semanticweb.ore.threading.EventThread;

public class ReasonerQueryExecutedCallbackEvent implements Event, ReasonerQueryExecutedCallback {
	private volatile QueryResponse mResponse = null;
	private EventThread mPostEventThread = null;
	private ClientExecutionTask mExecutionTask = null;
	
	public void reasonerQueryExecuted(QueryResponse response) {
		mResponse = response;
		mPostEventThread.postEvent(this);
	}	
	
	public QueryResponse getQueryResponse() {
		return mResponse;
	}
	
	public ClientExecutionTask getExecutionTask() {
		return mExecutionTask;
	}
	
	public ReasonerQueryExecutedCallbackEvent(EventThread thread, ClientExecutionTask executionTask) {
		mPostEventThread = thread;
		mExecutionTask = executionTask;
	}

}
