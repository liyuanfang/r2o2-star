package ore.verification;

import ore.querying.Query;
import ore.querying.QueryResponse;

public interface QueryResultNormaliserFactory {
	
	public QueryResultNormaliser createQueryResultNormaliser(Query query, QueryResponse queryResponse);

}
