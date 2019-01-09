package org.semanticweb.ore.evaluation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.verification.QueryResultVerificationReport;

public class QueryResultStorage {
	
	private HashMap<ReasonerDescription,HashMap<Query,QueryResultStorageItem>> mReasonerQueryItemMap = new HashMap<ReasonerDescription,HashMap<Query,QueryResultStorageItem>>();
	private HashMap<Query,HashMap<ReasonerDescription,QueryResultStorageItem>> mQueryReasonerItemMap = new HashMap<Query,HashMap<ReasonerDescription,QueryResultStorageItem>>();
	
	
	private Collection<ReasonerDescription> mInitReasonerCollection = null;
	private Collection<Query> mInitQueryCollection = null;
	
	
	public void storeQueryResult(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {	
		QueryResultStorageItem item = new QueryResultStorageItem(reasoner,query,queryResponse,verificationReport);
		insertQueryResultStorageItem(reasoner,query,item);
	}
	
	
	public QueryResultStorageItem getQueryResultStorageItem(ReasonerDescription reasoner, Query query) {	
		HashMap<Query,QueryResultStorageItem> queryItemMap = mReasonerQueryItemMap.get(reasoner);
		if (queryItemMap != null) {
			return queryItemMap.get(query);			
		}
		return null;
	}


	
	public void insertQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item) {	
		HashMap<Query,QueryResultStorageItem> queryItemMap = mReasonerQueryItemMap.get(reasoner);
		if (queryItemMap == null) {
			queryItemMap = new HashMap<Query,QueryResultStorageItem>();
			mReasonerQueryItemMap.put(reasoner,queryItemMap);
		}
		queryItemMap.put(query,item);
		
		HashMap<ReasonerDescription,QueryResultStorageItem> reasonerItemMap = mQueryReasonerItemMap.get(query);
		if (reasonerItemMap == null) {
			reasonerItemMap = new HashMap<ReasonerDescription,QueryResultStorageItem>();
			mQueryReasonerItemMap.put(query,reasonerItemMap);
		}
		reasonerItemMap.put(reasoner,item);		
	}
	
	public void initReasonerQueryStorage(Collection<ReasonerDescription> reasonerCollection, Collection<Query> queryCollection) {	
		mInitReasonerCollection = reasonerCollection;
		mInitQueryCollection = queryCollection;
		for (ReasonerDescription reasoner : reasonerCollection) {
			for (Query query : queryCollection) {
				HashMap<Query,QueryResultStorageItem> queryItemMap = mReasonerQueryItemMap.get(reasoner);
				if (queryItemMap == null) {
					queryItemMap = new HashMap<Query,QueryResultStorageItem>();
					mReasonerQueryItemMap.put(reasoner,queryItemMap);
				}
				if (!queryItemMap.containsKey(query)) {
					queryItemMap.put(query,null);
				
					HashMap<ReasonerDescription,QueryResultStorageItem> reasonerItemMap = mQueryReasonerItemMap.get(query);
					if (reasonerItemMap == null) {
						reasonerItemMap = new HashMap<ReasonerDescription,QueryResultStorageItem>();
						mQueryReasonerItemMap.put(query,reasonerItemMap);
					}
					if (!reasonerItemMap.containsKey(reasoner)) {
						reasonerItemMap.put(reasoner,null);		
					}
				}

			}			
		}
	}
	
	
	public Collection<ReasonerDescription> getStoredReasonerCollection() {
		if (mInitReasonerCollection != null) {
			return mInitReasonerCollection;
		}
		return mReasonerQueryItemMap.keySet();
	}	
	
	public Collection<Query> getStoredQueryCollection() {
		if (mInitQueryCollection != null) {
			return mInitQueryCollection;
		}
		return mQueryReasonerItemMap.keySet();
	}

	public boolean visitStoredResultsForReasoner(ReasonerDescription reasoner, QueryResultStorageItemVisitor visitor) {
		HashMap<Query,QueryResultStorageItem> queryItemMap = mReasonerQueryItemMap.get(reasoner);
		if (queryItemMap == null) {
			return false;
		}
		for (Entry<Query, QueryResultStorageItem> entry : queryItemMap.entrySet()) {
			if (visitor != null) {
				visitor.visitQueryResultStorageItem(reasoner, entry.getKey(), entry.getValue());
			}
		}
		return true;
	}
	
	
	public boolean visitStoredResultsForQuery(Query query, QueryResultStorageItemVisitor visitor) {
		HashMap<ReasonerDescription,QueryResultStorageItem> reasonerItemMap = mQueryReasonerItemMap.get(query);
		if (reasonerItemMap == null) {
			return false;
		}
		for (Entry<ReasonerDescription, QueryResultStorageItem> entry : reasonerItemMap.entrySet()) {
			if (visitor != null) {
				visitor.visitQueryResultStorageItem(entry.getKey(), query, entry.getValue());
			}
		}
		return true;
	}
	
	
	
}
