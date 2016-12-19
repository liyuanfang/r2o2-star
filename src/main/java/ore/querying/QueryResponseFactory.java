package ore.querying;

import ore.interfacing.ReasonerInterfaceType;
import ore.utilities.FilePathString;

public interface QueryResponseFactory {
	
	public QueryResponse createQueryResponse(FilePathString resultDataFileString, FilePathString reportFileString, FilePathString logFileString, FilePathString errorFileString, ReasonerInterfaceType usedInterface);

}
