package edu.monash.infotech.owl2metrics.translate;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLObjectVisitorExAdapter;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.*;

/**
 * @author Yuan-Fang Li
 * @version $Id: OWLUnaryPropertyAxiomTyper.java 10 2012-02-22 05:36:48Z yli $
 */
public class OWLUnaryPropertyAxiomTyper extends OWLObjectVisitorExAdapter<OWLRDFVocabulary> implements OWLAxiomVisitorEx<OWLRDFVocabulary> {
    @Override
    public OWLRDFVocabulary visit(OWLSubClassOfAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        return OWL_ASYMMETRIC_PROPERTY;
    }

    @Override
    public OWLRDFVocabulary visit(OWLReflexiveObjectPropertyAxiom axiom) {
        return OWL_REFLEXIVE_PROPERTY;
    }

    @Override
    public OWLRDFVocabulary visit(OWLDisjointClassesAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLDataPropertyDomainAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLObjectPropertyDomainAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLDifferentIndividualsAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLDisjointDataPropertiesAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLDisjointObjectPropertiesAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLObjectPropertyRangeAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLObjectPropertyAssertionAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLFunctionalObjectPropertyAxiom axiom) {
        return OWL_FUNCTIONAL_PROPERTY;
    }

    @Override
    public OWLRDFVocabulary visit(OWLSubObjectPropertyOfAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLDisjointUnionAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLSymmetricObjectPropertyAxiom axiom) {
        return OWL_SYMMETRIC_PROPERTY;
    }

    @Override
    public OWLRDFVocabulary visit(OWLDataPropertyRangeAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLFunctionalDataPropertyAxiom axiom) {
        return OWL_FUNCTIONAL_PROPERTY;
    }

    @Override
    public OWLRDFVocabulary visit(OWLEquivalentDataPropertiesAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLClassAssertionAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLEquivalentClassesAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLDataPropertyAssertionAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLTransitiveObjectPropertyAxiom axiom) {
        return OWL_TRANSITIVE_PROPERTY;
    }

    @Override
    public OWLRDFVocabulary visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        return OWL_IRREFLEXIVE_PROPERTY;
    }

    @Override
    public OWLRDFVocabulary visit(OWLSubDataPropertyOfAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        return OWL_INVERSE_FUNCTIONAL_PROPERTY;
    }

    @Override
    public OWLRDFVocabulary visit(OWLSameIndividualAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLSubPropertyChainOfAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLInverseObjectPropertiesAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(OWLHasKeyAxiom axiom) {
        return null;
    }

    @Override
    public OWLRDFVocabulary visit(SWRLRule rule) {
        return null;
    }
}
