package ore.competition;

import ore.utilities.FilePathString;
import org.joda.time.DateTime;

import java.util.List;

public class Competition {
	
	protected String mCompetitionName = null;
	protected FilePathString mCompetitionSourceString = null;
	protected String mQueryFilterString = null;
	protected FilePathString mQuerySortingFilePathString = null;
	protected String mOutputString = null;
	protected List<String> mReasonerList = null;
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	protected DateTime mStartingDate = null;
	protected DateTime mEndingDate = null;
	protected boolean mAllowRunningWithinDate = false;
	
	public Competition(String competitionName, FilePathString competitionSourceString, List<String> reasonerList) {
		mCompetitionName = competitionName;
		mCompetitionSourceString = competitionSourceString;
		mReasonerList = reasonerList;
	}

	public Competition(Competition comp) {
		mCompetitionName = comp.mCompetitionName;
		mCompetitionSourceString = comp.mCompetitionSourceString;
		mQueryFilterString = comp.mQueryFilterString;
		mQuerySortingFilePathString = comp.mQuerySortingFilePathString;
		mOutputString = comp.mOutputString;
		mReasonerList = comp.mReasonerList;
		mExecutionTimeout = comp.mExecutionTimeout;
		mProcessingTimeout = comp.mProcessingTimeout;
		mStartingDate = comp.mStartingDate;
		mEndingDate = comp.mEndingDate;
		mAllowRunningWithinDate = comp.mAllowRunningWithinDate;
	}
	
	public void setQueryFilterString(String queryFilterString) {
		mQueryFilterString = queryFilterString;
	}
	
	public void setOutputPathString(String outputPathString) {
		mOutputString = outputPathString;
	}
	
	public String getCompetitionName() {
		return mCompetitionName;
	}	

	public FilePathString getCompetitionSourceString() {
		return mCompetitionSourceString;
	}	

	public String getQueryFilterString() {
		return mQueryFilterString;
	}
	
	public FilePathString getQuerySortingFilePathString() {
		return mQuerySortingFilePathString;
	}	
	
	public void setQuerySortingFilePathString(FilePathString sortingFilePathString) {
		mQuerySortingFilePathString = sortingFilePathString;
	}

	public String getOutputString() {
		return mOutputString;
	}	
	
	public List<String> getReasonerList() {
		return mReasonerList;
	}
	
	public long getExecutionTimeout() {
		return mExecutionTimeout;
	}

	public long getProcessingTimeout() {
		return mProcessingTimeout;
	}
	
	public void setProcessingTimeout(long timeout) {
		mProcessingTimeout = timeout;
	}
	
	public void setExecutionTimeout(long timeout) {
		mExecutionTimeout = timeout;
	}
	
	public DateTime getDesiredStartingDate() {
		return mStartingDate;
	}

	public DateTime getDesiredEndingDate() {
		return mEndingDate;
	}

	public boolean isRunningOnlyAllowedWithinDates() {
		return mAllowRunningWithinDate;
	}
	
	public void setDesiredStartingDate(DateTime date) {
		mStartingDate = date;
	}
	
	public void setDesiredEndingDate(DateTime date) {
		mEndingDate = date;
	}
	
	public void setRunningOnlyAllowedWithinDates(boolean onlyWithinDates) {
		mAllowRunningWithinDate = onlyWithinDates;
	}
}
