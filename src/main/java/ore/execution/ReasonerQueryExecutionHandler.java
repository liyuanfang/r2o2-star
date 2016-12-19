package ore.execution;

import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.conversion.OntologyFormatRedirector;
import ore.execution.events.ReasonerProcessCompletedEvent;
import ore.execution.events.ReasonerProcessExecutionExceptionEvent;
import ore.execution.events.ReasonerProcessExecutionIOExceptionEvent;
import ore.execution.events.ReasonerProcessInitialiseEvent;
import ore.interfacing.ReasonerAdaptor;
import ore.interfacing.ReasonerAdaptorFactory;
import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.querying.QueryResponseStoringHandler;
import ore.threading.Event;
import ore.threading.EventThread;
import ore.utilities.FilePathString;
import org.apache.commons.exec.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ReasonerQueryExecutionHandler extends EventThread {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerQueryExecutionHandler.class);
	
	
	
	protected ReasonerAdaptorFactory mReasonerAdaptorFactory = null;	
	protected OntologyFormatRedirector mFormatRedirector = null;
	protected QueryResponseStoringHandler mResponseStoringHandler = null;
	
	protected Config mConfig = null;
	protected Query mQuery = null;
	protected ExecuteWatchdog mWatchDog = null;
	protected DefaultExecutor mExecutor = null;
	protected ReasonerAdaptor mReasonerAdaptor = null;
	protected ReasonerDescription mReasoner = null;
	
	protected ReasonerQueryExecutionReport mExecutionReport = null;
	protected boolean mExecutionActive = false;
	protected ReasonerQueryExecutedCallback mCallback = null;
	
	protected long mStartReasonerExecutionTime = 0;
	protected long mFinishReasonerExecutionTime = 0;
	
	protected long mExecutionTimeout = 0;
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mMemoryLimit = 0;
	protected long mDefaultMemoryLimit = 10737418240L;
	
	protected boolean mLoadResults = true;
	
	private DateTime mExecutionEndDateTime = null;
	private DateTime mExecutionStartDateTime = null;


	public void executeReasonerQuery(ReasonerDescription reasoner, Query query, String responseDestinationString, long executionTimeout, long memoryLimit, ReasonerQueryExecutedCallback callback) {
		postEvent(new ReasonerProcessInitialiseEvent(query, reasoner, responseDestinationString, executionTimeout, memoryLimit, callback));
	}

	
	public QueryResponse executeReasonerQuery(ReasonerDescription reasoner, Query query, String responseDestinationString) {
		return executeReasonerQuery(reasoner, query, responseDestinationString, 0, 0);
	}
	

	public QueryResponse executeReasonerQuery(ReasonerDescription reasoner, Query query, String responseDestinationString, long executionTimeout, long memoryLimit) {
		QueryResponse queryResponse = null;
		ReasonerQueryExecutedBlockingCallback callback = new ReasonerQueryExecutedBlockingCallback();
		executeReasonerQuery(reasoner,query,responseDestinationString,executionTimeout,memoryLimit,callback);
		try {
			callback.waitForCallback();
			queryResponse = callback.getQueryResponse();
		} catch (InterruptedException e) {
			mLogger.warn("Waiting for execution of query '{}' for reasoner '{}' interrupted.",query.toString(),reasoner.toString());
		}
		return queryResponse;
	}
		
	
	public ReasonerQueryExecutionHandler(ReasonerAdaptorFactory reasonerAdaptorFactory, OntologyFormatRedirector formatRedirector, QueryResponseStoringHandler responseStoringHandler, Config config) {
		mConfig = config;
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);
		mDefaultMemoryLimit = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_EXECUTION_MEMORY_LIMIT,mDefaultMemoryLimit);		

		mLoadResults = ConfigDataValueReader.getConfigDataValueBoolean(mConfig,ConfigType.CONFIG_TYPE_SAVE_LOAD_RESULTS_CODES,mLoadResults);		
		
		
		mReasonerAdaptorFactory = reasonerAdaptorFactory;
		mFormatRedirector = formatRedirector;
		mResponseStoringHandler = responseStoringHandler;
		startThread();
	}
	

	
	protected void threadStart() {
		super.threadStart();
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}	
	
	
	protected boolean isExecutionActive() {
		return mExecutionActive;
	}
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof ReasonerProcessInitialiseEvent) {
				ReasonerProcessInitialiseEvent rpie = (ReasonerProcessInitialiseEvent)e;
				if (isExecutionActive()) {
					mLogger.error("Query execution already in progress, query '{}' for reasoner '{}' cannot be processed.",rpie.getQuery().toString(),rpie.getReasonerDescription().toString());
				} else {
					mCallback = rpie.getCallback();
					if (initReasonerExecution(rpie.getReasonerDescription(), rpie.getQuery(), rpie.getResponseDestinationString(), rpie.getExecutionTimeout(), rpie.getMemoryLimit())) {
						executeReasoner(mReasonerAdaptor);
					}
				}
				return true;
			} else  if (e instanceof ReasonerProcessCompletedEvent) {
				if (isExecutionActive()) {
					mExecutionReport.setTimedOut(mWatchDog.killedProcess());
					mExecutionReport.setExecutionCompleted(true);
					finishReasonerExecution();
				}
				return true;
			} else  if (e instanceof ReasonerProcessExecutionExceptionEvent) {
				ReasonerProcessExecutionExceptionEvent rpeee = (ReasonerProcessExecutionExceptionEvent)e;
				if (isExecutionActive()) {
					mExecutionReport.setExecutionError(true);
					mExecutionReport.setTimedOut(mWatchDog.killedProcess());
					mExecutionReport.addErrorMessage(rpeee.getException().getMessage());
					finishReasonerExecution();
				}
				return true;
			} else  if (e instanceof ReasonerProcessExecutionIOExceptionEvent) {
				ReasonerProcessExecutionIOExceptionEvent rpeioe = (ReasonerProcessExecutionIOExceptionEvent)e;
				if (isExecutionActive()) {
					mExecutionReport.setExecutionError(true);
					mExecutionReport.setTimedOut(mWatchDog.killedProcess());
					mExecutionReport.addErrorMessage(rpeioe.getException().getMessage());
					finishReasonerExecution();
				}
				return true;
			}
		}
		return false;
	}
	
	

	
	protected void finishReasonerExecution() {
		mFinishReasonerExecutionTime = System.currentTimeMillis();
		mExecutionEndDateTime = new DateTime(DateTimeZone.UTC);
		
		mExecutionReport.setExecutionTime(mExecutionEndDateTime.getMillis()-mExecutionStartDateTime.getMillis());
		mExecutionReport.setExecutionStartDataTime(mExecutionStartDateTime);
		mExecutionReport.setExecutionEndDataTime(mExecutionEndDateTime);
		
		
		mReasonerAdaptor.completeReasonerExecution();
		
		boolean failed = false;
		
		if (mExecutionReport.hasTimedOut()) {
			failed = true;
			mLogger.warn("Execution of query '{}' for reasoner '{}' timed out.", mQuery.toString(),mReasoner.toString());
		}
		
		
		if (mExecutionReport.hasExecutionError()) {
			failed = true;
			mLogger.error("Execution of query '{}' for reasoner '{}' failed, got error '{}'.", new Object[]{mQuery.toString(),mReasoner.toString(),mExecutionReport.getErrorString()});
		} 
		
		
		if (!failed) {
			mLogger.info("Successfully executed query '{}' for reasoner '{}'.", mQuery.toString(),mReasoner.toString());
		}
		
		QueryResponse response = mReasonerAdaptor.createQueryResponse(mExecutionReport);
		
		if (response != null) {
			mResponseStoringHandler.saveQueryResponseData(response);
		}
		
		if (mCallback != null) {
			mCallback.reasonerQueryExecuted(response);
		}
		
		mExecutionActive = false;		
	}
	
	
	protected boolean initReasonerExecution(ReasonerDescription reasoner, Query query, String responseDestinationString, long executionTimeout, long memoryLimit) {
		mExecutionActive = false;
		mQuery = query;
		mReasoner = reasoner;
		
		mExecutionTimeout = executionTimeout;
		if (mExecutionTimeout <= 0) {
			mExecutionTimeout = mDefaultExecutionTimeout;
		}
		mMemoryLimit = memoryLimit;
		if (mMemoryLimit <= 0) {
			mMemoryLimit = mDefaultMemoryLimit;
		}
		
		mWatchDog = new ExecuteWatchdog(mExecutionTimeout);
		
		mLogger.info("Preparing execution of query '{}' for reasoner '{}' with time limit '{}' and memory limit '{}'.", new Object[]{mQuery.toString(),mReasoner.toString(),mExecutionTimeout,mMemoryLimit});
		
		mReasonerAdaptor = mReasonerAdaptorFactory.getReasonerAdapter(mReasoner, mQuery, responseDestinationString, mFormatRedirector);
		
		mExecutionReport = new ReasonerQueryExecutionReport();
		
		if (mLoadResults) {
			String responseFileString = mReasonerAdaptor.getResponseFileString();
			File responseFile = new File(responseFileString);
			if (responseFile.isFile() && responseFile.exists() && responseFile.length() > 0) {
				mLogger.info("Query response '{}' already exists, trying to load existing response.", responseFileString);
				QueryResponse loadedQueryResponse = mResponseStoringHandler.loadQueryResponseData(new FilePathString(responseFileString));
				if (loadedQueryResponse != null) {
					mLogger.info("Successfully loaded query response from file '{}' for reasoner '{}', skipping evaluation for query '{}'.", new Object[]{responseFileString,mReasoner.toString(),mQuery});
					if (mCallback != null) {
						mCallback.reasonerQueryExecuted(loadedQueryResponse);
					}
					return false;
				}
			}

		}
		
		return true;
	}
	
	
	protected boolean executeReasoner(ReasonerAdaptor adpater) {
		mExecutionActive = true;
		
		// command to be executed
		CommandLine commandLine = new CommandLine(adpater.getReasonerExecutionCommandString());
		
		boolean addTimeoutAsArgument = ConfigDataValueReader.getConfigDataValueBoolean(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_ADD_TIMEOUT_AS_ARGUMENT, false);
		if (addTimeoutAsArgument) {	
			commandLine.addArgument(new Long(mExecutionTimeout+1000).toString());
		}
		
		
		boolean addMemoryLimitAsArgument = ConfigDataValueReader.getConfigDataValueBoolean(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_ADD_MEMORY_LIMIT_AS_ARGUMENT, false);
		if (addMemoryLimitAsArgument) {	
			commandLine.addArgument(new Long(mMemoryLimit).toString());
		}
		
		// adding its arguments
		commandLine.addArguments(adpater.getReasonerExecutionArguments().toArray(new String[0]));
		
		PumpStreamHandler streamHandler = new PumpStreamHandler(adpater.getReasonerExecutionOutputStream(), adpater.getReasonerExecutionErrorStream(), adpater.getReasonerExecutionInputStream());
		
		// this is used to end the process when the JVM exits
		ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
		
		// our main command executor
		mExecutor = new DefaultExecutor();
		
		// setting the properties
		mExecutor.setStreamHandler(streamHandler);
		mExecutor.setWatchdog(mWatchDog);
		 
		// setting the working directory
		mExecutor.setWorkingDirectory(new File(adpater.getReasonerExecutionWorkingDirectoryString()));
		mExecutor.setProcessDestroyer(processDestroyer);
		
		mStartReasonerExecutionTime = System.currentTimeMillis();
		mExecutionStartDateTime = new DateTime(DateTimeZone.UTC);
		
		// executing the command
		try {
			mExecutor.execute(commandLine, new ExecuteResultHandler() {
				@Override
				public void onProcessComplete(int exitValue) {
					postEvent(new ReasonerProcessCompletedEvent(exitValue));					
				}
				@Override
				public void onProcessFailed(ExecuteException executionException) {
					postEvent(new ReasonerProcessExecutionExceptionEvent(executionException));
				}				
			});
			mLogger.info("Started execution of query '{}' for reasoner '{}'.", mQuery.toString(),mReasoner.toString());
			
		} catch (ExecuteException e) {
			postEvent(new ReasonerProcessExecutionExceptionEvent(e));
		} catch (IOException e) {
			postEvent(new ReasonerProcessExecutionIOExceptionEvent(e));
		}
		
		return true;
	}


	
}
