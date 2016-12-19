package ore.testing;

import ore.verification.ClassHierarchyReducer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.FileOutputStream;

public class NormalisationTest {

	public static void main(String[] args) {
		
		String ontologyFileString = "query-result-data.owl";

		OWLOntologyFormat owlOntologyFormat = new OWLXMLOntologyFormat();
    	String normalisedResultDataFileString = "query-result-data-normalised.owl.xml";

		
		OWLOntology ontology = null;
        OWLOntologyManager resultOntoManager = OWLManager.createOWLOntologyManager();
      	
        try {
			ontology = resultOntoManager.createOntology(IRI.create("http://ore/normalised-result-comparision-ontology"));
		} catch (OWLOntologyCreationException e) {
		}
		
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		
		OWLClass topClass = dataFactory.getOWLThing();
		OWLClass bottomClass = dataFactory.getOWLNothing();
		
		try {
			
			
	        OWLOntologyManager loadManager = OWLManager.createOWLOntologyManager();
	        
	        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
	        loaderConfig = loaderConfig.setLoadAnnotationAxioms(false);
	        loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
	    	
	        OWLOntology loadOntology = loadManager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(ontologyFileString)),loaderConfig);
	        
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
	        
	        
	        resultOntoManager.saveOntology(ontology,owlOntologyFormat,new FileOutputStream(new File(normalisedResultDataFileString)));
			
			
		} catch (Exception e) {
		}
	
	}

}
