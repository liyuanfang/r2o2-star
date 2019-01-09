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
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReasonerQueryResultAgreementReorganizer implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerQueryResultAgreementReorganizer.class);
	
	private Config mConfig = null;
	private String mResultOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	public ReasonerQueryResultAgreementReorganizer(Config config, String expectionsOutputString) {
		mResultOutputString = expectionsOutputString;
		mConfig = config;
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);		
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);		
	}
	

	protected class QueryReasonerData {	
		ReasonerDescription mReasoner = null;
		Query mQuery = null;
		String mResultHashCode = "-";
		
		boolean mValidResultHashCode = false;
		FilePathString mResultFile = null;
		

		
		public QueryReasonerData(Query query, ReasonerDescription reasoner) {
			mQuery = query; 
			mReasoner = reasoner;
		}

	}


	
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
					FilePathString resultFileString = null;
					
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
							resultFileString = queryResponse.getResultDataFilePathString();
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
					queryReasonerData.mResultFile = resultFileString;
					
				}				
			});
				
		}
		
		
		Collection<Query> queryCollection = resultStorage.getStoredQueryCollection();
		Collection<ReasonerDescription> reasonerCollection = resultStorage.getStoredReasonerCollection();

		
		for (Query query : queryCollection) {
			
			boolean allReasonersAgreed = true;
			String allHashCode = null;
			
			for (ReasonerDescription reasoner : reasonerCollection) {
				QueryReasonerData queryReasonerData = getQueryReasonerData(query, reasoner);
				if (queryReasonerData.mValidResultHashCode) {
					String hashCode = queryReasonerData.mResultHashCode;
					
					if (allHashCode == null) {
						allHashCode = hashCode;						
					} else {
						if (!hashCode.equals(allHashCode)) {
							allReasonersAgreed = false;
						}
					}
					
					
				} else {
					allReasonersAgreed = false;
				}
			}
			
			String reoganisationString = "disagreement/";
			if (allReasonersAgreed) {
				reoganisationString = "all_agreed/";
			}
			
			for (ReasonerDescription reasoner : reasonerCollection) {
				QueryReasonerData queryReasonerData = getQueryReasonerData(query, reasoner);
				try {
					String newResultFileString = mResultOutputString+reoganisationString+"inf_"+query.getOntologySourceString().getFileString()+"_"+reasoner.getOutputPathString()+".xml";
					if (queryReasonerData.mResultFile != null) {
						FileSystemHandler.copyFile(queryReasonerData.mResultFile, new FilePathString(newResultFileString));
					}
				} catch (Exception e) {
				}
			}

		}
				
				
	}

}
