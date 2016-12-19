package ore.querying;

import ore.utilities.FilePathString;

public class ClassificationQuery extends Query {

	@Override
	public QueryType getQueryType() {
		return QueryType.QUERY_TYPE_CLASSIFICATION;
	}
	
	public ClassificationQuery(FilePathString querySourceString, FilePathString ontologySourceString, QueryExpressivity queryExpressivity) {
		super(querySourceString,ontologySourceString, queryExpressivity);
	}
	
	public String toString() {
		return "Classify-Ontology-'"+mOntologySourceString+"'-Query";
	}

}
