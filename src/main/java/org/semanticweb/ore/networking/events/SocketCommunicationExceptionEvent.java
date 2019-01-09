package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.threading.Event;

public class SocketCommunicationExceptionEvent implements Event {
	
	protected Throwable mThrowable = null;
	
	public SocketCommunicationExceptionEvent(Throwable exception) {
		mThrowable = exception;
	}
	
	public Throwable getException() {
		return mThrowable;
	}

}
