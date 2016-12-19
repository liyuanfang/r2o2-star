package ore.evaluation;

import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.verification.QueryResultVerificationReport;

public class QueryResultStorageItem {
	private ReasonerDescription mReasoner = null;
	private Query mQuery = null;
	private QueryResponse mQueryResponse = null;
	private QueryResultVerificationReport mVerificationReport = null;
	
	
	public QueryResultStorageItem() {		
	}
	
	public QueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {	
		mReasoner = reasoner;
		mQuery = query;
		mQueryResponse = queryResponse;
		mVerificationReport = verificationReport;
	}
	
	public ReasonerDescription getReasoner() {
		return mReasoner;
	}
	
	public Query getQuery() {
		return mQuery;
	}
	
	public QueryResponse getQueryResponse() {
		return mQueryResponse;
	}
	
	public QueryResultVerificationReport getVerificationReport() {
		return mVerificationReport;
	}
}
