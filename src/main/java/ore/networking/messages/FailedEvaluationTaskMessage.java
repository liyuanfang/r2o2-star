package ore.networking.messages;

import java.util.HashMap;

public class FailedEvaluationTaskMessage extends Message {
		
	public FailedEvaluationTaskMessage(HashMap<String,String> keyValueMap) {
		super(MessageType.MESSAGE_TYPE_FAILED_EVALUATION_TASK);
		mKeyValueMap.put("ReasonerName",keyValueMap.get("ReasonerName"));
		mKeyValueMap.put("QueryName",keyValueMap.get("QueryName"));
		mKeyValueMap.put("OutputName",keyValueMap.get("OutputName"));		
	}
	
	public FailedEvaluationTaskMessage(String reasonerName, String queryName, String outputName) {
		super(MessageType.MESSAGE_TYPE_FAILED_EVALUATION_TASK);
		mKeyValueMap.put("ReasonerName",reasonerName);
		mKeyValueMap.put("QueryName",queryName);
		mKeyValueMap.put("OutputName",outputName);
	}
	
	
	public String getReasonerString() {
		return mKeyValueMap.get("ReasonerName");
	}
	
	public String getQueryString() {
		return mKeyValueMap.get("QueryName");
	}
	
	public String getOutputString() {
		return mKeyValueMap.get("OutputName");
	}
	
	
}
