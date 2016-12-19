package ore.networking.events;

import ore.threading.Event;
import org.joda.time.DateTime;

public class PrepareExecutionEvent implements Event {
	
	protected DateTime mPlannedExecutionTime = null;
	
	public PrepareExecutionEvent(DateTime plannedExecutionTime) {
		mPlannedExecutionTime = plannedExecutionTime;
	}	
	
	public DateTime getPlannedExecutionTime() {
		return mPlannedExecutionTime;
	}

}
