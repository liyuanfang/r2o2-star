package org.semanticweb.ore.competition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.semanticweb.ore.competition.events.AddCompetitionStatusUpdateListnerEvent;
import org.semanticweb.ore.competition.events.RemoveCompetitionStatusUpdateListnerEvent;
import org.semanticweb.ore.competition.events.UpdateCompetitionEvaluationStatusEvent;
import org.semanticweb.ore.competition.events.UpdateCompetitionReasonerProgressStatusEvent;
import org.semanticweb.ore.competition.events.UpdateCompetitionStatusEvent;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.threading.Event;
import org.semanticweb.ore.threading.EventThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionStatusUpdateCollectionManager extends EventThread implements CompetitionStatusUpdater {
	
	@SuppressWarnings("unused")
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionStatusUpdateCollectionManager.class);
	
	protected Config mConfig = null;
	protected CompetitionStatusUpdater mStatusUpdater = null;
	


	protected int mNextCompetitionID = 0;
	protected long mCurrentUpdateID = 1;
	protected ConcurrentHashMap<String,CompetitionStatusUpdateItem> mLastCompetitionStatusItemMap = new ConcurrentHashMap<String,CompetitionStatusUpdateItem>();
	
	protected boolean mAllFinished = false;;
	
	
	protected HashSet<CompetitionStatusUpdateListner> mListerSet = new HashSet<CompetitionStatusUpdateListner>();

	
	
	protected class ResortReasonerStatusUpdateItem implements Comparable<ResortReasonerStatusUpdateItem> {	
		CompetitionReasonerProgressStatus mStatus = null;
		long mProcessedTime = 0;
		int mCorrectlyProccessedCount = 0;
		int mRank = 0;
		int mPosition = 0;
		boolean mUpdated = false;
		long mLastUpdateID = 0;
		
		public ResortReasonerStatusUpdateItem(CompetitionReasonerProgressStatus status, long lastUpdateID) {
			mStatus = status;
			mProcessedTime = mStatus.getCorrectlyProccessedTime();
			mCorrectlyProccessedCount = mStatus.getCorrectlyProcessedCount();
			mLastUpdateID = lastUpdateID;
		}
		
		public ResortReasonerStatusUpdateItem(CompetitionReasonerProgressStatus status, boolean updated) {
			mStatus = status;
			mProcessedTime = mStatus.getCorrectlyProccessedTime();
			mCorrectlyProccessedCount = mStatus.getCorrectlyProcessedCount();
			mUpdated = true;
		}
		
		public CompetitionReasonerProgressStatus getReasonerStatus() {
			return mStatus;
		}
		
		public long getLastUpdateID() {
			return mLastUpdateID;
		}
		
		public int getRank() {
			return mRank;
		}
		
		public int getPosition() {
			return mPosition;
		}
		
		public boolean isUpdated() {
			return mUpdated;
		}
		
		public void setUpdated(boolean updated) {
			mUpdated = updated;
		}
		

		public boolean hasSameRank(ResortReasonerStatusUpdateItem data) {
			if (mCorrectlyProccessedCount > data.mCorrectlyProccessedCount) {
				return false;
			} else if (mCorrectlyProccessedCount < data.mCorrectlyProccessedCount) {
				return false;
			} else {
				if (mProcessedTime < data.mProcessedTime) {
					return false;
				} else if (mProcessedTime > data.mProcessedTime) {
					return false;
				} else {
					return true;
				}
			}
		}		
		
		@Override
		public int compareTo(ResortReasonerStatusUpdateItem data) {
			if (mCorrectlyProccessedCount > data.mCorrectlyProccessedCount) {
				return -1;
			} else if (mCorrectlyProccessedCount < data.mCorrectlyProccessedCount) {
				return 1;
			} else {
				if (mProcessedTime < data.mProcessedTime) {
					return -1;
				} else if (mProcessedTime > data.mProcessedTime) {
					return 1;
				} else {
					return mStatus.getReasonerName().toUpperCase().compareTo(data.mStatus.getReasonerName().toUpperCase());
				}
			}
		}
	}	
	

	
	public long getCurrentUpdateID() {
		return mCurrentUpdateID;
	}
	
	
	public Collection<CompetitionStatusUpdateItem> getUpdatedCompetitionStatusItemCollection(long lastUpdatedID) {
		ArrayList<CompetitionStatusUpdateItem> updatedList = new ArrayList<CompetitionStatusUpdateItem>();
		for (CompetitionStatusUpdateItem compStatUpItem : mLastCompetitionStatusItemMap.values()) {
			if (compStatUpItem.isCompetitionUpdated(lastUpdatedID) || compStatUpItem.isReasonerUpdated(lastUpdatedID) || compStatUpItem.isEvaluationUpdated(lastUpdatedID)) {
				updatedList.add(compStatUpItem);
			}
		}
		return updatedList;
	}
	
	
	public boolean allCompetitionsFinished() {
		if (mAllFinished) {
			return true;
		}
		return mAllFinished;
	}
	
	public void addUpdateListner(CompetitionStatusUpdateListner listner) {
		postEvent(new AddCompetitionStatusUpdateListnerEvent(listner));
	}

	public void removeUpdateListner(CompetitionStatusUpdateListner listner) {
		postEvent(new RemoveCompetitionStatusUpdateListnerEvent(listner));
	}	
	
		
	public CompetitionStatusUpdateCollectionManager(CompetitionStatusUpdater statusUpdater, Config config) {		
		mStatusUpdater = statusUpdater;
		mConfig = config;	
		startThread();
	}	
	
	public CompetitionStatusUpdateCollectionManager(Config config) {		
		mConfig = config;	
		startThread();
	}	
	
	protected void threadStart() {
		super.threadStart();
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof UpdateCompetitionStatusEvent) {	
				UpdateCompetitionStatusEvent ucse = (UpdateCompetitionStatusEvent)e;
				CompetitionStatus status = ucse.getCompetitionStatus();
				mAllFinished = false;
				processCompetitionStatusUpdate(status);
				checkAllCompetitionFinished();
				notifyUpdateListner();				
				return true;				
			} else if (e instanceof UpdateCompetitionReasonerProgressStatusEvent) {	
				UpdateCompetitionReasonerProgressStatusEvent ucrpse = (UpdateCompetitionReasonerProgressStatusEvent)e;
				CompetitionReasonerProgressStatus status = ucrpse.getCompetitionReasonerProgressStatus();
				mAllFinished = false;
				processReasonerProgressStatusUpdate(status);
				checkAllCompetitionFinished();
				notifyUpdateListner();				
				return true;
			}  else if (e instanceof UpdateCompetitionEvaluationStatusEvent) {	
				UpdateCompetitionEvaluationStatusEvent ucese = (UpdateCompetitionEvaluationStatusEvent)e;
				CompetitionEvaluationStatus status = ucese.getCompetitionEvaluationStatus();
				processEvaluationStatusUpdate(status);
				notifyUpdateListner();				
				return true;
			} else if (e instanceof AddCompetitionStatusUpdateListnerEvent) {	
				AddCompetitionStatusUpdateListnerEvent acsule = (AddCompetitionStatusUpdateListnerEvent)e;
				CompetitionStatusUpdateListner listner = acsule.getCompetitionStatusUpdateListner();
				mListerSet.add(listner);
				return true;
			} else if (e instanceof RemoveCompetitionStatusUpdateListnerEvent) {	
				RemoveCompetitionStatusUpdateListnerEvent rcsule = (RemoveCompetitionStatusUpdateListnerEvent)e;
				CompetitionStatusUpdateListner listner = rcsule.getCompetitionStatusUpdateListner();
				mListerSet.remove(listner);
				return true;
			}
		}
		return false;
	}
	
	
	
	protected void checkAllCompetitionFinished() {
		boolean allFinished = true;
		for (CompetitionStatusUpdateItem compStatUpItem : mLastCompetitionStatusItemMap.values()) {
			if (compStatUpItem.getCompetitionStatus().getExecutionState() != CompetitionExecutionState.COMPETITION_EXECUTION_STATE_FINISHED) {
				allFinished = false;
				break;
			}
		}
		mAllFinished = allFinished;
	}	
	

	protected void notifyUpdateListner() {
		for (CompetitionStatusUpdateListner listner : mListerSet) {
			listner.notifyUpdated();
		}		
	}
	
	protected void processReasonerProgressStatusUpdate(CompetitionReasonerProgressStatus reasonerStatus) {
		CompetitionStatusUpdateItem lastCompStatusItem = mLastCompetitionStatusItemMap.get(reasonerStatus.getCompetitionSourceString());
		if (lastCompStatusItem != null) {
			
			long nextUpdateID = mCurrentUpdateID+1;
			
			CompetitionStatus compStatus = lastCompStatusItem.getCompetitionStatus();
			Vector<CompetitionReasonerProgressStatusUpdateItem> lastReasonerProgressUpdateItemVector = lastCompStatusItem.getReasonerProgressUpdateItemVector();
			
			
			if (lastReasonerProgressUpdateItemVector == null) {
				Vector<CompetitionReasonerProgressStatusUpdateItem> newReasonerProgressUpdateItemVector = new Vector<CompetitionReasonerProgressStatusUpdateItem>();
				newReasonerProgressUpdateItemVector.setSize(compStatus.getReasonerCount());
				newReasonerProgressUpdateItemVector.set(reasonerStatus.getReasonerPosition()-1, new CompetitionReasonerProgressStatusUpdateItem(reasonerStatus, nextUpdateID));
				CompetitionStatusUpdateItem nextCompStatusItem = new CompetitionStatusUpdateItem(compStatus,lastCompStatusItem.getCompetitionID(),newReasonerProgressUpdateItemVector,lastCompStatusItem.getEvaluationMap(),lastCompStatusItem.getCompetitionUpdateID(),nextUpdateID,lastCompStatusItem.getEvaluationUpdateID());
				mLastCompetitionStatusItemMap.put(compStatus.getCompetitionSourceString(), nextCompStatusItem);
			} else {
				
			
				
				ArrayList<ResortReasonerStatusUpdateItem> statusUpdateItemList = new ArrayList<ResortReasonerStatusUpdateItem>(); 
				statusUpdateItemList.add(new ResortReasonerStatusUpdateItem(reasonerStatus,true));
				for (int i = 0; i < lastReasonerProgressUpdateItemVector.size(); ++i) {
					CompetitionReasonerProgressStatusUpdateItem reasonerStatusUpdateItem = lastReasonerProgressUpdateItemVector.get(i);
					if (reasonerStatusUpdateItem != null) {
						CompetitionReasonerProgressStatus lastReasonerStatus = reasonerStatusUpdateItem.getCompetitionReasonerProgressStatus();
						if (lastReasonerStatus.getReasonerSourceString().compareTo(reasonerStatus.getReasonerSourceString()) != 0) {
							ResortReasonerStatusUpdateItem statusUpdateItem = new ResortReasonerStatusUpdateItem(lastReasonerStatus,reasonerStatusUpdateItem.getUpdateID());
							statusUpdateItemList.add(statusUpdateItem);
						}
					}
				}
				
		
				Collections.sort(statusUpdateItemList);
				int nextRank = 1;
				for (ResortReasonerStatusUpdateItem statusUpdateItem : statusUpdateItemList) {
					statusUpdateItem.mRank = nextRank;
					statusUpdateItem.mPosition = nextRank;
					++nextRank;
				}
				
				ListIterator<ResortReasonerStatusUpdateItem> statusUpdateItemIt = statusUpdateItemList.listIterator();
				while (statusUpdateItemIt.hasNext()) {
					ResortReasonerStatusUpdateItem statusUpdateItem = statusUpdateItemIt.next();
					ListIterator<ResortReasonerStatusUpdateItem> statusUpdateItemIt2 = statusUpdateItemList.listIterator(statusUpdateItemIt.nextIndex());
					boolean stillIdentical = true;
					int identicalCount = 0;
					while (statusUpdateItemIt2.hasNext() && stillIdentical) {
						ResortReasonerStatusUpdateItem statusUpdateItem2 = statusUpdateItemIt2.next();
						if (statusUpdateItem.hasSameRank(statusUpdateItem2)) {
							identicalCount++;
						} else {
							stillIdentical = false;
						}
					}
					statusUpdateItem.mRank += identicalCount;
				}
				
				
				
				Vector<CompetitionReasonerProgressStatusUpdateItem> newReasonerProgressUpdateItemVector = new Vector<CompetitionReasonerProgressStatusUpdateItem>();
				newReasonerProgressUpdateItemVector.setSize(compStatus.getReasonerCount());
				for (ResortReasonerStatusUpdateItem statusUpdateItem : statusUpdateItemList) {
					int rank = statusUpdateItem.getRank();
					int position = statusUpdateItem.getPosition();
					CompetitionReasonerProgressStatus itemReasonerStatus = statusUpdateItem.getReasonerStatus();					
					if (itemReasonerStatus.getReasonerRank() != rank) {
						statusUpdateItem.setUpdated(true);
					}
					if (itemReasonerStatus.getReasonerPosition() != position) {
						statusUpdateItem.setUpdated(true);
					}
					boolean updated = statusUpdateItem.isUpdated();
					if (!updated) {
						newReasonerProgressUpdateItemVector.set(position-1, new CompetitionReasonerProgressStatusUpdateItem(itemReasonerStatus, statusUpdateItem.getLastUpdateID()));
					} else {
						CompetitionReasonerProgressStatus newItemReasonerStatus = new CompetitionReasonerProgressStatus(itemReasonerStatus);
						newItemReasonerStatus.setReasonerPosition(position);
						newItemReasonerStatus.setReasonerRank(rank);
						newReasonerProgressUpdateItemVector.set(position-1, new CompetitionReasonerProgressStatusUpdateItem(newItemReasonerStatus, nextUpdateID));
					}					
				}

				CompetitionStatusUpdateItem nextCompStatusItem = new CompetitionStatusUpdateItem(compStatus,lastCompStatusItem.getCompetitionID(),newReasonerProgressUpdateItemVector,lastCompStatusItem.getEvaluationMap(),lastCompStatusItem.getCompetitionUpdateID(),nextUpdateID,lastCompStatusItem.getEvaluationUpdateID());
				mLastCompetitionStatusItemMap.put(compStatus.getCompetitionSourceString(), nextCompStatusItem);
				
			}
			mCurrentUpdateID = nextUpdateID;
		}
	}
	
	
	protected void processCompetitionStatusUpdate(CompetitionStatus compStatus) {	
		long nextUpdateID = mCurrentUpdateID + 1;
		CompetitionStatusUpdateItem lastCompStatusItem = mLastCompetitionStatusItemMap.get(compStatus.getCompetitionSourceString());
		Vector<CompetitionReasonerProgressStatusUpdateItem> lastReasonerProgressUpdateItemVector = null;
		HashMap<String,CompetitionEvaluationStatusUpdateItem> lastEvalUpdateItemMap = null;
		long lastReasonersUpdateID = 0;
		long lastEvaluationUpdateID = 0;
		int competitionID = 0;
		if (lastCompStatusItem != null) {
			lastReasonerProgressUpdateItemVector = lastCompStatusItem.getReasonerProgressUpdateItemVector();
			lastReasonersUpdateID = lastCompStatusItem.getReasonersUpdateID();
			lastEvaluationUpdateID = lastCompStatusItem.getEvaluationUpdateID();
			lastEvalUpdateItemMap = lastCompStatusItem.getEvaluationMap();
			competitionID  = lastCompStatusItem.getCompetitionID();
		} else {
			competitionID = mNextCompetitionID++;
		}
		CompetitionStatusUpdateItem nextCompStatusItem = new CompetitionStatusUpdateItem(compStatus,competitionID,lastReasonerProgressUpdateItemVector,lastEvalUpdateItemMap,nextUpdateID,lastReasonersUpdateID,lastEvaluationUpdateID);
		mLastCompetitionStatusItemMap.put(compStatus.getCompetitionSourceString(), nextCompStatusItem);
		mCurrentUpdateID = nextUpdateID;
	}


	@SuppressWarnings({ "unchecked" })
	protected void processEvaluationStatusUpdate(CompetitionEvaluationStatus compEvalStatus) {	
		long nextUpdateID = mCurrentUpdateID + 1;
		CompetitionStatusUpdateItem lastCompStatusItem = mLastCompetitionStatusItemMap.get(compEvalStatus.getCompetitionSourceString());
		if (lastCompStatusItem != null) {
			HashMap<String,CompetitionEvaluationStatusUpdateItem> lastEvalUpdateItemMap = lastCompStatusItem.getEvaluationMap();
			HashMap<String,CompetitionEvaluationStatusUpdateItem> newEvalUpdateItemMap = null;   
			if (lastEvalUpdateItemMap != null) {
				newEvalUpdateItemMap = (HashMap<String, CompetitionEvaluationStatusUpdateItem>)lastEvalUpdateItemMap.clone();
			} else {
				newEvalUpdateItemMap = new HashMap<String,CompetitionEvaluationStatusUpdateItem>();
			}
			newEvalUpdateItemMap.put(compEvalStatus.getEvaluationSourceString(),new CompetitionEvaluationStatusUpdateItem(compEvalStatus, nextUpdateID));
			
			CompetitionStatusUpdateItem nextCompStatusItem = new CompetitionStatusUpdateItem(lastCompStatusItem.getCompetitionStatus(),lastCompStatusItem.getCompetitionID(),lastCompStatusItem.getReasonerProgressUpdateItemVector(),newEvalUpdateItemMap,lastCompStatusItem.getCompetitionUpdateID(),lastCompStatusItem.getReasonersUpdateID(),nextUpdateID);
			mLastCompetitionStatusItemMap.put(compEvalStatus.getCompetitionSourceString(), nextCompStatusItem);
			mCurrentUpdateID = nextUpdateID;
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
