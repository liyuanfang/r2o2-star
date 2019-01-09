package org.semanticweb.ore.verification.events;

import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.threading.Event;
import org.semanticweb.ore.verification.QueryResponseVerifiedCallback;

public class QueryResponseVerifyEvent implements Event {
	
	private ReasonerDescription mReasonerDescription = null;
	private Query mQuery = null;
	private QueryResponseVerifiedCallback mCallback = null;
	private QueryResponse mQueryResponse = null;
	

	public ReasonerDescription getReasonerDescription() {
		return mReasonerDescription;
	}
	
	
	public Query getQuery() {
		return mQuery;
	}
	
	public QueryResponseVerifiedCallback getCallback() {
		return mCallback;
	}	
	
	public QueryResponse getQueryResponse() {
		return mQueryResponse;
	}
	
	
	public QueryResponseVerifyEvent(Query query, ReasonerDescription reasoner, QueryResponse queryResponse, QueryResponseVerifiedCallback callback) {
		mReasonerDescription = reasoner;
		mQuery = query;
		mCallback = callback;
		mQueryResponse = queryResponse;
	}

}
