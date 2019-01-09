package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.messages.Message;
import org.semanticweb.ore.threading.Event;

public class SendMessageEvent implements Event {
	
	protected Message mMessage = null;
	
	public SendMessageEvent(Message message) {
		mMessage = message;
	}
	
	public Message getMessage() {
		return mMessage;
	}

}
