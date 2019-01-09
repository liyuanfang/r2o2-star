package org.semanticweb.ore.rendering;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.semanticweb.ore.interfacing.ReasonerInterfaceType;
import org.semanticweb.ore.querying.QueryResponse;

public class QueryResponseTSVRenderer extends TSVRenderer implements QueryResponseRenderer {
	
	public OutputStream mOutputStream = null;
	public OutputStreamWriter mOutputStreamWriter = null;
	
	public boolean renderQueryResponse(QueryResponse queryResponse) {
		boolean successfullyRendered = true;
		try {
			renderReasonerInterface(queryResponse.getUsedInterface());
			writeTSVLine("ReportFile", queryResponse.getReportFilePathString(), mOutputStreamWriter);
			writeTSVLine("ResultDataFile", queryResponse.getResultDataFilePathString(), mOutputStreamWriter);
			writeTSVLine("ErrorFile", queryResponse.getErrorFilePathString(), mOutputStreamWriter);
			writeTSVLine("LogFile", queryResponse.getLogFilePathString(), mOutputStreamWriter);			
			writeTSVLine("ExecutionTime", queryResponse.getExecutionTime(), mOutputStreamWriter);
			writeTSVLine("TimedOut", queryResponse.hasTimedOut(), mOutputStreamWriter);
			writeTSVLine("ExecutionError", queryResponse.hasExecutionError(), mOutputStreamWriter);
			writeTSVLine("ExecutionCompleted", queryResponse.hasExecutionCompleted(), mOutputStreamWriter);
			writeTSVLine("ReasonerQueryProcessingTime", queryResponse.getReasonerQueryProcessingTime(), mOutputStreamWriter);
			writeTSVLine("ReasonerQueryStarted", queryResponse.getReasonerQueryStarted(), mOutputStreamWriter);
			writeTSVLine("ReasonerQueryCompleted", queryResponse.getReasonerQueryCompleted(), mOutputStreamWriter);
			writeTSVLine("ReasonerConsoleOutputAvailable", queryResponse.getReasonerConsoleOutputAvailable(), mOutputStreamWriter);
			writeTSVLine("ReasonerErrorsAvailable", queryResponse.getReasonerErrorsAvailable(), mOutputStreamWriter);
			writeTSVLine("ResultDataAvailable", queryResponse.getResultDataAvailable(), mOutputStreamWriter);
			writeTSVLine("ReasonerOutputParsingError", queryResponse.getReasonerOutputParsingError(), mOutputStreamWriter);
			writeTSVLine("ExecutionStartTime", queryResponse.getExecutionStartDateTime(), mOutputStreamWriter);
			writeTSVLine("ExecutionEndTime", queryResponse.getExecutionEndDateTime(), mOutputStreamWriter);
			mOutputStreamWriter.close();
		} catch (IOException e) {
			successfullyRendered = false;
		}		
		return successfullyRendered;
	}
	
	
	public void renderReasonerInterface(ReasonerInterfaceType reasonerInterface) throws IOException {
		String interfaceTypeString = "UNKNOWN";
		if (reasonerInterface == ReasonerInterfaceType.REASONER_INTERFACE_ORE_V1) {
			interfaceTypeString = "OREv1";
		}
		writeTSVLine("InterfaceType", interfaceTypeString, mOutputStreamWriter);
	}	
	
	public QueryResponseTSVRenderer(OutputStream outputStream) {
		mOutputStream = outputStream;
		mOutputStreamWriter = new OutputStreamWriter(mOutputStream);
	}

	public QueryResponseTSVRenderer(String outputFileString) throws FileNotFoundException {
		mOutputStream = new FileOutputStream(outputFileString);
		mOutputStreamWriter = new OutputStreamWriter(mOutputStream);
	}

}
