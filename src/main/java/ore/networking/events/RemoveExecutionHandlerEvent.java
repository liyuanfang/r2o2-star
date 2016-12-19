package ore.networking.events;

import ore.networking.ExecutionTaskHandler;
import ore.threading.Event;

public class RemoveExecutionHandlerEvent implements Event {
	
	protected ExecutionTaskHandler mHandler = null;
	
	public RemoveExecutionHandlerEvent(ExecutionTaskHandler clientHandler) {
		mHandler = clientHandler;
	}
	
	public ExecutionTaskHandler getExecutionTaskHandler() {
		return mHandler;
	}

}
