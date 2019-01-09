package org.semanticweb.ore.evaluation;

import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.verification.QueryResultVerificationReport;

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
