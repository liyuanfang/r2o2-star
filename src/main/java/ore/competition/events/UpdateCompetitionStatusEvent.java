package ore.competition.events;

import ore.competition.CompetitionStatus;
import ore.threading.Event;

public class UpdateCompetitionStatusEvent implements Event {
	
	protected CompetitionStatus mStatus = null;
	
	public UpdateCompetitionStatusEvent(CompetitionStatus status) {
		mStatus = status;
	}	
	
	public CompetitionStatus getCompetitionStatus() {
		return mStatus;
	}

}
