package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionTaskProvider;
import org.semanticweb.ore.networking.ProcessingRequirements;
import org.semanticweb.ore.threading.Event;

public class ScheduleExecutionTaskProviderEvent implements Event {
	
	protected ExecutionTaskProvider mExecutionProvider = null;
	protected ProcessingRequirements mProcessingRequirements = null;
	
	public ScheduleExecutionTaskProviderEvent(ExecutionTaskProvider executionProvider, ProcessingRequirements processingRequirements) {
		mExecutionProvider = executionProvider;
		mProcessingRequirements = processingRequirements;
	}
	
	public ExecutionTaskProvider getExecutionTaskProvider() {
		return mExecutionProvider;
	}

	public ProcessingRequirements getProcessingRequirements() {
		return mProcessingRequirements;
	}

}
