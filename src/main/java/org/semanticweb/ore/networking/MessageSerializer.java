package org.semanticweb.ore.networking;

import java.util.Collection;

import org.semanticweb.ore.networking.messages.Message;

public interface MessageSerializer {
	
	public Collection<String> serializeMessage(Message message);
	
}
