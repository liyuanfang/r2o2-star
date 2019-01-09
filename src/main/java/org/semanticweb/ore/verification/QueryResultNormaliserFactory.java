package org.semanticweb.ore.verification;

import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;

public interface QueryResultNormaliserFactory {
	
	public QueryResultNormaliser createQueryResultNormaliser(Query query, QueryResponse queryResponse);

}
