package ore.execution;

import ore.competition.CompetitionEvaluationStatusSourcePathAdpater;
import ore.competition.CompetitionStatusUpdateLoadingManager;
import ore.competition.CompetitionStatusUpdater;
import ore.configuration.*;
import ore.networking.StatusServerUpdateManager;
import ore.parsing.ConfigTSVParser;
import ore.utilities.FilePathString;
import ore.utilities.FileSystemHandler;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class CompetitionReplayServer {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionReplayServer.class);
	

	public static void main(String[] args) {
		
		mLogger.info("Starting competition replay server.");
		
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
			mLogger.error("Incomplete argument list. Arguments must be: <Port> <CompetitionStatusMessagesFile> [<StartDateTime> <AdaptingEvaluationPath> <ConfigurationFile>].");
			return;
		}
		
		
		String portString = args[0];
		int port = Integer.valueOf(portString);
		
		String compMessageFileString = args[1];
		
		
		String startDateTimeString = null;
		DateTime startDateTime = null;
		if (args.length >= 3) {
			startDateTimeString = args[2];
			startDateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(startDateTimeString);
		}
		
		String matchString = null;
		if (args.length >= 4) {
			matchString = args[3];
		}
		
		String loadConfigFileString = null;
		if (args.length >= 5) {
			loadConfigFileString = args[4];
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
		
		
		
		
		CompetitionStatusUpdater statusUpdateManager = null;
		StatusServerUpdateManager serverStatusUpdateManager = new StatusServerUpdateManager(port+1,config,null);
		statusUpdateManager = serverStatusUpdateManager;
		
		if (matchString != null) {
			File adaptingFile = new File(compMessageFileString);
			String parentAdaptingString = adaptingFile.getParentFile().getAbsolutePath();
			CompetitionEvaluationStatusSourcePathAdpater statusAdapter = new CompetitionEvaluationStatusSourcePathAdpater(matchString,parentAdaptingString,serverStatusUpdateManager,config);
			statusUpdateManager = statusAdapter;
		}
		
		
		new CompetitionStatusUpdateLoadingManager(startDateTime,compMessageFileString,statusUpdateManager,config);
		

		

	
	}
	
	
	

	public static FilePathString lookForCompetitionFile(String fileString, Config initialConfig) {
		String baseDirectory = ConfigDataValueReader.getConfigDataValueString(initialConfig, ConfigType.CONFIG_TYPE_BASE_DIRECTORY);
		FilePathString competitionFileString = new FilePathString(baseDirectory+fileString);
		if (!FileSystemHandler.nonEmptyFileExists(competitionFileString)) {
			competitionFileString = new FilePathString(fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(competitionFileString)) {
			competitionFileString = new FilePathString(System.getProperty("user.dir")+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(competitionFileString)) {
			competitionFileString = new FilePathString(System.getProperty("user.dir")+ File.separator+fileString);
		}		
		if (!FileSystemHandler.nonEmptyFileExists(competitionFileString)) {
			competitionFileString = new FilePathString(baseDirectory+"competitions"+ File.separator+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(competitionFileString)) {
			competitionFileString = new FilePathString(baseDirectory+"competitions"+ File.separator+fileString+".dat");
		}
		if (!FileSystemHandler.nonEmptyFileExists(competitionFileString)) {
			competitionFileString = null;
		}
		return competitionFileString;
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
			configurationFileString = new FilePathString(System.getProperty("user.dir")+ File.separator+fileString);
		}		
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = new FilePathString(baseDirectory+"configs"+ File.separator+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = new FilePathString(baseDirectory+"configs"+ File.separator+fileString+".dat");
		}
		if (!FileSystemHandler.nonEmptyFileExists(configurationFileString)) {
			configurationFileString = null;
		}
		return configurationFileString;
	}	

}
