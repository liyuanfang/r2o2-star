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

public class ORE2014ELDLSeparatedAllQueriesGenerator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ORE2014ELDLSeparatedAllQueriesGenerator.class);

	final private static String mClassificationQuerySubDirectoryString = "classification";
	final private static String mRealisationQuerySubDirectoryString = "realisation";
	final private static String mConsistencyQuerySubDirectoryString = "consistency";

	public static void main(String[] args) {
		
		Config initialConfig = new InitialConfigBaseFactory().createConfig();

		String queriesString = ConfigDataValueReader.getConfigDataValueString(initialConfig, ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY);
		String ontologiesString = ConfigDataValueReader.getConfigDataValueString(initialConfig, ConfigType.CONFIG_TYPE_ONTOLOGIES_DIRECTORY);
		
		mLogger.info("Generating queries for '{}'.",ontologiesString);
		
		Collection<String> ontologyFileStringCollection = FileSystemHandler.collectRelativeFilesInSubdirectories(ontologiesString);
		
		DefaultQueryFactory queryFactory = new DefaultQueryFactory();
		
		int validOntologies = 0;
		int dlOntologies = 0;
		int dlClassificationQueries = 0;
		int dlRealisationQueries = 0;
		int dlConsistencyQueries = 0;
		
		int elOntologies = 0;
		int elClassificationQueries = 0;
		int elRealisationQueries = 0;
		int elConsistencyQueries = 0;		
		
		for (String ontologyFileString : ontologyFileStringCollection) {
			
			if (ontologyFileString.startsWith("ore2014")) {
				
				FilePathString ontologyFilePathString = new FilePathString(ontologiesString,ontologyFileString,RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);
				
				mLogger.info("Generating queries for '{}'.",ontologyFilePathString);
	
				QueryExpressivity queryExpressivity = new OntologyExpressivityChecker(ontologyFilePathString.getAbsoluteFilePathString()).createQueryExpressivity();
				boolean validOntology = false;
				boolean elOntology = false;
				String correctedQueryOntologyFileString = ontologyFileString.replace("ore2014/", "");
				if (queryExpressivity.isInELProfile()) {
					correctedQueryOntologyFileString = "ore2014-full/el/"+correctedQueryOntologyFileString;
					validOntology = true;
					elOntology = true;
					++elOntologies;
				} else if (queryExpressivity.isInDLProfile()) {
					correctedQueryOntologyFileString = "ore2014-full/dl/"+correctedQueryOntologyFileString;
					validOntology = true;
					elOntology = false;
					++dlOntologies;
				}
	
				if (validOntology) {
					++validOntologies;
					FilePathString classificationQueryFilePathString = new FilePathString(queriesString,mClassificationQuerySubDirectoryString+ File.separator+correctedQueryOntologyFileString+"-classify.dat",RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
					Query classQuery = queryFactory.createClassificationQuery(classificationQueryFilePathString, ontologyFilePathString, queryExpressivity);
					if (classQuery != null) {
						
						QueryTSVRenderer queryTSVRenderer = null;
						try {
							queryTSVRenderer = new QueryTSVRenderer(classificationQueryFilePathString.getAbsoluteFilePathString());
							queryTSVRenderer.renderQuery(classQuery);
							mLogger.info("Generated classification query '{}'.",classQuery);
							if (elOntology) {
								++elClassificationQueries;
							} else {
								++dlClassificationQueries;
							}
						} catch (IOException e) {
							mLogger.error("Saving query '{}' to '{}' failed, got IOException '{}'.",new Object[]{classQuery,classificationQueryFilePathString,e.getMessage()});
						}				
						
					}
					
					
					FilePathString consistencyQueryFilePathString = new FilePathString(queriesString,mConsistencyQuerySubDirectoryString+ File.separator+correctedQueryOntologyFileString+"-classify.dat",RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
					Query consQuery = queryFactory.createConsistencyQuery(consistencyQueryFilePathString, ontologyFilePathString, queryExpressivity);			
					if (consQuery != null) {
						
						QueryTSVRenderer queryTSVRenderer = null;
						try {
							queryTSVRenderer = new QueryTSVRenderer(consistencyQueryFilePathString.getAbsoluteFilePathString());
							queryTSVRenderer.renderQuery(consQuery);
							mLogger.info("Generated consistency query '{}'.",consQuery);	
							if (elOntology) {
								++elConsistencyQueries;
							} else {
								++dlConsistencyQueries;
							}
						} catch (IOException e) {
							mLogger.error("Saving query '{}' to '{}' failed, got IOException '{}'.",new Object[]{consQuery,consistencyQueryFilePathString,e.getMessage()});
						}				
						
					}
					
					FilePathString realisationQueryFilePathString = new FilePathString(queriesString,mRealisationQuerySubDirectoryString+ File.separator+correctedQueryOntologyFileString+"-classify.dat",RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
					OntologyExpressivityChecker ontExpChecker = new OntologyExpressivityChecker(ontologyFilePathString.getAbsoluteFilePathString());

					if (ontExpChecker.hasIndividuals()) {
						Query realQuery = queryFactory.createRealisationQuery(realisationQueryFilePathString, ontologyFilePathString, queryExpressivity);			
						if (realQuery != null) {
							
							QueryTSVRenderer queryTSVRenderer = null;
							try {
								queryTSVRenderer = new QueryTSVRenderer(realisationQueryFilePathString.getAbsoluteFilePathString());
								queryTSVRenderer.renderQuery(realQuery);
								mLogger.info("Generated realisation query '{}'.",realQuery);
								if (elOntology) {
									++elRealisationQueries;
								} else {
									++dlRealisationQueries;
								}
							} catch (IOException e) {
								mLogger.error("Saving query '{}' to '{}' failed, got IOException '{}'.",new Object[]{realQuery,realisationQueryFilePathString,e.getMessage()});
							}	
						}
						
					} else {
						mLogger.info("'{}' does not contain individuals, no query generated.",ontologyFilePathString);
					}	
				}
			}
			
		}		
		mLogger.info("Query generation for '{}' completed.",ontologiesString);
		
		mLogger.info("Found {} valid ontologies.",validOntologies);
		mLogger.info("Found {} valid EL ontologies.",elOntologies);
		mLogger.info("Found {} valid DL ontologies.",dlOntologies);
		
		mLogger.info("Generated {} DL classification queries.",dlClassificationQueries);
		mLogger.info("Generated {} EL classification queries.",elClassificationQueries);
		
		mLogger.info("Generated {} DL realisation queries.",dlRealisationQueries);
		mLogger.info("Generated {} EL realisation queries.",elRealisationQueries);
		
		mLogger.info("Generated {} DL consistency queries.",dlConsistencyQueries);
		mLogger.info("Generated {} EL consistency queries.",elConsistencyQueries);
		
		
	}
}
