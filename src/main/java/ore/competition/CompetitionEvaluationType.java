package ore.competition;

public enum CompetitionEvaluationType {
	
	COMPETITION_EVALUTION_TYPE_TABLE_CSV("table-csv","csv"),
	COMPETITION_EVALUTION_TYPE_TABLE_TSV("table-tsv","tsv"),
	COMPETITION_EVALUTION_TYPE_CHART_PNG("chart-png","png"),
	COMPETITION_EVALUTION_TYPE_CHART_HTML("chart-html","html");
	
	private String mShortName = null;
	private String mFileEndName = null;
	
	private CompetitionEvaluationType(String shortName, String fileEndName) {
		mShortName = shortName;		
		mFileEndName = fileEndName;
	}
	
	public String getShortString() {
		return mShortName;
	}

	public String getFileEndString() {
		return mFileEndName;
	}
}
