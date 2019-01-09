package org.semanticweb.ore.interfacing;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.semanticweb.ore.execution.ReasonerQueryExecutionReport;
import org.semanticweb.ore.querying.QueryResponse;

public interface ReasonerAdaptor {

	public boolean prepareReasonerExecution();
	public boolean completeReasonerExecution();
	
	public String getReasonerExecutionCommandString();
	public String getReasonerExecutionWorkingDirectoryString();	
	public Collection<String> getReasonerExecutionArguments();
	
	
	public String getResponseFileString();
	
	public InputStream getReasonerExecutionInputStream();
	public OutputStream getReasonerExecutionOutputStream();
	public OutputStream getReasonerExecutionErrorStream();
	
	public QueryResponse createQueryResponse(ReasonerQueryExecutionReport executionReport);

}
