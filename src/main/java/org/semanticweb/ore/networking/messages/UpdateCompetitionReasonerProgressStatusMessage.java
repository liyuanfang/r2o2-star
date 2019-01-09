package org.semanticweb.ore.networking.messages;

import java.util.HashMap;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.semanticweb.ore.competition.CompetitionReasonerProgressStatus;

public class UpdateCompetitionReasonerProgressStatusMessage extends Message {
	

	
	public UpdateCompetitionReasonerProgressStatusMessage(HashMap<String,String> keyValueMap) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_REASONER_PROGRESS_STATUS);
		mKeyValueMap.put("CompetitionName",keyValueMap.get("CompetitionName"));
		mKeyValueMap.put("CompetitionSourceString",keyValueMap.get("CompetitionSourceString"));
		mKeyValueMap.put("ReasonerName",keyValueMap.get("ReasonerName"));
		mKeyValueMap.put("ReasonerSourceString",keyValueMap.get("ReasonerSourceString"));
		mKeyValueMap.put("CorrectlyProccessedCount",keyValueMap.get("CorrectlyProccessedCount"));
		mKeyValueMap.put("TotalProccessedCount",keyValueMap.get("TotalProccessedCount"));
		mKeyValueMap.put("CorrectlyProccessedTime",keyValueMap.get("CorrectlyProccessedTime"));
		mKeyValueMap.put("TotalExecutionTime",keyValueMap.get("TotalExecutionTime"));
		mKeyValueMap.put("ReasonerRank",keyValueMap.get("ReasonerRank"));
		mKeyValueMap.put("ReasonerPosition",keyValueMap.get("ReasonerPosition"));
		mKeyValueMap.put("OutOfTimeCount",keyValueMap.get("OutOfTimeCount"));
		mKeyValueMap.put("TimeStamp",keyValueMap.get("TimeStamp"));
	}
	
	public UpdateCompetitionReasonerProgressStatusMessage(String competitionName, String competitionSourceString, String reasonerName, String reasonerSourceString, int rank, int position, int correctlyProccessedCount, int totalProccessedCount, int outOfTimeCount, long correctlyProccessedTime, long totalExecutionTime, DateTime timeStamp) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_REASONER_PROGRESS_STATUS);
		mKeyValueMap.put("CompetitionName",competitionName);
		mKeyValueMap.put("CompetitionSourceString",competitionSourceString);
		mKeyValueMap.put("ReasonerName",reasonerName);
		mKeyValueMap.put("ReasonerSourceString",reasonerSourceString);
		mKeyValueMap.put("CorrectlyProccessedCount",String.valueOf(correctlyProccessedCount));
		mKeyValueMap.put("TotalProccessedCount",String.valueOf(totalProccessedCount));
		mKeyValueMap.put("CorrectlyProccessedTime",String.valueOf(correctlyProccessedTime));
		mKeyValueMap.put("TotalExecutionTime",String.valueOf(totalExecutionTime));
		
		mKeyValueMap.put("ReasonerRank",String.valueOf(rank));
		mKeyValueMap.put("ReasonerPosition",String.valueOf(position));
		mKeyValueMap.put("OutOfTimeCount",String.valueOf(outOfTimeCount));
		mKeyValueMap.put("TimeStamp",timeStamp.toString());		
		
	}
	
	

	public UpdateCompetitionReasonerProgressStatusMessage(CompetitionReasonerProgressStatus competitionReasonerProgressStatus) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_REASONER_PROGRESS_STATUS);
		mKeyValueMap.put("CompetitionName",competitionReasonerProgressStatus.getCompetitionName());
		mKeyValueMap.put("CompetitionSourceString",competitionReasonerProgressStatus.getCompetitionSourceString());
		mKeyValueMap.put("ReasonerName",competitionReasonerProgressStatus.getReasonerName());
		mKeyValueMap.put("ReasonerSourceString",competitionReasonerProgressStatus.getReasonerSourceString());
		mKeyValueMap.put("CorrectlyProccessedCount",String.valueOf(competitionReasonerProgressStatus.getCorrectlyProcessedCount()));
		mKeyValueMap.put("TotalProccessedCount",String.valueOf(competitionReasonerProgressStatus.getTotalProcessedCount()));
		mKeyValueMap.put("CorrectlyProccessedTime",String.valueOf(competitionReasonerProgressStatus.getCorrectlyProccessedTime()));
		mKeyValueMap.put("TotalExecutionTime",String.valueOf(competitionReasonerProgressStatus.getTotalExecutionTime()));
		
		mKeyValueMap.put("ReasonerRank",String.valueOf(competitionReasonerProgressStatus.getReasonerRank()));
		mKeyValueMap.put("ReasonerPosition",String.valueOf(competitionReasonerProgressStatus.getReasonerPosition()));
		mKeyValueMap.put("OutOfTimeCount",String.valueOf(competitionReasonerProgressStatus.getOutOfTimeCount()));
		mKeyValueMap.put("TimeStamp",competitionReasonerProgressStatus.getTimeStamp().toString());
		
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
	
	public String getCompetitionName() {
		return mKeyValueMap.get("CompetitionName");
	}
	
	public String getCompetitionSourceString() {
		return mKeyValueMap.get("CompetitionSourceString");
	}
	
	public String getReasonerName() {
		return mKeyValueMap.get("ReasonerName");
	}
	
	public String getReasonerSourceString() {
		return mKeyValueMap.get("ReasonerSourceString");
	}
	
	
	public int getCorrectlyProccessedCount() {
		return Integer.valueOf(mKeyValueMap.get("CorrectlyProccessedCount"));
	}	
	
	public int getTotalProccessedCount() {
		return Integer.valueOf(mKeyValueMap.get("TotalProccessedCount"));
	}		
	
	
	public int getReasonerRank() {
		return Integer.valueOf(mKeyValueMap.get("ReasonerRank"));
	}	
	
	public int getReasonerPosition() {
		return Integer.valueOf(mKeyValueMap.get("ReasonerPosition"));
	}		
	
	public long getCorrectlyProccessedTime() {
		return Long.valueOf(mKeyValueMap.get("CorrectlyProccessedTime"));
	}	
	
	public long getTotalExecutionTime() {
		return Long.valueOf(mKeyValueMap.get("TotalExecutionTime"));
	}	

	public int getOutOfTimeCount() {
		return Integer.valueOf(mKeyValueMap.get("OutOfTimeCount"));
	}		
		
	
	public CompetitionReasonerProgressStatus createCompetitionReasonerProgressStatusFromMessage() {
		return new CompetitionReasonerProgressStatus(getCompetitionName(),getCompetitionSourceString(),getReasonerName(),getReasonerSourceString(),getReasonerRank(),getReasonerPosition(),getCorrectlyProccessedCount(),getTotalProccessedCount(),getOutOfTimeCount(),getCorrectlyProccessedTime(),getTotalExecutionTime(),getTimeStamp());
	}

}
