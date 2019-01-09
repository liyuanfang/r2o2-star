package org.semanticweb.ore.verification;

import org.semanticweb.ore.threading.Callback;

public interface QueryResponseVerifiedCallback extends Callback {
	
	public void queryResponseVerified(QueryResultVerificationReport verificationReport);

}
