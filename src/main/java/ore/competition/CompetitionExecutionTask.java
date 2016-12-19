package ore.competition;

import ore.interfacing.ReasonerDescription;
import ore.networking.ExecutionTask;
import ore.querying.Query;

public class CompetitionExecutionTask implements ExecutionTask {
	
	protected ReasonerDescription mReasoner = null;
	protected Query mQuery = null;
	protected String mOutputString = null;
	protected long mExecutionTimeout = 0;
	protected long mMemoryLimit = 0;
	
	public CompetitionExecutionTask(ReasonerDescription reasoner, Query query, String outputString, long executionTimeout, long memoryLimit) {
		mExecutionTimeout = executionTimeout;
		mReasoner = reasoner;
		mQuery = query;			
		mOutputString = outputString;
		mMemoryLimit = memoryLimit;
	}

	public ReasonerDescription getReasonerDescription() {
		return mReasoner;
	}

	public Query getQuery() {
		return mQuery;
	}		

	public String getOutputString() {
		return mOutputString;
	}		
	
	public long getExecutionTimeout() {
		return mExecutionTimeout;
	}

	public long getMemoryLimit() {
		return mMemoryLimit;
	}
}
