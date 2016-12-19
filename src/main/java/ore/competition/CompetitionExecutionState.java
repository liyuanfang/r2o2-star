package ore.competition;

public enum CompetitionExecutionState {
	
	COMPETITION_EXECUTION_STATE_WAITING("Waiting"),
	COMPETITION_EXECUTION_STATE_PREPARATION("Preparation"),
	COMPETITION_EXECUTION_STATE_QUEUED("Queued"),
	COMPETITION_EXECUTION_STATE_RUNNING("Running"),	
	COMPETITION_EXECUTION_STATE_FINISHED("Finished");
	
	private String mShortName = null;
	
	private CompetitionExecutionState(String shortName) {
		mShortName = shortName;		
	}
	
	public String getShortString() {
		return mShortName;
	}
	
}
