package org.semanticweb.ore.networking.events;

import java.util.ArrayList;

import org.semanticweb.ore.threading.Event;

public class ProcessReceivedDataEvent implements Event {
	
	protected ArrayList<String> mReadStringList = null;
	
	public ProcessReceivedDataEvent(ArrayList<String> readStringList) {
		mReadStringList = readStringList;
	}
	
	public ArrayList<String> getReadStringList() {
		return mReadStringList;
	}

}
