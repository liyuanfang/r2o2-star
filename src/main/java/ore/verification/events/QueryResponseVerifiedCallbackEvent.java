package ore.verification.events;

import ore.networking.ClientExecutionTask;
import ore.threading.Event;
import ore.threading.EventThread;
import ore.verification.QueryResponseVerifiedCallback;
import ore.verification.QueryResultVerificationReport;

public class QueryResponseVerifiedCallbackEvent implements Event, QueryResponseVerifiedCallback {
	private volatile QueryResultVerificationReport mVerificationReport = null;
	private EventThread mPostEventThread = null;
	private ClientExecutionTask mExecutionTask = null;
	
	public QueryResultVerificationReport getVerificationReport() {
		return mVerificationReport;
	}
	
	public ClientExecutionTask getExecutionTask() {
		return mExecutionTask;
	}
	
	public QueryResponseVerifiedCallbackEvent(EventThread thread, ClientExecutionTask executionTask) {
		mPostEventThread = thread;
		mExecutionTask = executionTask;
	}

	@Override
	public void queryResponseVerified(QueryResultVerificationReport verificationReport) {
		mVerificationReport = verificationReport;
		mPostEventThread.postEvent(this);
	}

}
