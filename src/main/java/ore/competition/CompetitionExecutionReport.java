package ore.competition;

import ore.networking.ExecutionReport;
import ore.querying.QueryResponse;
import ore.verification.QueryResultVerificationReport;

public class CompetitionExecutionReport implements ExecutionReport {
	
	protected CompetitionExecutionTask mTask = null;
	protected boolean mExecuted = false;
	protected boolean mOutOfTime = false;
	protected QueryResponse mQueryResponse = null;
	protected QueryResultVerificationReport mVerificationReport = null;
	
	public CompetitionExecutionReport(CompetitionExecutionTask task, boolean executed) {
		mTask = task;
		mExecuted = executed;			
	}
	
	
	public CompetitionExecutionReport(CompetitionExecutionTask task, boolean executed, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {
		mTask = task;
		mExecuted = executed;	
		mQueryResponse = queryResponse;
		mVerificationReport = verificationReport;
	}	
	
	
	public CompetitionExecutionReport(CompetitionExecutionTask task, boolean executed, boolean outOfDate) {
		mTask = task;
		mExecuted = executed;	
		mOutOfTime = outOfDate;
	}		

	public boolean isExecuted() {
		return mExecuted;
	}
	
	
	public boolean isOutOfTime() {
		return mOutOfTime;
	}

	public CompetitionExecutionTask getTask() {
		return mTask;
	}		
	
	public void setQueryResponse(QueryResponse queryResponse) {
		mQueryResponse = queryResponse;
	}	
	
	public QueryResponse getQueryResponse() {
		return mQueryResponse;
	}
	
	public QueryResultVerificationReport getVerificationReport() {
		return mVerificationReport;
	}
		
	public void setVerificationReport(QueryResultVerificationReport verificationReport) {
		mVerificationReport = verificationReport;
	}	
	
}
