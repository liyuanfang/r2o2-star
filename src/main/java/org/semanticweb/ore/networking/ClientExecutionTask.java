package org.semanticweb.ore.networking;

import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.networking.messages.ProcessEvaluationTaskMessage;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.verification.QueryResultVerificationReport;

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
