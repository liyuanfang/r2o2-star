package org.semanticweb.ore.networking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.semanticweb.ore.networking.messages.FailedEvaluationTaskMessage;
import org.semanticweb.ore.networking.messages.KeepConnectionAliveMessage;
import org.semanticweb.ore.networking.messages.Message;
import org.semanticweb.ore.networking.messages.MessageType;
import org.semanticweb.ore.networking.messages.ProcessEvaluationTaskMessage;
import org.semanticweb.ore.networking.messages.ProcessedEvaluationTaskMessage;
import org.semanticweb.ore.networking.messages.RequestEvaluationTaskMessage;
import org.semanticweb.ore.networking.messages.UpdateCompetitionEvaluationStatusMessage;
import org.semanticweb.ore.networking.messages.UpdateCompetitionReasonerProgressStatusMessage;
import org.semanticweb.ore.networking.messages.UpdateCompetitionStatusMessage;

public class DefaultMessageParsingFactory implements MessageParsingFactory {
	
	public Message createParsedMessage(Collection<String> stringList) {		
		Message message = null;
		
		if (stringList != null) {
			
			MessageType messageType = null;
			
			Iterator<String> iterator = stringList.iterator();
			if (iterator.hasNext()) {
				String messageTypeLineString = iterator.next();
				String[] stringArray = messageTypeLineString.split("\t");
				if (stringArray.length > 1) {
					String messageTypeString = stringArray[1].trim();
					messageType = MessageType.valueOf(messageTypeString);
				}
			}
			
			if (messageType != null) {
				
				HashMap<String,String> keyValueMap = new HashMap<String,String>(); 
				
				while (iterator.hasNext()) {
					String keyValueLineString = iterator.next();
					String[] stringArray = keyValueLineString.split("\t");
					if (stringArray.length > 1) {
						String keyString = stringArray[0].trim();
						String valueString = stringArray[1].trim();
						keyValueMap.put(keyString, valueString);
					}
					
				}
				
				
				
				if (messageType == MessageType.MESSAGE_TYPE_REQUEST_EVALUATION_TASK) {
					message = new RequestEvaluationTaskMessage();
				} else if (messageType == MessageType.MESSAGE_TYPE_PROCESS_EVALUATION_TASK) {
					message = new ProcessEvaluationTaskMessage(keyValueMap);
				} else if (messageType == MessageType.MESSAGE_TYPE_PROCESSED_EVALUATION_TASK) {
					message = new ProcessedEvaluationTaskMessage(keyValueMap);
				} else if (messageType == MessageType.MESSAGE_TYPE_KEEP_CONNECTION_ALIVE) {
					message = new KeepConnectionAliveMessage();
				} else if (messageType == MessageType.MESSAGE_TYPE_FAILED_EVALUATION_TASK) {
					message = new FailedEvaluationTaskMessage(keyValueMap);
				} else if (messageType == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_STATUS) {
					message = new UpdateCompetitionStatusMessage(keyValueMap);
				} else if (messageType == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_EVALUATION_STATUS) {
					message = new UpdateCompetitionEvaluationStatusMessage(keyValueMap);
				} else if (messageType == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_REASONER_PROGRESS_STATUS) {
					message = new UpdateCompetitionReasonerProgressStatusMessage(keyValueMap);
				}
				
			}
			
		}
		
		return message;
	}
	
}
