package org.semanticweb.ore.verification;

public class QueryResultVerificationReport {
	
	private QueryResultVerificationCorrectnessType mCorrectnessType = QueryResultVerificationCorrectnessType.QUERY_RESULT_UNKNOWN;
	private int mResultHashCode = 0;
	private boolean mSupported = false;
	
	public QueryResultVerificationCorrectnessType getCorrectnessType() {
		return mCorrectnessType;
	}
	
	public QueryResultVerificationReport(QueryResultVerificationCorrectnessType correctnessType, int resultHashCode, boolean supported) {
		mResultHashCode = resultHashCode;
		mCorrectnessType = correctnessType;
		mSupported = supported;
	}
	
	
	public boolean isSupported() {
		return mSupported;
	}
	
	public int getResultHashCode() {
		return mResultHashCode;
	}

}
