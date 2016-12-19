package ore.rendering;

import ore.querying.Query;
import ore.querying.QueryExpressivity;
import ore.querying.QueryType;
import ore.querying.SatisfiabilityQuery;
import ore.utilities.FileSystemHandler;

import java.io.*;

public class QueryTSVRenderer extends TSVRenderer implements QueryRenderer {
	
	public OutputStream mOutputStream = null;
	public OutputStreamWriter mOutputStreamWriter = null;
	
	public boolean renderQuery(Query query) throws IOException {
		mOutputStreamWriter = new OutputStreamWriter(mOutputStream);
		boolean successfullyRendered = true;
		try {
			renderQueryType(query.getQueryType());
			writeTSVLine("OntologySource", query.getOntologySourceString(), mOutputStreamWriter);
			if (query.getQueryType() == QueryType.QUERY_TYPE_SATISFIABILITY) {
				SatisfiabilityQuery satQuery = (SatisfiabilityQuery)query;
				writeTSVLine("SatisfiabilityTestingClass", satQuery.getClassString(), mOutputStreamWriter);
			}
			renderQueryExpressivity(query.getQueryExpressivity());
			mOutputStreamWriter.close();
		} catch (IOException e) {
			successfullyRendered = false;
		}	
		mOutputStreamWriter.close();
		return successfullyRendered;
	}
	
	public void renderQueryExpressivity(QueryExpressivity queryExpressivity) throws IOException {
		writeTSVLine("UsingRules", queryExpressivity.isUsingRules(), mOutputStreamWriter);
		writeTSVLine("UsingDatatypes", queryExpressivity.isUsingDatatypes(), mOutputStreamWriter);
		writeTSVLine("InDLProfile", queryExpressivity.isInDLProfile(), mOutputStreamWriter);
		writeTSVLine("InELProfile", queryExpressivity.isInELProfile(), mOutputStreamWriter);
		writeTSVLine("InRLProfile", queryExpressivity.isInRLProfile(), mOutputStreamWriter);
		writeTSVLine("InQLProfile", queryExpressivity.isInQLProfile(), mOutputStreamWriter);
	}
	
	
	public void renderQueryType(QueryType queryType) throws IOException {
		String queryTypeString = "UNKNOWN";
		if (queryType == QueryType.QUERY_TYPE_CLASSIFICATION) {
			queryTypeString = "classification";
		} else if (queryType == QueryType.QUERY_TYPE_CONSISTENCY) {
			queryTypeString = "consistency";
		} else if (queryType == QueryType.QUERY_TYPE_ENTAILMENT) {
			queryTypeString = "entailment";
		} else if (queryType == QueryType.QUERY_TYPE_SATISFIABILITY) {
			queryTypeString = "satisfiability";
		} else if (queryType == QueryType.QUERY_TYPE_REALISATION) {
			queryTypeString = "realisation";
		}
		writeTSVLine("QueryType", queryTypeString, mOutputStreamWriter);
	}	
	
	public QueryTSVRenderer(OutputStream outputStream) {
		mOutputStream = outputStream;		
	}

	public QueryTSVRenderer(String outputFileString) throws FileNotFoundException {
		FileSystemHandler.ensurePathToFileExists(outputFileString);
		mOutputStream = new FileOutputStream(outputFileString);
	}

}
