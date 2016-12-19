package ore.networking;

import ore.interfacing.ReasonerDescription;
import ore.networking.messages.ProcessEvaluationTaskMessage;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.verification.QueryResultVerificationReport;

public class ClientExecutionTask {
	
	private ProcessEvaluationTaskMessage mMessage = null;
	private ReasonerDescription mReasoner = null;
	private Query mQuery = null;
	private String mResponseDestinationString = null;
	
	private QueryResponse mQueryResponse = null;
	private QueryResultVerificationReport mVerificationReport = null;
	
	public ProcessEvaluationTaskMessage getProcessEvaluationTaskMessage() {
		return mMessage;
	}
	
	public ReasonerDescription getReasoner() {
		return mReasoner;
	}
	
	public Query getQuery() {
		return mQuery;
	}
	
	public String getResponseDestinationString() {
		return mResponseDestinationString;
	}
	
	public QueryResponse getQueryResponse() {
		return mQueryResponse;
	}
	
	public QueryResultVerificationReport getVerificationReport() {
		return mVerificationReport;
	}
	
	public void setQueryResponse(QueryResponse response) {
		mQueryResponse = response;
	}
	
	public void setVerificationReport(QueryResultVerificationReport verificationReport) {
		mVerificationReport = verificationReport;
	}
	
	public ClientExecutionTask(ProcessEvaluationTaskMessage message, ReasonerDescription reasoner, Query query, String responseDestinationString) {
		mMessage = message;
		mReasoner = reasoner;
		mQuery = query;
	}
	
}
