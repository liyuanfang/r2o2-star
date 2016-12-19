package ore.networking;

import ore.competition.*;
import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.evaluation.CompetitionEvaluator;
import ore.interfacing.ReasonerDescription;
import ore.networking.events.*;
import ore.threading.Event;
import ore.threading.EventThread;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class CompetitionExecutionManager extends EventThread implements ExecutionTaskProvider, CompetitionProcessingRequirements {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionExecutionManager.class);
	
	protected CompetitionHandler mCompetitionHandler = null;
	protected Config mConfig = null;
	protected CompetitionEvaluator mEvaluator = null;
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	protected int mHandlerCount = 0;
	protected int mReasonerCount = 0;
	protected int mProcessingReasonerCount = 0;
	protected int mQueryCount = 0;
	protected boolean mFixedHandlerReasonerAssociation = false;
	protected boolean mFinishedExecution = false;
	protected boolean mRestartExecution = false;
	
	protected boolean mRequiresExecutionHandlerPerReasoner = true;
	protected boolean mContinueExecutorLoss = false;
	protected boolean mExecutionStarted = false;
	protected DateTime mExecutionStartingTime = null;
	
	
	protected HashMap<ExecutionTaskHandler,ExecutionTaskHandlerItem> mHandlerItemMap = new HashMap<ExecutionTaskHandler,ExecutionTaskHandlerItem>();
	protected HashSet<ReasonerDescription> mAssociatedReasonerSet = new HashSet<ReasonerDescription>();
	
	protected class ExecutionTaskHandlerItem {	
		protected ExecutionTaskHandler mHandler = null;
		protected ReasonerDescription mAssociatedReasoner = null;
		
		public ExecutionTaskHandlerItem(ExecutionTaskHandler handler) {
			mHandler = handler;
		}
		
		public void clearAssociatedReasoner() {
			mAssociatedReasoner = null;
		}
		
		public ReasonerDescription getAssociatedReasoner() {
			return mAssociatedReasoner;
		}
		
		public void setAssociatedReasoner(ReasonerDescription associatedReasoner) {
			mAssociatedReasoner = associatedReasoner;
		}
		
	}
		
	public CompetitionExecutionManager(CompetitionEvaluator competitionEvaluator, CompetitionHandler competitionHandler, CompetitionStatusUpdater statusUpdater, Config config) {		
		mEvaluator = competitionEvaluator;
		mCompetitionHandler = competitionHandler;
		mStatusUpdater = statusUpdater;
		mConfig = config;
		mReasonerCount = mCompetitionHandler.getReasonerList().size();
		mProcessingReasonerCount = mReasonerCount;
		mQueryCount = mCompetitionHandler.getQueryList().size();
		
		mRequiresExecutionHandlerPerReasoner = ConfigDataValueReader.getConfigDataValueBoolean(mConfig, ConfigType.CONFIG_TYPE_COMPETITION_EXECUTOR_PER_REASONER, true);
		mContinueExecutorLoss = ConfigDataValueReader.getConfigDataValueBoolean(mConfig, ConfigType.CONFIG_TYPE_COMPETITION_CONTINUE_EXECUTOR_LOSS, false);
		if (!mRequiresExecutionHandlerPerReasoner) {
			mContinueExecutorLoss = true;
		}
		
		startThread();
	}	
	
	
	protected void threadStart() {
		super.threadStart();
		
		updateCompetitionStatus();
		for (ReasonerDescription reasoner : mCompetitionHandler.getReasonerList()) {
			updateCompetitionReasonerProgressStatus(reasoner);
		}
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}
	
		
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof RequestExecutionTaskEvent) {
				
				if (!mExecutionStarted) {
					mExecutionStarted = true;
					mExecutionStartingTime = new DateTime(DateTimeZone.UTC);
				}
				
				RequestExecutionTaskEvent rete = (RequestExecutionTaskEvent)e;
				ExecutionTaskHandler handler = rete.getExecutionTaskHandler();
				ExecutionTaskProvidedCallback callback = rete.getExecutionTaskProvidedCallback();
				
								
				if (callback != null) {
					
					boolean validDate = true;
					boolean noValidDateAnymore = false;
					if (canOnlyRunWithinDates()) {
//						Date currentJDate = new Date();
//						String string1 = currentJDate.toString();
//						String string2 = currentJDate.toGMTString();
						DateTime currentDate = new DateTime();
						DateTime startingDate = getDesiredStartingDate();
						DateTime endingDate = getDesiredEndingDate();
						if (startingDate != null) {
							if (currentDate.isBefore(startingDate)) {
								validDate = false;
							}
							if (currentDate.isAfter(endingDate)) {
								validDate = false;
								noValidDateAnymore = true;
							}
						}
					}
					
					
					CompetitionExecutionTask executionTask = null;
					if (validDate) {
						if (mHandlerCount < mProcessingReasonerCount || mHandlerCount > mProcessingReasonerCount) {
							
							if (mFixedHandlerReasonerAssociation) {
								for (Entry<ExecutionTaskHandler,ExecutionTaskHandlerItem> entry : mHandlerItemMap.entrySet()) {
									entry.getValue().clearAssociatedReasoner();
								}
								mAssociatedReasonerSet.clear();
							}
							
							if (mContinueExecutorLoss || mHandlerCount > mProcessingReasonerCount) {
								if (mFixedHandlerReasonerAssociation) {								
									mLogger.info("Switching to free association of execution handlers with reasoners.");
									mFixedHandlerReasonerAssociation = false;
								}					
								executionTask = mCompetitionHandler.getNextCompetitionExecutionTask();
							} else {
								mRestartExecution = true;
							}
						} else {	
							if (!mFixedHandlerReasonerAssociation) {
								mFixedHandlerReasonerAssociation = true;
								mLogger.info("Switching to fixed association of execution handlers with reasoners.");
							}
							ExecutionTaskHandlerItem handlerItem = mHandlerItemMap.get(handler);
							ReasonerDescription associatedReasoner = handlerItem.getAssociatedReasoner();
							Set<ReasonerDescription> completelyProcessedReasonerSet = mCompetitionHandler.getCompletelyProcessedReasonerSet();
							if (associatedReasoner == null) {	
								for (ReasonerDescription reasoner : mCompetitionHandler.getReasonerList()) {
									if (!mAssociatedReasonerSet.contains(reasoner) && !completelyProcessedReasonerSet.contains(reasoner)) {
										mAssociatedReasonerSet.add(reasoner);
										handlerItem.setAssociatedReasoner(reasoner);
										associatedReasoner = reasoner;
										break;
									}
								}
							}							
							if (associatedReasoner != null) {
								executionTask = mCompetitionHandler.getNextCompetitionExecutionTask(associatedReasoner);
							}
						}
					} else {						
						if (noValidDateAnymore) {
							mLogger.info("Processing of competition '{}' run out of time.",mCompetitionHandler.getCompetition().getCompetitionName());
							while ((executionTask = mCompetitionHandler.getNextCompetitionExecutionTask()) != null) {
								mCompetitionHandler.completeCompetitionExecutionTask(executionTask, new CompetitionExecutionReport(executionTask, false, true));
							}
							for (ReasonerDescription reasoner : mCompetitionHandler.getReasonerList()) {
								updateCompetitionReasonerProgressStatus(reasoner);
							}
							mProcessingReasonerCount = mReasonerCount - mCompetitionHandler.getCompletelyProcessedReasonerSet().size();
							executionTask = null;
						} else {
							mLogger.error("Trying to process competition '{}' at an invalid date/time.",mCompetitionHandler.getCompetition().getCompetitionName());
						}
					}
			
					if (executionTask != null) {
						mLogger.info("Scheduling execution of query '{}' for reasoner '{}'.",executionTask.getQuery(),executionTask.getReasonerDescription());
					} else {
						mLogger.info("No execution task available for competition '{}'.",mCompetitionHandler.getCompetition().getCompetitionName());
					}
					callback.provideExecutionTask(executionTask);
				}				
				return true;
				
			} else if (e instanceof ReportExecutionReportEvent) {
				ReportExecutionReportEvent rere = (ReportExecutionReportEvent)e;
				if (rere.getExecutionReport() instanceof CompetitionExecutionReport) {
					CompetitionExecutionReport executionReport = (CompetitionExecutionReport)rere.getExecutionReport();	
					mCompetitionHandler.completeCompetitionExecutionTask(executionReport.getTask(),executionReport);
					mProcessingReasonerCount = mReasonerCount - mCompetitionHandler.getCompletelyProcessedReasonerSet().size();
					updateCompetitionReasonerProgressStatus(executionReport.getTask().getReasonerDescription());
				}
				return true;
				
			} else if (e instanceof AddExecutionHandlerEvent) {
				AddExecutionHandlerEvent aehe = (AddExecutionHandlerEvent)e;	
				ExecutionTaskHandler handler = aehe.getExecutionTaskHandler();
				ExecutionTaskHandlerItem handlerItem = new ExecutionTaskHandlerItem(handler);
				mHandlerItemMap.put(handler, handlerItem);
				mHandlerCount = mHandlerItemMap.size();
				updateCompetitionStatus();
				return true;
				
			} else if (e instanceof PrepareExecutionEvent) {
				PrepareExecutionEvent pee = (PrepareExecutionEvent)e;
				mExecutionStartingTime = pee.getPlannedExecutionTime();
				updateCompetitionStatus();
				return true;				
				
			} else if (e instanceof RemoveExecutionHandlerEvent) {
				RemoveExecutionHandlerEvent rehe = (RemoveExecutionHandlerEvent)e;	
				ExecutionTaskHandler handler = rehe.getExecutionTaskHandler();	
				ExecutionTaskHandlerItem handlerItem = mHandlerItemMap.get(handler);
				if (handlerItem != null) {
					if (handlerItem.getAssociatedReasoner() != null) {
						mAssociatedReasonerSet.remove(handlerItem.getAssociatedReasoner());
					}
				}
				mHandlerItemMap.remove(handler);
				mHandlerCount = mHandlerItemMap.size();				
				
				if (mHandlerCount <= 0) {
					mRestartExecution = false;
					if (!mFinishedExecution && !canProvideMoreExecutionTask()) {
						mFinishedExecution = true;
						updateCompetitionStatus();
						mLogger.info("Execution for competition '{}' finished.",mCompetitionHandler.getCompetition().getCompetitionName());
						if (mEvaluator != null) {
							mEvaluator.evaluateCompetition();
						}
					}
				}
				updateCompetitionStatus();
				
				return true;
			}
		}
		return false;
	}

	
	protected void updateCompetitionStatus() {
		if (mStatusUpdater != null) {
			CompetitionExecutionState executionState = CompetitionExecutionState.COMPETITION_EXECUTION_STATE_WAITING;
			if (mExecutionStartingTime != null) {
				if (mExecutionStarted) {
					executionState = CompetitionExecutionState.COMPETITION_EXECUTION_STATE_QUEUED;
				} else {
					executionState = CompetitionExecutionState.COMPETITION_EXECUTION_STATE_PREPARATION;
				}
			}
			if (mHandlerCount > 0) {
				executionState = CompetitionExecutionState.COMPETITION_EXECUTION_STATE_RUNNING;
			}
			if (mFinishedExecution) {
				executionState = CompetitionExecutionState.COMPETITION_EXECUTION_STATE_FINISHED;
			}
			DateTime currDate = new DateTime(DateTimeZone.UTC);
			long executionTime = 0;
			if (mExecutionStartingTime != null) {	
				executionTime = currDate.getMillis() - mExecutionStartingTime.getMillis();
			}
			CompetitionStatus competitionStatus = new CompetitionStatus(mCompetitionHandler.getCompetition(), executionState, mHandlerCount, mReasonerCount, mQueryCount, currDate, executionTime);
			mStatusUpdater.updateCompetitionStatus(competitionStatus);
		}
	}		
	

	protected void updateCompetitionReasonerProgressStatus(ReasonerDescription reasoner) {
		if (mStatusUpdater != null && reasoner != null) {
			CompetitionReasonerProgressStatus competitionReasonerProgressStatus = mCompetitionHandler.getReasonerProgressStatus(reasoner);
			mStatusUpdater.updateCompetitionReasonerProgressStatus(competitionReasonerProgressStatus);
		}
	}		

	@Override
	public void postExecutionTaskRequest(ExecutionTaskHandler executionHandler, ExecutionTaskProvidedCallback callback) {
		postEvent(new RequestExecutionTaskEvent(executionHandler,callback));
	}


	@Override
	public void postExecutionReport(ExecutionTaskHandler executionHandler, ExecutionReport executionReport) {
		postEvent(new ReportExecutionReportEvent(executionHandler,executionReport));
	}


	@Override
	public void postExecutionTaskHandlerAddition(ExecutionTaskHandler executionHandler) {
		postEvent(new AddExecutionHandlerEvent(executionHandler));
	}


	@Override
	public void postExecutionTaskHandlerRemovement(ExecutionTaskHandler executionHandler) {
		postEvent(new RemoveExecutionHandlerEvent(executionHandler));
	}


	@Override
	public boolean canInitProcessing(int executionTaskHandlerCount) {
		if (mRequiresExecutionHandlerPerReasoner) {
			return executionTaskHandlerCount >= mProcessingReasonerCount;
		} else {
			return executionTaskHandlerCount > 0;
		}
	}


	@Override
	public boolean canAddExecutionTaskHandler(int totalCount) {
		if (mRestartExecution) {
			return false; 
		} else {
			if (mRequiresExecutionHandlerPerReasoner) {
				return totalCount <= mProcessingReasonerCount && canProvideMoreExecutionTask();
			} else {
				return canProvideMoreExecutionTask();
			}
		}
	}


	@Override
	public boolean canProvideMoreExecutionTask() {
		if (mFinishedExecution) {
			return false;
		}
		return mCompetitionHandler.getMaximumRemainingTaskCount() > 0;
	}


	@Override
	public String getProviderName() {
		return mCompetitionHandler.getCompetition().getCompetitionName();
	}


	@Override
	public DateTime getDesiredStartingDate() {
		return mCompetitionHandler.getCompetition().getDesiredStartingDate();
	}


	@Override
	public DateTime getDesiredEndingDate() {
		return mCompetitionHandler.getCompetition().getDesiredEndingDate();
	}


	@Override
	public boolean canOnlyRunWithinDates() {
		return mCompetitionHandler.getCompetition().isRunningOnlyAllowedWithinDates();
	}


	@Override
	public void postExecutionPreparation(DateTime plannedExecutionTime) {
		postEvent(new PrepareExecutionEvent(plannedExecutionTime));
	}



	
}
