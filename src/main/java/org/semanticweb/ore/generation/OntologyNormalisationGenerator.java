package org.semanticweb.ore.generation;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;

import org.semanticweb.ore.utilities.FileSystemHandler;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyNormalisationGenerator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(OntologyNormalisationGenerator.class);

	public static void main(String[] args) {

		
		String currentDirectory = System.getProperty("user.dir")+File.separator;
		
		String inputDirectoryString = currentDirectory+"data"+File.separator+"normalisations"+File.separator+"input"+File.separator;
		String outputDirectoryString = currentDirectory+"data"+File.separator+"normalisations"+File.separator+"output"+File.separator;
		
		mLogger.info("Normalise ontologies in '{}'.",inputDirectoryString);
		
		Collection<String> ontologyFileStringCollection = FileSystemHandler.collectRelativeFilesInSubdirectories(inputDirectoryString);
		
		for (String ontologyFileString : ontologyFileStringCollection) {	
			
			String ontologyInputFileString = inputDirectoryString+ontologyFileString;
			String ontologyOutputFileString = outputDirectoryString+ontologyFileString;
			
			mLogger.info("Normalising ontology '{}'.",ontologyFileString);

			FileSystemHandler.ensurePathToFileExists(ontologyOutputFileString);
			
			try {
		        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		        
		        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
		        loaderConfig = loaderConfig.setLoadAnnotationAxioms(false);
		        loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
		    	
		        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(ontologyInputFileString)),loaderConfig);		
		        
		        OWLOntologyMerger ontoMerger = new OWLOntologyMerger(manager);
		        
		        OWLOntologyManager managerMerged = OWLManager.createOWLOntologyManager();
		        OWLDataFactory factoryMerged = managerMerged.getOWLDataFactory();
		        
		        OWLOntology mergedOntologie = ontoMerger.createMergedOntology(managerMerged,ontology.getOntologyID().getOntologyIRI()); 
		        for (OWLClass owlClass : mergedOntologie.getClassesInSignature()) {
		        	OWLDeclarationAxiom decAxiom = factoryMerged.getOWLDeclarationAxiom(owlClass);
		        	managerMerged.addAxiom(mergedOntologie, decAxiom);		        	
		        }
		        for (OWLObjectProperty owlProperty : mergedOntologie.getObjectPropertiesInSignature()) {
		        	OWLDeclarationAxiom decAxiom = factoryMerged.getOWLDeclarationAxiom(owlProperty);
		        	managerMerged.addAxiom(mergedOntologie, decAxiom);		        	
		        }
		        for (OWLDataProperty owlProperty : mergedOntologie.getDataPropertiesInSignature()) {
		        	OWLDeclarationAxiom decAxiom = factoryMerged.getOWLDeclarationAxiom(owlProperty);
		        	managerMerged.addAxiom(mergedOntologie, decAxiom);		        	
		        }
		        
		        OWLXMLOntologyFormat owlOntologyFormat = new OWLXMLOntologyFormat();
		        manager.saveOntology(mergedOntologie,owlOntologyFormat,new FileOutputStream(new File(ontologyOutputFileString)));
		        
		        mLogger.info("Saved normalised ontology to '{}'.",ontologyFileString);
		        
			} catch (Exception e) {
			}

			
		}		
		mLogger.info("Normalising of ontologies for '{}' completed.",inputDirectoryString);
		
	}
	


}
