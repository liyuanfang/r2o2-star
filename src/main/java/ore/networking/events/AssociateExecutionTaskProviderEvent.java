package ore.networking.events;

import ore.networking.ExecutionTaskProvider;
import ore.threading.Event;

public class AssociateExecutionTaskProviderEvent implements Event {
	
	protected ExecutionTaskProvider mExecutionProvider = null;
	
	public AssociateExecutionTaskProviderEvent(ExecutionTaskProvider executionProvider) {
		mExecutionProvider = executionProvider;
	}
	
	public ExecutionTaskProvider getExecutionTaskProvider() {
		return mExecutionProvider;
	}

}
