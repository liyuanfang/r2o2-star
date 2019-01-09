package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionTask;
import org.semanticweb.ore.networking.ExecutionTaskProvidedCallback;
import org.semanticweb.ore.threading.Event;
import org.semanticweb.ore.threading.EventThread;

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
