package org.semanticweb.ore.competition;

import org.joda.time.DateTime;

public class CompetitionStatus {
	
	protected DateTime mTimeStamp = null;
	protected String mCompetitionName = null;
	protected String mCompetitionSourceString = null;
	protected int mExecutionHandlerCount = 0;
	protected int mReasonerCount = 0;
	protected int mQueryCount = 0;
	protected long mExecutionTime = 0;	
	protected DateTime mStartingDate = null;
	protected DateTime mEndingDate = null;
	protected CompetitionExecutionState mState = CompetitionExecutionState.COMPETITION_EXECUTION_STATE_QUEUED;

	
	public CompetitionStatus(CompetitionStatus status) {
		mCompetitionName = status.mCompetitionName;
		mCompetitionSourceString = status.mCompetitionSourceString;
		mExecutionHandlerCount = status.mExecutionHandlerCount;
		mReasonerCount = status.mReasonerCount;
		mQueryCount = status.mQueryCount;
		mState = status.mState;
		mEndingDate = status.mEndingDate;
		mStartingDate = status.mStartingDate;
		mTimeStamp = status.mTimeStamp;
		mExecutionTime = status.mExecutionTime;
	}
	
	
	public CompetitionStatus(String competitionName, String competitionSourceString, CompetitionExecutionState state, DateTime startingDate, DateTime endingDate, int executionHandlerCount, int reasonerCount, int queryCount, DateTime timeStamp, long executionTime) {
		mCompetitionName = competitionName;
		mCompetitionSourceString = competitionSourceString;
		mExecutionHandlerCount = executionHandlerCount;
		mReasonerCount = reasonerCount;
		mQueryCount = queryCount;
		mState = state;
		mStartingDate = startingDate;
		mEndingDate = endingDate;
		mTimeStamp = timeStamp;
		mExecutionTime = executionTime;
	}
	
	
	public CompetitionStatus(Competition competition, CompetitionExecutionState state, int executionHandlerCount, int reasonerCount, int queryCount, DateTime timeStamp, long executionTime) {
		mCompetitionName = competition.getCompetitionName();
		mCompetitionSourceString = competition.getCompetitionSourceString().getAbsoluteFilePathString();
		mExecutionHandlerCount = executionHandlerCount;
		mReasonerCount = reasonerCount;
		mQueryCount = queryCount;
		mState = state;
		mStartingDate = competition.getDesiredStartingDate();
		mEndingDate = competition.getDesiredEndingDate();
		mTimeStamp = timeStamp;
		mExecutionTime = executionTime;
	}

	public void setTimeStamp(DateTime dateTimeStamp) {
		mTimeStamp = dateTimeStamp;
	}
		
	public long getExecutionTime() {
		return mExecutionTime;
	}

	public DateTime getTimeStamp() {
		return mTimeStamp;
	}
	
	public String getCompetitionName() {
		return mCompetitionName;
	}

	public String getCompetitionSourceString() {
		return mCompetitionSourceString;
	}
	
	public int getExecutionHandlerCount() {
		return mExecutionHandlerCount;
	}
	
	public int getReasonerCount() {
		return mReasonerCount;
	}	

	public int getQueryCount() {
		return mQueryCount;
	}	
	

	public DateTime getStartingDate() {
		return mStartingDate;
	}	

	public DateTime getEndingDate() {
		return mEndingDate;
	}	
	
	public CompetitionExecutionState getExecutionState() {
		return mState;
	}
	
}
