package ore.networking;

import ore.networking.events.KeepConnectionAliveEvent;
import ore.networking.events.ProcessReceivedDataEvent;
import ore.networking.events.SendMessageEvent;
import ore.networking.events.SocketCommunicationExceptionEvent;
import ore.networking.messages.KeepConnectionAliveMessage;
import ore.networking.messages.Message;
import ore.networking.messages.MessageType;
import ore.threading.Event;
import ore.threading.EventThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

public class ConnectionHandler extends EventThread {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ConnectionHandler.class);
	
	protected Socket mSocket = null;
	protected MessageParsingFactory mMessageParsingFactory = null;
	protected MessageHandler mMessageHandler = null;
	protected MessageSerializer mMessageSerializer = null;
	protected BufferedWriter mWriter = null;
	protected boolean mCommunicationError = false;
	protected CommunicationErrorHandler mCommunicationErrorHandler = null;
	
	protected int mPort = 0;
	protected String mAddressString = null;
	
	protected long mConnectionAliveTime = 60*1000;
	protected long mConnectionAliveSendReceivedDiffCount = 0;
	protected long mMaxConnectionAliveSendReceivedDiffCount = 10;

	public ConnectionHandler(Socket socket, MessageParsingFactory messageParsingFactory, MessageSerializer messageSerializer, MessageHandler messageHandler, CommunicationErrorHandler errorHandler) {
		mSocket = socket;
		mCommunicationErrorHandler = errorHandler;
		mMessageParsingFactory = messageParsingFactory;
		mMessageHandler = messageHandler;
		mMessageSerializer = messageSerializer;
		startThread();
	}
	
	public ConnectionHandler(String addressString, int port, MessageParsingFactory messageParsingFactory, MessageSerializer messageSerializer, MessageHandler messageHandler, CommunicationErrorHandler errorHandler) {
		mAddressString = addressString;
		mPort = port;
		mCommunicationErrorHandler = errorHandler;
		mMessageParsingFactory = messageParsingFactory;
		mMessageHandler = messageHandler;
		mMessageSerializer = messageSerializer;
		startThread();
	}
	

	public ConnectionHandler(String addressString, int port, int reconnectTryCount, MessageParsingFactory messageParsingFactory, MessageSerializer messageSerializer, MessageHandler messageHandler, CommunicationErrorHandler errorHandler) {
		mAddressString = addressString;
		mPort = port;
		mCommunicationErrorHandler = errorHandler;
		mMessageParsingFactory = messageParsingFactory;
		mMessageHandler = messageHandler;
		mMessageSerializer = messageSerializer;
		mMaxConnectionAliveSendReceivedDiffCount = reconnectTryCount;
		startThread();
	}
	
	protected void createSocketWriter(final Socket socket) {
		if (!mCommunicationError) {
			try {
				mWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			} catch (IOException e) {
				mCommunicationError = true;
				mLogger.error("Failed to open output stream writer for socket, got IOException '{}'.",e.getMessage());			
				postEvent(new SocketCommunicationExceptionEvent(e));
			}
		}
	}
	
	protected void sendMessage(Message message) {
		postEvent(new SendMessageEvent(message));
	}
	
	
	protected void createSocketReader(final Socket socket) {
		if (!mCommunicationError) {
			new Thread(new Runnable() {
	
				@Override
				public void run() {
					BufferedReader reader = null;
					try {
						reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					} catch (IOException e) {
						mCommunicationError = true;
						mLogger.error("Failed to open input stream reader for socket, got IOException '{}'.",e.getMessage());
						postEvent(new SocketCommunicationExceptionEvent(e));
					}
					while (!mCommunicationError) {
						
						String line = null;
						ArrayList<String> readStringList = new ArrayList<String>();
						try {
							while ((line = reader.readLine()) != null) {	
								if (line.isEmpty()) {
									break;
								} else {
									readStringList.add(line);
								}
							}
							
							postEvent(new ProcessReceivedDataEvent(readStringList));
							
						} catch (IOException e) {
							mCommunicationError = true;
							mLogger.error("Failed to read from socket, got IOException '{}'.",e.getMessage());
							postEvent(new SocketCommunicationExceptionEvent(e));
						}
						
						
					}
					
				}
				
			}).start();
		}
	}

	
	protected Message parseMessage(ArrayList<String> readStringList) {
		Message message = null;
		if (mMessageParsingFactory != null) {
			message = mMessageParsingFactory.createParsedMessage(readStringList);
		}
		return message;
	}	
	
	

	protected Collection<String> serializeMessage(Message message) {
		Collection<String> messageSerialization = null;
		if (mMessageSerializer != null) {
			messageSerialization = mMessageSerializer.serializeMessage(message);
		}
		return messageSerialization;
	}	
	
	
	protected void sendData(Collection<String> stringCollection) {
		if (mWriter != null && !mCommunicationError) {
			try {
				for (String dataString : stringCollection) {
					mWriter.write(dataString+"\n");
				}
				mWriter.write("\n");
				mWriter.flush();
			} catch (IOException e) {
				mCommunicationError = true;
				mLogger.error("Failed to write to socket, got IOException '{}'.",e.getMessage());
				postEvent(new SocketCommunicationExceptionEvent(e));
			}
		}
	}	
	
	protected void threadStart() {
		super.threadStart();
		
		if (mSocket == null) {
			try {
				mSocket = new Socket(mAddressString,mPort);
			} catch (UnknownHostException e) {
				mCommunicationError = true;
				mLogger.error("Creation of socket to '{}:{}' failed, got UnknownHostException '{}'.",new Object[]{mAddressString,mPort,e.getMessage()});
				postEvent(new SocketCommunicationExceptionEvent(e));
			} catch (IOException e) {
				mCommunicationError = true;
				mLogger.error("Creation of socket to '{}:{}' failed, got IOException '{}'.",new Object[]{mAddressString,mPort,e.getMessage()});
				postEvent(new SocketCommunicationExceptionEvent(e));
			}
		}
		
		if (!mCommunicationError) {
			createSocketReader(mSocket);
			createSocketWriter(mSocket);		
			
			new Thread(new Runnable() {

				@Override
				public void run() {
					while (true) {
						try {
							Thread.sleep(mConnectionAliveTime);
						} catch (InterruptedException e) {
						}
						postEvent(new KeepConnectionAliveEvent());
					}
				}
				
			}).start();
		}
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}	
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof SocketCommunicationExceptionEvent) {
				SocketCommunicationExceptionEvent scee = (SocketCommunicationExceptionEvent)e;
				mCommunicationError = true;
				if (mCommunicationErrorHandler != null) {
					mCommunicationErrorHandler.handleCommunicationError(scee.getException());
				}
				stopThread();
				return true;
			} else if (e instanceof ProcessReceivedDataEvent) {
				ProcessReceivedDataEvent prde = (ProcessReceivedDataEvent)e;
				Message message = parseMessage(prde.getReadStringList());
				if (message != null) {
					if (message.getMessageType() == MessageType.MESSAGE_TYPE_KEEP_CONNECTION_ALIVE) {
						mConnectionAliveSendReceivedDiffCount = 0;
						//mLogger.info("Received connection alive message, continue connection.");
					} else if (mMessageHandler != null) {
						mMessageHandler.handleMessage(message);
					}
				}
				return true;
			} else if (e instanceof SendMessageEvent) {
				SendMessageEvent sme = (SendMessageEvent)e;
				Collection<String> messageSerialization = serializeMessage(sme.getMessage());
				sendData(messageSerialization);
				return true;
			} else if (e instanceof KeepConnectionAliveEvent) {
				sendMessage(new KeepConnectionAliveMessage());
				++mConnectionAliveSendReceivedDiffCount;
				if (mConnectionAliveSendReceivedDiffCount > mMaxConnectionAliveSendReceivedDiffCount) {					
					mCommunicationError = true;
					if (mCommunicationErrorHandler != null) {
						mCommunicationErrorHandler.handleCommunicationError(new Exception("Connection timed out"));
					}
					stopThread();
				}
			}
		}
		return false;
	}
	
	

	
}
