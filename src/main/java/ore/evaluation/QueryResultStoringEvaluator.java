package ore.evaluation;

import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.verification.QueryResultVerificationReport;

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
