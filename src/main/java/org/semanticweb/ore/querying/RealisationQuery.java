package org.semanticweb.ore.querying;

import org.semanticweb.ore.utilities.FilePathString;

public class RealisationQuery extends Query {

	@Override
	public QueryType getQueryType() {
		return QueryType.QUERY_TYPE_REALISATION;
	}
	
	public RealisationQuery(FilePathString querySourceString, FilePathString ontologySourceString, QueryExpressivity queryExpressivity) {
		super(querySourceString,ontologySourceString, queryExpressivity);
	}
	
	public String toString() {
		return "Realise-Ontology-'"+mOntologySourceString+"'-Query";
	}

}
