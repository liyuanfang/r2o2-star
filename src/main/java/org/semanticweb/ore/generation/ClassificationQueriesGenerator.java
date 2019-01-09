package org.semanticweb.ore.generation;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.configuration.InitialConfigBaseFactory;
import org.semanticweb.ore.querying.DefaultQueryFactory;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryExpressivity;
import org.semanticweb.ore.rendering.QueryTSVRenderer;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.semanticweb.ore.utilities.RelativeFilePathStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class ClassificationQueriesGenerator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ClassificationQueriesGenerator.class);

	final private static String mClassificationQuerySubDirectoryString = "classification";

	public static void main(String[] args) {
		
		Config initialConfig = new InitialConfigBaseFactory().createConfig();

		String queriesString = ConfigDataValueReader.getConfigDataValueString(initialConfig, ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY);
		String ontologiesString = ConfigDataValueReader.getConfigDataValueString(initialConfig, ConfigType.CONFIG_TYPE_ONTOLOGIES_DIRECTORY);
		
		mLogger.info("Generating queries for '{}'.",ontologiesString);
		
		Collection<String> ontologyFileStringCollection = FileSystemHandler.collectRelativeFilesInSubdirectories(ontologiesString);
		
		DefaultQueryFactory queryFactory = new DefaultQueryFactory();
		
		for (String ontologyFileString : ontologyFileStringCollection) {	
			
			
//			if (ontologyFileString.startsWith("el-test")) {
				
				FilePathString queryFilePathString = new FilePathString(queriesString,mClassificationQuerySubDirectoryString+File.separator+ontologyFileString+"-classify.dat",RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
				FilePathString ontologyFilePathString = new FilePathString(ontologiesString,ontologyFileString,RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);
				
				mLogger.info("Generating query for '{}'.",ontologyFilePathString);
	
				QueryExpressivity queryExpressivity = new OntologyExpressivityChecker(ontologyFilePathString.getAbsoluteFilePathString()).createQueryExpressivity();
				Query query = queryFactory.createClassificationQuery(queryFilePathString, ontologyFilePathString, queryExpressivity);			
				if (query != null) {
					
					QueryTSVRenderer queryTSVRenderer = null;
					try {
						queryTSVRenderer = new QueryTSVRenderer(queryFilePathString.getAbsoluteFilePathString());
						queryTSVRenderer.renderQuery(query);
					} catch (IOException e) {
						mLogger.error("Saving query '{}' to '{}' failed, got IOException '{}'.",new Object[]{query,queryFilePathString,e.getMessage()});
					}				
					
				}
				mLogger.info("Generated query '{}'.",query);
//			}
			
		}		
		mLogger.info("Query generation for '{}' completed.",ontologiesString);
		
	}
}
