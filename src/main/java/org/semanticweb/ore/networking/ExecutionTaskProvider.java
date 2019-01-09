package org.semanticweb.ore.networking;

import org.joda.time.DateTime;

public interface ExecutionTaskProvider {
	
	public void postExecutionTaskRequest(ExecutionTaskHandler executionHandler, ExecutionTaskProvidedCallback callback);
	public void postExecutionReport(ExecutionTaskHandler executionHandler, ExecutionReport executionReport);
	
	public void postExecutionTaskHandlerAddition(ExecutionTaskHandler executionHandler);
	public void postExecutionTaskHandlerRemovement(ExecutionTaskHandler executionHandler);
	
	public void postExecutionPreparation(DateTime plannedExecutionTime);
	
	public String getProviderName();
	
}
