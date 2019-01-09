package org.semanticweb.ore.execution;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigExtensionFactory;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.configuration.InitialConfigBaseFactory;
import org.semanticweb.ore.conversion.OntologyFormatDynamicConversionRedirector;
import org.semanticweb.ore.conversion.OntologyFormatNoRedictionRedirector;
import org.semanticweb.ore.conversion.OntologyFormatRedirector;
import org.semanticweb.ore.evaluation.SummarisationQueryResultEvaluator;
import org.semanticweb.ore.interfacing.DefaultReasonerAdaptorFactory;
import org.semanticweb.ore.interfacing.DefaultReasonerDescriptionFactory;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.interfacing.ReasonerDescriptionManager;
import org.semanticweb.ore.parsing.ConfigTSVParser;
import org.semanticweb.ore.parsing.QueryTSVParser;
import org.semanticweb.ore.querying.DefaultQueryFactory;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.querying.QueryType;
import org.semanticweb.ore.querying.TSVQueryResponseStoringHandler;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.semanticweb.ore.utilities.RelativeFilePathStringType;
import org.semanticweb.ore.verification.DefaultQueryResultNormaliserFactory;
import org.semanticweb.ore.verification.ExpectedQueryResultHashComparisonVerifier;
import org.semanticweb.ore.verification.QueryResultVerificationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReasonerQueryEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerQueryEvaluator.class);
	

	public static void main(String[] args) {
		
		mLogger.info("Starting reasoner evaluation.");
		
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
			mLogger.error("Incomplete argument list. Arguments must be: <QueryType> <ReasonerDataFile> [<ConfigurationFile>].");
			return;
		}
		
		String queryTypeString = args[0];
		String loadReasonerFileString = args[1];
		String loadConfigFileString = null;
		if (args.length > 2) {		
			loadConfigFileString = args[2];
		}
		
		QueryType testingQueryType = null;
		for (QueryType queryType : QueryType.values()) {
			if (queryType.getQueryTypeName().compareToIgnoreCase(queryTypeString) == 0) {
				testingQueryType = queryType;
				break;
			}
		}
		
		if (testingQueryType == null) {
			mLogger.error("Cannot detect query type, must be 'classification', 'consistency', or 'realisation', but got '{}'.",queryTypeString);
			return;
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
		
		String responseDirectory = ConfigDataValueReader.getConfigDataValueString(config, ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY);
		
		
		
		DefaultReasonerDescriptionFactory reasonerDescriptionFactory = new DefaultReasonerDescriptionFactory();
		ReasonerDescriptionManager reasonerManager =  new ReasonerDescriptionManager(reasonerDescriptionFactory,config);
		ReasonerDescription reasonerDescription = reasonerManager.loadReasonerDescription(loadReasonerFileString);
		
		
		if (reasonerDescription != null) {
			
			boolean useDateSubdirectoryForResponses = ConfigDataValueReader.getConfigDataValueBoolean(config, ConfigType.CONFIG_TYPE_RESPONSES_DATE_SUB_DIRECTORY,true);
			String responsesDateSubDirectroy = "";
			if (useDateSubdirectoryForResponses) {
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
				Date date = new Date();
				responsesDateSubDirectroy = dateFormat.format(date)+File.separator;
			}
				
			String queriesString = ConfigDataValueReader.getConfigDataValueString(config, ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY);
			mLogger.info("Processing queries from '{}'.",queriesString);
			
			Collection<String> queryFileStringCollection = FileSystemHandler.collectRelativeFilesInSubdirectories(queriesString);
			
			
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
			
			String reasonerEvaluationResponseDirectory = responseDirectory+"reasoner-evaluations"+File.separator+reasonerDescription.getOutputPathString()+File.separator+testingQueryType.getQueryTypeName().toLowerCase()+File.separator+responsesDateSubDirectroy;
			SummarisationQueryResultEvaluator resultEvaluator = new SummarisationQueryResultEvaluator(config,reasonerEvaluationResponseDirectory+"evaluation-summary.txt",reasonerEvaluationResponseDirectory+"evaluation-data");
			
			
			for (String relativeQueryFileString : queryFileStringCollection) {
				
				String completeQueryFileString = queriesString + relativeQueryFileString;
				FilePathString queryFileString = new FilePathString(queriesString,relativeQueryFileString,RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
				
				DefaultQueryFactory queryFactory = new DefaultQueryFactory(); 
				QueryTSVParser queryTSVParser = new QueryTSVParser(queryFactory, config, queryFileString);
				
				String responseDestinationString = reasonerEvaluationResponseDirectory+relativeQueryFileString+File.separator;
				
				Query query = null;
				
				try {
					query = queryTSVParser.parseQuery(completeQueryFileString);
				} catch (IOException e) {
					mLogger.error("Loading of query from '{}' failed, got IOException '{}'.",completeQueryFileString,e.getMessage());
				}
				
				if (query != null) {
					
					if (query.getQueryType() == testingQueryType) {
					
						QueryResponse queryResponse = reasonerQueryExecutionHandler.executeReasonerQuery(reasonerDescription, query, responseDestinationString);
						
						QueryResultVerificationReport verificationReport = resultVerifier.verifyResponse(reasonerDescription, query, queryResponse);
						
						resultEvaluator.evaluateQueryResponse(reasonerDescription, query, queryResponse, verificationReport);	
					}

				}
				
			}	
			mLogger.info("Completed processing of queries from '{}'.",queriesString);
			
			resultEvaluator.generateEvaluationSummary();
			
			reasonerQueryExecutionHandler.stopThread();
			
			
		}
		
		mLogger.info("Stopping reasoner evaluation.\n\n");
		
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
