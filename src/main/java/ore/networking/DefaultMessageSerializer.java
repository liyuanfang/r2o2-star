package ore.networking;

import ore.networking.messages.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

public class DefaultMessageSerializer implements MessageSerializer {
	
	public Collection<String> serializeMessage(Message message) {
		ArrayList<String> dataStringList = new ArrayList<String>();
		
		dataStringList.add("MessageType\t"+message.getMessageType().toString());
		
		Iterator<Entry<String,String>> keyValueIterator = message.getEntryIterator();
		while (keyValueIterator.hasNext()) {
			Entry<String,String> keyValueEntry = keyValueIterator.next();
			dataStringList.add(keyValueEntry.getKey()+"\t"+keyValueEntry.getValue());
		}
		
		return dataStringList;
	}
	
}
