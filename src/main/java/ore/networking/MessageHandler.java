package ore.networking;

import ore.networking.messages.Message;

public interface MessageHandler {
	
	public void handleMessage(Message message);
	
}
