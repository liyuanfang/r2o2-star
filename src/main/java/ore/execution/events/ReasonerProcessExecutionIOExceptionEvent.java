package ore.execution.events;

import ore.threading.Event;

import java.io.IOException;

public class ReasonerProcessExecutionIOExceptionEvent implements Event {
	
	private IOException mIOException = null;
	
	public IOException getException() {
		return mIOException;
	}
	
	public ReasonerProcessExecutionIOExceptionEvent(IOException ioException) {
		mIOException = ioException;
	}

}
