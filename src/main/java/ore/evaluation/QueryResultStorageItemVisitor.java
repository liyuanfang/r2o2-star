package ore.evaluation;

import ore.interfacing.ReasonerDescription;
import ore.querying.Query;

public interface QueryResultStorageItemVisitor {
	
	public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item);
	
}
