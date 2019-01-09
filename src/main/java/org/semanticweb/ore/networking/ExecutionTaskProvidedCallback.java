package org.semanticweb.ore.networking;

import org.semanticweb.ore.threading.Callback;

public interface ExecutionTaskProvidedCallback extends Callback {
	
	public void provideExecutionTask(ExecutionTask executionTask);

}
