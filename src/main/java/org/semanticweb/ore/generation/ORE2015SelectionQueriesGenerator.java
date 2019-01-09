package org.semanticweb.ore.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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

public class ORE2015SelectionQueriesGenerator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ORE2015SelectionQueriesGenerator.class);

	final private static String mClassificationQuerySubDirectoryString = "classification";
	final private static String mRealisationQuerySubDirectoryString = "realisation";
	final private static String mConsistencyQuerySubDirectoryString = "consistency";
	
	
	public static  HashMap<String,Integer> loadOntologyIndexMap(String fileString) {
		HashMap<String,Integer> ontologyIndexMap = new HashMap<String,Integer>();
		
		int number = 0;
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileString));
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;				
			while ((line = reader.readLine()) != null) {
				if (line != null && !line.isEmpty()) {
					String matchText = line.trim();
					matchText = matchText.replace("\\","/");
					++number;
					ontologyIndexMap.put(matchText,number);
				}
			}
			reader.close();				
			inputStream.close();
		} catch (IOException e) {
			mLogger.error("Parsing of '{}' failed.",fileString);
		}		
		
		return ontologyIndexMap;
	}
	
	
	
	public static Collection<String> getSortedOntologiesList(Collection<String> ontologyFileStringCollection, HashMap<String,Integer> ontologyIndexMap) {
		ArrayList<String> sortedOntologieFileStringList = new ArrayList<String>();
		int ontologyCount = ontologyFileStringCollection.size();
		int nextOntologyIndex = ontologyCount;
		
		HashMap<Integer,ArrayList<String>> indexOntologyListHash = new HashMap<Integer,ArrayList<String>>();
		for (String ontologyFileString : ontologyFileStringCollection) {	
			int index = getOntologieIndex(ontologyFileString, ontologyIndexMap);
			if (index >= 0) {			
				ArrayList<String> ontologyList = indexOntologyListHash.get(index);
				if (ontologyList == null) {
					ontologyList = new ArrayList<String>();
					indexOntologyListHash.put(index,ontologyList);
				}
				ontologyList.add(ontologyFileString);	
			}

		}
		
		for (int i = 0; i < nextOntologyIndex; ++i) {
			ArrayList<String> ontologyList = indexOntologyListHash.get(i);
			if (ontologyList != null) {	
				sortedOntologieFileStringList.addAll(ontologyList);
			}
		}
		return sortedOntologieFileStringList;
	}
	
	
	
	public static int getOntologieIndex(String ontologyFileString, HashMap<String,Integer> ontologyIndexMap) {
		if (ontologyIndexMap.containsKey(ontologyFileString)) {
			return ontologyIndexMap.get(ontologyFileString);
		}
		String pathReducedOntologyFileString = ontologyFileString.replace("\\", "/");
		if (ontologyIndexMap.containsKey(pathReducedOntologyFileString)) {
			return ontologyIndexMap.get(pathReducedOntologyFileString);
		}
		do {
			pathReducedOntologyFileString = pathReducedOntologyFileString.substring(pathReducedOntologyFileString.indexOf("/")+1);
			if (ontologyIndexMap.containsKey(pathReducedOntologyFileString)) {
				return ontologyIndexMap.get(pathReducedOntologyFileString);
			}
		} while (pathReducedOntologyFileString.contains("/"));
		
		return -1;
	}


	public static void main(String[] args) {
		
		
		mLogger.info("Starting query generation.");
		
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
			mLogger.error("Incomplete argument list. Arguments must be: <OntologyFilterFile> <MaxQueryCount> <QueryTpye> [<Profile>].");
			return;
		}
		
		
		String ontologyFilterFileString = args[0];
		int maxQueryGenerationCount = Integer.parseInt(args[1]);
		String queryTypeString = args[2];
		
		String profileString = null;
		if (args.length >= 4) {
			profileString = args[3];
		}
		
		HashMap<String,Integer> ontologyIndexMap = loadOntologyIndexMap(ontologyFilterFileString);
		
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
		
		boolean generateClassification = false;
		boolean generateRealisation = false;
		boolean generateConsistency = false;
		
		boolean generateForEL = true;
		boolean generateForDL = true;
		
		if (profileString != null) {
			if (profileString.equalsIgnoreCase("DL")) {
				generateForEL = false;
				generateForDL = true;
			} else if (profileString.equalsIgnoreCase("EL")) {
				generateForEL = true;
				generateForDL = false;
			}
		}
		
		if (queryTypeString.equalsIgnoreCase("all")) {
			generateClassification = generateRealisation = generateConsistency = true;
		}
		if (queryTypeString.equalsIgnoreCase("classification")) {
			generateClassification = true;
		}		
		if (queryTypeString.equalsIgnoreCase("realisation")) {
			generateRealisation = true;
		}		
		if (queryTypeString.equalsIgnoreCase("consistency")) {
			generateConsistency = true;
		}		
		
		Collection<String> sortedOntologyFileStringCollection = getSortedOntologiesList(ontologyFileStringCollection, ontologyIndexMap);
		int generatedQueryNumber = 0;
		
		for (String ontologyFileString : sortedOntologyFileStringCollection) {
			
			if (generatedQueryNumber >= maxQueryGenerationCount) {
				break;
			}
			
			if (ontologyFileString.startsWith("ore2015")) {
				
				FilePathString ontologyFilePathString = new FilePathString(ontologiesString,ontologyFileString,RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);
				
				mLogger.info("Generating queries for '{}'.",ontologyFilePathString);
	
				QueryExpressivity queryExpressivity = new OntologyExpressivityChecker(ontologyFilePathString.getAbsoluteFilePathString()).createQueryExpressivity();
				boolean validOntology = false;
				boolean elOntology = false;
				String correctedQueryOntologyFileString = ontologyFileString.replace("\\", "/");
				correctedQueryOntologyFileString = correctedQueryOntologyFileString.replace("ore2015/", "");
				if (queryExpressivity.isInELProfile()) {
					correctedQueryOntologyFileString = "ore2015/el/"+correctedQueryOntologyFileString;
					validOntology = true;
					elOntology = true;
					++elOntologies;
				} else if (queryExpressivity.isInDLProfile()) {
					correctedQueryOntologyFileString = "ore2015/dl/"+correctedQueryOntologyFileString;
					validOntology = true;
					elOntology = false;
					++dlOntologies;
				}
				
				if (elOntology && !generateForEL) {
					validOntology = false;
				}
				if (!elOntology && !generateForDL) {
					validOntology = false;
				}
	
				if (validOntology) {
					++validOntologies;
					
					
					if (generateClassification) {
						FilePathString classificationQueryFilePathString = new FilePathString(queriesString,mClassificationQuerySubDirectoryString+File.separator+correctedQueryOntologyFileString+"-classify.dat",RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
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
								++generatedQueryNumber;
							} catch (IOException e) {
								mLogger.error("Saving query '{}' to '{}' failed, got IOException '{}'.",new Object[]{classQuery,classificationQueryFilePathString,e.getMessage()});
							}				
							
						}
					}
					
					if (generateConsistency) {
						FilePathString consistencyQueryFilePathString = new FilePathString(queriesString,mConsistencyQuerySubDirectoryString+File.separator+correctedQueryOntologyFileString+"-consistency.dat",RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
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
								++generatedQueryNumber;
							} catch (IOException e) {
								mLogger.error("Saving query '{}' to '{}' failed, got IOException '{}'.",new Object[]{consQuery,consistencyQueryFilePathString,e.getMessage()});
							}				
							
						}
					}
					
					if (generateRealisation) {
						FilePathString realisationQueryFilePathString = new FilePathString(queriesString,mRealisationQuerySubDirectoryString+File.separator+correctedQueryOntologyFileString+"-realisation.dat",RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
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
									++generatedQueryNumber;
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
