package ore.querying;

import ore.utilities.FilePathString;

public interface QueryResponseStoringHandler {
	
	public QueryResponse loadQueryResponseData(FilePathString filePathString);
	
	public boolean saveQueryResponseData(QueryResponse response);

}
