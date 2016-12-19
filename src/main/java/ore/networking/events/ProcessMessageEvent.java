package ore.networking.events;

import ore.networking.messages.Message;
import ore.threading.Event;

public class ProcessMessageEvent implements Event {
	
	protected Message mMessage = null;
	
	public ProcessMessageEvent(Message message) {
		mMessage = message;
	}
	
	public Message getMessage() {
		return mMessage;
	}

}
