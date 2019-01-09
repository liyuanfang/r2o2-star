package org.semanticweb.ore.execution.events;

import org.semanticweb.ore.threading.Event;

public class ReasonerProcessCompletedEvent implements Event {
	
	private int mExitValue = 0;
	
	public int getExitValue() {
		return mExitValue;
	}
	
	public ReasonerProcessCompletedEvent(int exitValue) {
		mExitValue = exitValue;
	}

}
