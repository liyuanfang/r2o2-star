package ore.verification;

import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;

public interface QueryResultVerifier {
	
	public QueryResultVerificationReport verifyResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse);

}
