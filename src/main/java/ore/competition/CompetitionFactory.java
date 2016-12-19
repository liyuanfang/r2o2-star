package ore.competition;

import ore.utilities.FilePathString;

import java.util.List;

public interface CompetitionFactory {
	
	public Competition createCompetition(String competitionName, FilePathString competitionSourceString, List<String> reasonerList);
	
}
