package ore.competition;

import ore.utilities.FilePathString;

import java.util.List;

public class DefaultCompetitionFactory implements CompetitionFactory {

	@Override
	public Competition createCompetition(String competitionName, FilePathString competitionSourceString, List<String> reasonerList) {
		return new Competition(competitionName,competitionSourceString,reasonerList);
	}
	
	public DefaultCompetitionFactory() {
	}

}
