package ore.verification.events;

import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.threading.Event;
import ore.verification.QueryResponseVerifiedCallback;

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
