package ore.verification;

import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.interfacing.ReasonerDescription;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.querying.QueryResponseStoringHandler;
import ore.querying.QueryResultData;
import ore.utilities.FilePathString;
import ore.utilities.FileSystemHandler;
import ore.utilities.RelativeFilePathStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ExpectedQueryResultHashComparisonVerifier implements QueryResultVerifier {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ExpectedQueryResultHashComparisonVerifier.class);
	
	private Config mConfig = null;
	private boolean mLoadSaveHashCodes = false;
	private String mExpectedResultDirectoryString = null;
	private String mExpectedResultAbsoluteDirectoryString = null;
	private String mExpectedResultRelativeDirectoryString = null;
	private QueryResultNormaliserFactory mQueryResultNormalisationFactory = null;
	private QueryResponseStoringHandler mQueryResponseStoringHandler = null;
	
	private final static String mHashStringSuffix = ".hash";

	@Override
	public QueryResultVerificationReport verifyResponse(ReasonerDescription reasoner, Query query, QueryResponse queryResponse) {
		mLogger.info("Verifying query response for query '{}'.", query);
		QueryResultVerificationCorrectnessType correctness = QueryResultVerificationCorrectnessType.QUERY_RESULT_UNKNOWN;
		int hashCode = 0;
		ResultDataHashCode resultDataHashCode = getResultDataHashCode(query,queryResponse);
		if (resultDataHashCode.isHashCodeAvailable()) {
			hashCode = resultDataHashCode.getHashCode();
			
			ResultDataHashCode expectedResultDataHashCode = null;
			
			if (expectedResultDataHashCode == null) {
				QueryResponse expectedQueryResponse = loadExpectedQueryResponse(query);
				if (expectedQueryResponse != null) {				
					expectedResultDataHashCode = getResultDataHashCode(query,expectedQueryResponse);				
				}
			}
			if (expectedResultDataHashCode == null) {
				expectedResultDataHashCode = loadExpectedHashCode(query);
			}			
			
			if (expectedResultDataHashCode != null) {
				if (expectedResultDataHashCode.isHashCodeAvailable()) {				
					if (expectedResultDataHashCode.getHashCode() == resultDataHashCode.getHashCode()) {
						correctness = QueryResultVerificationCorrectnessType.QUERY_RESULT_CORRECT;
						mLogger.info("Query response from '{}' matches expected hash code '{}' from '{}'.", new Object[]{queryResponse.getReportFilePathString(),hashCode,query});
					} else {
						correctness = QueryResultVerificationCorrectnessType.QUERY_RESULT_INCORRECT;
						mLogger.warn("Query response from '{}' with hash code '{}' does NOT match expected hash code '{}' for '{}'.", new Object[]{queryResponse.getReportFilePathString(),hashCode,expectedResultDataHashCode.getHashCode(),query});
					}
				}
			} else {
				mLogger.error("Failed to load expected result for query '{}'.",query);
			}
		}						
		if (correctness == QueryResultVerificationCorrectnessType.QUERY_RESULT_UNKNOWN) {
			mLogger.error("Failed to verify query response from '{}' for query '{}'.",queryResponse.getReportFilePathString(),query);
		}
		boolean supported = reasoner.getExpressivitySupport().supportsQuery(query.getQueryExpressivity());
		return new QueryResultVerificationReport(correctness,hashCode,supported);
	}
	
	
	public ExpectedQueryResultHashComparisonVerifier(QueryResultNormaliserFactory queryResultNormalisationFactory, QueryResponseStoringHandler queryResponseStoringHandler, Config config) {
		mQueryResponseStoringHandler = queryResponseStoringHandler;
		mQueryResultNormalisationFactory = queryResultNormalisationFactory;
		mConfig = config;
		mExpectedResultDirectoryString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_EXPECTATIONS_DIRECTORY);
		mExpectedResultRelativeDirectoryString = mExpectedResultDirectoryString+"relative"+ File.separator;
		mExpectedResultAbsoluteDirectoryString = mExpectedResultDirectoryString+"absolute"+ File.separator;
		mLoadSaveHashCodes = ConfigDataValueReader.getConfigDataValueBoolean(mConfig, ConfigType.CONFIG_TYPE_SAVE_LOAD_RESULT_HASH_CODES, true);
	}
	
	
	
	

	protected FilePathString getExpectedResultDataString(FilePathString ontologySourceString) {
		return getExpectedFileString(ontologySourceString, "query-response.dat");
	}	
	
	
	protected FilePathString getExpectedFileString(FilePathString ontologySourceString, String suffixString) {
		String redirectSourceString = null;
		String expectationResponseRelativeSourceString = null;
		String expectationResponseBaseSourceString = null;
		if (ontologySourceString.isRelative()) {	
			expectationResponseBaseSourceString = mExpectedResultRelativeDirectoryString;
			redirectSourceString = ontologySourceString.getRelativeFilePathString();
		} else {			
			expectationResponseBaseSourceString = mExpectedResultAbsoluteDirectoryString;
			redirectSourceString = ontologySourceString.getAbsoluteFilePathString();
		}
		File redirectSourceFile = new File(redirectSourceString);
		while (redirectSourceFile != null) {
			String filePartString = redirectSourceFile.getName();
			if (expectationResponseRelativeSourceString == null) {
				expectationResponseRelativeSourceString = filePartString;
			} else {
				expectationResponseRelativeSourceString = filePartString + File.separator + expectationResponseRelativeSourceString;
			}
			redirectSourceFile = redirectSourceFile.getParentFile();
		}

		FilePathString redirectionFilePathString = new FilePathString(expectationResponseBaseSourceString,expectationResponseRelativeSourceString+ File.separator+suffixString, RelativeFilePathStringType.RELATIVE_TO_CONVERSIONS_DIRECTORY);
		return redirectionFilePathString;
	}		

	protected FilePathString getExpectedHashCodeString(FilePathString ontologySourceString) {
		return getExpectedFileString(ontologySourceString, "query-result-data.owl.hash");
	}	
		
	
	

	protected QueryResponse loadExpectedQueryResponse(Query query) {
		QueryResponse queryResponse = null;
		FilePathString expectedResponseFilePathString = getExpectedResultDataString(query.getQuerySourceString());
		if (FileSystemHandler.nonEmptyFileExists(expectedResponseFilePathString)) {
			queryResponse = mQueryResponseStoringHandler.loadQueryResponseData(expectedResponseFilePathString);
		}
		return queryResponse;
	}
	


	protected ResultDataHashCode loadExpectedHashCode(Query query) {
		ResultDataHashCode resultHashCode = null;
		FilePathString expectedHashCodeFilePathString = getExpectedHashCodeString(query.getQuerySourceString());
		if (FileSystemHandler.nonEmptyFileExists(expectedHashCodeFilePathString)) {
			resultHashCode = loadResultHashDataFromFile(expectedHashCodeFilePathString.getAbsoluteFilePathString());
		}
		return resultHashCode;
	}
	
	
		
	protected ResultDataHashCode getResultDataHashCode(Query query, QueryResponse queryResponse) {
		
		
		ResultDataHashCode resultHashCode = null;

		if (mLoadSaveHashCodes) {
			String resultDataString = queryResponse.getResultDataFilePathString().getAbsoluteFilePathString();
			String resultDataHashString = resultDataString+mHashStringSuffix;
			resultHashCode = loadResultHashDataFromFile(resultDataHashString);
		}

		if (resultHashCode == null || !resultHashCode.isHashCodeAvailable()) {
			resultHashCode = createResultHashData(query,queryResponse);
		}
		
		return resultHashCode;
	}
	

	protected ResultDataHashCode createResultHashData(Query query, QueryResponse queryResponse) {
		String resultDataString = queryResponse.getResultDataFilePathString().getAbsoluteFilePathString();
		String resultDataHashString = resultDataString+mHashStringSuffix;
		
		int resultHashCode = 0;
		boolean createdHashCode = false;
		
		QueryResultNormaliser normalizer = mQueryResultNormalisationFactory.createQueryResultNormaliser(query, queryResponse);
		if (normalizer != null) {
			QueryResultData normalisedResultData = normalizer.getNormalisedResult(query, queryResponse);
			if (normalisedResultData != null) {
				resultHashCode = normalisedResultData.getResultHashCode();
				createdHashCode = true;
				if (mLoadSaveHashCodes) {
					try {
						FileOutputStream outputStream = new FileOutputStream(new File(resultDataHashString));
						OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
						outputStreamWriter.write(new Integer(resultHashCode).toString());
						outputStreamWriter.close();
					} catch (IOException e) {
						mLogger.error("Saving hash code '{}' to '{}' failed, got IOException '{}'.",resultHashCode,e.getMessage());
					}
				}
			}
		}
		
		return new ResultDataHashCode(resultHashCode,createdHashCode);
	}
	
	
	
	protected ResultDataHashCode loadResultHashDataFromFile(String resultDataHashString) {
		boolean resultParsed = false;
		int resultHashCode = 0;
		try {
			File resultDataHashFile = new File(resultDataHashString);
			if (resultDataHashFile.isFile() && resultDataHashFile.exists() && resultDataHashFile.canRead() && resultDataHashFile.length() > 1) {
				FileInputStream inputStream = new FileInputStream(resultDataHashFile);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				String line = null;
				if ((line = reader.readLine()) != null) {
					String dataString = line.trim();
					try {
						resultHashCode = Integer.parseInt(dataString);
						resultParsed = true;
					} catch (NumberFormatException e) {
						mLogger.error("Parsing of result data hash code for '{}' failed, got NumberFormatException '{}'.",resultDataHashString,e.getMessage());
					}
				}
				reader.close();
			}
		} catch (IOException e) {
			mLogger.error("Parsing of result data hash code for '{}' failed, got IOException '{}'.",resultDataHashString,e.getMessage());
		}
		return new ResultDataHashCode(resultHashCode,resultParsed);
	}
	

	protected class ResultDataHashCode {
		private int mHashCode = 0;
		private boolean mHashCodeAvailable = false;
		
		public int getHashCode() {
			return mHashCode;
		}
		
		public boolean isHashCodeAvailable() {
			return mHashCodeAvailable;
		}
		
		public ResultDataHashCode(int hashCode, boolean available) {
			mHashCode = hashCode;
			mHashCodeAvailable = available;
		}
	}

}
