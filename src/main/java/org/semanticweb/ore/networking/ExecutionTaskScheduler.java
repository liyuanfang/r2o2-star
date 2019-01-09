package org.semanticweb.ore.networking;

public interface ExecutionTaskScheduler {
	
	public void postProviderRequest(ExecutionTaskHandler executionHandler);
	public void postRemoveHandler(ExecutionTaskHandler executionHandler);
	
	public void postSchedulingRequest(ExecutionTaskProvider executionProvider, ProcessingRequirements processingRequirements);
	
}
