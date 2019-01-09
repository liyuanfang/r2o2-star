package org.semanticweb.ore.evaluation;

import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.verification.QueryResultVerificationReport;

public class QueryResultStoringEvaluator implements QueryResultEvaluator {
	
	private QueryResultStorage mStorage = null;	
	
	public QueryResultStoringEvaluator() {
		mStorage = new QueryResultStorage();
	}
	
	public QueryResultStoringEvaluator(QueryResultStorage storage) {
		mStorage = storage;
		if (mStorage == null) {
			mStorage = new QueryResultStorage();
		}
	}
	
	public QueryResultStorage getQueryResultStorage() {
		return mStorage;
	}
	
	public void evaluateQueryResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {
		mStorage.storeQueryResult(reasoner, query, queryResponse, verificationReport);
	}	
	
}
