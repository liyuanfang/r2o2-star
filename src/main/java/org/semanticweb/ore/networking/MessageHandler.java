package org.semanticweb.ore.networking;

import org.semanticweb.ore.networking.messages.Message;

public interface MessageHandler {
	
	public void handleMessage(Message message);
	
}
