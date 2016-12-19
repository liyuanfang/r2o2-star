package ore.interfacing;

import ore.execution.ReasonerQueryExecutionReport;
import ore.querying.Query;
import ore.querying.QueryResponse;
import ore.utilities.FilePathString;
import ore.utilities.FileSystemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

public class ReasonerAdaptorORE implements ReasonerAdaptor {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerAdaptorORE.class);

	protected Query mQuery = null;
	protected ReasonerDescription mReasoner = null;
	
	protected FileOutputStream mOutputStream = null;
	
	protected FilePathString mResultOutputFileString = null;
	protected FilePathString mErrorOutputFileString = null;
	protected FilePathString mConsoleOutputFilePathString = null;
	protected FilePathString mResponseOutputFilePathString = null;
	
	protected ArrayList<String> mExecutionArgumentsList = new ArrayList<String>();
	protected boolean mQueryInitialisationFailed = false;
	
	@Override
	public boolean prepareReasonerExecution() {
		return !mQueryInitialisationFailed;
	}

	@Override
	public boolean completeReasonerExecution() {
		boolean successfullyCompleted = false;
		if (mOutputStream != null) {
			try {
				mOutputStream.close();
			} catch (IOException e) {
				mLogger.error("Closing output stream failed for reasoner '{}', got IOException '{}'.",mReasoner.toString(),e.getMessage());
				successfullyCompleted = false;
			}
		}
		return successfullyCompleted;
	}
	
	
	public boolean isFileAvailable(String fileString) {
		File resultOutputFile = new File(fileString);
		if (resultOutputFile.exists() && resultOutputFile.length() > 0) {
			return true;
		}
		return false;
	}	
	
	public void checkOutputAvailability(QueryResponse queryResponse) {
		if (isFileAvailable(mResultOutputFileString.getAbsoluteFilePathString())) {
			queryResponse.setResultDataAvailable(true);
		}
		if (isFileAvailable(mConsoleOutputFilePathString.getAbsoluteFilePathString())) {
			queryResponse.setReasonerConsoleOutputAvailable(true);
		}
		if (isFileAvailable(mErrorOutputFileString.getAbsoluteFilePathString())) {
			queryResponse.setReasonerErrorsAvailable(true);
		}
	}
	
	public boolean parseReasonerOutput(QueryResponse queryResponse) {
		
		boolean startedOperation = false;
		boolean completedOperation = false;
		boolean operationTimeParsed = false;
		boolean extractionError = false;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(mConsoleOutputFilePathString.getAbsoluteFilePathString()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				
				mLogger.debug("Parsing reasoner output '{}'.",line);
				
				if (line.toLowerCase().startsWith("started")) {
					queryResponse.setReasonerQueryStarted(true);
					startedOperation = true;
				}
				
				if (line.toLowerCase().startsWith("completed")) {
					queryResponse.setReasonerQueryCompleted(true);
					completedOperation = true;
				}
				
				
				StringTokenizer st = new StringTokenizer(line, ":");
				if (st.hasMoreElements()) {
					String tokenString1 = st.nextToken().trim().toLowerCase();
					
					if (st.hasMoreElements()) {
						String tokenString2 = st.nextToken().trim().toLowerCase();
					
						if (tokenString1.startsWith("operation time") || tokenString1.startsWith("classification time") || tokenString1.startsWith("sat time") || tokenString1.startsWith("satisfiability time") || tokenString1.startsWith("consistency time") || tokenString1.startsWith("realisation time") || tokenString1.startsWith("realization time")) {
							if (!operationTimeParsed) {
								try {							
									long operationTime = Long.parseLong(tokenString2);
									queryResponse.setReasonerQueryProcessingTime(operationTime);
									operationTimeParsed = true;
									mLogger.info("Extracted query processing time of '{}' milliseconds from '{}'.",operationTime,mConsoleOutputFilePathString);
								} catch (NumberFormatException e) {
									mLogger.warn("Parsing of query processing time to long from '{}' failed, got NumberFormatException '{}'.",mConsoleOutputFilePathString,e.getMessage());
								}
							}
							if (!operationTimeParsed) {
								try {							
									double operationTime = Double.parseDouble(tokenString2);
									operationTimeParsed = true;
									queryResponse.setReasonerQueryProcessingTime((long)operationTime);
									mLogger.info("Extracted query processing time of '{}' milliseconds from '{}'.",operationTime,mConsoleOutputFilePathString);
								} catch (NumberFormatException e) {
									mLogger.warn("Parsing of query processing time to double from '{}' failed, got NumberFormatException '{}'.",mConsoleOutputFilePathString,e.getMessage());
								}
							}
						}
					}
				}
				

			}
			reader.close();
		} catch (IOException e) {
			mLogger.error("Cannot parse reasoner output '{}', got IOException '{}'.",mConsoleOutputFilePathString,e.getMessage());
			extractionError = true;
		}
		
		if (!operationTimeParsed) {			
			mLogger.error("Failed extraction of query processing time from '{}'.",mConsoleOutputFilePathString);
			extractionError = true;
		}
		
		if (!startedOperation) {			
			mLogger.error("Failed extraction of query processing start from '{}'.",mConsoleOutputFilePathString);
			extractionError = true;
		}
		
		if (!completedOperation) {			
			mLogger.error("Failed extraction of query processing completion from '{}'.",mConsoleOutputFilePathString);
			extractionError = true;
		}		
		if (extractionError) {
			queryResponse.setReasonerOutputParsingError(true);
		}
		return !extractionError;
	}

	@Override
	public String getReasonerExecutionCommandString() {
		return mReasoner.getStarterScript().getAbsoluteFilePathString();
	}

	@Override
	public String getReasonerExecutionWorkingDirectoryString() {
		return mReasoner.getWorkingDirectory().getAbsoluteFilePathString();
	}

	@Override
	public Collection<String> getReasonerExecutionArguments() {
		return mExecutionArgumentsList;
	}

	@Override
	public InputStream getReasonerExecutionInputStream() {
		return null;
	}

	@Override
	public OutputStream getReasonerExecutionOutputStream() {
		try {
			FileSystemHandler.ensurePathToFileExists(mConsoleOutputFilePathString);
			mOutputStream = new FileOutputStream(mConsoleOutputFilePathString.getAbsoluteFilePathString());
		} catch (FileNotFoundException e) {
			mLogger.error("Cannot open output stream to file '{}', got FileNotFoundException '{}'.",mConsoleOutputFilePathString,e.getMessage());
		}
		return mOutputStream;
	}

	@Override
	public OutputStream getReasonerExecutionErrorStream() {
		return null;
	}

	@Override
	public QueryResponse createQueryResponse(ReasonerQueryExecutionReport executionReport) {
		QueryResponse response = new QueryResponse(mResultOutputFileString,mResponseOutputFilePathString,mConsoleOutputFilePathString,mErrorOutputFileString,ReasonerInterfaceType.REASONER_INTERFACE_ORE_V1);
		checkOutputAvailability(response);
		parseReasonerOutput(response);
		response.setExecutionCompleted(executionReport.hasExecutionCompleted());
		response.setExecutionError(executionReport.hasExecutionError());
		response.setTimedOut(executionReport.hasTimedOut());
		response.setExecutionTime(executionReport.getExecutionTime());
		response.setExecutionEndDateTime(executionReport.getExecutionEndDataTime());
		response.setExecutionStartDateTime(executionReport.getExecutionStartDataTime());
		return response;
	}
	
	
	@Override
	public String getResponseFileString() {
		return mResponseOutputFilePathString.getAbsoluteFilePathString();
	}



}
