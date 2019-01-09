package org.semanticweb.ore.competition;

public interface CompetitionStatusUpdater {
		
	public void updateCompetitionStatus(CompetitionStatus status);
	public void updateCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status);
	public void updateCompetitionEvaluationStatus(CompetitionEvaluationStatus status);
	
}
