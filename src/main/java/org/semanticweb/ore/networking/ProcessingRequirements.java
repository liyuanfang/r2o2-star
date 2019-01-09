package org.semanticweb.ore.networking;

import org.joda.time.DateTime;

public interface ProcessingRequirements {
	
	public boolean canInitProcessing(int executionTaskHandlerCount);
	
	public boolean canAddExecutionTaskHandler(int totalCount);
	public boolean canProvideMoreExecutionTask();
	

	public DateTime getDesiredStartingDate();
	public DateTime getDesiredEndingDate();
	public boolean canOnlyRunWithinDates();
	
}
