package ore.evaluation;

import ore.competition.Competition;

public interface StoredQueryResultEvaluator {
	
	public void evaluateCompetitionResults(QueryResultStorage resultStorage, Competition competition);

}
