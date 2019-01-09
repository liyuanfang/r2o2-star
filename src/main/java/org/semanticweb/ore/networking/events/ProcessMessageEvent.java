package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.messages.Message;
import org.semanticweb.ore.threading.Event;

public class ProcessMessageEvent implements Event {
	
	protected Message mMessage = null;
	
	public ProcessMessageEvent(Message message) {
		mMessage = message;
	}
	
	public Message getMessage() {
		return mMessage;
	}

}
