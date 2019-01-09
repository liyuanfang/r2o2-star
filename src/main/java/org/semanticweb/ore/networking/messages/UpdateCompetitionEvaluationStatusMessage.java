package org.semanticweb.ore.networking.messages;

import java.util.HashMap;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.competition.CompetitionEvaluationType;

public class UpdateCompetitionEvaluationStatusMessage extends Message {
	

	
	public UpdateCompetitionEvaluationStatusMessage(HashMap<String,String> keyValueMap) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_EVALUATION_STATUS);
		mKeyValueMap.put("CompetitionName",keyValueMap.get("CompetitionName"));
		mKeyValueMap.put("CompetitionSourceString",keyValueMap.get("CompetitionSourceString"));
		mKeyValueMap.put("EvaluationName",keyValueMap.get("EvaluationName"));
		mKeyValueMap.put("EvaluationSourceString",keyValueMap.get("EvaluationSourceString"));
		mKeyValueMap.put("EvaluationType",keyValueMap.get("EvaluationType"));
		mKeyValueMap.put("TimeStamp",keyValueMap.get("TimeStamp"));
	}
	
	public UpdateCompetitionEvaluationStatusMessage(String competitionName, String competitionSourceString, String evaluationName, String evaluationSourceString, CompetitionEvaluationType evaluationType, DateTime timeStamp) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_EVALUATION_STATUS);
		mKeyValueMap.put("CompetitionName",competitionName);
		mKeyValueMap.put("CompetitionSourceString",competitionSourceString);
		mKeyValueMap.put("EvaluationName",evaluationName);
		mKeyValueMap.put("EvaluationSourceString",evaluationSourceString);
		mKeyValueMap.put("EvaluationType",evaluationType.toString());		
		mKeyValueMap.put("TimeStamp",timeStamp.toString());		
	}	
	

	public UpdateCompetitionEvaluationStatusMessage(CompetitionEvaluationStatus competitionEvaluationStatus) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_EVALUATION_STATUS);
		mKeyValueMap.put("CompetitionName",competitionEvaluationStatus.getCompetitionName());
		mKeyValueMap.put("CompetitionSourceString",competitionEvaluationStatus.getCompetitionSourceString());
		mKeyValueMap.put("EvaluationName",competitionEvaluationStatus.getEvaluationName());
		mKeyValueMap.put("EvaluationSourceString",competitionEvaluationStatus.getEvaluationSourceString());
		mKeyValueMap.put("EvaluationType",competitionEvaluationStatus.getEvaluationType().toString());
		mKeyValueMap.put("TimeStamp",competitionEvaluationStatus.getTimeStamp().toString());
		
	}
	
	
	
	public String getCompetitionName() {
		return mKeyValueMap.get("CompetitionName");
	}
	


	public DateTime getTimeStamp() {
		DateTime date = null;
		String dateString = mKeyValueMap.get("TimeStamp");
		if (dateString.compareTo("null") != 0) {
			try {
				date = ISODateTimeFormat.dateTimeParser().parseDateTime(dateString);
			} catch (Exception e) {				
			}
		}
		return date;
	}
	
	
	public String getCompetitionSourceString() {
		return mKeyValueMap.get("CompetitionSourceString");
	}
	
	public String getEvaluationName() {
		return mKeyValueMap.get("EvaluationName");
	}
	
	public String getEvaluationSourceString() {
		return mKeyValueMap.get("EvaluationSourceString");
	}
	
	
	public CompetitionEvaluationType getEvaluationType() {
		return CompetitionEvaluationType.valueOf(mKeyValueMap.get("EvaluationType"));
	}	
		
		
	
	public CompetitionEvaluationStatus createCompetitionEvaluationStatusFromMessage() {
		return new CompetitionEvaluationStatus(getCompetitionName(),getCompetitionSourceString(),getEvaluationName(),getEvaluationSourceString(),getEvaluationType(),getTimeStamp());
	}

}
