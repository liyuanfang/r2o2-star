package ore.competition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Deprecated
public class CompetitionStatusUpdateCollector implements CompetitionStatusUpdater {
	
	@SuppressWarnings("unused")
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionStatusUpdateCollector.class);
	
	
	protected int mNextCompetitionID = 0;
	protected ConcurrentHashMap<String,Integer> mCompetitionIDMap = new ConcurrentHashMap<String,Integer>();
	protected ConcurrentHashMap<String,CompetitionStatus> mLastCompetitionStatusMap = new ConcurrentHashMap<String,CompetitionStatus>();
	protected ConcurrentHashMap<String,CompetitionReasonerProgressStatus> mLastCompetitionReasonerStatusMap = new ConcurrentHashMap<String,CompetitionReasonerProgressStatus>();
	

	protected ConcurrentHashMap<String,Vector<CompetitionReasonerProgressStatus>> mCompetitionStatusReasonerStatusMap = new ConcurrentHashMap<String,Vector<CompetitionReasonerProgressStatus>>();
	
	protected HashSet<CompetitionStatusUpdateListner> mListerSet = new HashSet<CompetitionStatusUpdateListner>();
	protected Lock mListnerLock = new ReentrantLock();
	protected boolean mAllFinished = false;
	
	
	public Collection<CompetitionStatus> getCompetitionStatusCollection() {
		return mLastCompetitionStatusMap.values();
	}

	public int getCompetitionID(CompetitionStatus competitionStatus) {
		return mCompetitionIDMap.get(competitionStatus.getCompetitionSourceString());
	}
	

	public Vector<CompetitionReasonerProgressStatus> getCompetitionReasonerProgressStatusVector(CompetitionStatus competitionStatus) {
		return mCompetitionStatusReasonerStatusMap.get(competitionStatus.getCompetitionSourceString());
	}
	
	
	public void addUpdateListner(CompetitionStatusUpdateListner listner) {
		mListnerLock.lock();
		mListerSet.add(listner);
		mListnerLock.unlock();		
	}

	public void removeUpdateListner(CompetitionStatusUpdateListner listner) {
		mListnerLock.lock();
		mListerSet.remove(listner);
		mListnerLock.unlock();		
	}
	
	public void notifyUpdateListner() {
		mListnerLock.lock();
		for (CompetitionStatusUpdateListner listner : mListerSet) {
			listner.notifyUpdated();
		}
		mListnerLock.unlock();		
	}
	
	public boolean allCompetitionsFinished() {
		return mAllFinished;
	}
	
	
	protected void generateCurrentCompetitionStatus() {
		for (CompetitionStatus competitionStatus : mLastCompetitionStatusMap.values()) {				
			
			class StatusUpdateItem implements Comparable<StatusUpdateItem> {
				CompetitionReasonerProgressStatus mStatus = null;
				long mProcessedTime = 0;
				int mCorrectlyProccessedCount = 0;
				int mRank = 0;
				int mPosition = 0;
				
				public StatusUpdateItem(CompetitionReasonerProgressStatus status) {
					mStatus = status;
					mProcessedTime = mStatus.getCorrectlyProccessedTime();
					mCorrectlyProccessedCount = mStatus.getCorrectlyProcessedCount();
				}
				
				public CompetitionReasonerProgressStatus getReasonerStatus() {
					return mStatus;
				}
				
				public int getRank() {
					return mRank;
				}
				
				public int getPosition() {
					return mPosition;
				}
				@Override
				public int compareTo(StatusUpdateItem data) {
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
							return 0;
						}
					}
				}
			}	
			
			HashMap<CompetitionReasonerProgressStatus,StatusUpdateItem> statusUpdateItemMap = new HashMap<CompetitionReasonerProgressStatus,StatusUpdateItem>();
			ArrayList<StatusUpdateItem> statusUpdateItemList = new ArrayList<StatusUpdateItem>();
			for (CompetitionReasonerProgressStatus reasonerStatus : mLastCompetitionReasonerStatusMap.values()) {
				if (reasonerStatus.getCompetitionSourceString().compareTo(competitionStatus.getCompetitionSourceString()) == 0) {
					StatusUpdateItem statusUpdateItem = new StatusUpdateItem(reasonerStatus);
					statusUpdateItemList.add(statusUpdateItem);
					statusUpdateItemMap.put(reasonerStatus, statusUpdateItem);
				}				
			}
			

			Collections.sort(statusUpdateItemList);
			int nextRank = 1;
			for (StatusUpdateItem statusUpdateItem : statusUpdateItemList) {
				statusUpdateItem.mRank = nextRank;
				statusUpdateItem.mPosition = nextRank;
				++nextRank;
			}
			
			ListIterator<StatusUpdateItem> statusUpdateItemIt = statusUpdateItemList.listIterator();
			while (statusUpdateItemIt.hasNext()) {
				StatusUpdateItem statusUpdateItem = statusUpdateItemIt.next();
				ListIterator<StatusUpdateItem> statusUpdateItemIt2 = statusUpdateItemList.listIterator(statusUpdateItemIt.nextIndex());
				boolean stillIdentical = true;
				int identicalCount = 0;
				while (statusUpdateItemIt2.hasNext() && stillIdentical) {
					StatusUpdateItem statusUpdateItem2 = statusUpdateItemIt2.next();
					if (statusUpdateItem.compareTo(statusUpdateItem2) == 0) {
						identicalCount++;
					} else {
						stillIdentical = false;
					}
				}
				statusUpdateItem.mRank += identicalCount;
			}
			
			
			Vector<CompetitionReasonerProgressStatus> reasonerStatusVector = new Vector<CompetitionReasonerProgressStatus>();
			reasonerStatusVector.setSize(competitionStatus.getReasonerCount());
			for (StatusUpdateItem statusUpdateItem : statusUpdateItemList) {
				int rank = statusUpdateItem.getRank();
				int position = statusUpdateItem.getPosition();
				CompetitionReasonerProgressStatus reasonerStatus = statusUpdateItem.getReasonerStatus();
				if (reasonerStatus.getReasonerRank() != rank) {
					reasonerStatus.setReasonerRank(rank);
				}
				if (reasonerStatus.getReasonerPosition() != position) {
					reasonerStatus.setReasonerPosition(position);
				}
				reasonerStatusVector.set(position-1, reasonerStatus);
			}
			
			
			mCompetitionStatusReasonerStatusMap.put(competitionStatus.getCompetitionSourceString(), reasonerStatusVector);
		}
		
		boolean allFinished = true;
		for (CompetitionStatus competitionStatus : mLastCompetitionStatusMap.values()) {
			if (competitionStatus.getExecutionState() != CompetitionExecutionState.COMPETITION_EXECUTION_STATE_FINISHED) {
				allFinished = false;
				break;
			}
		}
		mAllFinished = allFinished;
		
		notifyUpdateListner();
	}
	


	@Override
	public void updateCompetitionStatus(CompetitionStatus status) {
		mAllFinished = false;
		if (!mCompetitionIDMap.containsKey(status.getCompetitionSourceString())) {
			mCompetitionIDMap.put(status.getCompetitionSourceString(),mNextCompetitionID++);
		}		
		mLastCompetitionStatusMap.put(status.getCompetitionSourceString(),new CompetitionStatus(status));
		generateCurrentCompetitionStatus();
	}





	@Override
	public void updateCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status) {
		mAllFinished = false;
		mLastCompetitionReasonerStatusMap.put(status.getCompetitionSourceString()+":"+status.getReasonerSourceString(),new CompetitionReasonerProgressStatus(status));
		generateCurrentCompetitionStatus();
	}

	@Override
	public void updateCompetitionEvaluationStatus(CompetitionEvaluationStatus status) {
	}	

}
