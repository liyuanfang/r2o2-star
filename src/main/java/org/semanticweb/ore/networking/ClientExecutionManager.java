package org.semanticweb.ore.networking;

import java.util.concurrent.Semaphore;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.conversion.OntologyFormatDynamicConversionRedirector;
import org.semanticweb.ore.conversion.OntologyFormatNoRedictionRedirector;
import org.semanticweb.ore.conversion.OntologyFormatRedirector;
import org.semanticweb.ore.execution.ReasonerQueryExecutionHandler;
import org.semanticweb.ore.execution.events.ReasonerQueryExecutedCallbackEvent;
import org.semanticweb.ore.interfacing.DefaultReasonerAdaptorFactory;
import org.semanticweb.ore.interfacing.DefaultReasonerDescriptionFactory;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.interfacing.ReasonerDescriptionManager;
import org.semanticweb.ore.networking.events.CommunicationErrorEvent;
import org.semanticweb.ore.networking.events.EstablishReconnectionEvent;
import org.semanticweb.ore.networking.events.ProcessMessageEvent;
import org.semanticweb.ore.networking.events.WaitFinishNotificationEvent;
import org.semanticweb.ore.networking.messages.FailedEvaluationTaskMessage;
import org.semanticweb.ore.networking.messages.Message;
import org.semanticweb.ore.networking.messages.MessageType;
import org.semanticweb.ore.networking.messages.ProcessEvaluationTaskMessage;
import org.semanticweb.ore.networking.messages.ProcessedEvaluationTaskMessage;
import org.semanticweb.ore.networking.messages.RequestEvaluationTaskMessage;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryManager;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.querying.TSVQueryResponseStoringHandler;
import org.semanticweb.ore.threading.Event;
import org.semanticweb.ore.threading.EventThread;
import org.semanticweb.ore.verification.DefaultQueryResultNormaliserFactory;
import org.semanticweb.ore.verification.ExpectedQueryResultHashComparisonVerifier;
import org.semanticweb.ore.verification.QueryResponseVerificationHandler;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.semanticweb.ore.verification.QueryResultVerifier;
import org.semanticweb.ore.verification.events.QueryResponseVerifiedCallbackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientExecutionManager extends EventThread implements CommunicationErrorHandler, MessageHandler {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ClientExecutionManager.class);
	
	protected ConnectionHandler mConnectionHandler = null;
	protected String mAddressString = null;
	protected int mPort = 0;
	protected boolean mEvaluationExecution = false;
	protected Config mConfig = null;
	
	protected QueryManager mQueryManager = null;
	protected ReasonerDescriptionManager mReasonerManager = null;
	
	
	protected TSVQueryResponseStoringHandler mQueryResponseStoringHandler = null;
	protected OntologyFormatRedirector mConversionFormatRedirector = null;
	protected DefaultReasonerAdaptorFactory mReasonerAdaptorFactory = null;
	protected ReasonerQueryExecutionHandler mReasonerQueryExecutionHandler = null;
	
	
	protected DefaultQueryResultNormaliserFactory mNormalisationFactory = null;			
	
	
	protected QueryResultVerifier mResultVerifier = null;
	protected QueryResponseVerificationHandler mVerificationHandler = null;
	
	protected Semaphore mWaitFinishedSemaphore = new Semaphore(0);
	protected int mWaitFinishCount = 0;	
	protected boolean mFinished = false;
	protected int mReconnectCount = 10;
	protected long mReconnectEventSentTime = 30*1000;
	protected boolean mPendingReconnection = false;
	protected boolean mConnectionError = false;
	
	protected enum ExecutionState {		
		STATE_IDLE,
		STATE_EXECUTING,
		STATE_VERIFYING;		
	}
	
	protected ExecutionState mExecutionState = ExecutionState.STATE_IDLE;
	
	
	
	public ClientExecutionManager(String addressString, int port, QueryManager queryManager, ReasonerDescriptionManager reasonerManager, Config config) {		
		mAddressString = addressString;
		mPort = port;
		mConfig = config;
		mQueryManager = queryManager;
		mReasonerManager = reasonerManager;
		startThread();
	}
	
	public ClientExecutionManager(String addressString, int port, Config config) {		
		mAddressString = addressString;
		mPort = port;
		mConfig = config;
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
		
		if (mQueryManager == null) {
			mQueryManager = new QueryManager(mConfig);
		}
		if (mReasonerManager == null) {
			mReasonerManager = new ReasonerDescriptionManager(new DefaultReasonerDescriptionFactory(), mConfig);
		}
		
		mQueryResponseStoringHandler = new TSVQueryResponseStoringHandler(mConfig);
		
		if (!ConfigDataValueReader.getConfigDataValueBoolean(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_ONTOLOGY_CONVERTION, false)) {	
			mConversionFormatRedirector = new OntologyFormatNoRedictionRedirector(); 
		} else {
			mConversionFormatRedirector = new OntologyFormatDynamicConversionRedirector(mConfig);
		}

		
		mReasonerAdaptorFactory = new DefaultReasonerAdaptorFactory();
		mReasonerQueryExecutionHandler = new ReasonerQueryExecutionHandler(mReasonerAdaptorFactory,mConversionFormatRedirector,mQueryResponseStoringHandler,mConfig);
		
		
		mNormalisationFactory = new DefaultQueryResultNormaliserFactory(mConfig);			
		mResultVerifier = new ExpectedQueryResultHashComparisonVerifier(mNormalisationFactory,mQueryResponseStoringHandler,mConfig);
		mVerificationHandler = new QueryResponseVerificationHandler(mResultVerifier, mConfig);
		
		
		
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
		mConnectionHandler = new ConnectionHandler(mAddressString, mPort, new DefaultMessageParsingFactory(), new DefaultMessageSerializer(), this, this);
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
			} else if (e instanceof ReasonerQueryExecutedCallbackEvent) {
				ReasonerQueryExecutedCallbackEvent rqece = (ReasonerQueryExecutedCallbackEvent)e;
				mExecutionState = ExecutionState.STATE_VERIFYING;
				processEvaluationTaskExecuted(rqece);				
				return true;
			} else if (e instanceof QueryResponseVerifiedCallbackEvent) {
				QueryResponseVerifiedCallbackEvent qrvce = (QueryResponseVerifiedCallbackEvent)e;
				processEvaluationTaskVerified(qrvce);
				mExecutionState = ExecutionState.STATE_IDLE;
				requestEvaluationTask();				
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
					if (mExecutionState == ExecutionState.STATE_IDLE) {
						mConnectionError = false;
						mPendingReconnection = false;
						establishServerConnection();
						requestEvaluationTask();
					}
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
		if (message.getMessageType() == MessageType.MESSAGE_TYPE_PROCESS_EVALUATION_TASK) {
			processProcessEvaluationTaskMessage((ProcessEvaluationTaskMessage)message);
		}
	}
	

	public void processEvaluationTaskVerified(QueryResponseVerifiedCallbackEvent qrvce) {
		QueryResultVerificationReport verificationReport = qrvce.getVerificationReport();
		ClientExecutionTask executionTask = qrvce.getExecutionTask();	
		executionTask.setVerificationReport(verificationReport);
		ProcessEvaluationTaskMessage processEvaluationTaskMessage = executionTask.getProcessEvaluationTaskMessage();
		
		if (!mConnectionError) {
			mConnectionHandler.sendMessage(new ProcessedEvaluationTaskMessage(processEvaluationTaskMessage.getReasonerString(),processEvaluationTaskMessage.getQueryString(),processEvaluationTaskMessage.getOutputString(),executionTask.getQueryResponse(),executionTask.getVerificationReport()));
		}
	}
	
	
	public void processEvaluationTaskExecuted(ReasonerQueryExecutedCallbackEvent rqece) {
		QueryResponse queryResponse = rqece.getQueryResponse();
		ClientExecutionTask executionTask = rqece.getExecutionTask();
		ReasonerDescription reasoner = executionTask.getReasoner();
		Query query = executionTask.getQuery();
		executionTask.setQueryResponse(queryResponse);
		mVerificationHandler.verifyQueryResponse(reasoner, query, queryResponse, new QueryResponseVerifiedCallbackEvent(this,executionTask));
		
	}
	
	
	
	public void processProcessEvaluationTaskMessage(ProcessEvaluationTaskMessage message) {
		String queryName = message.getQueryString();
		Query query = mQueryManager.loadQuery(queryName);
		
		String reasonerName = message.getReasonerString();
		ReasonerDescription reasoner = mReasonerManager.loadReasonerDescription(reasonerName);
		
		String responseDirectory = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY);
		String responseDestinationString = responseDirectory + message.getOutputString();
		
		long executionTimeout = message.getExecutionTimeout();
		long memoryLimit = message.getMemoryLimit();
		
		if (query != null && reasoner != null) {
			ClientExecutionTask executionTask = new ClientExecutionTask(message,reasoner,query,responseDestinationString);
			
			mReasonerQueryExecutionHandler.executeReasonerQuery(reasoner, query, responseDestinationString, executionTimeout, memoryLimit, new ReasonerQueryExecutedCallbackEvent(this,executionTask));
			mExecutionState = ExecutionState.STATE_EXECUTING;
		} else {
			mExecutionState = ExecutionState.STATE_IDLE;
			if (!mConnectionError) {
				mConnectionHandler.sendMessage(new FailedEvaluationTaskMessage(reasonerName,queryName,message.getOutputString()));
			}

		}
	}

	
}
