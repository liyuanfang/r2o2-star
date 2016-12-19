package edu.monash.infotech.owl2metrics.translate;

import edu.monash.infotech.owl2metrics.translate.all.ClassExpNodeAdder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.util.Map;

import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_CLASS;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public interface OWL2Graph<T, Node, Edge> {
    String NODE_EDGE_NAME = "name";
    String NODE_TYPE_NAME = "type";
    String NODE_ANON_TYPE_NAME = "isAnon";
    String PROP_NODE_BUILDIN = "builtin";
    String PROPERTY_CHAIN_POSITION = "chain_position";
    String ANON_CLASS_TYPE = "anon_class_type";
    String OPERAND = "has_operand";
    String CARDINALITY = "has_cardinality";
    String VALUE = "has_value";
    String NODE_INDEX = "node";
    String CLASS_INDEX = "class";
    String EDGE_INDEX = "edge";
    String PROPERTY_KIND = "prop_kind";
    String DEPENDS_ON = "depends_on";
    String IS_EL = "is_el";

    OWLClass OWL_THING = OWLManager.getOWLDataFactory().getOWLThing();
    String OWL_CLASS_NAME = OWL_CLASS.toString();

    T loadOWLOntology(OWLOntologyDocumentSource source, boolean includeImports) throws OWLOntologyCreationException;

    T loadOWLOntology(OWLOntologyDocumentSource source, boolean includeImports, Map<String, String> iriMapping)
            throws OWLOntologyCreationException;

    T loadOWLOntology(OWLOntology ontology, boolean includeImports);

    T getGraph();

    Node findOrCreateClass(String name, String type, boolean isAnonymous);

    Node findOrCreateNode(String name, String type, boolean isAnonymous);

    Node createNode(String name, String type, boolean isAnonymous);

    Edge createRelationship(Node source, Node target, String typeAndName);

    Edge findRelationship(Node source, Node target, String relationshipName);

    <T extends OWLPropertyExpression> Edge createRelationship(Node source, Node target, T property);

    <T extends OWLQuantifiedRestriction> Node handleQuantifiedExp(T ce, String type);

    <T extends OWLCardinalityRestriction> Node handleCardExpression(T ce, String type);

    <P extends OWLPropertyExpression<?, ?>, S extends OWLUnaryPropertyAxiom<P>> void handleUnaryPropAxiom(S axiom);

    <P extends OWLPropertyExpression<?, ?>, T extends OWLPropertyDomainAxiom<P>> void handleDomAxiom(T axiom);

    <P extends OWLPropertyExpression<?, ?>, T extends OWLNaryPropertyAxiom<P>> void handleNaryPropertiesAxiom(T axiom);

    <T extends OWLNaryIndividualAxiom> void handleNarIndAxiom(T axiom);

    <P extends OWLPropertyExpression<?, ?>, R extends OWLPropertyRange, T extends OWLPropertyRangeAxiom<P, R>> void handleRanAxiom(T axiom);

    <T extends OWLPropertyExpression<?, ?>> void handleSubPropertyAxiom(OWLSubPropertyAxiom<T> axiom);

    ClassExpNodeAdder<Node, Edge> getClassExpNodeAdder();

    void shutdown();

    Node createNode(String name);

    Node setProperty(Node node, String key, String value);

    void addSubclassRel(Node subNode, Node supNode);

    Node getThingNode();

    OWLOntology getOntology();
}