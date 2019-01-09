package org.semanticweb.ore.wep;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.semanticweb.ore.competition.CompetitionStatusUpdateCollectionManager;
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

public class CompetitionStatusWebPageServer {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionStatusWebPageServer.class);
	
	

	

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
		
		if (args.length < 3) {
			mLogger.error("Incomplete argument list. Arguments must be: <Address (for Competition Server)> <Port (for Competition Server)> <Port (for Webserver)> [<ConfigurationFile>].");
			return;
		}
		
		
		String addressString = args[0];
		String portString = args[1];
		String webPortString = args[2];
		int port = Integer.valueOf(portString);
		int webPort = Integer.valueOf(webPortString);
		
		String loadConfigFileString = null;
		if (args.length > 3) {		
			loadConfigFileString = args[3];
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
		
		
		
		CompetitionStatusUpdateCollectionManager compStatusUpdateCollector = new CompetitionStatusUpdateCollectionManager(config);
		StatusClientUpdateManager statusClientUpdateManager = new StatusClientUpdateManager(addressString,port,compStatusUpdateCollector,config);	
		statusClientUpdateManager.startThread();
		
		
		
		String rootDirectory = ConfigDataValueReader.getConfigDataValueString(config, ConfigType.CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY);
		if (rootDirectory != null) {
			Server server = new Server();
	        SelectChannelConnector connector = new SelectChannelConnector();
	        connector.setPort(webPort);
	        server.addConnector(connector);
	        
	        LinkPageMapper linkPageMapper = new LinkPageMapper(config);
	        
	        ServletContextHandler statusServletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
	        statusServletHandler.setContextPath("/");
	        statusServletHandler.addServlet(new ServletHolder(new StatusMainWebPageServlet(compStatusUpdateCollector,linkPageMapper,config)),"/live.html");
	        statusServletHandler.addServlet(new ServletHolder(new StatusMainWebPageServlet(compStatusUpdateCollector,linkPageMapper,config)),"/liveScreen.html");
	        statusServletHandler.addServlet(new ServletHolder(new StatusMainWebPageServlet(compStatusUpdateCollector,linkPageMapper,config)),"/live/resultStream/*");
	        
	        statusServletHandler.addServlet(new ServletHolder(new ResultMainWebPageServlet(compStatusUpdateCollector,linkPageMapper,config)),"/results.html");
	        
	        statusServletHandler.addServlet(new ServletHolder(new ResultDownloadServlet(compStatusUpdateCollector,config)),"/results/download/*");
	        statusServletHandler.addServlet(new ServletHolder(new ResultViewWebPageServlet(compStatusUpdateCollector,config)),"/results/view");
	        
	        
//	        Collection<String> statusFilterList = Arrays.asList(new String[]{"/","/index.html"});
//	        RequestURLFilterHandler statusPageFilterHandler = new RequestURLFilterHandler(statusFilterList,statusServletHandler); 
	 
	        ResourceHandler resource_handler = new ResourceHandler();
	        resource_handler.setDirectoriesListed(true);  
	        resource_handler.setResourceBase(rootDirectory);
	 
	        HandlerList handlers = new HandlerList();
	        handlers.setHandlers(new Handler[] { statusServletHandler, resource_handler, new DefaultHandler() });
	        server.setHandler(handlers);
	 
	        try {
				server.start();
				server.join();
			} catch (Exception e) {			
			}
		}
        
		
		
		
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


}
