package org.semanticweb.ore.competition.events;

import org.semanticweb.ore.competition.CompetitionStatusUpdateListner;
import org.semanticweb.ore.threading.Event;

public class AddCompetitionStatusUpdateListnerEvent implements Event {
	
	protected CompetitionStatusUpdateListner mListner = null;
	
	public AddCompetitionStatusUpdateListnerEvent(CompetitionStatusUpdateListner listner) {
		mListner = listner;
	}	
	
	public CompetitionStatusUpdateListner getCompetitionStatusUpdateListner() {
		return mListner;
	}

}
