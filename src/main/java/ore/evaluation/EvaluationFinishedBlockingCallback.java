package ore.evaluation;

import java.util.concurrent.Semaphore;

public class EvaluationFinishedBlockingCallback implements EvaluationFinishedCallback {
	
	private Semaphore mBlockSemaphore = new Semaphore(0);
	
	public void waitForCallback() throws InterruptedException {
		mBlockSemaphore.acquire();
	}	

	@Override
	public void finishedEvaluation() {
		mBlockSemaphore.release();
	}

}
