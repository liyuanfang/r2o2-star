package ore.networking.events;

import ore.networking.ExecutionTaskHandler;
import ore.threading.Event;

public class AddExecutionHandlerEvent implements Event {
	
	protected ExecutionTaskHandler mHandler = null;
	
	public AddExecutionHandlerEvent(ExecutionTaskHandler handler) {
		mHandler = handler;
	}	
	
	public ExecutionTaskHandler getExecutionTaskHandler() {
		return mHandler;
	}

}
