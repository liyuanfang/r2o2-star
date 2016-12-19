package ore.threading;

public class Thread implements Runnable {

	protected java.lang.Thread mThread = null;
	
	public boolean startThread() {		
		if (mThread == null) {
			mThread = new java.lang.Thread(this);			
		}
		if (!mThread.isAlive()) {
			mThread.start();
			return true;
		}
		return false;
	}
	
	
	public boolean stopThread() {		
		if (mThread != null && mThread.isAlive()) {
			mThread.interrupt();			
		}
		return false;
	}
	

	@Override
	public void run() {	
		threadStart();
		threadRun();
		threadFinish();
	}
	
	protected void threadStart() {	
	}	

	protected void threadFinish() {	
	}	
	
	protected void threadRun() {	
	}	


}
