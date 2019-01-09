package org.semanticweb.ore.querying;

import org.semanticweb.ore.utilities.FilePathString;

public interface QueryResponseStoringHandler {
	
	public QueryResponse loadQueryResponseData(FilePathString filePathString);
	
	public boolean saveQueryResponseData(QueryResponse response);

}
