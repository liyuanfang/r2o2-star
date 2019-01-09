package org.semanticweb.ore.execution;

import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.threading.Callback;

public interface ReasonerQueryExecutedCallback extends Callback {
	
	public void reasonerQueryExecuted(QueryResponse response);

}
