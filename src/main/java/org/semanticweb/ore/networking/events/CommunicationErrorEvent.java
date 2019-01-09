package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.threading.Event;

public class CommunicationErrorEvent implements Event {
	
	protected Throwable mThrowable = null;
	
	public CommunicationErrorEvent(Throwable exception) {
		mThrowable = exception;
	}
	
	public Throwable getException() {
		return mThrowable;
	}

}
