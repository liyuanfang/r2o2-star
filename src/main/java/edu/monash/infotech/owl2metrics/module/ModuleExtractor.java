package edu.monash.infotech.owl2metrics.module;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import java.util.List;
import java.util.Set;

/**
 * @author Yuan-Fang Li
 * @version $Id: ModuleExtractor.java 148 2013-08-14 13:38:12Z yli $
 */
public class ModuleExtractor {
    private Logger logger = Logger.getLogger(getClass());
    private SyntacticLocalityModuleExtractor extractor;
    private OWLOntology ontology;

    public OWLOntology subtractModule(OWLOntologyManager manager, OWLOntology ontology, Set<OWLAxiom> axioms) {
        List<OWLOntologyChange> changes = manager.removeAxioms(ontology, axioms);
        logger.info(changes.size() + " axioms removed, sub ontology logical axiom # = " + ontology.getLogicalAxiomCount());
        return ontology;
    }

    public Set<OWLAxiom> extractModule(OWLOntologyManager manager, OWLOntology ontology,
                                       ModuleType type, Set<OWLEntity> signature) {
        logger.info("Ontology logical axiom # = " + ontology.getLogicalAxiomCount());
        if (this.ontology == null || !this.ontology.equals(ontology)) {
            this.ontology = ontology;
            extractor = new SyntacticLocalityModuleExtractor(manager, ontology, type);
        }
        Set<OWLAxiom> axioms = extractor.extract(signature);
        for (OWLOntology o : manager.getOntologies()) {
            if (!ontology.getOntologyID().equals(o.getOntologyID())) {
                manager.removeOntology(o);
            }
        }
        logger.info("Extracted axiom # = " + axioms.size());
        return axioms;
    }
}
