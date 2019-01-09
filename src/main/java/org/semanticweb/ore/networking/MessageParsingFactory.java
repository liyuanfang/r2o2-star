package org.semanticweb.ore.networking;

import java.util.Collection;

import org.semanticweb.ore.networking.messages.Message;

public interface MessageParsingFactory {
	
	public Message createParsedMessage(Collection<String> stringList);
	
}
