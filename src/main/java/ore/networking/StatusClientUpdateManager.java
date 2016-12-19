package ore.networking;

import ore.competition.CompetitionEvaluationStatus;
import ore.competition.CompetitionReasonerProgressStatus;
import ore.competition.CompetitionStatus;
import ore.competition.CompetitionStatusUpdater;
import ore.configuration.Config;
import ore.networking.events.CommunicationErrorEvent;
import ore.networking.events.EstablishReconnectionEvent;
import ore.networking.events.ProcessMessageEvent;
import ore.networking.events.WaitFinishNotificationEvent;
import ore.networking.messages.*;
import ore.threading.Event;
import ore.threading.EventThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

public class StatusClientUpdateManager extends EventThread implements CommunicationErrorHandler, MessageHandler {
	
	final private static Logger mLogger = LoggerFactory.getLogger(StatusClientUpdateManager.class);
	
	protected ConnectionHandler mConnectionHandler = null;
	protected String mAddressString = null;
	protected int mPort = 0;
	protected Config mConfig = null;

	
	
	protected Semaphore mWaitFinishedSemaphore = new Semaphore(0);
	protected int mWaitFinishCount = 0;	
	protected boolean mFinished = false;
	
	protected int mReconnectCount = 100;
	protected long mReconnectEventSentTime = 10*1000;
	protected boolean mPendingReconnection = false;
	protected boolean mConnectionError = false;
	
	
	
	protected CompetitionStatusUpdater mStatusUpdater = null;

	
		
	public StatusClientUpdateManager(String addressString, int port, CompetitionStatusUpdater updater, Config config) {
		mAddressString = addressString;
		mPort = port;
		mConfig = config;
		mStatusUpdater = updater;
		startThread();
	}	
	
	
	public void waitForFinished() {
		if (!mFinished) {
			postEvent(new WaitFinishNotificationEvent());
			try {
				mWaitFinishedSemaphore.acquire();
			} catch (InterruptedException e) {
				mLogger.error("Waiting for finisih interrupted, got InterruptedException '{}'.",e.getMessage());
			}
		}
	}
	
	
	protected void threadStart() {
		super.threadStart();			
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(mReconnectEventSentTime);
					} catch (InterruptedException e) {
					}
					postEvent(new EstablishReconnectionEvent());
				}
			}
			
		}).start();
		
		
		establishServerConnection();
		requestEvaluationTask();
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}	
	
	
	
	protected void establishServerConnection() {	
		mConnectionHandler = new ConnectionHandler(mAddressString, mPort, 1000000, new DefaultMessageParsingFactory(), new DefaultMessageSerializer(), this, this);
	}		
	
	protected void requestEvaluationTask() {
		if (!mConnectionError) {
			mConnectionHandler.sendMessage(new RequestEvaluationTaskMessage());
		}
	}
	
	
	protected void closeServerConnection() {	
		mConnectionHandler = null;
	}		
	
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof CommunicationErrorEvent) {
				mConnectionError = true;
				closeServerConnection();
				if (mReconnectCount > 0) {
					--mReconnectCount;		
					mPendingReconnection = true;
				} else {
					mFinished = true;
					if (mWaitFinishCount > 0) {
						mWaitFinishedSemaphore.release(mWaitFinishCount);
					}
				}
				return true;
			} else if (e instanceof ProcessMessageEvent) {
				ProcessMessageEvent pme = (ProcessMessageEvent)e;
				Message message = pme.getMessage();
				if (message != null) {	
					processMessage(message);
				}
				return true;
			} else if (e instanceof WaitFinishNotificationEvent) {
				if (mFinished) {
					mWaitFinishedSemaphore.release();
				} else {
					++mWaitFinishCount;
				}
				return true;
			} else if (e instanceof EstablishReconnectionEvent) {	
				if (mPendingReconnection) {
					mConnectionError = false;
					mPendingReconnection = false;
					establishServerConnection();
				}
				return true;	
			}
		}
		return false;
	}


	@Override
	public void handleCommunicationError(Throwable exception) {
		postEvent(new CommunicationErrorEvent(exception));
	}


	@Override
	public void handleMessage(Message message) {
		postEvent(new ProcessMessageEvent(message));
	}
	
	
	
	
	public void processMessage(Message message) {	
		if (message.getMessageType() == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_STATUS) {
			UpdateCompetitionStatusMessage ucsm = (UpdateCompetitionStatusMessage)message;
			CompetitionStatus status = ucsm.createCompetitionStatusFromMessage();
			if (mStatusUpdater != null) {
				mStatusUpdater.updateCompetitionStatus(status);
			}
		} else if (message.getMessageType() == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_REASONER_PROGRESS_STATUS) {
			UpdateCompetitionReasonerProgressStatusMessage ucrpsm = (UpdateCompetitionReasonerProgressStatusMessage)message;
			CompetitionReasonerProgressStatus status = ucrpsm.createCompetitionReasonerProgressStatusFromMessage();
			if (mStatusUpdater != null) {
				mStatusUpdater.updateCompetitionReasonerProgressStatus(status);
			}
		} else if (message.getMessageType() == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_EVALUATION_STATUS) {
			UpdateCompetitionEvaluationStatusMessage ucesm = (UpdateCompetitionEvaluationStatusMessage)message;
			CompetitionEvaluationStatus status = ucesm.createCompetitionEvaluationStatusFromMessage();
			if (mStatusUpdater != null) {
				mStatusUpdater.updateCompetitionEvaluationStatus(status);
			}
		}
	}
	


	
}
