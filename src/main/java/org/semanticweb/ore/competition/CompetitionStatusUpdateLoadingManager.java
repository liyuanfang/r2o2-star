package org.semanticweb.ore.competition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.networking.DefaultMessageParsingFactory;
import org.semanticweb.ore.networking.messages.Message;
import org.semanticweb.ore.networking.messages.MessageType;
import org.semanticweb.ore.networking.messages.UpdateCompetitionEvaluationStatusMessage;
import org.semanticweb.ore.networking.messages.UpdateCompetitionReasonerProgressStatusMessage;
import org.semanticweb.ore.networking.messages.UpdateCompetitionStatusMessage;
import org.semanticweb.ore.threading.Thread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionStatusUpdateLoadingManager extends Thread {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionStatusUpdateLoadingManager.class);
	protected Config mConfig = null;
	
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	protected HashMap<String,Competition> mNameCompetitionMap = null;

	
	
	
	protected HashMap<String,CompetitionEvaluationStatus> mUpdateEvalStatMap = new HashMap<String,CompetitionEvaluationStatus>();
	protected HashMap<String,CompetitionReasonerProgressStatus> mUpdateReasonerStatMap = new HashMap<String,CompetitionReasonerProgressStatus>();
	protected HashMap<String,CompetitionStatus> mUpdateStatMap = new HashMap<String,CompetitionStatus>();
	
	
	protected HashMap<String,DateTime> mCompetitionFirstRunningDateMap = new HashMap<String,DateTime>();
	protected String mInputString = null;
	
	
	protected DefaultMessageParsingFactory mMessageParser = new DefaultMessageParsingFactory();
	
	
	protected InputStreamReader mInputStreamReader = null;
	protected BufferedReader mBufferedReader = null;
	protected boolean mReadingError = false;
	
	protected DateTime mStartDateTime = null;
	protected DateTime mEndDateTime = null;
	protected boolean mSendingStarted = false;
	protected DateTime mSendingStartDateTime = null;
	protected long mSendingDelay = 0;
	
	
	protected boolean mUpdateDateTimeStamps = true;
		
		
	public CompetitionStatusUpdateLoadingManager(DateTime startDateTime, String inputString, CompetitionStatusUpdater statusUpdater, Config config) {
		mStatusUpdater = statusUpdater;
		mConfig = config;	
		
		mInputString = inputString;
		
		mStartDateTime = startDateTime;
				
		startThread();
	}	
	
	
	protected void threadStart() {
		super.threadStart();
	}	

	protected void threadFinish() {
		super.threadFinish();
	}	
	
	
	protected boolean processCompetitionStatus(CompetitionStatus compStatus) {
		DateTime dateTimeStamp = compStatus.getTimeStamp();
		boolean sendStatus = true;
		if (mStartDateTime != null) {
			if (dateTimeStamp.isBefore(mStartDateTime)) {
				sendStatus = false;
			}
		}
		boolean continueStatusProcessing = true;
		if (mEndDateTime != null) {
			if (dateTimeStamp.isAfter(mEndDateTime)) {
				continueStatusProcessing = false;
			}
		}
		if (sendStatus && !mSendingStarted) {		
			initUpdateStatusSending(dateTimeStamp);
		}
		compStatus.setTimeStamp(delayStatusUpdateSending(dateTimeStamp));		
		mUpdateStatMap.put(compStatus.getCompetitionSourceString(), compStatus);
		if (sendStatus) {
			mStatusUpdater.updateCompetitionStatus(compStatus);
		}
		return continueStatusProcessing;
	}
	
	
	

	
	protected boolean processCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus compStatus) {
		DateTime dateTimeStamp = compStatus.getTimeStamp();
		boolean sendStatus = true;
		if (mStartDateTime != null) {
			if (dateTimeStamp.isBefore(mStartDateTime)) {
				sendStatus = false;
			}
		}
		boolean continueStatusProcessing = true;
		if (mEndDateTime != null) {
			if (dateTimeStamp.isAfter(mEndDateTime)) {
				continueStatusProcessing = false;
			}
		}
		if (sendStatus && !mSendingStarted) {		
			initUpdateStatusSending(dateTimeStamp);
		}
		compStatus.setTimeStamp(delayStatusUpdateSending(dateTimeStamp));		
		mUpdateReasonerStatMap.put(compStatus.getCompetitionSourceString()+compStatus.getReasonerSourceString(), compStatus);
		if (sendStatus) {
			mStatusUpdater.updateCompetitionReasonerProgressStatus(compStatus);
		}
		return continueStatusProcessing;
	}
	
	
	


	
	protected boolean processCompetitionEvaluationStatus(CompetitionEvaluationStatus compStatus) {
		DateTime dateTimeStamp = compStatus.getTimeStamp();
		boolean sendStatus = true;
		if (mStartDateTime != null) {
			if (dateTimeStamp.isBefore(mStartDateTime)) {
				sendStatus = false;
			}
		}
		boolean continueStatusProcessing = true;
		if (mEndDateTime != null) {
			if (dateTimeStamp.isAfter(mEndDateTime)) {
				continueStatusProcessing = false;
			}
		}
		if (sendStatus && !mSendingStarted) {		
			initUpdateStatusSending(dateTimeStamp);
		}		
		compStatus.setTimeStamp(delayStatusUpdateSending(dateTimeStamp));		
		mUpdateEvalStatMap.put(compStatus.getCompetitionSourceString()+compStatus.getEvaluationSourceString(), compStatus);
		if (sendStatus) {
			mStatusUpdater.updateCompetitionEvaluationStatus(compStatus);
		}
		return continueStatusProcessing;
	}
	
	
	
	
	protected DateTime delayStatusUpdateSending(DateTime dataTime) {
		DateTime currentDateTime = new DateTime(DateTimeZone.UTC);
		
		if (mSendingStartDateTime != null) {
			long timeDiff = dataTime.getMillis()-currentDateTime.getMillis()+mSendingDelay;
			if (timeDiff > 0) {
				mLogger.info("Delaying sending of next status update message for {} ms.",timeDiff);
				try {
					java.lang.Thread.sleep(timeDiff);
				} catch (InterruptedException e) {				
				}
			}
		}
		if (mUpdateDateTimeStamps) {		
			currentDateTime = new DateTime(DateTimeZone.UTC);
		}
		return currentDateTime;
	}

	
	
	protected void initUpdateStatusSending(DateTime dateTime) {	
		if (!mSendingStarted) {
			mSendingStarted = true;
			mSendingStartDateTime = new DateTime(DateTimeZone.UTC);
			mSendingDelay = mSendingStartDateTime.getMillis()-dateTime.getMillis();
			if (mStatusUpdater != null) {
				for (CompetitionStatus compStatus : mUpdateStatMap.values()) {
					mStatusUpdater.updateCompetitionStatus(compStatus);
				}
				for (CompetitionReasonerProgressStatus compReasStatus : mUpdateReasonerStatMap.values()) {
					mStatusUpdater.updateCompetitionReasonerProgressStatus(compReasStatus);
				}
				for (CompetitionEvaluationStatus compEvalStatus : mUpdateEvalStatMap.values()) {
					mStatusUpdater.updateCompetitionEvaluationStatus(compEvalStatus);
				}
			}
		}
	}

	
	protected void threadRun() {
		initStatusReading(mInputString);
		
		try {
			boolean continueProcessing = true;
			mLogger.error("Starting reading and streaming status updates from {}",mInputString);
			
			while (continueProcessing) {
				Collection<String> statusDataStrings = readNextStatusData();
				if (statusDataStrings == null) {
					continueProcessing = false;
				} else if (statusDataStrings.size() > 0) {
					Message message = mMessageParser.createParsedMessage(statusDataStrings);
					if (message.getMessageType() == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_STATUS) {
						UpdateCompetitionStatusMessage compUpdMessage = (UpdateCompetitionStatusMessage)message;
						CompetitionStatus compStatus = compUpdMessage.createCompetitionStatusFromMessage();
						continueProcessing = processCompetitionStatus(compStatus);
					} else if (message.getMessageType() == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_EVALUATION_STATUS) {
						UpdateCompetitionEvaluationStatusMessage compEvalUpdMessage = (UpdateCompetitionEvaluationStatusMessage)message;
						CompetitionEvaluationStatus compEvalStatus = compEvalUpdMessage.createCompetitionEvaluationStatusFromMessage();
						continueProcessing = processCompetitionEvaluationStatus(compEvalStatus);
					} else if (message.getMessageType() == MessageType.MESSAGE_TYPE_UPDATE_COMPETITION_REASONER_PROGRESS_STATUS) {
						UpdateCompetitionReasonerProgressStatusMessage compReasProgUpdMessage = (UpdateCompetitionReasonerProgressStatusMessage)message;
						CompetitionReasonerProgressStatus compReasProgStatus = compReasProgUpdMessage.createCompetitionReasonerProgressStatusFromMessage();
						continueProcessing = processCompetitionReasonerProgressStatus(compReasProgStatus);
					}
				}
			}
			mLogger.error("Finished reading and streaming status updates");
		} catch (Exception e) {	
			mLogger.error("Failed to read status updates, got Exception {}",e.getMessage());
			mReadingError = true;
		}
		
		closeStatusReading();		
	}



	protected void initStatusReading(String file) {
		try {
			mInputStreamReader = new InputStreamReader(new FileInputStream(new File(mInputString))); 
			mBufferedReader = new BufferedReader(mInputStreamReader);
		} catch (IOException e) {
			mLogger.error("Cannot read from file {}, got IOException {}",file,e.getMessage());
			mReadingError = true;
		}
	}
	
	protected void closeStatusReading() {
		try {
			if (mBufferedReader != null) {
				mBufferedReader.close();				
			}
			if (mInputStreamReader != null) {
				mInputStreamReader.close();
			}
		} catch (IOException e) {
			mReadingError = true;
		}
	}
	

	protected Collection<String> readNextStatusData() throws IOException {
		ArrayList<String> stringList = new ArrayList<String>();		
		String line = null;
		while ((line = mBufferedReader.readLine()) != null) {	
			if (line.isEmpty()) {
				break;
			} else {
				stringList.add(line);
			}
		}
		if (line == null) { 
			return null;
		} else {
			return stringList;
		}
	}
	
	
	
	
}
