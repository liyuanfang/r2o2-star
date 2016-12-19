package ore.networking.events;

import ore.networking.ExecutionTask;
import ore.networking.ExecutionTaskProvidedCallback;
import ore.threading.Event;
import ore.threading.EventThread;

public class ExecutionTaskProvidedCallbackEvent implements Event, ExecutionTaskProvidedCallback {
	private volatile ExecutionTask mExecutionTask = null;
	private EventThread mPostEventThread = null;
	
	public ExecutionTask getExecutionTask() {
		return mExecutionTask;
	}
		
	public ExecutionTaskProvidedCallbackEvent(EventThread thread) {
		mPostEventThread = thread;
	}


	@Override
	public void provideExecutionTask(ExecutionTask executionTask) {
		mExecutionTask = executionTask;
		mPostEventThread.postEvent(this);
	}


}
