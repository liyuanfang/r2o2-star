package ore.networking.events;

import ore.threading.Event;

public class SocketCommunicationExceptionEvent implements Event {
	
	protected Throwable mThrowable = null;
	
	public SocketCommunicationExceptionEvent(Throwable exception) {
		mThrowable = exception;
	}
	
	public Throwable getException() {
		return mThrowable;
	}

}
