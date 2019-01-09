package org.semanticweb.ore.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
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

public class ORE2014SelectionOntologiesCopier {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ORE2014SelectionOntologiesCopier.class);

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
		int copiedOntologyNumber = 0;
		
		for (String ontologyFileString : sortedOntologyFileStringCollection) {
			
			if (copiedOntologyNumber >= maxQueryGenerationCount) {
				break;
			}
			
			if (ontologyFileString.startsWith("ore2014") && !ontologyFileString.startsWith("ore2014-")) {
				
				FilePathString ontologyFilePathString = new FilePathString(ontologiesString,ontologyFileString,RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);
				
				mLogger.info("Copy ontology for '{}'.",ontologyFilePathString);
	
				QueryExpressivity queryExpressivity = new OntologyExpressivityChecker(ontologyFilePathString.getAbsoluteFilePathString()).createQueryExpressivity();
				boolean validOntology = false;
				boolean elOntology = false;
				String correctedQueryOntologyFileString = ontologyFileString.replace("\\", "/");
				correctedQueryOntologyFileString = correctedQueryOntologyFileString.replace("ore2014/", "");
				if (queryExpressivity.isInELProfile()) {
					correctedQueryOntologyFileString = "el/"+correctedQueryOntologyFileString;
					validOntology = true;
					elOntology = true;
					++elOntologies;
				} else if (queryExpressivity.isInDLProfile()) {
					correctedQueryOntologyFileString = "dl/"+correctedQueryOntologyFileString;
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
						++copiedOntologyNumber;
						FilePathString classificationCopiedFilePathString = new FilePathString(ontologiesString,"ore2014-copied"+File.separator+mClassificationQuerySubDirectoryString+File.separator+correctedQueryOntologyFileString,RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);
						FileSystemHandler.ensurePathToFileExists(classificationCopiedFilePathString);
						try {
							Files.copy(new File(ontologyFilePathString.getAbsoluteFilePathString()).toPath(), new File(classificationCopiedFilePathString.getAbsoluteFilePathString()).toPath());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					if (generateConsistency) {
						++copiedOntologyNumber;
						FilePathString consistencyCopiedFilePathString = new FilePathString(ontologiesString,"ore2014-copied"+File.separator+mConsistencyQuerySubDirectoryString+File.separator+correctedQueryOntologyFileString,RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);
						FileSystemHandler.ensurePathToFileExists(consistencyCopiedFilePathString);
						try {
							Files.copy(new File(ontologyFilePathString.getAbsoluteFilePathString()).toPath(), new File(consistencyCopiedFilePathString.getAbsoluteFilePathString()).toPath());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					if (generateRealisation) {
						++copiedOntologyNumber;
						FilePathString realisationCopiedFilePathString = new FilePathString(ontologiesString,"ore2014-copied"+File.separator+mRealisationQuerySubDirectoryString+File.separator+correctedQueryOntologyFileString,RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);
						FileSystemHandler.ensurePathToFileExists(realisationCopiedFilePathString);
						try {
							Files.copy(new File(ontologyFilePathString.getAbsoluteFilePathString()).toPath(), new File(realisationCopiedFilePathString.getAbsoluteFilePathString()).toPath());
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
			}
			
		}		
		mLogger.info("Copying ontologies for '{}' completed.",ontologiesString);
		
		mLogger.info("Found {} valid ontologies.",validOntologies);
		mLogger.info("Found {} valid EL ontologies.",elOntologies);
		mLogger.info("Found {} valid DL ontologies.",dlOntologies);
		
		mLogger.info("Copied {} DL classification ontologies.",dlClassificationQueries);
		mLogger.info("Copied {} EL classification ontologies.",elClassificationQueries);
		
		mLogger.info("Copied {} DL realisation ontologies.",dlRealisationQueries);
		mLogger.info("Copied {} EL realisation ontologies.",elRealisationQueries);
		
		mLogger.info("Copied {} DL consistency ontologies.",dlConsistencyQueries);
		mLogger.info("Copied {} EL consistency ontologies.",elConsistencyQueries);
		
		
	}
}
