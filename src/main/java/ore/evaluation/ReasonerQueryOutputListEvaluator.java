package ore.evaluation;

import ore.competition.Competition;
import ore.competition.CompetitionEvaluationStatus;
import ore.competition.CompetitionEvaluationType;
import ore.competition.CompetitionStatusUpdater;
import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.utilities.FilePathString;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class ReasonerQueryOutputListEvaluator implements StoredQueryResultEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerQueryOutputListEvaluator.class);
	
	private Config mConfig = null;
	private String mResultOutputString = null;
	
	protected long mDefaultExecutionTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mDefaultProcessingTimeout = 1000 * 60 * 5; // 5 minutes
	protected long mExecutionTimeout = 0;
	protected long mProcessingTimeout = 0;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	public ReasonerQueryOutputListEvaluator(Config config, String expectionsOutputString, CompetitionStatusUpdater statusUpdater) {
		mResultOutputString = expectionsOutputString;
		mConfig = config;
		mStatusUpdater = statusUpdater;
		
		mDefaultExecutionTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig, ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT,mDefaultExecutionTimeout);
		mDefaultProcessingTimeout = ConfigDataValueReader.getConfigDataValueLong(mConfig,ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT,mDefaultProcessingTimeout);		
	}
	
	protected enum EvaluationType {
		ET_CONSOLE_OUTPUT("Console-Output"),
		ET_REPORTED_ERRORS("Reported-Errors");
		
		String mShortString = null;
		
		private EvaluationType(String shortString) {
			mShortString = shortString;
		}
	}
	

	
	
	protected class QueryReasonerData {	
		ReasonerDescription mReasoner = null;
		Query mQuery = null;
		String mConsoleOutput = null;
		String mReportedError = null;
		
		String getValueString(EvaluationType evalType) {
			String string = null;
			if (evalType == EvaluationType.ET_REPORTED_ERRORS) {
				string = String.valueOf(mReportedError);
			} else if (evalType == EvaluationType.ET_CONSOLE_OUTPUT) {
				string = String.valueOf(mConsoleOutput);
			}
			return string;
		}
		
		QueryReasonerData(ReasonerDescription reasoner, Query query) {
			mReasoner = reasoner;
			mQuery = query;
		}
		
		void setData(String consoleOutputString, String reportedErrorString) {
			mReportedError = reportedErrorString.replace("\t", " ").replace("\n", "<br>");
			mConsoleOutput = consoleOutputString.replace("\t", " ").replace("\n", "<br>");
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
			data = new QueryReasonerData(reasoner,query);
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
	
	
	

	protected String loadFileIntoString(String fileString) {
		String string = null;
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileString));
			StringBuilder sb = new StringBuilder();
	        byte[] b = new byte[4096];
	        for (int i; (i = inputStream.read(b)) != -1;) {
	        	sb.append(new String(b, 0, i));
	        }		        
			inputStream.close();
			
			string = sb.toString();
			
		} catch (Exception e) {
			mLogger.error("Could not access file '{}', got Exception '{}'",fileString,e.getMessage());
		}	
		return string;
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
					String consoleOutputString = null;
					String reportedErrorsString = null;
					
					if (item != null) {
						QueryResponse queryResponse = item.getQueryResponse();
						if (queryResponse != null) {
							if (queryResponse.getReasonerConsoleOutputAvailable()) {
								FilePathString consoleOutputFilePathString = queryResponse.getLogFilePathString();
								consoleOutputString = loadFileIntoString(consoleOutputFilePathString.getAbsoluteFilePathString());
							}
							if (queryResponse.getReasonerErrorsAvailable()) {
								FilePathString errorsFilePathString = queryResponse.getErrorFilePathString();
								reportedErrorsString = loadFileIntoString(errorsFilePathString.getAbsoluteFilePathString());
							}
						}
					}

					if (consoleOutputString == null) {
						consoleOutputString = "";
					}
					if (reportedErrorsString == null) {
						reportedErrorsString = "";
					}

					QueryReasonerData queryReasonerData = getQueryReasonerData(query, reasoner);
					queryReasonerData.setData(consoleOutputString,reportedErrorsString);
					
				}				
			});
				
		}
		
		
		Collection<Query> queryCollection = resultStorage.getStoredQueryCollection();
		Collection<ReasonerDescription> reasonerCollection = resultStorage.getStoredReasonerCollection();
		
		ArrayList<String> reasonerNameList = new ArrayList<String>();
		ArrayList<String> queryNameList = new ArrayList<String>();

		for (ReasonerDescription reasoner : reasonerCollection) {		
			reasonerNameList.add(reasoner.getReasonerName());
		}
		for (Query query : queryCollection) {		
			queryNameList.add(query.getOntologySourceString().getPreferedRelativeFilePathString());
		}


		for (ReasonerDescription reasoner : reasonerCollection) {	
			
			for (EvaluationType evalType : EvaluationType.values()) {	
				
				String evaluationOutputString = "Console-";
				if (evalType == EvaluationType.ET_REPORTED_ERRORS) {
					evaluationOutputString = "Errors-";
				}
				ArrayList<String> evalNameList = new ArrayList<String>();
				evalNameList.add(evalType.mShortString);
			
			
				try {
					EvaluationDataTable<Query,EvaluationType,String> reasonerTable = new EvaluationDataTable<Query,EvaluationType,String>();
					ArrayList<EvaluationType> evalTypeList = new ArrayList<EvaluationType>();
					evalTypeList.add(evalType);
					reasonerTable.initTable(queryCollection, evalTypeList);
					reasonerTable.initColumnHeaders(evalNameList);
					reasonerTable.initRowHeaders(queryNameList);	
					
					HashMap<Query,QueryReasonerData> queryDataMap = mReasonerQueryDataHash.get(reasoner);
					for (Query query : queryCollection) {	
						QueryReasonerData queryReasonerData = queryDataMap.get(query);
						if (evalType == EvaluationType.ET_CONSOLE_OUTPUT) {
							reasonerTable.setData(query, EvaluationType.ET_CONSOLE_OUTPUT, queryReasonerData.mConsoleOutput);
						} else {
							reasonerTable.setData(query, EvaluationType.ET_REPORTED_ERRORS, queryReasonerData.mReportedError);
						}
					}
					
					reasonerTable.writeTSVTable(mResultOutputString+"Reasoner-Outputs-"+evaluationOutputString+reasoner.getReasonerName()+".tsv");
					if (mStatusUpdater != null) {
						mStatusUpdater.updateCompetitionEvaluationStatus(new CompetitionEvaluationStatus(competition,"Reasoner-Outputs-"+evaluationOutputString+reasoner.getReasonerName(),mResultOutputString+"Reasoner-Outputs-"+evaluationOutputString+reasoner.getReasonerName()+".tsv",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV,new DateTime(DateTimeZone.UTC)));
					}
				} catch (Exception e) {
					mLogger.error("Failed to create result table for reasoner '{}', got Exception '{}'",reasoner,e.getMessage());
				}
			}
			
		}
		
		
		
		
		
		
		
		
				
	}

}
