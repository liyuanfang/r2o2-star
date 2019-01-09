package org.semanticweb.ore.networking.events;

import org.semanticweb.ore.networking.ExecutionReport;
import org.semanticweb.ore.networking.ExecutionTaskHandler;
import org.semanticweb.ore.threading.Event;

public class ReportExecutionReportEvent implements Event {
	
	protected ExecutionTaskHandler mHandler = null;
	protected ExecutionReport mExecutionReport = null;
	
	public ReportExecutionReportEvent(ExecutionTaskHandler handler, ExecutionReport executionReport) {
		mHandler = handler;
		mExecutionReport = executionReport;
	}
	
	public ExecutionReport getExecutionReport() {
		return mExecutionReport;
	}
	
	public ExecutionTaskHandler getExecutionTaskHandler() {
		return mHandler;
	}

}
