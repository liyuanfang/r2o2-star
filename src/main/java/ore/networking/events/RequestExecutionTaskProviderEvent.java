package ore.networking.events;

import ore.networking.ExecutionTaskHandler;
import ore.threading.Event;

public class RequestExecutionTaskProviderEvent implements Event {
	
	protected ExecutionTaskHandler mExecutionHandler = null;
	
	public RequestExecutionTaskProviderEvent(ExecutionTaskHandler executionHandler) {
		mExecutionHandler = executionHandler;
	}
	
	public ExecutionTaskHandler getExecutionTaskHandler() {
		return mExecutionHandler;
	}


}
