package org.semanticweb.ore.networking.messages;

public class KeepConnectionAliveMessage extends Message {
	
	
	public KeepConnectionAliveMessage() {
		super(MessageType.MESSAGE_TYPE_KEEP_CONNECTION_ALIVE);
	}

}
