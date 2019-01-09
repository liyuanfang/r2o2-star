package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionTaskHandler;
import org.semanticweb.ore.networking.ExecutionTaskProvidedCallback;
import org.semanticweb.ore.threading.Event;

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
