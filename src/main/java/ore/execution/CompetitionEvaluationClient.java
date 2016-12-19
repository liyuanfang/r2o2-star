package ore.execution;

import ore.configuration.*;
import ore.networking.ClientExecutionManager;
import ore.parsing.ConfigTSVParser;
import ore.utilities.FilePathString;
import ore.utilities.FileSystemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class CompetitionEvaluationClient {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionEvaluationClient.class);
	

	public static void main(String[] args) {
		
		mLogger.info("Starting competition evaluation client.");
		
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
		
		
		ClientExecutionManager clientExecutionManager = new ClientExecutionManager(addressString,port,config);
		clientExecutionManager.waitForFinished();
		
		
		mLogger.info("Stopping competition evaluation client.\n\n");
		
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
