package ore.competition.events;

import ore.competition.CompetitionStatusUpdateListner;
import ore.threading.Event;

public class RemoveCompetitionStatusUpdateListnerEvent implements Event {
	
	protected CompetitionStatusUpdateListner mListner = null;
	
	public RemoveCompetitionStatusUpdateListnerEvent(CompetitionStatusUpdateListner listner) {
		mListner = listner;
	}	
	
	public CompetitionStatusUpdateListner getCompetitionStatusUpdateListner() {
		return mListner;
	}

}
