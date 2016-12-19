package ore.networking;

import ore.networking.messages.Message;

import java.util.Collection;

public interface MessageSerializer {
	
	public Collection<String> serializeMessage(Message message);
	
}
