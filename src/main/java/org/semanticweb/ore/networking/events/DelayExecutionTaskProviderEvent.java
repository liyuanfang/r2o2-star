package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionTaskProvider;
import org.semanticweb.ore.networking.ProcessingRequirements;
import org.semanticweb.ore.threading.Event;

public class DelayExecutionTaskProviderEvent implements Event {
	
	protected ExecutionTaskProvider mExecutionProvider = null;
	protected ProcessingRequirements mProcessingRequirements = null;
	protected boolean mPreparing = true;
	
	public DelayExecutionTaskProviderEvent(ExecutionTaskProvider executionProvider, ProcessingRequirements processingRequirements, boolean preparing) {
		mExecutionProvider = executionProvider;
		mProcessingRequirements = processingRequirements;
		mPreparing = preparing;
	}
	
	public DelayExecutionTaskProviderEvent(ExecutionTaskProvider executionProvider, ProcessingRequirements processingRequirements) {
		mExecutionProvider = executionProvider;
		mProcessingRequirements = processingRequirements;
	}
	
	public ExecutionTaskProvider getExecutionTaskProvider() {
		return mExecutionProvider;
	}

	public ProcessingRequirements getProcessingRequirements() {
		return mProcessingRequirements;
	}
	
	public boolean getPreparing() {
		return mPreparing;
	}

}
