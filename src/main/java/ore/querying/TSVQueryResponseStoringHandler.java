package ore.querying;

import ore.configuration.Config;
import ore.parsing.QueryResponseTSVParser;
import ore.rendering.QueryResponseTSVRenderer;
import ore.utilities.FilePathString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TSVQueryResponseStoringHandler implements QueryResponseStoringHandler {
	
	final private static Logger mLogger = LoggerFactory.getLogger(TSVQueryResponseStoringHandler.class);

	private Config mConfig = null;
	
	public QueryResponse loadQueryResponseData(FilePathString filePathString) {
		DefaultQueryResponseFactory factory = new DefaultQueryResponseFactory();
		QueryResponseTSVParser parser = new QueryResponseTSVParser(factory, mConfig, filePathString);
		QueryResponse queryResponse = null;
		try {
			queryResponse = parser.parseQueryResponse(filePathString.getAbsoluteFilePathString());
		} catch (IOException e) {
			mLogger.error("Loading of query response from '{}' failed, got IOException '{}'.",filePathString,e.getMessage());
		}
		return queryResponse;
	}
	
	public boolean saveQueryResponseData(QueryResponse response) {
		boolean savedQueryResponse = false;
		QueryResponseTSVRenderer renderer = null;
		try {
			renderer = new QueryResponseTSVRenderer(response.getReportFilePathString().getAbsoluteFilePathString());
			renderer.renderQueryResponse(response);
			savedQueryResponse = true;
		} catch (IOException e) {
			mLogger.error("Saving of query response to '{}' failed, got IOException '{}'.",response.getReportFilePathString(),e.getMessage());
		}
		
		return savedQueryResponse;
	}
	
	
	public TSVQueryResponseStoringHandler(Config config) {
		mConfig = config;
	}

}
