package ore.competition.events;

import ore.competition.CompetitionEvaluationStatus;
import ore.threading.Event;

public class UpdateCompetitionEvaluationStatusEvent implements Event {
	
	protected CompetitionEvaluationStatus mStatus = null;
	
	public UpdateCompetitionEvaluationStatusEvent(CompetitionEvaluationStatus status) {
		mStatus = status;
	}	
	
	public CompetitionEvaluationStatus getCompetitionEvaluationStatus() {
		return mStatus;
	}

}
