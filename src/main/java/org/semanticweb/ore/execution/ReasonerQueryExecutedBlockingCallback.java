package org.semanticweb.ore.execution;

import java.util.concurrent.Semaphore;

import org.semanticweb.ore.querying.QueryResponse;

public class ReasonerQueryExecutedBlockingCallback implements ReasonerQueryExecutedCallback {
	private volatile QueryResponse mResponse = null;
	private Semaphore mBlockSemaphore = new Semaphore(0);
	
	public void reasonerQueryExecuted(QueryResponse response) {
		mResponse = response;
		mBlockSemaphore.release();
	}
	
	public void waitForCallback() throws InterruptedException {
		mBlockSemaphore.acquire();
	}
	
	public QueryResponse getQueryResponse() {
		return mResponse;
	}

}
