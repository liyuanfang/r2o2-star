package org.semanticweb.ore.networking.events;

import org.joda.time.DateTime;
import org.semanticweb.ore.threading.Event;

public class PrepareExecutionEvent implements Event {
	
	protected DateTime mPlannedExecutionTime = null;
	
	public PrepareExecutionEvent(DateTime plannedExecutionTime) {
		mPlannedExecutionTime = plannedExecutionTime;
	}	
	
	public DateTime getPlannedExecutionTime() {
		return mPlannedExecutionTime;
	}

}
