package org.semanticweb.ore.evaluation;

import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;

public interface QueryResultStorageItemVisitor {
	
	public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item);
	
}
