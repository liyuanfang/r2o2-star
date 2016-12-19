package ore.verification;

import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.querying.QueryResultData;

public interface QueryResultNormaliser {
	
	public QueryResultData getNormalisedResult(Query query, QueryResponse response);

}
