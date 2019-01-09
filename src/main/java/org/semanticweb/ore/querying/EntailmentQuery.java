package org.semanticweb.ore.querying;

import org.semanticweb.ore.utilities.FilePathString;

public class EntailmentQuery extends Query {
	
	private FilePathString mEntailmentAxiomOntologyString;

	@Override
	public QueryType getQueryType() {
		return QueryType.QUERY_TYPE_ENTAILMENT;
	}
	
	public EntailmentQuery(FilePathString querySourceString, FilePathString entailmentAxiomOntologyString, FilePathString ontologySourceString, QueryExpressivity queryExpressivity) {
		super(querySourceString,ontologySourceString, queryExpressivity);
		mEntailmentAxiomOntologyString = entailmentAxiomOntologyString;
	}
	
	public FilePathString getEntailmentAxiomOntologySourceString() {
		return mEntailmentAxiomOntologyString;
	}
	
	public String toString() {
		return "Test-'"+mEntailmentAxiomOntologyString+"'-Axioms-Entailed-In-Ontology-'"+mOntologySourceString+"'-Query";
	}

}
