package org.semanticweb.ore.verification;

import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;

public interface QueryResultVerifier {
	
	public QueryResultVerificationReport verifyResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse);

}
