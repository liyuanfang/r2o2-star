package ore.networking;

import ore.threading.Callback;

public interface ExecutionTaskProvidedCallback extends Callback {
	
	public void provideExecutionTask(ExecutionTask executionTask);

}
