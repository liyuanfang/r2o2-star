package org.semanticweb.ore.querying;

import org.semanticweb.ore.interfacing.ReasonerInterfaceType;
import org.semanticweb.ore.utilities.FilePathString;

public class DefaultQueryResponseFactory implements QueryResponseFactory {

	@Override
	public QueryResponse createQueryResponse(FilePathString resultDataFileString, FilePathString reportFileString, FilePathString logFileString, FilePathString errorFileString, ReasonerInterfaceType usedInterface) {
		return new QueryResponse(resultDataFileString,reportFileString,logFileString,errorFileString,usedInterface);
	}
	
	
	public DefaultQueryResponseFactory() {		
	}

}
