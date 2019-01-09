package org.semanticweb.ore.competition.events;

import org.semanticweb.ore.competition.CompetitionReasonerProgressStatus;
import org.semanticweb.ore.threading.Event;

public class UpdateCompetitionReasonerProgressStatusEvent implements Event {
	
	protected CompetitionReasonerProgressStatus mStatus = null;
	
	public UpdateCompetitionReasonerProgressStatusEvent(CompetitionReasonerProgressStatus status) {
		mStatus = status;
	}	
	
	public CompetitionReasonerProgressStatus getCompetitionReasonerProgressStatus() {
		return mStatus;
	}

}
