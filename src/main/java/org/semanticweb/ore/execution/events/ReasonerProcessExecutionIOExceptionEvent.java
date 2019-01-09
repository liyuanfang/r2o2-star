package org.semanticweb.ore.execution.events;

import java.io.IOException;

import org.semanticweb.ore.threading.Event;

public class ReasonerProcessExecutionIOExceptionEvent implements Event {
	
	private IOException mIOException = null;
	
	public IOException getException() {
		return mIOException;
	}
	
	public ReasonerProcessExecutionIOExceptionEvent(IOException ioException) {
		mIOException = ioException;
	}

}
