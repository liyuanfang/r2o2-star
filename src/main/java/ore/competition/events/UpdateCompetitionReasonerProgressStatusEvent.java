package ore.competition.events;

import ore.competition.CompetitionReasonerProgressStatus;
import ore.threading.Event;

public class UpdateCompetitionReasonerProgressStatusEvent implements Event {
	
	protected CompetitionReasonerProgressStatus mStatus = null;
	
	public UpdateCompetitionReasonerProgressStatusEvent(CompetitionReasonerProgressStatus status) {
		mStatus = status;
	}	
	
	public CompetitionReasonerProgressStatus getCompetitionReasonerProgressStatus() {
		return mStatus;
	}

}
