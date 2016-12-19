package ore.competition;

import ore.interfacing.ReasonerDescription;
import org.joda.time.DateTime;

public class CompetitionReasonerProgressStatus {
	
	protected DateTime mTimeStamp = null;
	protected String mCompetitionName = null;
	protected String mCompetitionSourceString = null;
	protected String mReasonerName = null;
	protected String mReasonerSourceString = null;
	protected int mCorrectlyProcessedCount = 0;
	protected int mTotalProcessedCount = 0;
	protected long mCorrectlyProccessedTime = 0;
	protected long mTotalExecutionTime = 0;
	protected int mReasonerRank = 0;
	protected int mReasonerPosition = 0;
	protected int mOutOfTimeCount = 0;

	

	public CompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status) {
		mCompetitionName = status.mCompetitionName;
		mCompetitionSourceString = status.mCompetitionSourceString;
		mReasonerName = status.mReasonerName;
		mReasonerSourceString = status.mReasonerSourceString;
		mCorrectlyProcessedCount = status.mCorrectlyProcessedCount;
		mTotalProcessedCount = status.mTotalProcessedCount;
		mCorrectlyProccessedTime = status.mCorrectlyProccessedTime;
		mTotalExecutionTime = status.mTotalExecutionTime;
		mReasonerRank = status.mReasonerRank;
		mReasonerPosition = status.mReasonerPosition;	
		mOutOfTimeCount = status.mOutOfTimeCount;
		mTimeStamp = status.mTimeStamp;
	}	
	
	public CompetitionReasonerProgressStatus(String competitionName, String competitionSourceString, String reasonerName, String reasonerSourceString, int rank, int position, int correctlyProccessedCount, int totalProccessedCount, int outOfTimeCount, long correctlyProccessedTime, long totalExecutionTime, DateTime timeStamp) {
		mCompetitionName = competitionName;
		mCompetitionSourceString = competitionSourceString;
		mReasonerName = reasonerName;
		mReasonerSourceString = reasonerSourceString;
		mCorrectlyProcessedCount = correctlyProccessedCount;
		mTotalProcessedCount = totalProccessedCount;
		mCorrectlyProccessedTime = correctlyProccessedTime;
		mTotalExecutionTime = totalExecutionTime;
		mReasonerRank = rank;
		mReasonerPosition = position;
		mOutOfTimeCount = outOfTimeCount;
		mTimeStamp = timeStamp;
	}
	
	
	public CompetitionReasonerProgressStatus(Competition competition, ReasonerDescription reasoner, int rank, int position, int correctlyProccessedCount, int totalProccessedCount, int outOfTimeCount, long correctlyProccessedTime, long totalExecutionTime, DateTime timeStamp) {
		mCompetitionName = competition.getCompetitionName();
		mCompetitionSourceString = competition.getCompetitionSourceString().getAbsoluteFilePathString();
		mReasonerName = reasoner.getReasonerName();
		mReasonerSourceString = reasoner.getSourceFilePathString().getAbsoluteFilePathString();
		mCorrectlyProcessedCount = correctlyProccessedCount;
		mTotalProcessedCount = totalProccessedCount;
		mCorrectlyProccessedTime = correctlyProccessedTime;
		mTotalExecutionTime = totalExecutionTime;
		mReasonerRank = rank;
		mReasonerPosition = position;		
		mOutOfTimeCount = outOfTimeCount;
		mTimeStamp = timeStamp;
	}	

	public void setTimeStamp(DateTime dateTimeStamp) {
		mTimeStamp = dateTimeStamp;
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
	
	public String getReasonerName() {
		return mReasonerName;
	}	
	
	public String getReasonerSourceString() {
		return mReasonerSourceString;
	}		
	
	public int getCorrectlyProcessedCount() {
		return mCorrectlyProcessedCount;
	}
	
	public int getTotalProcessedCount() {
		return mTotalProcessedCount;
	}
	
	public long getCorrectlyProccessedTime() {
		return mCorrectlyProccessedTime;
	}
	
	public long getTotalExecutionTime() {
		return mTotalExecutionTime;
	}
	
	
	public int getReasonerRank() {
		return mReasonerRank;
	}
	

	public void setReasonerRank(int rank) {
		mReasonerRank = rank;
	}
	
	public int getReasonerPosition() {
		return mReasonerPosition;
	}

	public void setReasonerPosition(int position) {
		mReasonerPosition = position;
	}	
	
	
	public int getOutOfTimeCount() {
		return mOutOfTimeCount;
	}	
}
