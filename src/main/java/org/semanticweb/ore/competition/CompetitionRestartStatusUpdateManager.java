package org.semanticweb.ore.competition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;

import org.joda.time.DateTime;
import org.semanticweb.ore.competition.events.UpdateCompetitionEvaluationStatusEvent;
import org.semanticweb.ore.competition.events.UpdateCompetitionReasonerProgressStatusEvent;
import org.semanticweb.ore.competition.events.UpdateCompetitionStatusEvent;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.evaluation.CompetitionEvaluatorHandler;
import org.semanticweb.ore.evaluation.QueryResultStorage;
import org.semanticweb.ore.interfacing.ReasonerDescriptionManager;
import org.semanticweb.ore.networking.CompetitionExecutionManager;
import org.semanticweb.ore.networking.ServerExecutionManager;
import org.semanticweb.ore.querying.QueryManager;
import org.semanticweb.ore.threading.Event;
import org.semanticweb.ore.threading.EventThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionRestartStatusUpdateManager extends EventThread implements CompetitionStatusUpdater {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionRestartStatusUpdateManager.class);
	protected Config mConfig = null;
	
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	protected CompetitionStatusUpdater mRestartStatusUpdater = null;
	protected ServerExecutionManager mServerExecutionManager;
	
	
	protected HashMap<String,CompetitionStatus> mLastCompetitionStatusMap = new HashMap<String,CompetitionStatus>();
	protected HashMap<String,CompetitionReasonerProgressStatus> mLastCompetitionReasonerStatusMap = new HashMap<String,CompetitionReasonerProgressStatus>();
	
	
	protected HashSet<String> mCompetitionRestartSet = new HashSet<String>();
		
	
	protected HashMap<String,DateTime> mCompetitionFirstRunningDateMap = new HashMap<String,DateTime>();
	
	protected ArrayList<Competition> mCompetitionRepeatList = new ArrayList<Competition>();
	protected long mRepeatDelayMilliseconds = 1000*60*5;
	
	
	protected ReasonerDescriptionManager mReasonerManager = null;
	protected QueryManager mQueryManager = null;
	
	
		
	public CompetitionRestartStatusUpdateManager(ServerExecutionManager serverExecutionManager, ReasonerDescriptionManager reasonerManager, QueryManager queryManager, CompetitionStatusUpdater statusUpdater, Collection<Competition> competitionRepeatCollection, Config config) {
		mServerExecutionManager = serverExecutionManager;
		mStatusUpdater = statusUpdater;
		mConfig = config;	
		mCompetitionRepeatList.addAll(competitionRepeatCollection);
		mReasonerManager = reasonerManager;
		mQueryManager = queryManager;
		mRestartStatusUpdater = this;
		startThread();
	}	
	
	
	protected void threadStart() {
		super.threadStart();		
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}	
	
	
	protected void checkRestartCompetition() {
		
		if (mCompetitionRestartSet.isEmpty()) {
			
			boolean allCompetitionsFinished = true;
			for (CompetitionStatus compStatus : mLastCompetitionStatusMap.values()) {
				if (compStatus.getExecutionState() != CompetitionExecutionState.COMPETITION_EXECUTION_STATE_FINISHED) {
					allCompetitionsFinished = false;
					break;
				}
			}
			
			if (allCompetitionsFinished) {
				
				
				
				if (!mCompetitionRepeatList.isEmpty()) {
					ListIterator<Competition> nextCompIt = mCompetitionRepeatList.listIterator();
					Competition nextRepeatCompetition = nextCompIt.next();
					nextCompIt.remove();
					final Competition newNextCompetition = new Competition(nextRepeatCompetition);		
					DateTime startDateTime = new DateTime().plus(mRepeatDelayMilliseconds);
					newNextCompetition.setDesiredStartingDate(startDateTime);
					if (nextRepeatCompetition.getDesiredEndingDate() != null && nextRepeatCompetition.getDesiredStartingDate() != null) {
						DateTime endDateTime = startDateTime.plus(nextRepeatCompetition.getDesiredEndingDate().getMillis() - nextRepeatCompetition.getDesiredStartingDate().getMillis());
						newNextCompetition.setDesiredEndingDate(endDateTime);
					}
					mCompetitionRepeatList.add(newNextCompetition);
					
					mLogger.info("Scheduling restart of competition '{}'.",newNextCompetition.getCompetitionName());
					
					mCompetitionRestartSet.add(newNextCompetition.getCompetitionSourceString().getAbsoluteFilePathString());
	
					new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								Thread.sleep(mRepeatDelayMilliseconds/2);
							} catch (InterruptedException e) {
							}
							CompetitionHandler competitionHandler = new CompetitionHandler(mQueryManager,mReasonerManager,mConfig);
							QueryResultStorage resultStorage = new QueryResultStorage();
							if (competitionHandler.initCompetition(newNextCompetition, resultStorage)) {
								
								CompetitionEvaluatorHandler competitionEvaluatorHandler = new CompetitionEvaluatorHandler(competitionHandler,newNextCompetition,resultStorage,mRestartStatusUpdater,null,mConfig);
								
								CompetitionExecutionManager competitionExecutionManager = new CompetitionExecutionManager(competitionEvaluatorHandler,competitionHandler,mRestartStatusUpdater,mConfig);
								mServerExecutionManager.postSchedulingRequest(competitionExecutionManager, competitionExecutionManager);
							}
						}
						
					}).start();
					

				}
			}
		}
	}
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof UpdateCompetitionStatusEvent) {
				UpdateCompetitionStatusEvent ucse = (UpdateCompetitionStatusEvent)e;
				CompetitionStatus status = ucse.getCompetitionStatus();
				mLastCompetitionStatusMap.put(status.getCompetitionSourceString(),status);
				
				if (status.getExecutionState() == CompetitionExecutionState.COMPETITION_EXECUTION_STATE_QUEUED) {
					mCompetitionFirstRunningDateMap.put(status.getCompetitionSourceString(), null);				
					mCompetitionRestartSet.remove(status.getCompetitionSourceString());
				} else if (status.getExecutionState() == CompetitionExecutionState.COMPETITION_EXECUTION_STATE_RUNNING) {
					DateTime dateTime = mCompetitionFirstRunningDateMap.get(status.getCompetitionSourceString());
					if (dateTime != null) {
						mCompetitionFirstRunningDateMap.put(status.getCompetitionSourceString(), new DateTime());
					}
				}
				
				mStatusUpdater.updateCompetitionStatus(status);
				checkRestartCompetition();
				return true;
			} else if (e instanceof UpdateCompetitionReasonerProgressStatusEvent) {
				UpdateCompetitionReasonerProgressStatusEvent ucrpse = (UpdateCompetitionReasonerProgressStatusEvent)e;
				CompetitionReasonerProgressStatus status = ucrpse.getCompetitionReasonerProgressStatus();
				mLastCompetitionReasonerStatusMap.put(status.getCompetitionSourceString()+":"+status.getReasonerSourceString(),status);
				mStatusUpdater.updateCompetitionReasonerProgressStatus(status);
				checkRestartCompetition();
				return true;
			} else if (e instanceof UpdateCompetitionEvaluationStatusEvent) {
				UpdateCompetitionEvaluationStatusEvent ucese = (UpdateCompetitionEvaluationStatusEvent)e;
				CompetitionEvaluationStatus status = ucese.getCompetitionEvaluationStatus();
				mStatusUpdater.updateCompetitionEvaluationStatus(status);
				return true;
			}
		}
		return false;
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
