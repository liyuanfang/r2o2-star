package ore.execution.events;

import ore.execution.ReasonerQueryExecutedCallback;
import ore.networking.ClientExecutionTask;
import ore.querying.QueryResponse;
import ore.threading.Event;
import ore.threading.EventThread;

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
