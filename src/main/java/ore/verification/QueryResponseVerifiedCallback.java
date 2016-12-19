package ore.verification;

import ore.threading.Callback;

public interface QueryResponseVerifiedCallback extends Callback {
	
	public void queryResponseVerified(QueryResultVerificationReport verificationReport);

}
