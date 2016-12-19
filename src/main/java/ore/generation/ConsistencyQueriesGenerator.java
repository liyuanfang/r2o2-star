package ore.generation;

import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.configuration.InitialConfigBaseFactory;
import ore.querying.DefaultQueryFactory;
import ore.querying.Query;
import ore.querying.QueryExpressivity;
import ore.rendering.QueryTSVRenderer;
import ore.utilities.FilePathString;
import ore.utilities.FileSystemHandler;
import ore.utilities.RelativeFilePathStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class ConsistencyQueriesGenerator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ConsistencyQueriesGenerator.class);
	
	final private static String mConsistencyQuerySubDirectoryString = "consistency";

	public static void main(String[] args) {
		
		Config initialConfig = new InitialConfigBaseFactory().createConfig();

		String queriesString = ConfigDataValueReader.getConfigDataValueString(initialConfig, ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY);
		String ontologiesString = ConfigDataValueReader.getConfigDataValueString(initialConfig, ConfigType.CONFIG_TYPE_ONTOLOGIES_DIRECTORY);
		
		mLogger.info("Generating queries for '{}'.",ontologiesString);
		
		Collection<String> ontologyFileStringCollection = FileSystemHandler.collectRelativeFilesInSubdirectories(ontologiesString);
		
		DefaultQueryFactory queryFactory = new DefaultQueryFactory();
		
		for (String ontologyFileString : ontologyFileStringCollection) {
			
			FilePathString queryFilePathString = new FilePathString(queriesString,mConsistencyQuerySubDirectoryString+ File.separator+ontologyFileString+"-cons.dat",RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
			FilePathString ontologyFilePathString = new FilePathString(ontologiesString,ontologyFileString,RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);
			
			mLogger.info("Generating query for '{}'.",ontologyFilePathString);

			QueryExpressivity queryExpressivity = new OntologyExpressivityChecker(ontologyFilePathString.getAbsoluteFilePathString()).createQueryExpressivity();
			Query query = queryFactory.createConsistencyQuery(queryFilePathString, ontologyFilePathString, queryExpressivity);			
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
			
		}		
		mLogger.info("Query generation for '{}' completed.",ontologiesString);
		
	}
	


}
