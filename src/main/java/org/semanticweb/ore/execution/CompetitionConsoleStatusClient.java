package org.semanticweb.ore.execution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Vector;

import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.competition.CompetitionExecutionState;
import org.semanticweb.ore.competition.CompetitionReasonerProgressStatus;
import org.semanticweb.ore.competition.CompetitionStatus;
import org.semanticweb.ore.competition.CompetitionStatusUpdater;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigExtensionFactory;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.configuration.InitialConfigBaseFactory;
import org.semanticweb.ore.networking.StatusClientUpdateManager;
import org.semanticweb.ore.parsing.ConfigTSVParser;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionConsoleStatusClient implements CompetitionStatusUpdater {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionConsoleStatusClient.class);
	
	
	protected HashMap<String,CompetitionStatus> mLastCompetitionStatusMap = new HashMap<String,CompetitionStatus>();
	protected HashMap<String,CompetitionReasonerProgressStatus> mLastCompetitionReasonerStatusMap = new HashMap<String,CompetitionReasonerProgressStatus>();
	
	

	public static void main(String[] args) {
		
		mLogger.info("Starting competition status client.");
		
		String argumentString = null;
		for (String argument : args) {
			if (argumentString != null) {
				argumentString += ", '"+argument+"'";
			} else {
				argumentString = "'"+argument+"'";
			}
		}
		mLogger.info("Arguments: {}.",argumentString);
		
		if (args.length < 2) {
			mLogger.error("Incomplete argument list. Arguments must be: <Address> <port> [<ConfigurationFile>].");
			return;
		}
		
		
		String addressString = args[0];
		String portString = args[1];
		int port = Integer.valueOf(portString);
		
		String loadConfigFileString = null;
		if (args.length > 2) {		
			loadConfigFileString = args[2];
		}
		
	
		Config initialConfig = new InitialConfigBaseFactory().createConfig();		
		if (loadConfigFileString == null) {
			loadConfigFileString = "configs" + File.separator + "default-config.dat";
		}		
		
		Config config = initialConfig;	
		if (loadConfigFileString != null) {
			FilePathString configurationFilePathString = lookForConfigurationFile(loadConfigFileString,initialConfig);
			
			if (configurationFilePathString != null) {
				mLogger.info("Loading configuration from '{}'.",configurationFilePathString);
				ConfigExtensionFactory configFactory = new ConfigExtensionFactory(config);
				ConfigTSVParser configParser = new ConfigTSVParser(configFactory, config, configurationFilePathString);
				try {
					Config loadedConfig = configParser.parseConfig(configurationFilePathString.getAbsoluteFilePathString());
					config = loadedConfig;
				} catch (IOException e) {
					mLogger.error("Failed to load configuration '{}', got IOException '{}'.",configurationFilePathString,e.getMessage());
				}
			} else {
				mLogger.error("Cannot find configuration file '{}'.",loadConfigFileString);
			}
		}
		
		
		CompetitionConsoleStatusClient statusClient = new CompetitionConsoleStatusClient();
		StatusClientUpdateManager statusClientUpdateManager = new StatusClientUpdateManager(addressString,port,statusClient,config);
		statusClientUpdateManager.waitForFinished();
		
		
		
		
		mLogger.info("Stopping competition status client.\n\n");
		
		System.exit(0);
	}
	
		
	
	
	
	public static FilePathString lookForConfigurationFile(String fileString, Config initialConfig) {		
		String baseDirectory = ConfigDataValueReader.getConfigDataValueString(initialConfig, ConfigType.CONFIG_TYPE_BASE_DIRECTORY);
		FilePathString configurationFileString = new FilePathString(baseDirectory+fileString);
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = new FilePathString(fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = new FilePathString(System.getProperty("user.dir")+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = new FilePathString(System.getProperty("user.dir")+File.separator+fileString);
		}		
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = new FilePathString(baseDirectory+"configs"+File.separator+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = new FilePathString(baseDirectory+"configs"+File.separator+fileString+".dat");
		}
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = null;
		}
		return configurationFileString;
	}


	public final static void clearConsole() {
	    try {
	        final String os = System.getProperty("os.name");

	        if (os.contains("Windows")) {
	            Runtime.getRuntime().exec("cls");
	        } else {
	            Runtime.getRuntime().exec("clear");
	        }
	    } catch (final Exception e) {
	    }
	}
	
	
	public String fillUpCut(String string, int pos) {
		if (string.length() > pos) {
			string = string.substring(0, pos);
		}
		while (string.length() < pos) {
			string += " ";
		}
		return string;
	}
	public String prefill(int number, int count) {
		return prefill(String.valueOf(number),count);
	}

	public String prefill(String string, int count) {
		while (string.length() < count) {
			string = " "+string;
		}
		return string;
	}	
	
	public void printCurrentCompetitionStatus() {
		String outputString = "";
		for (CompetitionStatus status : mLastCompetitionStatusMap.values()) {	
			
			CompetitionExecutionState executionState = status.getExecutionState();
			
			String competitionString = fillUpCut("Competition: "+status.getCompetitionName(),50);
			competitionString = fillUpCut(competitionString+"  ["+prefill(executionState.getShortString(),8)+"]",64);
			competitionString = fillUpCut(competitionString+"#R: "+prefill(status.getReasonerCount(),2),70);
			competitionString = fillUpCut(competitionString+", #Q: "+prefill(status.getQueryCount(),3),80);
			competitionString += "\r\n";
				
			int reasonerCount = status.getReasonerCount();
			Vector<String> reasonerConsolePrintStringVector = new Vector<String>();
			reasonerConsolePrintStringVector.setSize(reasonerCount);
			for (int i = 0; i < reasonerCount; ++i) {
				String unknownString = prefill(i+1,2)+". ????????\r\n";
				reasonerConsolePrintStringVector.set(i, unknownString);
			}
			
			
			
			
			
			
			
			class StatusUpdateItem implements Comparable<StatusUpdateItem> {	
				CompetitionReasonerProgressStatus mStatus = null;
				long mProcessedTime = 0;
				int mCorrectlyProccessedCount = 0;
				int mRank = 0;
				int mPosition = 0;
				
				public StatusUpdateItem(CompetitionReasonerProgressStatus status) {
					mStatus = status;
					mProcessedTime = mStatus.getCorrectlyProccessedTime();
					mCorrectlyProccessedCount = mStatus.getCorrectlyProcessedCount();
				}
				
				public int getRank() {
					return mRank;
				}
				
				public int getPosition() {
					return mPosition;
				}
				@Override
				public int compareTo(StatusUpdateItem data) {
					if (mCorrectlyProccessedCount > data.mCorrectlyProccessedCount) {
						return -1;
					} else if (mCorrectlyProccessedCount < data.mCorrectlyProccessedCount) {
						return 1;
					} else {
						if (mProcessedTime < data.mProcessedTime) {
							return -1;
						} else if (mProcessedTime > data.mProcessedTime) {
							return 1;
						} else {
							return 0;
						}
					}
				}
			}	
			
			HashMap<CompetitionReasonerProgressStatus,StatusUpdateItem> statusUpdateItemMap = new HashMap<CompetitionReasonerProgressStatus,StatusUpdateItem>(); 			
			ArrayList<StatusUpdateItem> statusUpdateItemList = new ArrayList<StatusUpdateItem>(); 			
			for (CompetitionReasonerProgressStatus reasonerStatus : mLastCompetitionReasonerStatusMap.values()) {
				if (reasonerStatus.getCompetitionSourceString().compareTo(status.getCompetitionSourceString()) == 0) {
					StatusUpdateItem statusUpdateItem = new StatusUpdateItem(reasonerStatus);
					statusUpdateItemList.add(statusUpdateItem);
					statusUpdateItemMap.put(reasonerStatus, statusUpdateItem);
				}				
			}
			

			Collections.sort(statusUpdateItemList);
			int nextRank = 1;
			for (StatusUpdateItem statusUpdateItem : statusUpdateItemList) {
				statusUpdateItem.mRank = nextRank;
				statusUpdateItem.mPosition = nextRank;
				++nextRank;
			}
			
			ListIterator<StatusUpdateItem> statusUpdateItemIt = statusUpdateItemList.listIterator();
			while (statusUpdateItemIt.hasNext()) {
				StatusUpdateItem statusUpdateItem = statusUpdateItemIt.next();
				ListIterator<StatusUpdateItem> statusUpdateItemIt2 = statusUpdateItemList.listIterator(statusUpdateItemIt.nextIndex());
				boolean stillIdentical = true;
				int identicalCount = 0;
				while (statusUpdateItemIt2.hasNext() && stillIdentical) {
					StatusUpdateItem statusUpdateItem2 = statusUpdateItemIt2.next();
					if (statusUpdateItem.compareTo(statusUpdateItem2) == 0) {
						identicalCount++;
					} else {
						stillIdentical = false;
					}
				}
				statusUpdateItem.mRank += identicalCount;
			}				
		
						
			
			
			
			
			
			for (CompetitionReasonerProgressStatus reasonerStatus : mLastCompetitionReasonerStatusMap.values()) {
				if (reasonerStatus.getCompetitionSourceString().compareTo(status.getCompetitionSourceString()) == 0) {	
					StatusUpdateItem statusUpdateItem = statusUpdateItemMap.get(reasonerStatus);
					
					String reasonerString = prefill(statusUpdateItem.getRank(),2)+". "+reasonerStatus.getReasonerName();
					reasonerString = fillUpCut(reasonerString,14);
					int totalCharCount = 50;
					int corrProcCharCount = totalCharCount*reasonerStatus.getCorrectlyProcessedCount()/status.getQueryCount();
					int remainProcCharCount = totalCharCount*(status.getQueryCount()-(reasonerStatus.getTotalProcessedCount()-reasonerStatus.getCorrectlyProcessedCount()))/status.getQueryCount();
					String progressString = "";
					while (progressString.length() < corrProcCharCount) {
						progressString += "=";
					}
					while (progressString.length() < remainProcCharCount) {
						progressString += " ";
					}
					while (progressString.length() < totalCharCount) {
						progressString += "/";
					}
					progressString = "["+progressString+"]";
					reasonerString = fillUpCut(reasonerString+progressString,66);
//					String processedString = prefill(reasonerStatus.getCorrectlyProcessedCount(),3)+"/"+prefill(reasonerStatus.getTotalProcessedCount(),3)+"/"+prefill(status.getQueryCount(),3);
					String processedString = prefill(reasonerStatus.getCorrectlyProcessedCount(),3)+"/"+prefill(status.getQueryCount(),3);
					if (reasonerStatus.getTotalProcessedCount()-reasonerStatus.getCorrectlyProcessedCount() < 10) {
						processedString += " ";
					}
					processedString += "(-"+String.valueOf(reasonerStatus.getTotalProcessedCount()-reasonerStatus.getCorrectlyProcessedCount())+")";
					reasonerString = fillUpCut(reasonerString+" "+processedString,80);
					reasonerConsolePrintStringVector.set(statusUpdateItem.getPosition()-1,reasonerString+"\r\n");
				}
			}
			
	
			
			
			for (int i = 0; i < reasonerCount; ++i) {
				competitionString += reasonerConsolePrintStringVector.get(i);
			}
			competitionString += "\r\n";
			
			outputString += competitionString;
		}
		
		
		
		clearConsole();
		System.out.print(outputString);
	}
	


	@Override
	public void updateCompetitionStatus(CompetitionStatus status) {
		mLastCompetitionStatusMap.put(status.getCompetitionSourceString(),status);
		printCurrentCompetitionStatus();
	}





	@Override
	public void updateCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status) {
		mLastCompetitionReasonerStatusMap.put(status.getCompetitionSourceString()+":"+status.getReasonerSourceString(),status);
		printCurrentCompetitionStatus();
	}





	@Override
	public void updateCompetitionEvaluationStatus(CompetitionEvaluationStatus status) {
	}	

}
