package ore.verification;

import java.util.concurrent.Semaphore;

public class QueryResponseVerifiedBlockingCallback implements QueryResponseVerifiedCallback {
	private volatile QueryResultVerificationReport mVerificationReport = null;
	private Semaphore mBlockSemaphore = new Semaphore(0);
	
	public void waitForCallback() throws InterruptedException {
		mBlockSemaphore.acquire();
	}
	
	public QueryResultVerificationReport getVerificationReport() {
		return mVerificationReport;
	}

	@Override
	public void queryResponseVerified(QueryResultVerificationReport verificationReport) {
		mVerificationReport = verificationReport;
		mBlockSemaphore.release();
	}

}
