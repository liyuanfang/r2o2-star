package ore.evaluation;

import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.verification.QueryResultVerificationReport;

public interface QueryResultEvaluator {
	
	public void evaluateQueryResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport);

}
