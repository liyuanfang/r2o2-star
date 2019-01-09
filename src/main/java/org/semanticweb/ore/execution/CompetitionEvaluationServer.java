package org.semanticweb.ore.execution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.semanticweb.ore.competition.Competition;
import org.semanticweb.ore.competition.CompetitionHandler;
import org.semanticweb.ore.competition.CompetitionRestartStatusUpdateManager;
import org.semanticweb.ore.competition.CompetitionStatusUpdateStoringManager;
import org.semanticweb.ore.competition.CompetitionStatusUpdater;
import org.semanticweb.ore.competition.DefaultCompetitionFactory;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigExtensionFactory;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.configuration.InitialConfigBaseFactory;
import org.semanticweb.ore.evaluation.CompetitionEvaluatorHandler;
import org.semanticweb.ore.evaluation.EvaluationFinishedBlockingCallback;
import org.semanticweb.ore.evaluation.QueryResultStorage;
import org.semanticweb.ore.interfacing.DefaultReasonerDescriptionFactory;
import org.semanticweb.ore.interfacing.ReasonerDescriptionManager;
import org.semanticweb.ore.networking.CompetitionExecutionManager;
import org.semanticweb.ore.networking.ServerExecutionManager;
import org.semanticweb.ore.networking.StatusServerUpdateManager;
import org.semanticweb.ore.parsing.CompetitionTSVParser;
import org.semanticweb.ore.parsing.ConfigTSVParser;
import org.semanticweb.ore.querying.QueryManager;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionEvaluationServer {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionEvaluationServer.class);
	

	public static void main(String[] args) {
		
		mLogger.info("Starting competition evaluation server.");
		
		String argumentString = null;
		for (String argument : args) {
			if (argumentString != null) {
				argumentString += ", '"+argument+"'";
			} else {
				argumentString = "'"+argument+"'";
			}
		}
		mLogger.info("Arguments: {}.",argumentString);
		
		if (args.length < 3) {
			mLogger.error("Incomplete argument list. Arguments must be: <Port> <Competition1> ... <CompetitionN> <ConfigurationFile>.");
			return;
		}
		
		
		String portString = args[0];
		int port = Integer.valueOf(portString);
		
		ArrayList<String> loadCompetitionStringList = new ArrayList<String>();
		for (int i = 1; i < args.length-1; ++i) {
			String loadCompetitionString = args[i];
			loadCompetitionStringList.add(loadCompetitionString);
		}
		
		String loadConfigFileString = args[args.length-1];
		
	
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
		
		
		
		
		mLogger.info("Starting loading of competitions.");
		
		ArrayList<Competition> competitionList = new ArrayList<Competition>();
		for (String loadCompetitionString : loadCompetitionStringList) {			
			FilePathString competitionFilePathString = lookForCompetitionFile(loadCompetitionString,initialConfig);
			
			Competition competition = null;
			if (competitionFilePathString != null) {
				DefaultCompetitionFactory competitionFactory = new DefaultCompetitionFactory();
				CompetitionTSVParser competitionTSVParser = new CompetitionTSVParser(competitionFactory, config, competitionFilePathString);
						
				try {
					competition = competitionTSVParser.parseCompetition(competitionFilePathString.getAbsoluteFilePathString());
				} catch (IOException e) {
					mLogger.error("Loading competition from '{}' failed, got IOException '{}'.",competitionFilePathString,e.getMessage());
				}
			}
			
			if (competition != null) {
				competitionList.add(competition);
			} else {
				mLogger.error("Loading competition from '{}' failed.",competitionFilePathString);
			}
		}
		
		
		mLogger.info("Loaded {} competitions.",competitionList.size());
		
		
		CompetitionStatusUpdater statusUpdateManager = null;		
		CompetitionStatusUpdateStoringManager statusUpdateStoringManager = new CompetitionStatusUpdateStoringManager(competitionList,"statusUpdateMessages.txt",null,config);		
		StatusServerUpdateManager serverStatusUpdateManager = new StatusServerUpdateManager(port+1,config,statusUpdateStoringManager);
		
		
		DefaultReasonerDescriptionFactory reasonerDescriptionFactory = new DefaultReasonerDescriptionFactory();
		ReasonerDescriptionManager reasonerManager =  new ReasonerDescriptionManager(reasonerDescriptionFactory,config);
		QueryManager queryManager = new QueryManager(config);		
		ServerExecutionManager serverExecutionManager = new ServerExecutionManager(port,config);
		
		boolean infiniteCompetitionRepeating = ConfigDataValueReader.getConfigDataValueBoolean(config, ConfigType.CONFIG_TYPE_COMPETITION_INFINITE_REPEAT);
		if (infiniteCompetitionRepeating) {		
			statusUpdateManager = new CompetitionRestartStatusUpdateManager(serverExecutionManager,reasonerManager,queryManager,serverStatusUpdateManager,competitionList,config);
		}
		if (statusUpdateManager == null) {		
			statusUpdateManager = serverStatusUpdateManager;
		}
		
		
		

		
		ArrayList<EvaluationFinishedBlockingCallback> evaluationFinishedBlockerList = new ArrayList<EvaluationFinishedBlockingCallback>(); 
		


		DateTime currDateTime = new DateTime();
		long compNumber = 0;
		
		for (Competition competition : competitionList) {
			++compNumber;
			
			if (competition.getDesiredStartingDate() == null) {
				DateTime tmpStartDateTime = currDateTime.plus(compNumber);
				competition.setDesiredStartingDate(tmpStartDateTime);
			}
	
			CompetitionHandler competitionHandler = new CompetitionHandler(queryManager,reasonerManager,config);
			QueryResultStorage resultStorage = new QueryResultStorage();
			if (competitionHandler.initCompetition(competition, resultStorage)) {
				
				mLogger.info("Competition '{}' initialised.",competition.getCompetitionName());

				
				EvaluationFinishedBlockingCallback evaluationFinishedCallback = new EvaluationFinishedBlockingCallback();
				CompetitionEvaluatorHandler competitionEvaluatorHandler = new CompetitionEvaluatorHandler(competitionHandler,competition,resultStorage,statusUpdateManager,evaluationFinishedCallback,config);
				evaluationFinishedBlockerList.add(evaluationFinishedCallback);
				
				CompetitionExecutionManager competitionExecutionManager = new CompetitionExecutionManager(competitionEvaluatorHandler,competitionHandler,statusUpdateManager,config);
				serverExecutionManager.postSchedulingRequest(competitionExecutionManager, competitionExecutionManager);
			}
		}
		
		
		boolean terminateAfterExecution = ConfigDataValueReader.getConfigDataValueBoolean(config, ConfigType.CONFIG_TYPE_NETWORKING_TERMINATE_AFTER_EXECUTION);

		if (!infiniteCompetitionRepeating && terminateAfterExecution) {
		
		
			for (EvaluationFinishedBlockingCallback evaluationFinishedBlocker : evaluationFinishedBlockerList) {
				try {
					evaluationFinishedBlocker.waitForCallback();
				} catch (InterruptedException e) {
					mLogger.error("Waiting for evaluation finished interrupted, got InterruptedException '{}'",e.getMessage());				
				}
			}
			
	
			
			mLogger.info("Stopping competition evaluation server.\n\n");
			
			System.exit(0);
		}

	
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
			competitionFileString = new FilePathString(System.getProperty("user.dir")+File.separator+fileString);
		}		
		if (!FileSystemHandler.nonEmptyFileExists(competitionFileString)) {
			competitionFileString = new FilePathString(baseDirectory+"competitions"+File.separator+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(competitionFileString)) {
			competitionFileString = new FilePathString(baseDirectory+"competitions"+File.separator+fileString+".dat");
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

}
