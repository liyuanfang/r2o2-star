package org.semanticweb.ore.competition.events;

import org.semanticweb.ore.competition.CompetitionStatusUpdateListner;
import org.semanticweb.ore.threading.Event;

public class RemoveCompetitionStatusUpdateListnerEvent implements Event {
	
	protected CompetitionStatusUpdateListner mListner = null;
	
	public RemoveCompetitionStatusUpdateListnerEvent(CompetitionStatusUpdateListner listner) {
		mListner = listner;
	}	
	
	public CompetitionStatusUpdateListner getCompetitionStatusUpdateListner() {
		return mListner;
	}

}
