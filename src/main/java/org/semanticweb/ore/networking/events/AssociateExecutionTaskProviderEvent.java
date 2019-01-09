package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionTaskProvider;
import org.semanticweb.ore.threading.Event;

public class AssociateExecutionTaskProviderEvent implements Event {
	
	protected ExecutionTaskProvider mExecutionProvider = null;
	
	public AssociateExecutionTaskProviderEvent(ExecutionTaskProvider executionProvider) {
		mExecutionProvider = executionProvider;
	}
	
	public ExecutionTaskProvider getExecutionTaskProvider() {
		return mExecutionProvider;
	}

}
