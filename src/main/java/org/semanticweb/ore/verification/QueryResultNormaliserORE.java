package org.semanticweb.ore.verification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.querying.Query;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.querying.QueryResultData;
import org.semanticweb.ore.querying.QueryType;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResultNormaliserORE implements QueryResultNormaliser {	
	
	final private static Logger mLogger = LoggerFactory.getLogger(QueryResultNormaliserORE.class);
	
	protected Config mConfig = null;
	protected boolean mConfWriteNormalisedOntologies = false;
	
	
	public QueryResultNormaliserORE(Config config) {
		mConfig = config;
		mConfWriteNormalisedOntologies = ConfigDataValueReader.getConfigDataValueBoolean(mConfig, ConfigType.CONFIG_TYPE_WRITE_NORMALISED_RESULTS, false);
	}
	

	@Override
	public QueryResultData getNormalisedResult(Query query, QueryResponse response) {
		
		OWLOntology ontology = null;
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
      	
        try {
			ontology = manager.createOntology(IRI.create("http://org.semanticweb.ore/normalised-result-comparision-ontology"));
		} catch (OWLOntologyCreationException e) {
		}
        
        
        if (query.getQueryType() == QueryType.QUERY_TYPE_CONSISTENCY) {
        	loadConsistencyResultDataIntoOntology(ontology,response);
        } else if (query.getQueryType() == QueryType.QUERY_TYPE_SATISFIABILITY) {
        	loadSatisfiabilityResultDataIntoOntology(ontology,response);
        } else if (query.getQueryType() == QueryType.QUERY_TYPE_CLASSIFICATION) {
        	loadClassificationResultDataIntoOntology(ontology,response);
        } else if (query.getQueryType() == QueryType.QUERY_TYPE_REALISATION) {
        	loadRealisationResultDataIntoOntology(ontology,response);
        } else if (query.getQueryType() == QueryType.QUERY_TYPE_ENTAILMENT) {
        	loadEntailmentResultDataIntoOntology(ontology,response);
        }           
        
        if (mConfWriteNormalisedOntologies) {
    		OWLOntologyFormat owlOntologyFormat = new OWLXMLOntologyFormat();
        	String normalisedResultDataFileString = response.getResultDataFilePathString()+"-normalised.owl.xml";
	        try {
				mLogger.info("Saving normalised result data to '{}'.",normalisedResultDataFileString);
				// does not close the output stream??
 				//manager.saveOntology(ontology,owlOntologyFormat,new FileDocumentTarget(new File(normalisedResultDataFileString)));
				

				manager.saveOntology(ontology,owlOntologyFormat,new FileOutputStream(new File(normalisedResultDataFileString)));
				
			} catch (Exception e) {
				mLogger.error("Failed to save normalised result data to '{}', got Exception '{}'.",normalisedResultDataFileString,e.getMessage());
			}
	        
	        
//        	try {
//				FileWriter axiomsHashCodesWriter = new FileWriter(response.getResultDataFilePathString()+"-axioms-hash-codes", false);
//				for (OWLAxiom axiom : ontology.getAxioms()) {
//					axiomsHashCodesWriter.write(axiom.toString().hashCode() + " << " + axiom.toString() + "\n");
//				}
//				axiomsHashCodesWriter.close();
//			} catch (IOException e) {
//			}	


        }
        

		return new OntologyPackedQueryResultData(ontology);
	}
	
	
	
	public boolean loadConsistencyResultDataIntoOntology(OWLOntology ontology, QueryResponse response) {
		boolean successfullyLoaded = false;
		FilePathString resultDataFilePathString = response.getResultDataFilePathString();
		
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		
		boolean foundConsistency = false;
		boolean foundInconsistency = false;
		boolean foundValue = false;
		
		try {
			
			FileInputStream inputStream = new FileInputStream(new File(resultDataFilePathString.getAbsoluteFilePathString()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));			
			String line = null;
			while (!foundValue && (line = reader.readLine()) != null) {
				String dataString = line.trim();
				if (!foundValue && dataString.equalsIgnoreCase("true")) {
					foundConsistency = true;
					foundValue = true;
				}
				if (!foundValue && dataString.equalsIgnoreCase("false")) {
					foundInconsistency = true;
					foundValue = true;
				}
			}
			reader.close();
			
			if (foundValue) {
				OWLClass topClass = dataFactory.getOWLThing();
				OWLClass bottomClass = dataFactory.getOWLNothing();
				OWLAxiom axiom = null;
				if (foundConsistency) {
					axiom = dataFactory.getOWLSubClassOfAxiom(bottomClass, topClass);					
				} else if (foundInconsistency) {					
					axiom = dataFactory.getOWLSubClassOfAxiom(topClass, bottomClass);					
				}
				if (axiom != null) {
					manager.addAxiom(ontology, axiom);
					successfullyLoaded = true;
				}
			}
			
			if (!successfullyLoaded) {			
				mLogger.error("Failed to load consistency result data from '{}'.",resultDataFilePathString);
			}
			
		} catch (Exception e) {
			mLogger.error("Failed to load consistency result data from '{}', got Exception '{}'.",resultDataFilePathString,e.getMessage());
		}
		

		
		return successfullyLoaded;
	}

	
	

	public boolean loadEntailmentResultDataIntoOntology(OWLOntology ontology, QueryResponse response) {
		boolean successfullyLoaded = false;
		FilePathString resultDataFilePathString = response.getResultDataFilePathString();
		
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		
		boolean foundConsistency = false;
		boolean foundInconsistency = false;
		boolean foundValue = false;
		
		try {
			
			FileInputStream inputStream = new FileInputStream(new File(resultDataFilePathString.getAbsoluteFilePathString()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));			
			String line = null;
			while (!foundValue && (line = reader.readLine()) != null) {
				String dataString = line.trim();
				if (!foundValue && dataString.equalsIgnoreCase("true")) {
					foundConsistency = true;
					foundValue = true;
				}
				if (!foundValue && dataString.equalsIgnoreCase("false")) {
					foundInconsistency = true;
					foundValue = true;
				}
			}
			reader.close();
			
			if (foundValue) {
				OWLClass topClass = dataFactory.getOWLThing();
				OWLClass bottomClass = dataFactory.getOWLNothing();
				OWLAxiom axiom = null;
				if (foundConsistency) {
					axiom = dataFactory.getOWLSubClassOfAxiom(bottomClass, topClass);					
				} else if (foundInconsistency) {					
					axiom = dataFactory.getOWLSubClassOfAxiom(topClass, bottomClass);					
				}
				if (axiom != null) {
					manager.addAxiom(ontology, axiom);
					successfullyLoaded = true;
				}
			}
			
			if (!successfullyLoaded) {			
				mLogger.error("Failed to load entailment result data from '{}'.",resultDataFilePathString);
			}
			
		} catch (Exception e) {
			mLogger.error("Failed to load entailment result data from '{}', got Exception '{}'.",resultDataFilePathString,e.getMessage());
		}
		

		
		return successfullyLoaded;
	}

	
	

	
	public boolean loadSatisfiabilityResultDataIntoOntology(OWLOntology ontology, QueryResponse response) {
		boolean successfullyLoaded = false;
		FilePathString resultDataFilePathString = response.getResultDataFilePathString();
		
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		

		ArrayList<String> satisfiableClassList = new ArrayList<String>();
		ArrayList<String> unsatisfiableClassList = new ArrayList<String>();
		boolean foundValues = false;
		
		try {
			
			FileInputStream inputStream = new FileInputStream(new File(resultDataFilePathString.getAbsoluteFilePathString()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));			
			String line = null;
			while ((line = reader.readLine()) != null) {
				String dataString = line.trim();
				int sepIndex = dataString.lastIndexOf(',');
				if (sepIndex != -1) {
					String satString = dataString.substring(sepIndex+1).trim();
					String classString = dataString.substring(0,sepIndex).trim();
				
					if (satString.equalsIgnoreCase("true")) {
						satisfiableClassList.add(classString);
						foundValues = true;
					}
					if (satString.equalsIgnoreCase("false")) {
						unsatisfiableClassList.add(classString);
						foundValues = true;
					}
				}
			}
			reader.close();
			
			if (foundValues) {
				OWLClass topClass = dataFactory.getOWLThing();
				OWLClass bottomClass = dataFactory.getOWLNothing();
				for (String classString : satisfiableClassList) {
					OWLClass namedClass = dataFactory.getOWLClass(IRI.create(classString));
					OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(namedClass, topClass);
					manager.addAxiom(ontology, axiom);
				}
				for (String classString : unsatisfiableClassList) {
					OWLClass namedClass = dataFactory.getOWLClass(IRI.create(classString));
					OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(namedClass, bottomClass);
					manager.addAxiom(ontology, axiom);
				}
				successfullyLoaded = true;

			}
			
			if (!successfullyLoaded) {			
				mLogger.error("Failed to load satisfiability result data from '{}'.",resultDataFilePathString);
			}
			
		} catch (Exception e) {
			mLogger.error("Failed to load satisfiability result data from '{}', got Exception '{}'.",resultDataFilePathString,e.getMessage());
		}
		

		
		return successfullyLoaded;
	}

		
	
	
	
	
	
	
	

	

	
	public boolean loadClassificationResultDataIntoOntology(OWLOntology ontology, QueryResponse response) {
		boolean successfullyLoaded = false;
		FilePathString resultDataFilePathString = response.getResultDataFilePathString();
		
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		
		OWLClass topClass = dataFactory.getOWLThing();
		OWLClass bottomClass = dataFactory.getOWLNothing();
		
		try {
			
			
	        OWLOntologyManager loadManager = OWLManager.createOWLOntologyManager();
	        
	        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
	        loaderConfig = loaderConfig.setLoadAnnotationAxioms(false);
	        loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
	    	
	        OWLOntology loadOntology = loadManager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(resultDataFilePathString.getAbsoluteFilePathString())),loaderConfig);
	        
	        ClassHierarchyReducer classHierarchyReducer = new ClassHierarchyReducer(ontology);
	        
	        for (OWLAxiom axiom : loadOntology.getAxioms()) {
	        	if (axiom.getAxiomType() == AxiomType.SUBCLASS_OF) {
	        		OWLSubClassOfAxiom subClassAxiom = (OWLSubClassOfAxiom)axiom;
	        		OWLClassExpression subClassExpression = subClassAxiom.getSubClass();
	        		OWLClassExpression superClassExpression = subClassAxiom.getSuperClass();
	        		OWLClass subClass = null;
	        		OWLClass superClass = null;
	        		if (subClassExpression instanceof OWLClass) {
	        			subClass = (OWLClass)subClassExpression;
	        		}
	        		if (superClassExpression instanceof OWLClass) {
	        			superClass = (OWLClass)superClassExpression;
	        		}
	        		if (subClass != null && superClass != null) {
	        			if (subClass != bottomClass) {
	        				classHierarchyReducer.addSubClassRelation(subClass, superClass);
	        			}
	        		}	        		
	        	} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
	        		OWLEquivalentClassesAxiom eqClassesAxiom = (OWLEquivalentClassesAxiom)axiom;
	        		if (eqClassesAxiom.contains(bottomClass)) {	        			
		        		for (OWLClassExpression classExp1 : eqClassesAxiom.getClassExpressions()) {
		        			OWLClass subClass = null;
         	        		if (classExp1 instanceof OWLClass) {
        	        			subClass = (OWLClass)classExp1;
        	        		}
		        			classHierarchyReducer.addSubClassRelation(subClass, bottomClass);
		        		}
	        		} else if (eqClassesAxiom.contains(topClass)) {	        			
		        		for (OWLClassExpression classExp1 : eqClassesAxiom.getClassExpressions()) {
		        			OWLClass superClass = null;
         	        		if (classExp1 instanceof OWLClass) {
         	        			superClass = (OWLClass)classExp1;
        	        		}
		        			classHierarchyReducer.addSubClassRelation(topClass,superClass);
		        		}
	        		} else {
		        		for (OWLClassExpression classExp1 : eqClassesAxiom.getClassExpressions()) {
		        			for (OWLClassExpression classExp2 : eqClassesAxiom.getClassExpressions()) {
		        				if (classExp1 != classExp2) {
		        	        		OWLClass subClass = null;
		        	        		OWLClass superClass = null;
		        	        		if (classExp1 instanceof OWLClass) {
		        	        			subClass = (OWLClass)classExp1;
		        	        		}
		        	        		if (classExp2 instanceof OWLClass) {
		        	        			superClass = (OWLClass)classExp2;
		        	        		}
		        	        		if (subClass != null && superClass != null) {
		        	        			classHierarchyReducer.addSubClassRelation(subClass, superClass);
		        	        		}	        		
		        				}
		        			}
		        		}
	        			
	        		}
	        	}
	        }
	        
	        
	        for (OWLClass owlClass : loadOntology.getClassesInSignature()) {
	        	if (!classHierarchyReducer.hasOWLClass(owlClass)) {
        			classHierarchyReducer.addSubClassRelation(owlClass, topClass);	        		
	        	}
	        }
	        
	        ontology = classHierarchyReducer.createReducedOntology();
	        successfullyLoaded = true;	        			
			
			
		} catch (Exception e) {
			mLogger.error("Failed to load classification result data from '{}', got Exception '{}'.",resultDataFilePathString,e.getMessage());
		}
		
		return successfullyLoaded;
	}

		
		

	

	
	public boolean loadRealisationResultDataIntoOntology(OWLOntology ontology, QueryResponse response) {
		boolean successfullyLoaded = false;
		FilePathString resultDataFilePathString = response.getResultDataFilePathString();
		
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		
		OWLClass topClass = dataFactory.getOWLThing();
		OWLClass bottomClass = dataFactory.getOWLNothing();
		
		try {
			
			
	        OWLOntologyManager loadManager = OWLManager.createOWLOntologyManager();
	        
	        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
	        loaderConfig = loaderConfig.setLoadAnnotationAxioms(false);
	        loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
	    	
	        OWLOntology loadOntology = loadManager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(resultDataFilePathString.getAbsoluteFilePathString())),loaderConfig);

	        


	        ClassHierarchyReducer classHierarchyReducer = new ClassHierarchyReducer(ontology);
	        
	        for (OWLAxiom axiom : loadOntology.getAxioms()) {
	        	if (axiom.getAxiomType() == AxiomType.SUBCLASS_OF) {
	        		OWLSubClassOfAxiom subClassAxiom = (OWLSubClassOfAxiom)axiom;
	        		OWLClassExpression subClassExpression = subClassAxiom.getSubClass();
	        		OWLClassExpression superClassExpression = subClassAxiom.getSuperClass();
	        		OWLClass subClass = null;
	        		OWLClass superClass = null;
	        		if (subClassExpression instanceof OWLClass) {
	        			subClass = (OWLClass)subClassExpression;
	        		}
	        		if (superClassExpression instanceof OWLClass) {
	        			superClass = (OWLClass)superClassExpression;
	        		}
	        		if (subClass != null && superClass != null) {
	        			if (subClass != bottomClass) {
	        				classHierarchyReducer.addSubClassRelation(subClass, superClass);
	        			}
	        		}	        		
	        	} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
	        		
	        	} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
	        		OWLEquivalentClassesAxiom eqClassesAxiom = (OWLEquivalentClassesAxiom)axiom;
	        		if (eqClassesAxiom.contains(bottomClass)) {	        			
		        		for (OWLClassExpression classExp1 : eqClassesAxiom.getClassExpressions()) {
		        			OWLClass subClass = null;
         	        		if (classExp1 instanceof OWLClass) {
        	        			subClass = (OWLClass)classExp1;
        	        		}
		        			classHierarchyReducer.addSubClassRelation(subClass, bottomClass);
		        		}
	        		} else if (eqClassesAxiom.contains(topClass)) {	        			
		        		for (OWLClassExpression classExp1 : eqClassesAxiom.getClassExpressions()) {
		        			OWLClass superClass = null;
         	        		if (classExp1 instanceof OWLClass) {
         	        			superClass = (OWLClass)classExp1;
        	        		}
		        			classHierarchyReducer.addSubClassRelation(topClass,superClass);
		        		}
	        		} else {	
			        	for (OWLClassExpression classExp1 : eqClassesAxiom.getClassExpressions()) {
		        			for (OWLClassExpression classExp2 : eqClassesAxiom.getClassExpressions()) {
		        				if (classExp1 != classExp2) {
		        	        		OWLClass subClass = null;
		        	        		OWLClass superClass = null;
		        	        		if (classExp1 instanceof OWLClass) {
		        	        			subClass = (OWLClass)classExp1;
		        	        		}
		        	        		if (classExp2 instanceof OWLClass) {
		        	        			superClass = (OWLClass)classExp2;
		        	        		}
		        	        		if (subClass != null && superClass != null) {
		        	        			classHierarchyReducer.addSubClassRelation(subClass, superClass);
		        	        		}	        		
		        				}
		        			}
		        		}
		        			
	        		}
	        	}
	        }
	        
	        for (OWLAxiom axiom : loadOntology.getAxioms()) {
	        	if (axiom.getAxiomType() == AxiomType.CLASS_ASSERTION) {
	        		OWLClassAssertionAxiom classAssertionAxiom = (OWLClassAssertionAxiom)axiom; 
	        		if (classAssertionAxiom.getClassExpression() == bottomClass) {	        			
	        			classHierarchyReducer.addSubClassRelation(topClass, bottomClass);
	        			break;
	        		}
	        	}
	        }
	        
 
	        
	        
	        if (!classHierarchyReducer.isInconsistent()) {
	        	
		        for (OWLAxiom axiom : loadOntology.getAxioms()) {
		        	if (axiom.getAxiomType() == AxiomType.CLASS_ASSERTION) {
		        		manager.addAxiom(ontology, axiom);
		        	}
		        }
		        
		        
		        for (OWLNamedIndividual owlIndividual : loadOntology.getIndividualsInSignature()) {
	        		OWLAxiom axiom = dataFactory.getOWLClassAssertionAxiom(topClass, owlIndividual);
	        		manager.addAxiom(ontology, axiom);
		        }
		        
	        } else {	        	
        		OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(topClass, bottomClass);
        		manager.addAxiom(ontology, axiom);
	        }
	        
	        successfullyLoaded = true;	        			
			
			
		} catch (Exception e) {
			mLogger.error("Failed to load realisation result data from '{}', got Exception '{}'.",resultDataFilePathString,e.getMessage());
		}
		
		return successfullyLoaded;
	}

		
		
	
	
	
}
