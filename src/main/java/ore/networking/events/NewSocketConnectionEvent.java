package ore.networking.events;

import ore.threading.Event;

import java.net.Socket;

public class NewSocketConnectionEvent implements Event {
	
	protected Socket mSocket = null;
	
	public NewSocketConnectionEvent(Socket socket) {
		mSocket = socket;
	}
	
	public Socket getSocket() {
		return mSocket;
	}

}
