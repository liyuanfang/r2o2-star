package ore.networking;

import ore.competition.CompetitionEvaluationStatus;
import ore.competition.CompetitionReasonerProgressStatus;
import ore.competition.CompetitionStatus;
import ore.competition.CompetitionStatusUpdater;
import ore.competition.events.UpdateCompetitionEvaluationStatusEvent;
import ore.competition.events.UpdateCompetitionReasonerProgressStatusEvent;
import ore.competition.events.UpdateCompetitionStatusEvent;
import ore.configuration.Config;
import ore.networking.events.NewSocketConnectionEvent;
import ore.networking.messages.Message;
import ore.networking.messages.UpdateCompetitionEvaluationStatusMessage;
import ore.networking.messages.UpdateCompetitionReasonerProgressStatusMessage;
import ore.networking.messages.UpdateCompetitionStatusMessage;
import ore.threading.Event;
import ore.threading.EventThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class StatusServerUpdateManager extends EventThread implements CompetitionStatusUpdater {
	
	final private static Logger mLogger = LoggerFactory.getLogger(StatusServerUpdateManager.class);
	
	protected ConnectionHandler mConnectionHandler = null;
	protected boolean mCommunicationError = false;
	protected int mListenPort = 0;
	protected ServerSocket mListenSocket = null;
	protected Config mConfig = null;
	protected int mClientNumber = 0;
	
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	protected ArrayList<UpdateClientHandlerItem> mClientHandlerSet = new ArrayList<UpdateClientHandlerItem>();
	
	protected HashMap<String,CompetitionStatus> mLastCompetitionStatusMap = new HashMap<String,CompetitionStatus>();
	protected HashMap<String,CompetitionReasonerProgressStatus> mLastCompetitionReasonerStatusMap = new HashMap<String,CompetitionReasonerProgressStatus>();
	protected HashMap<String,CompetitionEvaluationStatus> mLastCompetitionEvaluationStatusMap = new HashMap<String,CompetitionEvaluationStatus>();
	
	

	protected class UpdateClientHandlerItem implements MessageHandler, CommunicationErrorHandler {
		protected ConnectionHandler mConnectionHandler = null;
		protected String mAddress = null;
		protected int mClientNumber = 0;
		protected boolean mCommunicationError = false; 
		
		public UpdateClientHandlerItem(Socket socket, int number) {
			mAddress = socket.getRemoteSocketAddress().toString();
			mConnectionHandler = new ConnectionHandler(socket, new DefaultMessageParsingFactory(), new DefaultMessageSerializer(), this, this);
			mClientNumber = number;
		}		

		public String toString() {
			return "Client-"+mClientNumber+mAddress;
		}
		

		@Override
		public void handleCommunicationError(Throwable exception) {
			mCommunicationError = true;
			mLogger.error("Lost connection to update client '{}', got Exception '{}'.",toString(),exception.getMessage());
		}
		
		@Override
		public void handleMessage(Message message) {
			if (!mCommunicationError) {				
			}
		}
		
		public void sendCompetitionStatus(CompetitionStatus status) {			
			if (!mCommunicationError) {	
				mConnectionHandler.sendMessage(new UpdateCompetitionStatusMessage(status));
			}
		}
		
		public void sendCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status) {			
			if (!mCommunicationError) {		
				mConnectionHandler.sendMessage(new UpdateCompetitionReasonerProgressStatusMessage(status));
			}
		}

		public void sendCompetitionEvaluationStatus(CompetitionEvaluationStatus status) {
			if (!mCommunicationError) {		
				mConnectionHandler.sendMessage(new UpdateCompetitionEvaluationStatusMessage(status));
			}
		}
		
	}
	
	
	
	
		
	public StatusServerUpdateManager(int port, Config config, CompetitionStatusUpdater statusUpdater) {		
		mStatusUpdater = statusUpdater;
		mConfig = config;
		mListenPort = port;
		startThread();
	}	
	
	
	protected void threadStart() {
		super.threadStart();
		
		try {			
			mListenSocket = new ServerSocket(mListenPort);
			mLogger.info("Created server socket to listen on port {}",mListenPort);
		} catch (IOException e) {
			mCommunicationError = true;
			mLogger.error("Failed to create server socket to listen on port '{}', got IOException '{}'.",mListenPort,e.getMessage());
		}
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {		
				
				while (!mCommunicationError) {
					try {
						Socket socket = mListenSocket.accept();
						postEvent(new NewSocketConnectionEvent(socket));
					} catch (IOException e) {
						mCommunicationError = true;
						mLogger.error("Failed to create server socket to listen on port '{}', got IOException '{}'.",mListenPort,e.getMessage());
					}
					
				}
				
			}
			
		}).start();
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}	
	
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof NewSocketConnectionEvent) {
				NewSocketConnectionEvent nsce = (NewSocketConnectionEvent)e;
				Socket socket = nsce.getSocket();
				String remoteAddressString = socket.getRemoteSocketAddress().toString();
				UpdateClientHandlerItem clientHandlerItem = new UpdateClientHandlerItem(socket,mClientNumber);				
				mLogger.info("New network update client connected from '{}', identifying client as '{}'.",remoteAddressString,clientHandlerItem.toString());
				mClientHandlerSet.add(clientHandlerItem);
				broadcastStatusToClient(clientHandlerItem);
				++mClientNumber;
				return true;
			} else if (e instanceof UpdateCompetitionStatusEvent) {
				UpdateCompetitionStatusEvent ucse = (UpdateCompetitionStatusEvent)e;
				CompetitionStatus status = ucse.getCompetitionStatus();
				mLastCompetitionStatusMap.put(status.getCompetitionSourceString(),status);
				broadcastCompetitionStatus(status);
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionStatus(status);
				}
				return true;
			} else if (e instanceof UpdateCompetitionReasonerProgressStatusEvent) {
				UpdateCompetitionReasonerProgressStatusEvent ucrpse = (UpdateCompetitionReasonerProgressStatusEvent)e;
				CompetitionReasonerProgressStatus status = ucrpse.getCompetitionReasonerProgressStatus();
				mLastCompetitionReasonerStatusMap.put(status.getCompetitionSourceString()+":"+status.getReasonerSourceString(),status);
				broadcastCompetitionReasonerProgressStatus(status);
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionReasonerProgressStatus(status);
				}				
				return true;
			} else if (e instanceof UpdateCompetitionEvaluationStatusEvent) {
				UpdateCompetitionEvaluationStatusEvent ucese = (UpdateCompetitionEvaluationStatusEvent)e;
				CompetitionEvaluationStatus status = ucese.getCompetitionEvaluationStatus();
				mLastCompetitionEvaluationStatusMap.put(status.getCompetitionSourceString()+":"+status.getEvaluationSourceString(),status);
				broadcastCompetitionEvaluationStatus(status);
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(status);
				}
				return true;
			}
		}
		return false;
	}
	
	

	protected void broadcastStatusToClient(UpdateClientHandlerItem clientHandlerItem) {
		for (CompetitionStatus status : mLastCompetitionStatusMap.values()) {
			clientHandlerItem.sendCompetitionStatus(status);
		}
		for (CompetitionReasonerProgressStatus status : mLastCompetitionReasonerStatusMap.values()) {
			clientHandlerItem.sendCompetitionReasonerProgressStatus(status);
		}		
		for (CompetitionEvaluationStatus status : mLastCompetitionEvaluationStatusMap.values()) {
			clientHandlerItem.sendCompetitionEvaluationStatus(status);
		}		
	}	
	
	
	protected void broadcastCompetitionStatus(CompetitionStatus status) {	
		for (UpdateClientHandlerItem clientHandlerItem : mClientHandlerSet) {
			clientHandlerItem.sendCompetitionStatus(status);
		}
	}

	protected void broadcastCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status) {	
		for (UpdateClientHandlerItem clientHandlerItem : mClientHandlerSet) {
			clientHandlerItem.sendCompetitionReasonerProgressStatus(status);
		}
	}

	protected void broadcastCompetitionEvaluationStatus(CompetitionEvaluationStatus status) {	
		for (UpdateClientHandlerItem clientHandlerItem : mClientHandlerSet) {
			clientHandlerItem.sendCompetitionEvaluationStatus(status);
		}
	}

	
	@Override
	public void updateCompetitionStatus(CompetitionStatus status) {
		postEvent(new UpdateCompetitionStatusEvent(status));
	}


	@Override
	public void updateCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status) {
		postEvent(new UpdateCompetitionReasonerProgressStatusEvent(status));
	}


	@Override
	public void updateCompetitionEvaluationStatus(CompetitionEvaluationStatus status) {
		postEvent(new UpdateCompetitionEvaluationStatusEvent(status));
	}


	
	
}
