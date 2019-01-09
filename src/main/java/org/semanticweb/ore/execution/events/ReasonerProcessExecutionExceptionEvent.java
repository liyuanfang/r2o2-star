package org.semanticweb.ore.execution.events;

import org.apache.commons.exec.ExecuteException;
import org.semanticweb.ore.threading.Event;

public class ReasonerProcessExecutionExceptionEvent implements Event {
	
	private ExecuteException mExeException = null;
	
	public ExecuteException getException() {
		return mExeException;
	}
	
	public ReasonerProcessExecutionExceptionEvent(ExecuteException exeException) {
		mExeException = exeException;
	}

}
