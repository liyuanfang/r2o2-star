package org.semanticweb.ore.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.semanticweb.ore.competition.Competition;
import org.semanticweb.ore.competition.CompetitionStatusUpdater;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.verification.QueryResultVerificationCorrectnessType;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReasonerQueryNumberOfResultsWithinTimeGenerationEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerQueryNumberOfResultsWithinTimeGenerationEvaluator.class);
	
	private Config mConfig = null;
	private String mOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;

	protected EvaluationChartPrintingHandler mChartPrintingHandler = null;

	
	protected enum EvaluationType {
		ET_EXECUTION_TIME("Execution-Time","Execution Time"),
		ET_CORRECTLY_PROCESSED_TIME("Correctly-Processed-Time","Correctly Processed Time");		
		
		String mShortString = null;
		String mTitleString = null;
		
		private EvaluationType(String shortString, String titleName) {
			mShortString = shortString;
			mTitleString = titleName;
		}
	}
	

	
	

	public ReasonerQueryNumberOfResultsWithinTimeGenerationEvaluator(Config config, String outputString, CompetitionStatusUpdater statusUpdater) {
		mOutputString = outputString;
		mConfig = config;
		mStatusUpdater = statusUpdater;
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);		
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);
		
		mChartPrintingHandler = new EvaluationChartPrintingHandler(mConfig,mOutputString,statusUpdater);

	}
	
	
	protected class QueryProcessingData implements Comparable<QueryProcessingData> {
		Query mQuery = null;			
		long mValue = 0;

		public QueryProcessingData(Query query) {
			mQuery = query;
		}
		
		public void setValue(long value) {
			mValue += value;
		}

		@Override
		public int compareTo(QueryProcessingData qpd) {
			if (mValue < qpd.mValue) {
				return -1;
			} else if (mValue > qpd.mValue) {
				return 1;
			} 
			return 0;
		}
	}	
	
	
	
	protected HashMap<EvaluationType,HashMap<ReasonerDescription,HashMap<Query,QueryProcessingData>>> mTypeReasonerQueryDataMap = new HashMap<EvaluationType,HashMap<ReasonerDescription,HashMap<Query,QueryProcessingData>>>();

	
	protected QueryProcessingData getProcessingData(EvaluationType evalType, ReasonerDescription reasoner, Query query) {
		HashMap<ReasonerDescription,HashMap<Query,QueryProcessingData>> reasonerQueryDataMap = mTypeReasonerQueryDataMap.get(evalType);
		if (reasonerQueryDataMap == null) {
			reasonerQueryDataMap = new HashMap<ReasonerDescription,HashMap<Query,QueryProcessingData>>();
			mTypeReasonerQueryDataMap.put(evalType,reasonerQueryDataMap);
		}
		HashMap<Query,QueryProcessingData> queryDataMap = reasonerQueryDataMap.get(reasoner);
		if (queryDataMap == null) {
			queryDataMap = new HashMap<Query,QueryProcessingData>();
			reasonerQueryDataMap.put(reasoner,queryDataMap);
		}
		QueryProcessingData processingData = queryDataMap.get(query);
		if (processingData == null) {
			processingData = new QueryProcessingData(query);
			queryDataMap.put(query, processingData);
		}
		return processingData;
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
		
		
		
		Collection<Query> queryCollection = resultStorage.getStoredQueryCollection();
		Collection<ReasonerDescription> reasonerCollection = resultStorage.getStoredReasonerCollection();
		
		ArrayList<String> queryNameList = new ArrayList<String>();
		ArrayList<String> evalNameList = new ArrayList<String>();

		ArrayList<String> queryNumberStringList = new ArrayList<String>();

		int nextQueryNumber = 1;
		for (Query query : queryCollection) {		
			queryNameList.add(query.getOntologySourceString().getPreferedRelativeFilePathString());
			queryNumberStringList.add(String.valueOf(nextQueryNumber++));
		}
		for (EvaluationType evalType : EvaluationType.values()) {	
			evalNameList.add(evalType.mShortString);
		}
		
		


		
		for (Query query : queryCollection) {
			
			resultStorage.visitStoredResultsForQuery(query, new QueryResultStorageItemVisitor() {
				@Override
				public void visitQueryResultStorageItem(ReasonerDescription reasoner, Query query, QueryResultStorageItem item) {
					boolean resultCorrect = false;
					boolean resultValid = true;
					long processingTime = 0;
					long executionTime = 0;
					if (item != null) {
						QueryResponse queryResponse = item.getQueryResponse();
						if (queryResponse != null) {
							processingTime = queryResponse.getReasonerQueryProcessingTime();		
							executionTime = queryResponse.getExecutionTime();
							if (queryResponse.hasTimedOut() || queryResponse.hasExecutionError() || !queryResponse.hasExecutionCompleted() || queryResponse.getReasonerQueryProcessingTime() > mProcessingTimeout) {
								resultValid = false;
							}
						} else {
							resultValid = false;
						}
						QueryResultVerificationReport verificationReport = item.getVerificationReport();
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
						QueryProcessingData processingData = getProcessingData(EvaluationType.ET_CORRECTLY_PROCESSED_TIME, reasoner, query);
						processingData.setValue(processingTime);
					}
					QueryProcessingData processingData = getProcessingData(EvaluationType.ET_EXECUTION_TIME, reasoner, query);
					processingData.setValue(executionTime);

				}			
			});
		}
		
		
		
		
		for (EvaluationType evalType : EvaluationType.values()) {
			HashMap<ReasonerDescription,HashMap<Query,QueryProcessingData>> reasonerQueryDataMap = mTypeReasonerQueryDataMap.get(evalType);
			
			EvaluationChartPrintingData chartData = new EvaluationChartPrintingData("Comparison of Accumulated "+evalType.mTitleString); 
			
			
			for (ReasonerDescription reasoner : reasonerCollection) {		
				String dataName = reasoner.getReasonerName();
				ArrayList<String> dataStringList = new ArrayList<String>(); 
				
				HashMap<Query,QueryProcessingData> queryDataMap = reasonerQueryDataMap.get(reasoner);
				ArrayList<QueryProcessingData> dataValueList = new ArrayList<QueryProcessingData>();
				long accumulatedValue = 0;
				for (Query query : queryCollection) {
					QueryProcessingData processingData = queryDataMap.get(query);
					if (processingData != null) {
						long newAccumulatedValue = accumulatedValue+processingData.mValue;
						processingData.mValue = newAccumulatedValue; 
						accumulatedValue = newAccumulatedValue;
						dataValueList.add(processingData);
					}
				}
				
				for (QueryProcessingData data : dataValueList) {		
					dataStringList.add(String.valueOf(data.mValue));
				}
				
				chartData.addDataSerie(dataStringList,dataName);
				chartData.setDataValuesNames(queryNumberStringList);
				chartData.setDataTitle("Number of queries");
				chartData.setValuesTitle("Accumulated time in milliseconds");
			}
			
			mChartPrintingHandler.printCactusChart(competition, "Query-Accumulated-"+evalType.mShortString, chartData);
			mChartPrintingHandler.printLogarithmicCactusChart(competition, "Query-Accumulated-"+evalType.mShortString+"-Logarithmic", chartData);

		}
		
		
		
		
		
		

		
	}

}
