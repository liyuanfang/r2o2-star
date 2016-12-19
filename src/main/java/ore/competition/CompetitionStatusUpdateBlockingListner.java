package ore.competition;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CompetitionStatusUpdateBlockingListner implements CompetitionStatusUpdateListner {
	
	protected Semaphore mBlockingSemaphore = new Semaphore(0);
		
	public void notifyUpdated() {	
		if (mBlockingSemaphore.availablePermits() <= 1) {
			mBlockingSemaphore.release();
		}
	}
	
	public void waitUpdated() {
		try {
			mBlockingSemaphore.acquire();
		} catch (InterruptedException e) {
		}
	}
	

	public void waitUpdated(long time) {
		try {
			mBlockingSemaphore.tryAcquire(time, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
	}
	
}
