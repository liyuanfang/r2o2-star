package ore.networking;

import ore.networking.messages.Message;

import java.util.Collection;

public interface MessageParsingFactory {
	
	public Message createParsedMessage(Collection<String> stringList);
	
}
