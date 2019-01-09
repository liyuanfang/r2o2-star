package org.semanticweb.ore.querying;

import org.semanticweb.ore.utilities.FilePathString;

public class SatisfiabilityQuery extends Query {
	
	private String mClassString;

	@Override
	public QueryType getQueryType() {
		return QueryType.QUERY_TYPE_SATISFIABILITY;
	}
	
	public SatisfiabilityQuery(FilePathString querySourceString, String classString, FilePathString ontologySourceString, QueryExpressivity queryExpressivity) {
		super(querySourceString,ontologySourceString, queryExpressivity);
		mClassString = classString;
	}
	
	public String getClassString() {
		return mClassString;
	}
	
	public String toString() {
		return "Test-'"+mClassString+"'-Satisfiability-For-Ontology-'"+mOntologySourceString+"'-Query";
	}

}
