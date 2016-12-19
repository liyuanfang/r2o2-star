package ore.execution;

import ore.querying.QueryResponse;
import ore.threading.Callback;

public interface ReasonerQueryExecutedCallback extends Callback {
	
	public void reasonerQueryExecuted(QueryResponse response);

}
