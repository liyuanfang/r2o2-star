package org.semanticweb.ore.querying;

import org.semanticweb.ore.utilities.FilePathString;

public interface QueryFactory {
	
	public ClassificationQuery createClassificationQuery(FilePathString querySource, FilePathString queryOntologySource, QueryExpressivity queryExpressivity);

	public ConsistencyQuery createConsistencyQuery(FilePathString querySource, FilePathString queryOntologySource, QueryExpressivity queryExpressivity);

	public SatisfiabilityQuery createSatisfiabilityQuery(FilePathString querySource, String classString, FilePathString queryOntologySource, QueryExpressivity queryExpressivity);

	public RealisationQuery createRealisationQuery(FilePathString querySource, FilePathString queryOntologySource, QueryExpressivity queryExpressivity);

	public EntailmentQuery createEntailmentQuery(FilePathString querySource, FilePathString entailmentAxiomOntologyString, FilePathString queryOntologySource, QueryExpressivity queryExpressivity);
	
}
