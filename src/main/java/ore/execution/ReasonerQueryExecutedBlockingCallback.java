package ore.execution;

import ore.querying.QueryResponse;

import java.util.concurrent.Semaphore;

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
