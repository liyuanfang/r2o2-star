package org.semanticweb.ore.competition;

import java.util.List;

import org.semanticweb.ore.utilities.FilePathString;

public interface CompetitionFactory {
	
	public Competition createCompetition(String competitionName, FilePathString competitionSourceString, List<String> reasonerList);
	
}
