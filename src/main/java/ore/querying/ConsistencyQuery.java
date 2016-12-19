package ore.querying;

import ore.utilities.FilePathString;

public class ConsistencyQuery extends Query {

	@Override
	public QueryType getQueryType() {
		return QueryType.QUERY_TYPE_CONSISTENCY;
	}
	
	public ConsistencyQuery(FilePathString querySourceString, FilePathString ontologySourceString, QueryExpressivity queryExpressivity) {
		super(querySourceString,ontologySourceString, queryExpressivity);
	}
	
	public String toString() {
		return "Check-Ontology-'"+mOntologySourceString+"'-Consistency-Query";
	}

}
