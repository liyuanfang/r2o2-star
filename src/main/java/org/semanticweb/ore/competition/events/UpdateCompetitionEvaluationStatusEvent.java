package org.semanticweb.ore.competition.events;

import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.threading.Event;

public class UpdateCompetitionEvaluationStatusEvent implements Event {
	
	protected CompetitionEvaluationStatus mStatus = null;
	
	public UpdateCompetitionEvaluationStatusEvent(CompetitionEvaluationStatus status) {
		mStatus = status;
	}	
	
	public CompetitionEvaluationStatus getCompetitionEvaluationStatus() {
		return mStatus;
	}

}
