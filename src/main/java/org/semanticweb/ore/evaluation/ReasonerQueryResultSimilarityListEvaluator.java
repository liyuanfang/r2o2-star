package org.semanticweb.ore.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.semanticweb.ore.competition.Competition;
import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.competition.CompetitionEvaluationType;
import org.semanticweb.ore.competition.CompetitionStatusUpdater;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReasonerQueryResultSimilarityListEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerQueryResultSimilarityListEvaluator.class);
	
	private Config mConfig = null;
	private String mResultOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	public ReasonerQueryResultSimilarityListEvaluator(Config config, String expectionsOutputString, CompetitionStatusUpdater statusUpdater) {
		mResultOutputString = expectionsOutputString;
		mConfig = config;
		mStatusUpdater = statusUpdater;
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);		
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);		
	}
	
	protected enum EvaluationSummaryType {
		ET_NO_RESULT_HASH_CODE("No-Result-Hash-Code"),
		ET_MAX_SIMILAR_RESULT_COUNT("Max-Similar-Result-Count"),
		ET_DIFFERENT_RESULT_COUNT("Different-Result-Count"),
		ET_DRAW_OCCURRED("Draw-Occurred"),
		ET_DRAW_PARTIES_COUNT("Draw-Parties-Count");

		String mShortString = null;
		
		private EvaluationSummaryType(String shortString) {
			mShortString = shortString;
		}
	}

	protected enum EvaluationReasonerType {
		ET_VALID_HASH_CODE("Has-Valid-Hash-Code"),
		ET_SIMILAR_RESULT_COUNT("Similar-Result-Count"),
		ET_DIFFERENT_RESULT_COUNT("Different-Result-Count"),
		ET_DRAW_PARTICIPATION("Draw-Participation");

		String mShortString = null;
		
		private EvaluationReasonerType(String shortString) {
			mShortString = shortString;
		}
	}

	protected class QueryReasonerData {	
		ReasonerDescription mReasoner = null;
		Query mQuery = null;
		String mResultHashCode = "-";
		
		boolean mValidResultHashCode = false;
		int mDifferentHashCodeCount = 0;
		int mSimilarHashCodeCount = 0;
		int mDrawParticipation = 0;
		int mDrawPartiesCount = 0;
		
		
		String getValueString(EvaluationReasonerType evalType) {
			String string = null;
			if (evalType == EvaluationReasonerType.ET_VALID_HASH_CODE) {
				string = String.valueOf(mValidResultHashCode);
			} else if (evalType == EvaluationReasonerType.ET_SIMILAR_RESULT_COUNT) {
				string = String.valueOf(mSimilarHashCodeCount);
			} else if (evalType == EvaluationReasonerType.ET_DIFFERENT_RESULT_COUNT) {
				string = String.valueOf(mDifferentHashCodeCount);
			} else if (evalType == EvaluationReasonerType.ET_DRAW_PARTICIPATION) {
				string = String.valueOf(mDrawParticipation);
			}
			return string;
		}
		
		public QueryReasonerData(Query query, ReasonerDescription reasoner) {
			mQuery = query; 
			mReasoner = reasoner;
		}

	}

	
	protected class QueryData {	
		Query mQuery = null;

		
		int mNoResultCount = 0;				
		int mMaxSimilarResultCount = 0;
		int mDifferentResultCount = 0;
		int mDrawOccurred = 0;
		int mDrawPartiesCount = 0;		
		String getValueString(EvaluationSummaryType evalType) {
			String string = null;
			if (evalType == EvaluationSummaryType.ET_NO_RESULT_HASH_CODE) {
				string = String.valueOf(mNoResultCount);
			} else if (evalType == EvaluationSummaryType.ET_MAX_SIMILAR_RESULT_COUNT) {
				string = String.valueOf(mMaxSimilarResultCount);
			} else if (evalType == EvaluationSummaryType.ET_DIFFERENT_RESULT_COUNT) {
				string = String.valueOf(mDifferentResultCount);
			} else if (evalType == EvaluationSummaryType.ET_DRAW_OCCURRED) {
				string = String.valueOf(mDrawOccurred);
			} else if (evalType == EvaluationSummaryType.ET_DRAW_PARTIES_COUNT) {
				string = String.valueOf(mDrawPartiesCount);
			}
			return string;
		}
		
		QueryData(Query query) {
			mQuery = query;
		}
		
		void setData(int noResultCount, int maxSimilarResultCount, int differentResultCount, int drawOccured, int drawPartiesCount) {
			mDifferentResultCount = differentResultCount;
			mMaxSimilarResultCount = maxSimilarResultCount;
			mNoResultCount = noResultCount;	
			mDrawOccurred = drawOccured;
			mDrawPartiesCount = drawPartiesCount;
		}
	}	
	
	
	protected HashMap<Query,QueryData> mQueryDataHash = new HashMap<Query,QueryData>();

	
	protected HashMap<Query,HashMap<ReasonerDescription,QueryReasonerData>> mQueryReasonerDataHash = new HashMap<Query,HashMap<ReasonerDescription,QueryReasonerData>>();
	protected HashMap<ReasonerDescription,HashMap<Query,QueryReasonerData>> mReasonerQueryDataHash = new HashMap<ReasonerDescription,HashMap<Query,QueryReasonerData>>();


	protected QueryReasonerData getQueryReasonerData(Query query, ReasonerDescription reasoner) {
		HashMap<ReasonerDescription,QueryReasonerData> reasonerDataMap = mQueryReasonerDataHash.get(query);
		if (reasonerDataMap == null) {
			reasonerDataMap = new HashMap<ReasonerDescription,QueryReasonerData>(); 
			mQueryReasonerDataHash.put(query, reasonerDataMap);
		}
		QueryReasonerData data = reasonerDataMap.get(reasoner);
		if (data == null) {
			data = new QueryReasonerData(query,reasoner);
			reasonerDataMap.put(reasoner, data);
		}
		
		HashMap<Query,QueryReasonerData> queryDataMap = mReasonerQueryDataHash.get(reasoner);
		if (queryDataMap == null) {
			queryDataMap = new HashMap<Query,QueryReasonerData>(); 
			mReasonerQueryDataHash.put(reasoner, queryDataMap);
		}
		queryDataMap.put(query, data);
		return data;
	}
	

	protected QueryData getQueryData(Query query) {
		QueryData queryData = mQueryDataHash.get(query);
		if (queryData == null) {
			queryData = new QueryData(query); 
			mQueryDataHash.put(query, queryData);
		}
		return queryData;
	}
	
	
	public void evaluateCompetitionResults(QueryResultStorage resultStorage, Competition competition) {
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
		

		
		
		for (Query query : resultStorage.getStoredQueryCollection()) {		

			
			resultStorage.visitStoredResultsForQuery(query, new QueryResultStorageItemVisitor() {
				
				@Override
				public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item) {
					boolean resultCorrect = false;
					boolean resultValid = true;
					long processingTime = 0;
					long executionTime = 0;
					
					boolean reportedError = false;
					
					String resultHashCode = "";
					
					if (item != null) {
						QueryResponse queryResponse = item.getQueryResponse();
						if (queryResponse != null) {
							executionTime = queryResponse.getExecutionTime();
							processingTime = queryResponse.getReasonerQueryProcessingTime();
							if (queryResponse.hasTimedOut() || queryResponse.getReasonerQueryProcessingTime() > mProcessingTimeout) {
								resultValid = false;
							} else if (queryResponse.hasExecutionError() || !queryResponse.hasExecutionCompleted()) {
								resultValid = false;
							}
							if (queryResponse.getReasonerErrorsAvailable()) {
								reportedError = true;
							}
						} else {
							resultValid = false;
						}
						QueryResultVerificationReport verificationReport = item.getVerificationReport();
						if (verificationReport != null) {
							resultHashCode = String.valueOf(verificationReport.getResultHashCode());
						}

					} else {
						resultValid = false;
					}
					
					boolean validHashCode = false;
					if (resultValid) {		
						validHashCode = true;
					}
					QueryReasonerData queryReasonerData = getQueryReasonerData(query, reasoner);
					queryReasonerData.mValidResultHashCode = validHashCode;
					if (validHashCode) {
						queryReasonerData.mResultHashCode = resultHashCode;
					}
					
				}				
			});
				
		}
		
		
		Collection<Query> queryCollection = resultStorage.getStoredQueryCollection();
		Collection<ReasonerDescription> reasonerCollection = resultStorage.getStoredReasonerCollection();

		
		for (Query query : queryCollection) {
			QueryData queryData = getQueryData(query);
			int maxSimilarHashCodeCount = 0;
			int differentHashCodeCount = 0;
			int noValidHashCodeCount = 0;
			int validHashCodeCount = 0;
			HashSet<String> differentHashCodeSet = new HashSet<String>();
			HashMap<String,Integer> hashCodeCountSet = new HashMap<String,Integer>(); 
			for (ReasonerDescription reasoner : reasonerCollection) {
				int similarCount = 0;
				int differentCount = 0;
				QueryReasonerData queryReasonerData = getQueryReasonerData(query, reasoner);
				if (queryReasonerData.mValidResultHashCode) {
					String hashCode = queryReasonerData.mResultHashCode;
					differentHashCodeSet.add(hashCode);
					int prevHashCodeCount = 0;
					if (hashCodeCountSet.containsKey(hashCode)) {
						prevHashCodeCount = hashCodeCountSet.get(hashCode); 
					}
					hashCodeCountSet.put(hashCode, prevHashCodeCount+1);
					++validHashCodeCount;
					
					for (ReasonerDescription otherReasoner : reasonerCollection) {
						if (reasoner != otherReasoner) {	
							QueryReasonerData otherQueryReasonerData = getQueryReasonerData(query, otherReasoner);
							if (otherQueryReasonerData.mValidResultHashCode) {
								if (hashCode.equals(otherQueryReasonerData.mResultHashCode)) {	
									++similarCount;
								} else {
									++differentCount;
								}
							}
						}
					}
					
					queryReasonerData.mDifferentHashCodeCount = differentCount;
					queryReasonerData.mSimilarHashCodeCount = similarCount;
					
					maxSimilarHashCodeCount = Math.max(maxSimilarHashCodeCount, similarCount);
					
				} else {
					++noValidHashCodeCount;
				}
			}

			int maxHashCodeCount = 0;
			int maxHashCodeCountCount = 0;
			for (Entry<String, Integer> hashCodeCountEntry : hashCodeCountSet.entrySet()) {
				Integer hashCodeCount = hashCodeCountEntry.getValue();
				if (hashCodeCount > maxHashCodeCount) {
					maxHashCodeCount = hashCodeCount;
					maxHashCodeCountCount = 1; 
				} else if (hashCodeCount == maxHashCodeCount) {
					maxHashCodeCountCount += 1; 
				}
			}
			
			int drawOccurred = 0;
			if (maxHashCodeCountCount > 1) {
				drawOccurred = 1;
			}
			
			int drawPartiesCount = 0;
			if (drawOccurred == 1) {				
				for (ReasonerDescription reasoner : reasonerCollection) {
					QueryReasonerData queryReasonerData = getQueryReasonerData(query, reasoner);
					if (queryReasonerData.mValidResultHashCode) {
						String hashCode = queryReasonerData.mResultHashCode;
						int hashCodeCount = hashCodeCountSet.get(hashCode);
						if (hashCodeCount == maxHashCodeCount) {	
							queryReasonerData.mDrawParticipation = 1;
						}
					}				
				}
				drawPartiesCount = maxHashCodeCountCount;
			}
			
			
			
			differentHashCodeCount = differentHashCodeSet.size();
			queryData.setData(noValidHashCodeCount, maxSimilarHashCodeCount, differentHashCodeCount, drawOccurred, drawPartiesCount);
		}
		
		
		
		
		
		ArrayList<String> reasonerNameList = new ArrayList<String>();
		ArrayList<String> queryNameList = new ArrayList<String>();
		ArrayList<String> evalNameList = new ArrayList<String>();
		ArrayList<String> evalReasonerNameList = new ArrayList<String>();

		for (ReasonerDescription reasoner : reasonerCollection) {		
			reasonerNameList.add(reasoner.getReasonerName());
		}
		for (Query query : queryCollection) {		
			queryNameList.add(query.getOntologySourceString().getPreferedRelativeFilePathString());
		}
		for (EvaluationReasonerType evalType : EvaluationReasonerType.values()) {	
			evalReasonerNameList.add(evalType.mShortString);
		}
		for (EvaluationSummaryType evalType : EvaluationSummaryType.values()) {	
			evalNameList.add(evalType.mShortString);
		}

		
		
		
		
		
		try {
			EvaluationDataTable<Query,EvaluationSummaryType,String> reasonerTable = new EvaluationDataTable<Query,EvaluationSummaryType,String>();
			reasonerTable.initTable(queryCollection, Arrays.asList(EvaluationSummaryType.values()));
			reasonerTable.initColumnHeaders(evalNameList);
			reasonerTable.initRowHeaders(queryNameList);	
			
			
			for (Query query : queryCollection) {	
				QueryData queryData = getQueryData(query);
				reasonerTable.setData(query, EvaluationSummaryType.ET_NO_RESULT_HASH_CODE, String.valueOf(queryData.mNoResultCount));
				reasonerTable.setData(query, EvaluationSummaryType.ET_MAX_SIMILAR_RESULT_COUNT, String.valueOf(queryData.mMaxSimilarResultCount));
				reasonerTable.setData(query, EvaluationSummaryType.ET_DIFFERENT_RESULT_COUNT, String.valueOf(queryData.mDifferentResultCount));
				reasonerTable.setData(query, EvaluationSummaryType.ET_DRAW_OCCURRED, String.valueOf(queryData.mDrawOccurred));
				reasonerTable.setData(query, EvaluationSummaryType.ET_DRAW_PARTIES_COUNT, String.valueOf(queryData.mDrawPartiesCount));
			}
			
			reasonerTable.writeCSVTable(mResultOutputString+"Result-Similarity.csv");
			if (mStatusUpdater != null) {
				mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Result-Similarity",mResultOutputString+"Result-Similarity.csv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV,new DateTime(DateTimeZone.UTC)));
			}
			reasonerTable.writeTSVTable(mResultOutputString+"Result-Similarity.tsv");
			if (mStatusUpdater != null) {
				mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Result-Similarity",mResultOutputString+"Result-Similarity.tsv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV,new DateTime(DateTimeZone.UTC)));
			}
		} catch (Exception e) {
			mLogger.error("Failed to create result similarity table, got Exception '{}'",e.getMessage());
		}
		
		
		
		
		
		
		
		for (ReasonerDescription reasoner : reasonerCollection) {	
			try {
				EvaluationDataTable<Query,EvaluationReasonerType,String> reasonerTable = new EvaluationDataTable<Query,EvaluationReasonerType,String>();
				reasonerTable.initTable(queryCollection, Arrays.asList(EvaluationReasonerType.values()));
				reasonerTable.initColumnHeaders(evalReasonerNameList);
				reasonerTable.initRowHeaders(queryNameList);	
				
				HashMap<Query,QueryReasonerData> queryDataMap = mReasonerQueryDataHash.get(reasoner);
				for (Query query : queryCollection) {	
					QueryReasonerData queryReasonerData = queryDataMap.get(query);
					reasonerTable.setData(query, EvaluationReasonerType.ET_VALID_HASH_CODE, String.valueOf(queryReasonerData.mValidResultHashCode));
					reasonerTable.setData(query, EvaluationReasonerType.ET_SIMILAR_RESULT_COUNT, String.valueOf(queryReasonerData.mSimilarHashCodeCount));
					reasonerTable.setData(query, EvaluationReasonerType.ET_DIFFERENT_RESULT_COUNT, String.valueOf(queryReasonerData.mDifferentHashCodeCount));
					reasonerTable.setData(query, EvaluationReasonerType.ET_DRAW_PARTICIPATION, String.valueOf(queryReasonerData.mDrawParticipation));
				}
				
				reasonerTable.writeCSVTable(mResultOutputString+"Result-Similarity-"+reasoner.getReasonerName()+".csv");
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Result-Similarity-"+reasoner.getReasonerName(),mResultOutputString+"Result-Similarity-"+reasoner.getReasonerName()+".csv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV,new DateTime(DateTimeZone.UTC)));
				}
				reasonerTable.writeTSVTable(mResultOutputString+"Result-Similarity-"+reasoner.getReasonerName()+".tsv");
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Result-Similarity-"+reasoner.getReasonerName(),mResultOutputString+"Result-Similarity-"+reasoner.getReasonerName()+".tsv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV,new DateTime(DateTimeZone.UTC)));
				}
			} catch (Exception e) {
				mLogger.error("Failed to create result similarity table for reasoner '{}', got Exception '{}'",reasoner,e.getMessage());
			}
			
		}
		
			
		
		
		
		
				
	}

}
