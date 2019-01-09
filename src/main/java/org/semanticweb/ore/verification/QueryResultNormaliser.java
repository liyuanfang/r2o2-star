package org.semanticweb.ore.verification;

import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.querying.QueryResultData;

public interface QueryResultNormaliser {
	
	public QueryResultData getNormalisedResult(Query query, QueryResponse response);

}
