package org.semanticweb.ore.querying;

import org.semanticweb.ore.utilities.FilePathString;

public class DefaultQueryFactory implements QueryFactory {

	@Override
	public ClassificationQuery createClassificationQuery(FilePathString querySource, FilePathString queryOntologySource, QueryExpressivity queryExpressivity) {
		return new ClassificationQuery(querySource, queryOntologySource, queryExpressivity);
	}

	@Override
	public ConsistencyQuery createConsistencyQuery(FilePathString querySource, FilePathString queryOntologySource, QueryExpressivity queryExpressivity) {
		return new ConsistencyQuery(querySource, queryOntologySource, queryExpressivity);
	}

	@Override
	public RealisationQuery createRealisationQuery(FilePathString querySource, FilePathString queryOntologySource, QueryExpressivity queryExpressivity) {
		return new RealisationQuery(querySource, queryOntologySource, queryExpressivity);
	}

	@Override
	public SatisfiabilityQuery createSatisfiabilityQuery(FilePathString querySource, String classString, FilePathString queryOntologySource, QueryExpressivity queryExpressivity) {
		return new SatisfiabilityQuery(querySource, classString, queryOntologySource, queryExpressivity);
	}	

	@Override
	public EntailmentQuery createEntailmentQuery(FilePathString querySource, FilePathString entailmentAxiomOntologyString, FilePathString queryOntologySource, QueryExpressivity queryExpressivity) {
		return new EntailmentQuery(querySource, entailmentAxiomOntologyString, queryOntologySource, queryExpressivity);
	}	
	
	public DefaultQueryFactory() {
	}

}
