package org.semanticweb.ore.competition.events;

import org.semanticweb.ore.competition.CompetitionStatus;
import org.semanticweb.ore.threading.Event;

public class UpdateCompetitionStatusEvent implements Event {
	
	protected CompetitionStatus mStatus = null;
	
	public UpdateCompetitionStatusEvent(CompetitionStatus status) {
		mStatus = status;
	}	
	
	public CompetitionStatus getCompetitionStatus() {
		return mStatus;
	}

}
