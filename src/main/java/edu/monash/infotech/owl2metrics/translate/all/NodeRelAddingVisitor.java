package edu.monash.infotech.owl2metrics.translate.all;

import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.util.List;

import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.*;

/**
 * @author Yuan-Fang Li
 * @version $Id: NodeRelAddingVisitor.java 104 2012-10-29 13:18:11Z yli $
 */
public class NodeRelAddingVisitor<Node, Relationship> extends OWLAxiomVisitorAdapter implements OWLLogicalAxiomVisitor {
    protected OWL2Graph<?, Node, Relationship> owl2Graph;

    protected Logger logger = Logger.getLogger(getClass());

    public NodeRelAddingVisitor(OWL2Graph<?, Node, Relationship> owl2Graph) {
        this.owl2Graph = owl2Graph;
    }

    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        OWLClassExpression subClass = axiom.getSubClass();
        OWLClassExpression superClass = axiom.getSuperClass();
        Node subClsNode = subClass.accept(owl2Graph.getClassExpNodeAdder());
        Node supClsNode = superClass.accept(owl2Graph.getClassExpNodeAdder());
        owl2Graph.createRelationship(subClsNode, supClsNode, RDFS_SUBCLASS_OF.toString());
    }

    @Override
    public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        OWLIndividual subject = axiom.getSubject();
        OWLIndividual object = axiom.getObject();
        subject.toStringID();
        Node sbjNode = owl2Graph.findOrCreateNode(subject.toString(), OWL_INDIVIDUAL.toString(), subject.isAnonymous());
        Node objNode = owl2Graph.findOrCreateNode(object.toString(), OWL_INDIVIDUAL.toString(), object.isAnonymous());
        owl2Graph.createRelationship(sbjNode, objNode, axiom.getProperty());
    }

    @Override
    public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLDisjointClassesAxiom axiom) {
        for (OWLDisjointClassesAxiom ax : axiom.asPairwiseAxioms()) {
            List<OWLClassExpression> list = ax.getClassExpressionsAsList();
            Node sbjNode = list.get(0).accept(owl2Graph.getClassExpNodeAdder());
            Node objNode = list.get(1).accept(owl2Graph.getClassExpNodeAdder());
            owl2Graph.createRelationship(sbjNode, objNode, OWL_DISJOINT_WITH.toString());
        }
    }

    @Override
    public void visit(OWLDataPropertyDomainAxiom axiom) {
        owl2Graph.handleDomAxiom(axiom);
    }

    @Override
    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        owl2Graph.handleDomAxiom(axiom);
    }

    @Override
    public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        owl2Graph.handleNaryPropertiesAxiom(axiom);
    }

    @Override
    public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        OWLDataPropertyExpression property = axiom.getProperty();
        OWLIndividual subject = axiom.getSubject();
        Node sbjNode = owl2Graph.findOrCreateNode(subject.toString(), OWL_INDIVIDUAL.toString(), subject.isAnonymous());
        OWLLiteral object = axiom.getObject();
        Node objNode = owl2Graph.findOrCreateNode(object.toString(), OWL_INDIVIDUAL.toString(), object.isBoolean());
        owl2Graph.createRelationship(sbjNode, objNode, property);
    }

    @Override
    public void visit(OWLDifferentIndividualsAxiom axiom) {
        owl2Graph.handleNarIndAxiom(axiom);
    }

    @Override
    public void visit(OWLDisjointDataPropertiesAxiom axiom) {
        owl2Graph.handleNaryPropertiesAxiom(axiom);
    }

    @Override
    public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
        owl2Graph.handleNaryPropertiesAxiom(axiom);
    }

    @Override
    public void visit(OWLObjectPropertyRangeAxiom axiom) {
        owl2Graph.handleRanAxiom(axiom);
    }

    @Override
    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        OWLIndividual subject = axiom.getSubject();
        Node sbjNode = owl2Graph.findOrCreateNode(subject.toString(), OWL_INDIVIDUAL.toString(), subject.isAnonymous());
        OWLIndividual object = axiom.getObject();
        Node objNode = owl2Graph.findOrCreateNode(object.toString(), OWL_INDIVIDUAL.toString(), object.isAnonymous());
        owl2Graph.createRelationship(sbjNode, objNode, axiom.getProperty());
    }

    @Override
    public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLSubObjectPropertyOfAxiom axiom) {
        owl2Graph.handleSubPropertyAxiom(axiom);
    }

    @Override
    public void visit(OWLDisjointUnionAxiom axiom) {
        axiom.getOWLEquivalentClassesAxiom().accept(this);
        axiom.getOWLDisjointClassesAxiom().accept(this);
    }

    @Override
    public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLDataPropertyRangeAxiom axiom) {
        owl2Graph.handleRanAxiom(axiom);
    }

    @Override
    public void visit(OWLFunctionalDataPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
        owl2Graph.handleNaryPropertiesAxiom(axiom);
    }

    @Override
    public void visit(OWLClassAssertionAxiom axiom) {
        Node clsNode = axiom.getClassExpression().accept(owl2Graph.getClassExpNodeAdder());
        OWLIndividual individual = axiom.getIndividual();
        Node indNode = owl2Graph.findOrCreateNode(individual.toString(), OWL_INDIVIDUAL.toString(), individual.isAnonymous());
        owl2Graph.createRelationship(indNode, clsNode, RDF_TYPE.toString());
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        for (OWLEquivalentClassesAxiom pair : axiom.asPairwiseAxioms()) {
            List<OWLClassExpression> list = pair.getClassExpressionsAsList();
            Node c1Node = list.get(0).accept(owl2Graph.getClassExpNodeAdder());
            Node c2Node = list.get(1).accept(owl2Graph.getClassExpNodeAdder());
            owl2Graph.createRelationship(c1Node, c2Node, OWL_EQUIVALENT_CLASS.toString());
        }
    }

    @Override
    public void visit(OWLDataPropertyAssertionAxiom axiom) {
        OWLIndividual subject = axiom.getSubject();
        Node sNode = owl2Graph.findOrCreateNode(subject.toString(), OWL_INDIVIDUAL.toString(), subject.isAnonymous());
        OWLLiteral object = axiom.getObject();
        Node oNode;
        if (null == object.getDatatype()) {
            oNode = owl2Graph.findOrCreateNode(object.toString(), OWLRDFVocabulary.RDFS_LITERAL.toString(), false);
        } else {
            oNode = owl2Graph.findOrCreateNode(object.toString(), object.getDatatype().toString(), false);
        }
        owl2Graph.createRelationship(sNode, oNode, axiom.getProperty());
    }

    @Override
    public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLSubDataPropertyOfAxiom axiom) {
        owl2Graph.handleSubPropertyAxiom(axiom);
    }

    @Override
    public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLSameIndividualAxiom axiom) {
        owl2Graph.handleNarIndAxiom(axiom);
    }

    @Override
    public void visit(OWLSubPropertyChainOfAxiom axiom) {
        String name = OWL_PROPERTY_CHAIN_AXIOM.toString() + "#" + System.nanoTime();
        Node chainNode = owl2Graph.createNode(name, OWL_OBJECT_PROPERTY.toString(), true);
        List<OWLObjectPropertyExpression> list = axiom.getPropertyChain();

        for (int i = 0; i < list.size(); i++) {
            OWLObjectPropertyExpression pe = list.get(i);
            Node pNode = owl2Graph.findOrCreateNode(pe.toString(), OWL_OBJECT_PROPERTY.toString(), pe.isAnonymous());
            pNode = owl2Graph.setProperty(pNode, OWL2Graph.PROPERTY_CHAIN_POSITION, Integer.toString(i));
            owl2Graph.createRelationship(chainNode, pNode, OWL_PROPERTY_CHAIN_AXIOM.toString());
        }
        OWLObjectPropertyExpression superProperty = axiom.getSuperProperty();
        Node supNode = owl2Graph.findOrCreateNode(superProperty.toString(), OWL_OBJECT_PROPERTY.toString(), superProperty.isAnonymous());
        owl2Graph.createRelationship(chainNode, supNode, RDFS_SUB_PROPERTY_OF.toString());
    }

    @Override
    public void visit(OWLInverseObjectPropertiesAxiom axiom) {
        OWLObjectPropertyExpression first = axiom.getFirstProperty();
        Node firstNode = owl2Graph.findOrCreateNode(first.toString(), OWL_OBJECT_PROPERTY.toString(), first.isAnonymous());
        OWLObjectPropertyExpression second = axiom.getSecondProperty();
        Node secondNode = owl2Graph.findOrCreateNode(second.toString(), OWL_OBJECT_PROPERTY.toString(), second.isAnonymous());
        owl2Graph.createRelationship(firstNode, secondNode, OWL_INVERSE_OF.toString());
    }

    @Override
    public void visit(OWLHasKeyAxiom axiom) {
        Node clsNode = axiom.getClassExpression().accept(owl2Graph.getClassExpNodeAdder());
        for (OWLObjectPropertyExpression p : axiom.getObjectPropertyExpressions()) {
            Node kNode = owl2Graph.findOrCreateNode(p.toString(), OWL_OBJECT_PROPERTY.toString(), p.isAnonymous());
            owl2Graph.createRelationship(clsNode, kNode, OWL_HAS_KEY.toString());
        }
        for (OWLDataPropertyExpression p : axiom.getDataPropertyExpressions()) {
            Node dNode = owl2Graph.findOrCreateNode(p.toString(), OWL_DATA_PROPERTY.toString(), p.isAnonymous());
            owl2Graph.createRelationship(clsNode, dNode, OWL_HAS_KEY.toString());
        }
    }

    @Override
    public void visit(SWRLRule rule) {
        logger.warn("SWRL rules not supported!");
    }
}
