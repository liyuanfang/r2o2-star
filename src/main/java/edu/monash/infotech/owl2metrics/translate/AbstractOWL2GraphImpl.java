package edu.monash.infotech.owl2metrics.translate;

import edu.monash.infotech.owl2metrics.translate.all.ClassExpNodeAdder;
import edu.monash.infotech.owl2metrics.translate.all.NodeRelAddingVisitor;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedNode;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLNaryIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyRange;
import org.semanticweb.owlapi.model.OWLPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLQuantifiedRestriction;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;
import org.semanticweb.owlapi.model.OWLUnaryPropertyAxiom;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_DATATYPE;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_DATA_PROPERTY;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_DIFFERENT_FROM;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_DISJOINT_WITH;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_EQUIVALENT_PROPERTY;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_INDIVIDUAL;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_OBJECT_PROPERTY;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_SAME_AS;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_DOMAIN;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_RANGE;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_SUBCLASS_OF;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_SUB_PROPERTY_OF;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public abstract class AbstractOWL2GraphImpl<T, Node, Edge> implements OWL2Graph<T, Node, Edge> {
    protected Logger logger = Logger.getLogger(getClass());

    protected OWLOntology ontology;
    
    protected NodeRelAddingVisitor<Node, Edge> adder;
    protected ClassExpNodeAdder<Node, Edge> ceAdder;
    protected OWLUnaryPropertyAxiomTyper propTyper;
    
    protected Node thingNode;

    public AbstractOWL2GraphImpl() {
        propTyper = new OWLUnaryPropertyAxiomTyper();
    }

    @Override
    public Node findOrCreateClass(String name, String type, boolean isAnonymous) {
        Node node = findOrCreateNode(name, OWL_CLASS_NAME, isAnonymous);
        if (isAnonymous) {
            addSubclassRel(node, thingNode);
            setProperty(node, ANON_CLASS_TYPE, type);
        }
        return node;
    }

    @Override
    public <P extends OWLPropertyExpression<?, ?>, T extends OWLUnaryPropertyAxiom<P>> void handleUnaryPropAxiom(T axiom) {
        OWLPropertyExpression property = axiom.getProperty();
        String type;
        if (property.isObjectPropertyExpression()) {
            type = OWL_OBJECT_PROPERTY.toString();
        } else {
            type = OWL_DATA_PROPERTY.toString();
        }
        Node node = findOrCreateNode(property.toString(), type, property.isAnonymous());
        OWLRDFVocabulary voc = axiom.accept(propTyper);
        //setProperty(node, PROPERTY_KIND, voc.toString());
        Set<String> kinds = (Set<String>) ((NamedNode) node).getProperties().get(PROPERTY_KIND);
        if (kinds == null) {
            kinds = new HashSet<String>();
        }
        kinds.add(voc.toString());
        ((NamedNode) node).getProperties().put(PROPERTY_KIND, kinds);
        //createRelationship(node, node, voc.toString());
    }

    @Override
    public <T extends OWLQuantifiedRestriction> Node handleQuantifiedExp(T ce, String type) {
        Node anonNode = findOrCreateClass(ce.toString(), type, true);
        Node fillerNode;
        if (ce.isObjectRestriction()) {
            fillerNode = ((OWLClassExpression) ce.getFiller()).accept(ceAdder);
        } else {
            OWLDataRange filler = (OWLDataRange) ce.getFiller();
            fillerNode = findOrCreateNode(filler.toString(), OWL_DATATYPE.toString(), !filler.isDatatype());
        }
        createRelationship(anonNode, fillerNode, ce.getProperty());
        return anonNode;
    }

    @Override
    public <T extends OWLPropertyExpression<?, ?>> void handleSubPropertyAxiom(OWLSubPropertyAxiom<T> axiom) {
        T subProperty = axiom.getSubProperty();
        Node subNode = findOrCreateNode(subProperty.toString(), OWL_OBJECT_PROPERTY.toString(), subProperty.isAnonymous());
        T superProperty = axiom.getSuperProperty();
        Node supNode = findOrCreateNode(superProperty.toString(), OWL_OBJECT_PROPERTY.toString(), superProperty.isAnonymous());
        createRelationship(subNode, supNode, RDFS_SUB_PROPERTY_OF.toString());
    }

    @Override
    public <P extends OWLPropertyExpression<?, ?>, R extends OWLPropertyRange, T extends OWLPropertyRangeAxiom<P, R>> void handleRanAxiom(T axiom) {
        P property = axiom.getProperty();
        String type;
        Node objNode;
        if (property.isObjectPropertyExpression()) {
            type = OWL_OBJECT_PROPERTY.toString();
            objNode = ((OWLClassExpression) axiom.getRange()).accept(ceAdder);

        } else {
            type = OWL_DATA_PROPERTY.toString();
            OWLDataRange range = (OWLDataRange) axiom.getRange();
            objNode = findOrCreateNode(range.toString(), OWL_DATATYPE.toString(), !range.isDatatype());
        }
        Node sbjNode = findOrCreateNode(property.toString(), type, property.isAnonymous());
        createRelationship(sbjNode, objNode, RDFS_RANGE.toString());
    }

    @Override
    public <P extends OWLPropertyExpression<?, ?>, T extends OWLNaryPropertyAxiom<P>> void handleNaryPropertiesAxiom(T axiom) {
        String type, rel;
        if (axiom instanceof OWLEquivalentObjectPropertiesAxiom) {
            type = OWL_OBJECT_PROPERTY.toString();
            rel = OWL_EQUIVALENT_PROPERTY.toString();
        } else if (axiom instanceof OWLEquivalentDataPropertiesAxiom) {
            type = OWL_DATA_PROPERTY.toString();
            rel = OWL_EQUIVALENT_PROPERTY.toString();
        } else if (axiom instanceof OWLDisjointObjectPropertiesAxiom) {
            type = OWL_OBJECT_PROPERTY.toString();
            rel = OWL_DISJOINT_WITH.toString();
        } else if (axiom instanceof OWLDisjointDataPropertiesAxiom) {
            type = OWL_DATA_PROPERTY.toString();
            rel = OWL_DISJOINT_WITH.toString();
        } else if (axiom instanceof OWLInverseObjectPropertiesAxiom) {
            type = OWL_OBJECT_PROPERTY.toString();
            rel = OWLRDFVocabulary.OWL_INVERSE_OF.toString();
        } else {
            throw new IllegalArgumentException("Unsupported axiom type: " + axiom);
        }
        List<P> list = new ArrayList<P>(axiom.getProperties());
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                P subject = list.get(i);
                P object = list.get(j);
                Node sbjNode = findOrCreateNode(subject.toString(), type, subject.isAnonymous());
                Node objNode = findOrCreateNode(object.toString(), type, object.isAnonymous());
                createRelationship(sbjNode, objNode, rel);
            }
        }
    }

    @Override
    public <T extends OWLNaryIndividualAxiom> void handleNarIndAxiom(T axiom) {
        String type;
        Set<? extends OWLNaryIndividualAxiom> pairs;
        if (axiom instanceof OWLDifferentIndividualsAxiom) {
            type = OWL_DIFFERENT_FROM.toString();
            pairs = ((OWLDifferentIndividualsAxiom) axiom).asPairwiseAxioms();
        } else {
            type = OWL_SAME_AS.toString();
            pairs = ((OWLSameIndividualAxiom) axiom).asPairwiseAxioms();
        }
        for (OWLNaryIndividualAxiom ax : pairs) {
            List<OWLIndividual> indList = new ArrayList<OWLIndividual>(ax.getIndividuals());
            OWLIndividual subject = indList.get(0);
            Node sbjNode = findOrCreateNode(subject.toString(), OWL_INDIVIDUAL.toString(), subject.isAnonymous());
            OWLIndividual object = indList.get(1);
            Node objNode = findOrCreateNode(object.toString(), OWL_INDIVIDUAL.toString(), object.isAnonymous());
            createRelationship(sbjNode, objNode, type);
        }
    }

    @Override
    public <P extends OWLPropertyExpression<?, ?>, T extends OWLPropertyDomainAxiom<P>> void handleDomAxiom(T axiom) {
        P property = axiom.getProperty();
        String type;
        if (property.isObjectPropertyExpression()) {
            type = OWL_OBJECT_PROPERTY.toString();
        } else {
            type = OWL_DATA_PROPERTY.toString();
        }

        Node sbjNode = findOrCreateNode(property.toString(), type, property.isAnonymous());
        Node objNode = axiom.getDomain().accept(ceAdder);
        createRelationship(sbjNode, objNode, RDFS_DOMAIN.toString());
    }

    @Override
    public <T extends OWLCardinalityRestriction> Node handleCardExpression(T ce, String type) {
        Node anonNode = findOrCreateClass(ce.toString(), type, true);

        anonNode  = setProperty(anonNode, CARDINALITY, Integer.toString(ce.getCardinality()));
        if (ce.isQualified()) {
            Node fillerNode;
            if (ce.isObjectRestriction()) {
                OWLClassExpression filler = (OWLClassExpression) ce.getFiller();
                fillerNode = filler.accept(ceAdder);
            } else {
                OWLDataRange filler = (OWLDataRange) ce.getFiller();
                fillerNode = findOrCreateNode(filler.toString(), OWL_DATATYPE.toString(), !filler.isDatatype());
            }
            createRelationship(anonNode, fillerNode, ce.getProperty());
        }
        return anonNode;
    }

    @Override
    public void addSubclassRel(Node subNode, Node supNode) {
        createRelationship(subNode, supNode, RDFS_SUBCLASS_OF.toString());
    }

    @Override
    public Node getThingNode() {
        return thingNode;
    }

    @Override
    public ClassExpNodeAdder<Node, Edge> getClassExpNodeAdder() {
        return ceAdder;
    }

    @Override
    public OWLOntology getOntology() {
        return ontology;
    }
}
