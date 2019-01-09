package org.semanticweb.ore.execution;

import java.io.File;
import java.io.IOException;

import org.semanticweb.ore.competition.Competition;
import org.semanticweb.ore.competition.CompetitionHandler;
import org.semanticweb.ore.competition.CompetitionExecutionReport;
import org.semanticweb.ore.competition.CompetitionExecutionTask;
import org.semanticweb.ore.competition.DefaultCompetitionFactory;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigExtensionFactory;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.configuration.InitialConfigBaseFactory;
import org.semanticweb.ore.conversion.OntologyFormatDynamicConversionRedirector;
import org.semanticweb.ore.conversion.OntologyFormatNoRedictionRedirector;
import org.semanticweb.ore.conversion.OntologyFormatRedirector;
import org.semanticweb.ore.evaluation.EvaluationFinishedBlockingCallback;
import org.semanticweb.ore.evaluation.CompetitionEvaluatorHandler;
import org.semanticweb.ore.evaluation.QueryResultStorage;
import org.semanticweb.ore.interfacing.DefaultReasonerAdaptorFactory;
import org.semanticweb.ore.interfacing.DefaultReasonerDescriptionFactory;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.interfacing.ReasonerDescriptionManager;
import org.semanticweb.ore.parsing.CompetitionTSVParser;
import org.semanticweb.ore.parsing.ConfigTSVParser;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryManager;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.querying.TSVQueryResponseStoringHandler;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.semanticweb.ore.verification.DefaultQueryResultNormaliserFactory;
import org.semanticweb.ore.verification.ExpectedQueryResultHashComparisonVerifier;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionEvaluator.class);
	

	public static void main(String[] args) {
		
		mLogger.info("Starting competition evaluation.");
		
		String argumentString = null;
		for (String argument : args) {
			if (argumentString != null) {
				argumentString += ", '"+argument+"'";
			} else {
				argumentString = "'"+argument+"'";
			}
		}
		mLogger.info("Arguments: {}.",argumentString);
		
		if (args.length < 1) {
			mLogger.error("Incomplete argument list. Arguments must be: <Competition> [<ConfigurationFile>].");
			return;
		}
		
		String loadCompetitionString = args[0];
		String loadConfigFileString = null;
		if (args.length > 1) {		
			loadConfigFileString = args[1];
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
		
		
		
		
		DefaultReasonerDescriptionFactory reasonerDescriptionFactory = new DefaultReasonerDescriptionFactory();
		ReasonerDescriptionManager reasonerManager =  new ReasonerDescriptionManager(reasonerDescriptionFactory,config);
		
		QueryManager queryManager = new QueryManager(config);
		
		FilePathString competitionFilePathString = lookForCompetitionFile(loadCompetitionString,initialConfig);
		
		DefaultCompetitionFactory competitionFactory = new DefaultCompetitionFactory();
		CompetitionTSVParser competitionTSVParser = new CompetitionTSVParser(competitionFactory, config, competitionFilePathString);
		

		Competition competition = null;
		
		

		
		try {
			competition = competitionTSVParser.parseCompetition(competitionFilePathString.getAbsoluteFilePathString());
		} catch (IOException e) {
			mLogger.error("Loading competition from '{}' failed, got IOException '{}'.",competitionFilePathString,e.getMessage());
		}		
		
	
		String responseDirectory = ConfigDataValueReader.getConfigDataValueString(config, ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY);
		
		
		if (competition != null) {
			
			
			CompetitionHandler competitionHandler = new CompetitionHandler(queryManager,reasonerManager,config);
			
			QueryResultStorage resultStorage = new QueryResultStorage();
			
			if (competitionHandler.initCompetition(competition, resultStorage)) {
			
				TSVQueryResponseStoringHandler queryResponseStoringHandler = new TSVQueryResponseStoringHandler(config);
				OntologyFormatRedirector conversionFormatRedirector = null;
				if (!ConfigDataValueReader.getConfigDataValueBoolean(config, ConfigType.CONFIG_TYPE_EXECUTION_ONTOLOGY_CONVERTION, false)) {	
					conversionFormatRedirector = new OntologyFormatNoRedictionRedirector(); 
				} else {
					conversionFormatRedirector = new OntologyFormatDynamicConversionRedirector(config);
				}
				
				DefaultReasonerAdaptorFactory reasonerAdaptorFactory = new DefaultReasonerAdaptorFactory();
				ReasonerQueryExecutionHandler reasonerQueryExecutionHandler = new ReasonerQueryExecutionHandler(reasonerAdaptorFactory,conversionFormatRedirector,queryResponseStoringHandler,config);
				
				
				DefaultQueryResultNormaliserFactory normalisationFactory = new DefaultQueryResultNormaliserFactory(config);			
				ExpectedQueryResultHashComparisonVerifier resultVerifier = new ExpectedQueryResultHashComparisonVerifier(normalisationFactory,queryResponseStoringHandler,config);
								
				CompetitionExecutionTask executionTask = null;
				while ((executionTask = competitionHandler.getNextCompetitionExecutionTask()) != null) {
					
					ReasonerDescription reasonerDescription = executionTask.getReasonerDescription();
					Query query = executionTask.getQuery();
					
					String responseDestinationString = responseDirectory+executionTask.getOutputString();
					
					QueryResponse queryResponse = reasonerQueryExecutionHandler.executeReasonerQuery(reasonerDescription, query, responseDestinationString, executionTask.getExecutionTimeout(), executionTask.getMemoryLimit());
					
					QueryResultVerificationReport verificationReport = resultVerifier.verifyResponse(reasonerDescription, query, queryResponse);
					
					CompetitionExecutionReport competitionReport = new CompetitionExecutionReport(executionTask, true, queryResponse, verificationReport);
					
					competitionHandler.completeCompetitionExecutionTask(executionTask, competitionReport);
					
				}
				
				EvaluationFinishedBlockingCallback evaluationFinishedCallback = new EvaluationFinishedBlockingCallback();
				CompetitionEvaluatorHandler competitionEvaluatorHandler = new CompetitionEvaluatorHandler(competitionHandler,competition,resultStorage,null,evaluationFinishedCallback,config);
				competitionEvaluatorHandler.evaluateCompetition();
				try {
					evaluationFinishedCallback.waitForCallback();
				} catch (InterruptedException e) {
					mLogger.error("Waiting for evaluation finished interrupted, got InterruptedException '{}'",e.getMessage());
				}				
				
				
				reasonerQueryExecutionHandler.stopThread();
				
			}		
		
			
		}
		
		mLogger.info("Stopping competition evaluation.\n\n");
		
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
