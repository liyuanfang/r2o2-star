package org.semanticweb.ore.competition;

import org.joda.time.DateTime;


public class CompetitionEvaluationStatus {
	
	protected DateTime mTimeStamp = null;
	protected String mCompetitionName = null;
	protected String mCompetitionSourceString = null;
	protected String mEvaluationName = null;
	protected String mEvaluationSourceString = null;
	protected CompetitionEvaluationType mEvaluationType = null;

	

	public CompetitionEvaluationStatus(CompetitionEvaluationStatus status) {
		mCompetitionName = status.mCompetitionName;
		mCompetitionSourceString = status.mCompetitionSourceString;
		mEvaluationName = status.mEvaluationName;
		mEvaluationSourceString = status.mEvaluationSourceString;
		mEvaluationType = status.mEvaluationType;
		mTimeStamp = status.mTimeStamp;
	}	
	
	public CompetitionEvaluationStatus(String competitionName, String competitionSourceString, String evaluationName, String evaluationSourceString, CompetitionEvaluationType evaluationType, DateTime timeStamp) {
		mCompetitionName = competitionName;
		mCompetitionSourceString = competitionSourceString;
		mEvaluationName = evaluationName;
		mEvaluationSourceString = evaluationSourceString;
		mEvaluationType = evaluationType;
		mTimeStamp = timeStamp;
	}
	
	
	public CompetitionEvaluationStatus(Competition competition, String evaluationName, String evaluationSourceString, CompetitionEvaluationType evaluationType, DateTime timeStamp) {
		mCompetitionName = competition.getCompetitionName();
		mCompetitionSourceString = competition.getCompetitionSourceString().getAbsoluteFilePathString();
		mEvaluationName = evaluationName;
		mEvaluationSourceString = evaluationSourceString;
		mEvaluationType = evaluationType;
		mTimeStamp = timeStamp;
	}	
	
	public DateTime getTimeStamp() {
		return mTimeStamp;
	}
	

	public void setTimeStamp(DateTime dateTimeStamp) {
		mTimeStamp = dateTimeStamp;
	}
	
	public String getCompetitionName() {
		return mCompetitionName;
	}	
	
	public String getCompetitionSourceString() {
		return mCompetitionSourceString;
	}	
	
	public String getEvaluationName() {
		return mEvaluationName;
	}	
	
	public String getEvaluationSourceString() {
		return mEvaluationSourceString;
	}	

	public void setEvaluationSourceString(String evalSourceString) {
		mEvaluationSourceString = evalSourceString;
	}

	public CompetitionEvaluationType getEvaluationType() {
		return mEvaluationType;
	}	
}
