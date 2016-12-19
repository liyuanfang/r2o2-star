package ore.networking.messages;

import ore.competition.CompetitionExecutionState;
import ore.competition.CompetitionStatus;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;
import java.util.HashMap;

public class UpdateCompetitionStatusMessage extends Message {
	

	
	public UpdateCompetitionStatusMessage(HashMap<String,String> keyValueMap) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_STATUS);
		mKeyValueMap.put("CompetitionName",keyValueMap.get("CompetitionName"));
		mKeyValueMap.put("HandlerCount",keyValueMap.get("HandlerCount"));
		mKeyValueMap.put("ReasonerCount",keyValueMap.get("ReasonerCount"));
		mKeyValueMap.put("QueryCount",keyValueMap.get("QueryCount"));
		mKeyValueMap.put("CompetitionSourceString",keyValueMap.get("CompetitionSourceString"));
		mKeyValueMap.put("CompetitionExecutionState",keyValueMap.get("CompetitionExecutionState"));
		mKeyValueMap.put("StartingDate",keyValueMap.get("StartingDate"));
		mKeyValueMap.put("EndingDate",keyValueMap.get("EndingDate"));
		mKeyValueMap.put("TimeStamp",keyValueMap.get("TimeStamp"));
		mKeyValueMap.put("ExecutionTime",keyValueMap.get("ExecutionTime"));
	}
	
	public UpdateCompetitionStatusMessage(String competitionName, String competitionSourceString, CompetitionExecutionState state, Date startingDate, Date endingDate, int handlerCount, int reasonerCount, int queryCount, DateTime timeStamp, long executionTime) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_STATUS);
		mKeyValueMap.put("CompetitionName",competitionName);
		mKeyValueMap.put("HandlerCount", String.valueOf(handlerCount));
		mKeyValueMap.put("ReasonerCount", String.valueOf(reasonerCount));
		mKeyValueMap.put("QueryCount", String.valueOf(queryCount));
		mKeyValueMap.put("CompetitionSourceString",competitionSourceString);
		mKeyValueMap.put("CompetitionExecutionState",state.toString());
		if (startingDate != null) {
			mKeyValueMap.put("StartingDate",startingDate.toString());
		} else {
			mKeyValueMap.put("StartingDate","null");
		}
		if (endingDate != null) {
			mKeyValueMap.put("EndingDate",endingDate.toString());
		} else {
			mKeyValueMap.put("EndingDate","null");
		}
		mKeyValueMap.put("TimeStamp",timeStamp.toString());		
		mKeyValueMap.put("ExecutionTime", String.valueOf(executionTime));
	}
	
	public UpdateCompetitionStatusMessage(CompetitionStatus status) {
		super(MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_STATUS);
		mKeyValueMap.put("CompetitionName",status.getCompetitionName());
		mKeyValueMap.put("HandlerCount", String.valueOf(status.getExecutionHandlerCount()));
		mKeyValueMap.put("ReasonerCount", String.valueOf(status.getReasonerCount()));
		mKeyValueMap.put("QueryCount", String.valueOf(status.getQueryCount()));
		mKeyValueMap.put("CompetitionSourceString",status.getCompetitionSourceString());
		mKeyValueMap.put("CompetitionExecutionState",status.getExecutionState().toString());
		DateTime startingDate = status.getStartingDate();
		DateTime endingDate = status.getEndingDate();
		if (startingDate != null) {
			mKeyValueMap.put("StartingDate",startingDate.toString());
		} else {
			mKeyValueMap.put("StartingDate","null");
		}
		if (endingDate != null) {
			mKeyValueMap.put("EndingDate",endingDate.toString());
		} else {
			mKeyValueMap.put("EndingDate","null");
		}
		mKeyValueMap.put("TimeStamp",status.getTimeStamp().toString());
		mKeyValueMap.put("ExecutionTime", String.valueOf(status.getExecutionTime()));
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

	public DateTime getStartingDate() {
		DateTime date = null;
		String dateString = mKeyValueMap.get("StartingDate");
		if (dateString.compareTo("null") != 0) {
			try {
				date = ISODateTimeFormat.dateTimeParser().parseDateTime(dateString);
			} catch (Exception e) {
			}
		}
		return date;
	}
	

	public DateTime getEndingDate() {
		DateTime date = null;
		String dateString = mKeyValueMap.get("EndingDate");
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

	public int getExecutionHandlerCount() {
		return Integer.valueOf(mKeyValueMap.get("HandlerCount"));
	}	
	

	public int getReasonerCount() {
		return Integer.valueOf(mKeyValueMap.get("ReasonerCount"));
	}	
	

	public int getQueryCount() {
		return Integer.valueOf(mKeyValueMap.get("QueryCount"));
	}		
	

	public long getExecutionTime() {
		return Long.valueOf(mKeyValueMap.get("ExecutionTime"));
	}		

	public CompetitionExecutionState getCompetitionExecutionState() {
		return CompetitionExecutionState.valueOf(mKeyValueMap.get("CompetitionExecutionState"));
	}	
	
	public CompetitionStatus createCompetitionStatusFromMessage() {
		return new CompetitionStatus(getCompetitionName(),getCompetitionSourceString(),getCompetitionExecutionState(),getStartingDate(),getEndingDate(),getExecutionHandlerCount(),getReasonerCount(),getQueryCount(),getTimeStamp(),getExecutionTime());
	}
	

}
