package ore.networking.events;

import ore.networking.ExecutionTaskProvider;
import ore.networking.ProcessingRequirements;
import ore.threading.Event;

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
