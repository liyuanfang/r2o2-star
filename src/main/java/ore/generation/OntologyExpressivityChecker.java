package ore.generation;

import ore.querying.QueryExpressivity;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class OntologyExpressivityChecker {
	
	final private static Logger mLogger = LoggerFactory.getLogger(OntologyExpressivityChecker.class);

	private OWLOntology mOntology = null;
	
	OntologyExpressivityChecker(String ontologyFileString) {
		try {
	        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	        
	        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
	        loaderConfig = loaderConfig.setLoadAnnotationAxioms(false);
	        loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
	    	
	        mOntology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(ontologyFileString)),loaderConfig);
		} catch (Exception e) {
			mLogger.error("Loading of ontology '{}' failed, got Exception '{}'.",ontologyFileString,e.getMessage());
		}		
	}
	
	
	
	public boolean isInQL() {
		OWL2QLProfile qlProfile = new OWL2QLProfile();
		return qlProfile.checkOntology(mOntology).isInProfile();
	}
	

	public boolean isInRL() {
		OWL2RLProfile rlProfile = new OWL2RLProfile();
		return rlProfile.checkOntology(mOntology).isInProfile();
	}
	

	public boolean isInDL() {
		OWL2DLProfile dlProfile = new OWL2DLProfile();
		return dlProfile.checkOntology(mOntology).isInProfile();
	}
	

	public boolean isInEL() {
		OWL2ELProfile elProfile = new OWL2ELProfile();
		return elProfile.checkOntology(mOntology).isInProfile();
	}	
	
	public boolean usingDatatypes() {
		return mOntology.getDataPropertiesInSignature(true).size() > 0;
	}	
	
	public boolean usingRules() {
		return mOntology.getAxioms(AxiomType.SWRL_RULE).size() > 0;
	}	
	
	
	
	public boolean hasIndividuals() {
		return mOntology.getIndividualsInSignature(true).size() > 0;
	}		
	
	QueryExpressivity createQueryExpressivity() {
		boolean inDLProfile = false;
		boolean inELProfile = false;
		boolean inRLProfile = false;
		boolean inQLProfile = false;
		boolean usingDatatypes = false;
		boolean usingRules = false;
		
		if (mOntology != null) {
			inDLProfile = isInDL();
			inELProfile = isInEL();
			inRLProfile = isInRL();
			inQLProfile = isInQL();
			usingDatatypes = usingDatatypes();
			usingRules = usingRules();
		}
		
		QueryExpressivity queryExpressivity = new QueryExpressivity(inDLProfile, inELProfile, inRLProfile, inQLProfile, usingDatatypes, usingRules);
		return queryExpressivity;
	}

}
