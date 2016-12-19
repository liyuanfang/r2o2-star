package ore.competition;

import ore.competition.events.UpdateCompetitionEvaluationStatusEvent;
import ore.competition.events.UpdateCompetitionReasonerProgressStatusEvent;
import ore.competition.events.UpdateCompetitionStatusEvent;
import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.networking.DefaultMessageSerializer;
import ore.networking.messages.Message;
import ore.networking.messages.UpdateCompetitionEvaluationStatusMessage;
import ore.networking.messages.UpdateCompetitionReasonerProgressStatusMessage;
import ore.networking.messages.UpdateCompetitionStatusMessage;
import ore.threading.Event;
import ore.threading.EventThread;
import ore.utilities.FileSystemHandler;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;

public class CompetitionStatusUpdateStoringManager extends EventThread implements CompetitionStatusUpdater {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionStatusUpdateStoringManager.class);
	protected Config mConfig = null;
	
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
	protected HashMap<String,Competition> mNameCompetitionMap = new HashMap<String,Competition>();
	protected HashMap<String,String> mNameOutputMap = new HashMap<String,String>();
		
	
	protected HashMap<String,DateTime> mCompetitionFirstRunningDateMap = new HashMap<String,DateTime>();
	protected String mOutputString = null;
	
	
	protected DefaultMessageSerializer mMessageSerializer = new DefaultMessageSerializer();
		
		
	public CompetitionStatusUpdateStoringManager(Collection<Competition> competitionCollection, String outputString, CompetitionStatusUpdater statusUpdater, Config config) {
		mStatusUpdater = statusUpdater;
		mConfig = config;	
		
		String responseDirectory = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY);
		mOutputString = responseDirectory+"competition-evaluations"+ File.separator+outputString;
		FileSystemHandler.ensurePathToFileExists(mOutputString);
		
		for (Competition comp : competitionCollection) {
			mNameCompetitionMap.put(comp.getCompetitionSourceString().getAbsoluteFilePathString(),comp);
			String competitionOutputString = comp.getOutputString();
			if (competitionOutputString == null) {
				competitionOutputString = comp.getCompetitionName();
			}
			
			String outputDirectory = responseDirectory+"competition-evaluations"+ File.separator+competitionOutputString+ File.separator+"statusUpdateMessages.txt";
			FileSystemHandler.ensurePathToFileExists(outputDirectory);
			
			mNameOutputMap.put(comp.getCompetitionSourceString().getAbsoluteFilePathString(),outputDirectory);
		}
		
		startThread();
	}	
	
	
	protected void threadStart() {
		super.threadStart();		
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}	
	
	
	
	
	protected void writeData(OutputStreamWriter outputStreamWriter, Collection<String> stringCollection) throws IOException {
		if (outputStreamWriter != null ) {
			for (String dataString : stringCollection) {
				outputStreamWriter.write(dataString+"\n");
			}
			outputStreamWriter.write("\n");
			outputStreamWriter.flush();
		}
	}		
	

	protected void addMessageToFile(String fileString, Message message) {
		Collection<String> dataStrings = mMessageSerializer.serializeMessage(message);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(new File(fileString),true);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
			writeData(outputStreamWriter,dataStrings);
			outputStreamWriter.flush();
			outputStreamWriter.close();
		} catch (Exception e) {
			mLogger.error("Failed to write to file, got Exception '{}'.",e.getMessage());
		}
	}
	
	protected void storeCompetitionStatusUpdate(CompetitionStatus status) {
		UpdateCompetitionStatusMessage compStatusUpdateMessage = new UpdateCompetitionStatusMessage(status);
		String compOutputFileString = mNameOutputMap.get(status.getCompetitionSourceString());
		if (compOutputFileString != null) {
			addMessageToFile(compOutputFileString,compStatusUpdateMessage);
		}
		if (mOutputString != null) {
			addMessageToFile(mOutputString,compStatusUpdateMessage);
		}
	}

	protected void storeCompetitionEvaluationStatusUpdate(CompetitionEvaluationStatus status) {
		UpdateCompetitionEvaluationStatusMessage compStatusEvalUpdateMessage = new UpdateCompetitionEvaluationStatusMessage(status);
		String compOutputFileString = mNameOutputMap.get(status.getCompetitionSourceString());
		if (compOutputFileString != null) {
			addMessageToFile(compOutputFileString,compStatusEvalUpdateMessage);
		}
		if (mOutputString != null) {
			addMessageToFile(mOutputString,compStatusEvalUpdateMessage);
		}
	}

	protected void storeCompetitionReasonerProgressStatusUpdate(CompetitionReasonerProgressStatus status) {
		UpdateCompetitionReasonerProgressStatusMessage compReasProgStatUpdateMessage = new UpdateCompetitionReasonerProgressStatusMessage(status);
		String compOutputFileString = mNameOutputMap.get(status.getCompetitionSourceString());
		if (compOutputFileString != null) {
			addMessageToFile(compOutputFileString,compReasProgStatUpdateMessage);
		}
		if (mOutputString != null) {
			addMessageToFile(mOutputString,compReasProgStatUpdateMessage);
		}
	}
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof UpdateCompetitionStatusEvent) {
				UpdateCompetitionStatusEvent ucse = (UpdateCompetitionStatusEvent)e;
				CompetitionStatus status = ucse.getCompetitionStatus();
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionStatus(status);
				}
				storeCompetitionStatusUpdate(status);
				return true;
			} else if (e instanceof UpdateCompetitionReasonerProgressStatusEvent) {
				UpdateCompetitionReasonerProgressStatusEvent ucrpse = (UpdateCompetitionReasonerProgressStatusEvent)e;
				CompetitionReasonerProgressStatus status = ucrpse.getCompetitionReasonerProgressStatus();
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionReasonerProgressStatus(status);
				}
				storeCompetitionReasonerProgressStatusUpdate(status);
				return true;
			} else if (e instanceof UpdateCompetitionEvaluationStatusEvent) {
				UpdateCompetitionEvaluationStatusEvent ucese = (UpdateCompetitionEvaluationStatusEvent)e;
				CompetitionEvaluationStatus status = ucese.getCompetitionEvaluationStatus();
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(status);
				}
				storeCompetitionEvaluationStatusUpdate(status);
				return true;
			}
		}
		return false;
	}
	
	

	@Override
	public void updateCompetitionStatus(CompetitionStatus status) {
		postEvent(new UpdateCompetitionStatusEvent(status));
	}


	@Override
	public void updateCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status) {
		postEvent(new UpdateCompetitionReasonerProgressStatusEvent(status));
	}


	@Override
	public void updateCompetitionEvaluationStatus(CompetitionEvaluationStatus status) {
		postEvent(new UpdateCompetitionEvaluationStatusEvent(status));
	}


	
	
}
