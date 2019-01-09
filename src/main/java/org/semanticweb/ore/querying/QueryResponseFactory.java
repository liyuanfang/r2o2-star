package org.semanticweb.ore.querying;

import org.semanticweb.ore.interfacing.ReasonerInterfaceType;
import org.semanticweb.ore.utilities.FilePathString;

public interface QueryResponseFactory {
	
	public QueryResponse createQueryResponse(FilePathString resultDataFileString, FilePathString reportFileString, FilePathString logFileString, FilePathString errorFileString, ReasonerInterfaceType usedInterface);

}
