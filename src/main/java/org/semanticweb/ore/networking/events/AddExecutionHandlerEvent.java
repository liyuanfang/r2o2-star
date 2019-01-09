package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionTaskHandler;
import org.semanticweb.ore.threading.Event;

public class AddExecutionHandlerEvent implements Event {
	
	protected ExecutionTaskHandler mHandler = null;
	
	public AddExecutionHandlerEvent(ExecutionTaskHandler handler) {
		mHandler = handler;
	}	
	
	public ExecutionTaskHandler getExecutionTaskHandler() {
		return mHandler;
	}

}
