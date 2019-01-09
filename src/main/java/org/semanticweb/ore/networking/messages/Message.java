package org.semanticweb.ore.networking.messages;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public abstract class Message {
	
	protected MessageType mMessageType = null;
	protected HashMap<String,String> mKeyValueMap = new HashMap<String,String>(); 
	
	public Message(MessageType type) {
		mMessageType = type;
	}
	
	public MessageType getMessageType() {
		return mMessageType;
	}
	
	
	public void addKeyValue(String key, String value) {
		mKeyValueMap.put(key, value);
	}
	
	public String getKeyValue(String key) {
		return mKeyValueMap.get(key);
	}
	
	public Iterator<Entry<String,String>> getEntryIterator() {
		return mKeyValueMap.entrySet().iterator();
	}

}
