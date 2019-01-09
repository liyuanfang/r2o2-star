package org.semanticweb.ore.evaluation;

import org.semanticweb.ore.competition.Competition;

public interface StoredQueryResultEvaluator {
	
	public void evaluateCompetitionResults(QueryResultStorage resultStorage, Competition competition);

}
