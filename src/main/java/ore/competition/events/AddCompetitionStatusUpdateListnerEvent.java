package ore.competition.events;

import ore.competition.CompetitionStatusUpdateListner;
import ore.threading.Event;

public class AddCompetitionStatusUpdateListnerEvent implements Event {
	
	protected CompetitionStatusUpdateListner mListner = null;
	
	public AddCompetitionStatusUpdateListnerEvent(CompetitionStatusUpdateListner listner) {
		mListner = listner;
	}	
	
	public CompetitionStatusUpdateListner getCompetitionStatusUpdateListner() {
		return mListner;
	}

}
