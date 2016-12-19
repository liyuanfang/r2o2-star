package ore.networking.messages;

import java.util.HashMap;

public class ProcessEvaluationTaskMessage extends Message {
	

	
	public ProcessEvaluationTaskMessage(HashMap<String,String> keyValueMap) {
		super(MessageType.MESSAGE_TYPE_PROCESS_EVALUATION_TASK);
		mKeyValueMap.put("ReasonerName",keyValueMap.get("ReasonerName"));
		mKeyValueMap.put("QueryName",keyValueMap.get("QueryName"));
		mKeyValueMap.put("OutputName",keyValueMap.get("OutputName"));
		mKeyValueMap.put("ExecutionTimeout",keyValueMap.get("ExecutionTimeout"));
		mKeyValueMap.put("MemoryLimit",keyValueMap.get("MemoryLimit"));
	}
	
	public ProcessEvaluationTaskMessage(String reasonerName, String queryName, String outputName, long executionTimeout, long memoryLimit) {
		super(MessageType.MESSAGE_TYPE_PROCESS_EVALUATION_TASK);
		mKeyValueMap.put("ReasonerName",reasonerName);
		mKeyValueMap.put("QueryName",queryName);
		mKeyValueMap.put("OutputName",outputName);
		mKeyValueMap.put("ExecutionTimeout", String.valueOf(executionTimeout));
		mKeyValueMap.put("MemoryLimit", String.valueOf(memoryLimit));
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
	
	public long getExecutionTimeout() {
		return Long.valueOf(mKeyValueMap.get("ExecutionTimeout"));
	}	

	public long getMemoryLimit() {
		return Long.valueOf(mKeyValueMap.get("MemoryLimit"));
	}	
	

}
