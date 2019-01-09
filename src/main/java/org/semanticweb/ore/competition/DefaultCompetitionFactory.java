package org.semanticweb.ore.competition;

import java.util.List;

import org.semanticweb.ore.utilities.FilePathString;

public class DefaultCompetitionFactory implements CompetitionFactory {

	@Override
	public Competition createCompetition(String competitionName, FilePathString competitionSourceString, List<String> reasonerList) {
		return new Competition(competitionName,competitionSourceString,reasonerList);
	}
	
	public DefaultCompetitionFactory() {
	}

}
