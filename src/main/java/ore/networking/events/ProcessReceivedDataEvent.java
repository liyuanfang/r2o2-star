package ore.networking.events;

import ore.threading.Event;

import java.util.ArrayList;

public class ProcessReceivedDataEvent implements Event {
	
	protected ArrayList<String> mReadStringList = null;
	
	public ProcessReceivedDataEvent(ArrayList<String> readStringList) {
		mReadStringList = readStringList;
	}
	
	public ArrayList<String> getReadStringList() {
		return mReadStringList;
	}

}
