package ore.threading;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;


public class EventThread extends Thread {
	
	private ConcurrentLinkedQueue<Event> mEventQueue = new ConcurrentLinkedQueue<Event>();
	private Semaphore mEventQueueSemaphore = new Semaphore(0);
	
	public void postEvent(Event event) {
		mEventQueue.add(event);
		mEventQueueSemaphore.release();
	}
	
	public EventThread() {		
	}
	
	
	protected boolean processEvent(Event e) {
		return false;
	}
	
	@Override
	protected void threadRun() {
		while (!mThread.isInterrupted()) {
			try {
				mEventQueueSemaphore.acquire();
				Event e = mEventQueue.poll();
				processEvent(e);
			} catch (InterruptedException e) {
				mThread.interrupt();
			}
		}
	}

}
