package ore.competition;

public class CompetitionEvaluationStatusUpdateItem {
	
	protected CompetitionEvaluationStatus mStatus = null;
	protected long mUpdateID = 0;
		
	public CompetitionEvaluationStatusUpdateItem(CompetitionEvaluationStatus status, long updateID) {
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
	
	public CompetitionEvaluationStatus getCompetitionEvaluationStatus() {
		return mStatus;
	}
	
}
