package ore.verification;

import ore.configuration.Config;
import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.threading.Event;
import ore.threading.EventThread;
import ore.verification.events.QueryResponseVerifyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResponseVerificationHandler extends EventThread {
	
	final private static Logger mLogger = LoggerFactory.getLogger(QueryResponseVerificationHandler.class);
	
	protected QueryResultVerifier mResultVerifier = null;
	protected Config mConfig = null;


	public void verifyQueryResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResponseVerifiedCallback callback) {
		postEvent(new QueryResponseVerifyEvent(query, reasoner, queryResponse, callback));
	}
	

	public QueryResultVerificationReport verifyQueryResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse) {
		QueryResultVerificationReport verificationReport = null;
		QueryResponseVerifiedBlockingCallback callback = new QueryResponseVerifiedBlockingCallback();
		verifyQueryResponse(reasoner,query,queryResponse,callback);
		try {
			callback.waitForCallback();
			verificationReport = callback.getVerificationReport();
		} catch (InterruptedException e) {
			mLogger.warn("Waiting for verification of query response for query '{}' for reasoner '{}' interrupted.",query.toString(),reasoner.toString());
		}
		return verificationReport;
	}
		
	
	public QueryResponseVerificationHandler(QueryResultVerifier resultVerifier, Config config) {
		mConfig = config;
		mResultVerifier = resultVerifier;
		
		startThread();
	}
	
	
	protected void threadStart() {
		super.threadStart();
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}	
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof QueryResponseVerifyEvent) {	
				QueryResponseVerifyEvent qrve = (QueryResponseVerifyEvent)e;
				
				QueryResultVerificationReport verificationReport = mResultVerifier.verifyResponse(qrve.getReasonerDescription(), qrve.getQuery(), qrve.getQueryResponse());
				QueryResponseVerifiedCallback callback = qrve.getCallback();
				if (callback != null) {
					callback.queryResponseVerified(verificationReport);
				}
				
				return true;
			}
		}
		return false;
	}
	
}
