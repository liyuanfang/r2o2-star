package edu.monash.infotech.r2o2.reasoner;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;

/**
 * Created by ybkan on 20/07/2016.
 */
public class ReasonerRunner {

    private ReasonerWrapper.REASONER_ID _reasonerId;
    private String _inputOntFileName;

    private OWLManager _owlManager;
    private OWLOntology _owlOntology;
    private OWLReasoner _owlReasoner;
    private OWLReasonerFactory _owlReasonerFactory;
    private ReasonerWrapper _reasonerWrapper;

    public ReasonerRunner(ReasonerWrapper.REASONER_ID reasonerId) {
        _reasonerId = reasonerId;
    }

    public void setOntology (String inputOntFileName) {
        _inputOntFileName = inputOntFileName;
    }

    public void createReasoner() throws Exception {

        // create an ontology manager
        OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();

        // load ontology
        _owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(new File(_inputOntFileName));

        // create a reasoner wrapper
        _reasonerWrapper = new ReasonerWrapper("http://localhost:8081");

        final ReasonerWrapper.ReasonerBundle bundle = _reasonerWrapper.getBundle(_reasonerId);
        _owlReasoner = bundle.factory.createReasoner(_owlOntology, bundle.config);
    }

    public boolean doClassification() {

        try {
            boolean consistent = _owlReasoner.isConsistent();
            if (consistent) {
                _owlReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
                for (OWLClass cls : _owlOntology.getClassesInSignature(true)) {
                    _owlReasoner.getSuperClasses(cls, true);
                }
            } else {
                System.out.println("Ont inconsistent: " + _owlOntology.getOntologyID());
                return false;
            }
            _owlReasoner.dispose();
        } catch (Exception e) {
            System.err.println("Ont error: " + _owlOntology.getOntologyID());
            return false;
        }

        return true;
    }
}
