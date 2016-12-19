package ore.networking.events;

import ore.networking.messages.Message;
import ore.threading.Event;

public class SendMessageEvent implements Event {
	
	protected Message mMessage = null;
	
	public SendMessageEvent(Message message) {
		mMessage = message;
	}
	
	public Message getMessage() {
		return mMessage;
	}

}
