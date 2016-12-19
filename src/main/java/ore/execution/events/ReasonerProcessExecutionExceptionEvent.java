package ore.execution.events;

import ore.threading.Event;
import org.apache.commons.exec.ExecuteException;

public class ReasonerProcessExecutionExceptionEvent implements Event {
	
	private ExecuteException mExeException = null;
	
	public ExecuteException getException() {
		return mExeException;
	}
	
	public ReasonerProcessExecutionExceptionEvent(ExecuteException exeException) {
		mExeException = exeException;
	}

}
