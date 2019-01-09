package org.semanticweb.ore.evaluation;

import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.verification.QueryResultVerificationReport;

public interface QueryResultEvaluator {
	
	public void evaluateQueryResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport);

}
