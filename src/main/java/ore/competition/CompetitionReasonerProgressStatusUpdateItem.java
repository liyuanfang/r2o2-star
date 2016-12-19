package ore.competition;

public class CompetitionReasonerProgressStatusUpdateItem {
	
	protected CompetitionReasonerProgressStatus mStatus = null;
	protected long mUpdateID = 0;
		
	public CompetitionReasonerProgressStatusUpdateItem(CompetitionReasonerProgressStatus status, long updateID) {
		mStatus = status;
		mUpdateID = updateID;
	}
	
	public long getUpdateID() {
		return mUpdateID;
	}
	
	public boolean isUpdated(long updateID) {
		if (updateID < mUpdateID) {
			return true;
		}
		return false;
	}
	
	public CompetitionReasonerProgressStatus getCompetitionReasonerProgressStatus() {
		return mStatus;
	}
	
}
