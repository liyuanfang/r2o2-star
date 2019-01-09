package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionTaskHandler;
import org.semanticweb.ore.threading.Event;

public class RemoveExecutionHandlerEvent implements Event {
	
	protected ExecutionTaskHandler mHandler = null;
	
	public RemoveExecutionHandlerEvent(ExecutionTaskHandler clientHandler) {
		mHandler = clientHandler;
	}
	
	public ExecutionTaskHandler getExecutionTaskHandler() {
		return mHandler;
	}

}
