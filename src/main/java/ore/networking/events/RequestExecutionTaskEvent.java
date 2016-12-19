package ore.networking.events;

import ore.networking.ExecutionTaskHandler;
import ore.networking.ExecutionTaskProvidedCallback;
import ore.threading.Event;

public class RequestExecutionTaskEvent implements Event {
	
	protected ExecutionTaskHandler mHandler = null;
	protected ExecutionTaskProvidedCallback mCallback = null;
	
	public RequestExecutionTaskEvent(ExecutionTaskHandler clientHandler, ExecutionTaskProvidedCallback callback) {
		mHandler = clientHandler;
		mCallback = callback;
	}
	
	public ExecutionTaskProvidedCallback getExecutionTaskProvidedCallback() {
		return mCallback;
	}
	
	public ExecutionTaskHandler getExecutionTaskHandler() {
		return mHandler;
	}

}
