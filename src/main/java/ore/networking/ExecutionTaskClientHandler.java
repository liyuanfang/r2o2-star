package ore.networking;

import ore.competition.CompetitionExecutionReport;
import ore.competition.CompetitionExecutionTask;
import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.interfacing.ReasonerDescription;
import ore.networking.events.AssociateExecutionTaskProviderEvent;
import ore.networking.events.CommunicationErrorEvent;
import ore.networking.events.ExecutionTaskProvidedCallbackEvent;
import ore.networking.events.ProcessMessageEvent;
import ore.networking.messages.Message;
import ore.networking.messages.MessageType;
import ore.networking.messages.ProcessEvaluationTaskMessage;
import ore.networking.messages.ProcessedEvaluationTaskMessage;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.threading.Event;
import ore.threading.EventThread;
import ore.utilities.FilePathString;
import ore.utilities.RelativeFilePathStringType;
import ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;

public class ExecutionTaskClientHandler extends EventThread implements ExecutionTaskHandler, CommunicationErrorHandler, MessageHandler {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ExecutionTaskClientHandler.class);
	
	protected ConnectionHandler mConnectionHandler = null;
	protected boolean mCommunicationError = false;
	protected Socket mSocket = null;
	protected Config mConfig = null;
	protected ExecutionTaskProvider mAssociatedProvider = null;
	protected boolean mPendingExecutionTaskRequest = false;
	
	protected ExecutionTaskScheduler mExecutionScheduler = null;
	
	protected CompetitionExecutionTask mCurrentCompetitionExecutionTask = null;
	
		
	public ExecutionTaskClientHandler(Socket socket, ExecutionTaskScheduler executionScheduler, Config config) {
		mConfig = config;
		mSocket = socket;
		mExecutionScheduler = executionScheduler;
		startThread();
	}	
	
	
	protected void threadStart() {
		super.threadStart();
		
		mConnectionHandler = new ConnectionHandler(mSocket, new DefaultMessageParsingFactory(), new DefaultMessageSerializer(), this, this);
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof CommunicationErrorEvent) {
				mCommunicationError = true;
				mExecutionScheduler.postRemoveHandler(this);
				if (mCurrentCompetitionExecutionTask != null && mAssociatedProvider != null) {
					mAssociatedProvider.postExecutionReport(this, new CompetitionExecutionReport(mCurrentCompetitionExecutionTask, false));
				}
				mAssociatedProvider = null;
				mPendingExecutionTaskRequest = false;
				mLogger.error("Error occured while communication with network client, connection closed.");
				stopThread();
				return true;
			} else if (e instanceof AssociateExecutionTaskProviderEvent) {
				AssociateExecutionTaskProviderEvent aetpe = (AssociateExecutionTaskProviderEvent)e;
				mAssociatedProvider = aetpe.getExecutionTaskProvider();
				if (mPendingExecutionTaskRequest) {
					mPendingExecutionTaskRequest = false;
					requestNextExecutionTask();
				}
				return true;
			} else if (e instanceof ExecutionTaskProvidedCallbackEvent) {
				ExecutionTaskProvidedCallbackEvent etpce = (ExecutionTaskProvidedCallbackEvent)e;
				ExecutionTask executionTask = etpce.getExecutionTask();
				if (executionTask != null) {
					startProcessingExecutionTask(executionTask);
				} else {
					mPendingExecutionTaskRequest = true;
					mExecutionScheduler.postProviderRequest(this);
				}
				return true;
			} else if (e instanceof ProcessMessageEvent) {
				ProcessMessageEvent pme = (ProcessMessageEvent)e;
				if (!mCommunicationError) {
					processMessage(pme.getMessage());
				}
				return true;
			}
		}
		return false;
	}


	public void startProcessingExecutionTask(ExecutionTask executionTask) {
		if (executionTask instanceof CompetitionExecutionTask) {
			mCurrentCompetitionExecutionTask = (CompetitionExecutionTask)executionTask;
			
			String outputString = mCurrentCompetitionExecutionTask.getOutputString();
			ReasonerDescription reasoner = mCurrentCompetitionExecutionTask.getReasonerDescription();
			Query query = mCurrentCompetitionExecutionTask.getQuery();
			long executionTimeout = mCurrentCompetitionExecutionTask.getExecutionTimeout();
			long memoryLimit = mCurrentCompetitionExecutionTask.getMemoryLimit();
			
			String reasonerString = reasoner.getSourceFilePathString().getPreferedRelativeFilePathString();
			String queryString = query.getQuerySourceString().getPreferedRelativeFilePathString();
			
			mConnectionHandler.sendMessage(new ProcessEvaluationTaskMessage(reasonerString,queryString,outputString,executionTimeout,memoryLimit));
			
		}
	}

	public void requestNextExecutionTask() {
		if (!mCommunicationError) {
			mAssociatedProvider.postExecutionTaskRequest(this, new ExecutionTaskProvidedCallbackEvent(this));			
		}
	}
	
	public void processMessage(Message message) {
		if (message.getMessageType() == MessageType.MESSAGE_TYPE_PROCESSED_EVALUATION_TASK) {
			ProcessedEvaluationTaskMessage petm = (ProcessedEvaluationTaskMessage)message;
			
			if (mCurrentCompetitionExecutionTask != null && mAssociatedProvider != null) { 
				String responseDirectory = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY);
				String responseDestinationString = responseDirectory+mCurrentCompetitionExecutionTask.getOutputString();
				
				FilePathString resultDataFileString = new FilePathString(responseDestinationString, petm.getResultDataFileString(), RelativeFilePathStringType.RELATIVE_TO_BASE_DIRECTORY);
				FilePathString reportFileString = new FilePathString(responseDestinationString, petm.getReportFileString(), RelativeFilePathStringType.RELATIVE_TO_BASE_DIRECTORY);
				FilePathString logFileString = new FilePathString(responseDestinationString, petm.getLogFileString(), RelativeFilePathStringType.RELATIVE_TO_BASE_DIRECTORY);
				FilePathString errorFileString = new FilePathString(responseDestinationString, petm.getErrorFileString(), RelativeFilePathStringType.RELATIVE_TO_BASE_DIRECTORY);			
				QueryResponse queryResponse = new QueryResponse(resultDataFileString, reportFileString, logFileString, errorFileString, petm.getInterface());
				queryResponse.setExecutionCompleted(petm.getExecutionCompleted());
				queryResponse.setExecutionError(petm.getExecutionError());
				queryResponse.setExecutionTime(petm.getExecutionTime());
				queryResponse.setReasonerConsoleOutputAvailable(petm.getReasonerConsoleOutputAvailable());
				queryResponse.setReasonerErrorsAvailable(petm.getReasonerErrorsAvailable());
				queryResponse.setReasonerOutputParsingError(petm.getReasonerOutputParsingError());
				queryResponse.setReasonerQueryCompleted(petm.getReasonerQueryCompleted());
				queryResponse.setReasonerQueryProcessingTime(petm.getReasonerQueryProcessingTime());
				queryResponse.setReasonerQueryStarted(petm.getReasonerQueryStarted());
				queryResponse.setResultDataAvailable(petm.getResultDataAvailable());
				queryResponse.setTimedOut(petm.getTimedOut());
				queryResponse.setExecutionStartDateTime(petm.getExecutionStartTime());
				queryResponse.setExecutionEndDateTime(petm.getExecutionEndTime());
				
				QueryResultVerificationReport verificationReport = new QueryResultVerificationReport(petm.getResultCorrectness(), petm.getResultHashCode(), petm.getReasonerSupport());
				
				CompetitionExecutionReport executionReport = new CompetitionExecutionReport(mCurrentCompetitionExecutionTask,true,queryResponse,verificationReport);
				mAssociatedProvider.postExecutionReport(this, executionReport);		
			}
			mCurrentCompetitionExecutionTask = null;			
		} else if (message.getMessageType() == MessageType.MESSAGE_TYPE_REQUEST_EVALUATION_TASK) {
			if (!mCommunicationError) {
				if (mAssociatedProvider != null) {
					requestNextExecutionTask();
				} else {
					mPendingExecutionTaskRequest = true;
				}
			}
		} else if (message.getMessageType() == MessageType.MESSAGE_TYPE_FAILED_EVALUATION_TASK) {
			if (mCurrentCompetitionExecutionTask != null && mAssociatedProvider != null) {
				mAssociatedProvider.postExecutionReport(this, new CompetitionExecutionReport(mCurrentCompetitionExecutionTask, false));	
			}
		}
	}	

	
	@Override
	public void handleMessage(Message message) {
		postEvent(new ProcessMessageEvent(message));
	}


	@Override
	public void handleCommunicationError(Throwable exception) {
		postEvent(new CommunicationErrorEvent(exception));
	}


	@Override
	public void postAssociatedExecutionTaskProvider(ExecutionTaskProvider provider) {
		postEvent(new AssociateExecutionTaskProviderEvent(provider));
	}



	
}
