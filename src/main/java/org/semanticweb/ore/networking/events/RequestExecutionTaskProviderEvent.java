package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionTaskHandler;
import org.semanticweb.ore.threading.Event;

public class RequestExecutionTaskProviderEvent implements Event {
	
	protected ExecutionTaskHandler mExecutionHandler = null;
	
	public RequestExecutionTaskProviderEvent(ExecutionTaskHandler executionHandler) {
		mExecutionHandler = executionHandler;
	}
	
	public ExecutionTaskHandler getExecutionTaskHandler() {
		return mExecutionHandler;
	}


}
