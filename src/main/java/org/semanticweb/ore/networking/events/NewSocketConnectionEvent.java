package org.semanticweb.ore.networking.events;

import java.net.Socket;

import org.semanticweb.ore.threading.Event;

public class NewSocketConnectionEvent implements Event {
	
	protected Socket mSocket = null;
	
	public NewSocketConnectionEvent(Socket socket) {
		mSocket = socket;
	}
	
	public Socket getSocket() {
		return mSocket;
	}

}
