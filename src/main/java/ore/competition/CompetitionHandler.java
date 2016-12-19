package ore.competition;

import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.evaluation.QueryResultStorage;
import ore.interfacing.ReasonerDescription;
import ore.interfacing.ReasonerDescriptionManager;
import ore.querying.Query;
import ore.querying.QueryManager;
import ore.querying.QueryResponse;
import ore.utilities.FileSystemHandler;
import ore.verification.QueryResultVerificationCorrectnessType;
import ore.verification.QueryResultVerificationReport;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class CompetitionHandler {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionHandler.class);
	
	protected Competition mCompetition = null;
	protected Config mConfig = null;
	protected QueryManager mQueryManager = null;
	protected ReasonerDescriptionManager mReasonerManager = null;
	protected QueryResultStorage mResultStorage = null;
	
	protected String mCompetitionOutputString = null;
	
	protected int mMaxRemainingTasks = 0;
	protected int mSuccessfullyProcessedTasks = 0;
	protected int mCurrentProcessingTasks = 0;

	protected List<Query> mQueryList = null;
	protected List<ReasonerDescription> mReasonerList = null;
	
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	protected long mMemoryLimit = 0;
	
	protected int mNextPosition = 0;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes


	protected HashSet<CompetitionExecutionItem> mRemainingItemSet = null;
	protected HashSet<ReasonerDescription> mRemainingReasonerSet = null;

	protected HashSet<CompetitionExecutionItem> mProcessingItemSet = null;
	protected HashSet<ReasonerDescription> mProcessingReasonerSet = null;
	
	protected HashSet<CompetitionExecutionItem> mProcessedItemSet = null;
	protected HashSet<ReasonerDescription> mProcessedReasonerSet = null;
	
	protected HashMap<ReasonerDescription,CompetitionExecutionItem> mReasonerItemMap = null;
	
	protected HashSet<CompetitionExecutionTask> mAktiveTaskSet = null;

	
	
	protected ArrayList<CompetitionExecutionItem> mRankingSortedItemList = new ArrayList<CompetitionExecutionItem>();
	

	protected enum ItemExecutionStatus {		
		ITEM_EXECUTION_REMAINING,
		ITEM_EXECUTION_PROCESSING,
		ITEM_EXECUTION_PROCESSED;		
	}
	

	
	protected class CompetitionExecutionItem implements Comparable<CompetitionExecutionItem> {
		public ItemExecutionStatus mStatus = ItemExecutionStatus.ITEM_EXECUTION_REMAINING;
		public int mProcessingCount = 0;
		public int mRemainingCount = 0;
		public int mProcessedCount = 0;
		public int mOutOfTimeCount = 0;
		public ReasonerDescription mReasoner = null;
		public long mAccumulatedExecutionTime = 0;
		public long mAccumulatedCorrectlyProcessingTime = 0;
		public int mCorrectlyProccessedCount = 0;	
		public int mRank = 0;
		public int mPosition = 0;
		
		public ArrayList<Query> mRemainingQueryList = new ArrayList<Query>();
		public ArrayList<Query> mProcessedQueryList = new ArrayList<Query>();
		public HashSet<Query> mProcessingQuerySet = new HashSet<Query>();
		public ArrayList<Query> mOutOfTimeQueryList = new ArrayList<Query>();
		
		
		public void setRank(int rank) {
			mRank = rank;
		}
	
		public void setPosition(int position) {
			mPosition = position;
		}	
		
		public int getRank() {
			return mRank;
		}
		
		public int getPosition() {
			return mPosition;
		}
		
		public void incCorrectlyProccessedCount(int incCount) {
			mCorrectlyProccessedCount += incCount;
		}
		
		public void incExecutionTime(long incCount) {
			mAccumulatedExecutionTime += incCount;
		}		
		
		public void incCorrectlyProcessingTime(long incCount) {
			mAccumulatedCorrectlyProcessingTime += incCount;
		}		
		
		public long getAccumulatedCorrectlyProcessingTime() {
			return mAccumulatedCorrectlyProcessingTime;
		}		
		
		public long getAccumulatedExecutionTime() {
			return mAccumulatedExecutionTime;
		}		
		
		public int getCorrectlyProccessedCount() {
			return mCorrectlyProccessedCount;
		}		

		public boolean hasRemainingQueriesToProcess() {
			return mRemainingCount > 0;
		}
		
		public boolean hasProcessingQueries() {
			return mProcessingCount > 0;
		}		
		
		public CompetitionExecutionItem(ReasonerDescription reasoner, Collection<Query> queryList, int postion) {
			mReasoner = reasoner;
			mPosition = postion;
			mRemainingQueryList.addAll(queryList);
			mRemainingCount = mRemainingQueryList.size();
		}
		
		public int getRemainingQueryCount() {
			return mRemainingCount;
		}

		public int getProcessingQueryCount() {
			return mProcessingCount;
		}

		public int getProcessedQueryCount() {
			return mProcessedCount;
		}

		public int getOutOfTimeQueryCount() {
			return mOutOfTimeCount;
		}
		public Query getNextRemainingQuery(boolean moveToProcessing) {
			Query nextQuery = null;
			Iterator<Query> queryIt = mRemainingQueryList.iterator();
			if (queryIt.hasNext()) {				
				nextQuery = queryIt.next();
				if (moveToProcessing) {
					--mRemainingCount;
					queryIt.remove();
					mProcessingQuerySet.add(nextQuery);
					++mProcessingCount;
				}
			}
			return nextQuery;
		}
		
		
		public void changeQueryProcessed(Query query) {
			if (mProcessingQuerySet.contains(query)) {
				--mProcessingCount;
				mProcessingQuerySet.remove(query);
				mProcessedQueryList.add(query);
				++mProcessedCount;
			}
		}
		
		public void changeQueryOutOfTime(Query query) {
			if (mProcessingQuerySet.contains(query)) {
				--mProcessingCount;
				mProcessingQuerySet.remove(query);
				mOutOfTimeQueryList.add(query);
				++mOutOfTimeCount;
			}
		}
		
		public void resetQueryProcessing(Query query) {
			if (mProcessingQuerySet.contains(query)) {
				--mProcessingCount;
				mProcessingQuerySet.remove(query);
				mRemainingQueryList.add(0,query);
				++mRemainingCount;
			}
		}		
		
		public void setStatus(ItemExecutionStatus status) {
			mStatus = status;
		}
		
		public ItemExecutionStatus getStatus() {
			return mStatus;
		}

		public ReasonerDescription getReasoner() {
			return mReasoner;
		}

		public boolean hasSameRank(CompetitionExecutionItem data) {
			if (mCorrectlyProccessedCount > data.mCorrectlyProccessedCount) {
				return false;
			} else if (mCorrectlyProccessedCount < data.mCorrectlyProccessedCount) {
				return false;
			} else {
				if (mAccumulatedCorrectlyProcessingTime < data.mAccumulatedCorrectlyProcessingTime) {
					return false;
				} else if (mAccumulatedCorrectlyProcessingTime > data.mAccumulatedCorrectlyProcessingTime) {
					return false;
				} else {
					return true;
				}
			}
		}
		
		@Override
		public int compareTo(CompetitionExecutionItem item) {
			if (mCorrectlyProccessedCount > item.mCorrectlyProccessedCount) {
				return -1;
			} else if (mCorrectlyProccessedCount < item.mCorrectlyProccessedCount) {
				return 1;
			} else {
				if (mAccumulatedCorrectlyProcessingTime < item.mAccumulatedCorrectlyProcessingTime) {
					return -1;
				} else if (mAccumulatedCorrectlyProcessingTime > item.mAccumulatedCorrectlyProcessingTime) {
					return 1;
				} else {
					return mReasoner.getReasonerName().toUpperCase().compareTo(item.mReasoner.getReasonerName().toUpperCase());					
				}
			}
		}
	}
	
	protected void changeItemStatus(CompetitionExecutionItem item, ItemExecutionStatus status) {	
		if (item.getStatus() != status) {
			ReasonerDescription reasoner = item.getReasoner();
			ItemExecutionStatus prevStatus = item.getStatus();
			if (prevStatus == ItemExecutionStatus.ITEM_EXECUTION_REMAINING) {
				mRemainingItemSet.remove(item);		
				mRemainingReasonerSet.remove(reasoner);
			} else if (prevStatus == ItemExecutionStatus.ITEM_EXECUTION_PROCESSING) {
				mProcessingItemSet.remove(item);		
				mProcessingReasonerSet.remove(reasoner);
			} else if (prevStatus == ItemExecutionStatus.ITEM_EXECUTION_PROCESSED) {
				mProcessedItemSet.remove(item);		
				mProcessedReasonerSet.remove(reasoner);
			}
			
			if (status == ItemExecutionStatus.ITEM_EXECUTION_REMAINING) {
				mRemainingItemSet.add(item);		
				mRemainingReasonerSet.add(reasoner);
			} else if (status == ItemExecutionStatus.ITEM_EXECUTION_PROCESSING) {
				mProcessingItemSet.add(item);		
				mProcessingReasonerSet.add(reasoner);
			} else if (status == ItemExecutionStatus.ITEM_EXECUTION_PROCESSED) {
				mProcessedItemSet.add(item);		
				mProcessedReasonerSet.add(reasoner);
			}
			item.setStatus(status);
		}
	}
	

	public int getMaximumRemainingTaskCount() {
		return mMaxRemainingTasks;
	}
	
	

	public String getCompetitionOutputString() {
		return mCompetitionOutputString;
	}
	
	public List<ReasonerDescription> getReasonerList() {
		return mReasonerList;
	}
	
	public List<Query> getQueryList() {
		return mQueryList;
	}
	public Competition getCompetition() {
		return mCompetition;
	}
	
	
	public boolean isCompetitionExecutionCompleted() {
		return mCurrentProcessingTasks <= 0 && mMaxRemainingTasks <= 0;
	}	
	


	public CompetitionExecutionTask getNextCompetitionExecutionTask() {
		CompetitionExecutionTask task = null;
		int maxRemainingQuery = 0;
		CompetitionExecutionItem maxRemainingQueryItem = null;
		for (CompetitionExecutionItem item : mRemainingItemSet) {		
			if (maxRemainingQueryItem == null || maxRemainingQuery < item.getRemainingQueryCount()) {	
				maxRemainingQueryItem = item;
				maxRemainingQuery = item.getRemainingQueryCount();
			}
		}

		for (CompetitionExecutionItem item : mProcessingItemSet) {		
			if (item.hasRemainingQueriesToProcess()) {
				if (maxRemainingQueryItem == null || maxRemainingQuery < item.getRemainingQueryCount()+item.getProcessingQueryCount()) {	
					maxRemainingQueryItem = item;
					maxRemainingQuery = item.getRemainingQueryCount()+item.getProcessingQueryCount();
				}			
			}
		}
		if (maxRemainingQueryItem != null) {
			CompetitionExecutionItem item = maxRemainingQueryItem;
			++mCurrentProcessingTasks;
			--mMaxRemainingTasks;
			Query nextQuery = item.getNextRemainingQuery(true);
			
			String queryResponseString = FileSystemHandler.relativeAbsoluteResolvedFileString(nextQuery.getOntologySourceString());
			String reasonerEvaluationResponseDirectory = "competition-evaluations"+ File.separator+mCompetitionOutputString+ File.separator+"reasoners-responses"+ File.separator+item.getReasoner().getOutputPathString()+ File.separator;
			String responseDestinationString = reasonerEvaluationResponseDirectory+queryResponseString+ File.separator;
			
			task = new CompetitionExecutionTask(item.getReasoner(), nextQuery, responseDestinationString, mExecutionTimeout, mMemoryLimit);
			mAktiveTaskSet.add(task);
			changeItemStatus(item, ItemExecutionStatus.ITEM_EXECUTION_PROCESSING);
		}
		return task;
	}
	
	
	
	public CompetitionExecutionTask getNextCompetitionExecutionTask(ReasonerDescription reasoner) {
		CompetitionExecutionTask task = null;
		CompetitionExecutionItem item = mReasonerItemMap.get(reasoner);
		if (item.hasRemainingQueriesToProcess()) {
			++mCurrentProcessingTasks;
			--mMaxRemainingTasks;
			Query nextQuery = item.getNextRemainingQuery(true);
			
			String queryResponseString = FileSystemHandler.relativeAbsoluteResolvedFileString(nextQuery.getOntologySourceString());
			String reasonerEvaluationResponseDirectory = "competition-evaluations"+ File.separator+mCompetitionOutputString+ File.separator+"reasoners-responses"+ File.separator+item.getReasoner().getOutputPathString()+ File.separator;
			String responseDestinationString = reasonerEvaluationResponseDirectory+queryResponseString+ File.separator;
			
			task = new CompetitionExecutionTask(reasoner, nextQuery, responseDestinationString, mExecutionTimeout, mMemoryLimit);
			mAktiveTaskSet.add(task);
			changeItemStatus(item, ItemExecutionStatus.ITEM_EXECUTION_PROCESSING);
		}
		return task;
	}
	
	
	public void updateRankingSatus(CompetitionExecutionItem updatedItem, ReasonerDescription reasoner, Query query, QueryResponse queryResponse, QueryResultVerificationReport verificationReport) {
				
	
		boolean resultCorrect = false;
		boolean resultValid = true;
		long processingTime = 0;
		long executionTime = 0;
		if (updatedItem != null) {
			if (queryResponse != null) {
				if (queryResponse.hasTimedOut() || queryResponse.hasExecutionError() || !queryResponse.hasExecutionCompleted() || queryResponse.getReasonerQueryProcessingTime() > mProcessingTimeout) {
					resultValid = false;
				} else {
					processingTime = queryResponse.getReasonerQueryProcessingTime();								
				}
				executionTime = queryResponse.getExecutionTime();
			} else {
				resultValid = false;
			}
			if (verificationReport != null && resultValid) {
				if (verificationReport.getCorrectnessType() == QueryResultVerificationCorrectnessType.QUERY_RESULT_CORRECT) {
					resultCorrect = true;
				} else if (verificationReport.getCorrectnessType() == QueryResultVerificationCorrectnessType.QUERY_RESULT_INCORRECT) {
					resultCorrect = false;
				} else {
					resultValid = false;
				}
			} else {
				resultValid = false;
			}
		} else {
			resultValid = false;
		}
		
		if (resultValid && resultCorrect) {
			updatedItem.incCorrectlyProccessedCount(1);
			updatedItem.incCorrectlyProcessingTime(processingTime);
		}
		updatedItem.incExecutionTime(executionTime);
			
		
		Collections.sort(mRankingSortedItemList);
		
		
		int nextRank = 1;
		for (CompetitionExecutionItem item : mRankingSortedItemList) {
			item.setPosition(nextRank);
			item.setRank(nextRank++);			
		}
		
		
		ListIterator<CompetitionExecutionItem> itemIt = mRankingSortedItemList.listIterator();
		while (itemIt.hasNext()) {
			CompetitionExecutionItem item = itemIt.next();
			ListIterator<CompetitionExecutionItem> itemIt2 = mRankingSortedItemList.listIterator(itemIt.nextIndex());
			boolean stillIdentical = true;
			int identicalCount = 0;
			while (itemIt2.hasNext() && stillIdentical) {
				CompetitionExecutionItem item2 = itemIt2.next();
				if (item.hasSameRank(item2)) {
					identicalCount++;
				} else {
					stillIdentical = false;
				}
			}
			item.mRank += identicalCount;
		}

		
	}
	
	
	
	public CompetitionReasonerProgressStatus getReasonerProgressStatus(ReasonerDescription reasoner) {
		CompetitionExecutionItem item = mReasonerItemMap.get(reasoner);
		return new CompetitionReasonerProgressStatus(mCompetition,reasoner,item.getRank(),item.getPosition(),item.getCorrectlyProccessedCount(),item.getProcessedQueryCount(),item.getOutOfTimeQueryCount(),item.getAccumulatedCorrectlyProcessingTime(),item.getAccumulatedExecutionTime(),new DateTime(DateTimeZone.UTC));
	}
	

	public Set<ReasonerDescription> getCompletelyProcessedReasonerSet() {
		return mProcessedReasonerSet;
	}
	
	

	public void completeCompetitionExecutionTask(CompetitionExecutionTask task, CompetitionExecutionReport report) {
		if (mAktiveTaskSet.contains(task)) {
			mAktiveTaskSet.remove(task);
			boolean successfullyExecuted = report.isExecuted();
			boolean executionOutOfTime = report.isOutOfTime();
			ReasonerDescription reasoner = task.getReasonerDescription();
			Query query = task.getQuery();
			CompetitionExecutionItem item = mReasonerItemMap.get(reasoner);
			if (successfullyExecuted) {
				++mSuccessfullyProcessedTasks;
				item.changeQueryProcessed(query);
				QueryResponse queryResponse = report.getQueryResponse();
				QueryResultVerificationReport verificationReport = report.getVerificationReport();
				mResultStorage.storeQueryResult(reasoner, query, queryResponse, verificationReport);
				updateRankingSatus(item, reasoner, query, queryResponse, verificationReport);
			} else {
				if (executionOutOfTime) {
					item.changeQueryOutOfTime(query);
				} else {
					item.resetQueryProcessing(query);
					++mMaxRemainingTasks;
				}
			}
			--mCurrentProcessingTasks;
			if (!item.hasProcessingQueries()) {
				if (item.hasRemainingQueriesToProcess()) {
					changeItemStatus(item, ItemExecutionStatus.ITEM_EXECUTION_REMAINING);
				} else {
					changeItemStatus(item, ItemExecutionStatus.ITEM_EXECUTION_PROCESSED);
				}
			}
		}
	}	
	
	
	public boolean initCompetition(Competition competition, QueryResultStorage resultStorage) {
		mResultStorage = resultStorage;
		mCompetition = competition;
		
		mLogger.info("Loading queries for competition '{}'.",competition.getCompetitionName());
		
		mQueryList = mQueryManager.loadFilterSortDefaultQueries(mCompetition.getQueryFilterString(), mCompetition.getQuerySortingFilePathString());
		
		mLogger.info("Loaded {} queries for competition '{}'.",mQueryList.size(),competition.getCompetitionName());
		
		mReasonerList = mReasonerManager.loadReasonerDescriptions(mCompetition.getReasonerList());
		if (mReasonerList == null || mReasonerList.isEmpty()) {
			return false;
		}
		mResultStorage.initReasonerQueryStorage(mReasonerList, mQueryList);
		
		
		mRemainingItemSet = new HashSet<CompetitionExecutionItem>();
		mRemainingReasonerSet = new HashSet<ReasonerDescription>();

		mProcessingItemSet = new HashSet<CompetitionExecutionItem>();
		mProcessingReasonerSet = new HashSet<ReasonerDescription>();
		
		mProcessedItemSet = new HashSet<CompetitionExecutionItem>();
		mProcessedReasonerSet = new HashSet<ReasonerDescription>();
		
		mReasonerItemMap = new HashMap<ReasonerDescription,CompetitionExecutionItem>();
		
		mAktiveTaskSet = new HashSet<CompetitionExecutionTask>();
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);	
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);
		
		mProcessingTimeout = competition.getProcessingTimeout();
		mExecutionTimeout = competition.getExecutionTimeout();
		if (mExecutionTimeout <= 0) {
			mExecutionTimeout = mDefaultExecutionTimeout;
		}
		if (mProcessingTimeout <= 0) {
			mProcessingTimeout = mDefaultProcessingTimeout;
		}
		if (mProcessingTimeout <= 0) {
			mProcessingTimeout = mExecutionTimeout;
		}
		
		mMemoryLimit = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_EXECUTION_MEMORY_LIMIT,mMemoryLimit);
		
		
		mMaxRemainingTasks = 0;
		mSuccessfullyProcessedTasks = 0;
		mCurrentProcessingTasks = 0;
		
		mCompetitionOutputString = mCompetition.getOutputString();
		if (mCompetitionOutputString == null) {
			mCompetitionOutputString = mCompetition.getCompetitionName();
		}
		
		for (ReasonerDescription reasoner : mReasonerList) {
			CompetitionExecutionItem reasonerItem = new CompetitionExecutionItem(reasoner,mQueryList,++mNextPosition);
			mRankingSortedItemList.add(reasonerItem);
			mRemainingItemSet.add(reasonerItem);
			mRemainingReasonerSet.add(reasoner);
			mReasonerItemMap.put(reasoner, reasonerItem);
			mMaxRemainingTasks += reasonerItem.getRemainingQueryCount();
			if (!reasonerItem.hasRemainingQueriesToProcess()) {
				changeItemStatus(reasonerItem, ItemExecutionStatus.ITEM_EXECUTION_PROCESSED);
			}
		}
		return true;
	}
	
	public CompetitionHandler(QueryManager queryManager, ReasonerDescriptionManager reasonerManager, Config config) {
		mQueryManager = queryManager;
		mReasonerManager = reasonerManager;
		mConfig = config;
	}

}
