package ore.networking.events;

import ore.networking.ExecutionReport;
import ore.networking.ExecutionTaskHandler;
import ore.threading.Event;

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
