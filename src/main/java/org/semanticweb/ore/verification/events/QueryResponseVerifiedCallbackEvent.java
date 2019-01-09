package org.semanticweb.ore.verification.events;

import org.semanticweb.ore.networking.ClientExecutionTask;
import org.semanticweb.ore.threading.Event;
import org.semanticweb.ore.threading.EventThread;
import org.semanticweb.ore.verification.QueryResponseVerifiedCallback;
import org.semanticweb.ore.verification.QueryResultVerificationReport;

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
