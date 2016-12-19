package edu.monash.infotech.owl2metrics.translate.named;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import edu.monash.infotech.owl2metrics.translate.all.NodeRelAddingVisitor;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.*;

/**
 * @author Yuan-Fang Li
 * @version $Id: NamedNodeRelAddingVisitor.java 104 2012-10-29 13:18:11Z yli $
 */

// TODO: count anonymous axioms
public class NamedNodeRelAddingVisitor<Node, Relationship> extends NodeRelAddingVisitor<Node, Relationship> implements OWLLogicalAxiomVisitor {

    private Function<OWLEntity, Node> entityToNodeFunction;

    public NamedNodeRelAddingVisitor(final OWL2Graph<?, Node, Relationship> owl2Graph) {
        super(owl2Graph);
        this.entityToNodeFunction = new Function<OWLEntity, Node>() {

            public Node apply(@Nullable OWLEntity input) {
                return owl2Graph.findOrCreateNode(input.toString(), input.getEntityType().getVocabulary().toString(), false);
            }
        };
    }

    @Override
    public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLClassAssertionAxiom axiom) {
        Collection<Node> subNodes = getDependentNodes(axiom.getIndividual().getIndividualsInSignature());
        Set<Node> allNodes = getDependeeNodes(axiom, subNodes);
        addDependsOnRelations(subNodes, allNodes);
    }

    private void addDependsOnRelations(Collection<Node> subNodes, Set<Node> allNodes) {
        for (Node n : subNodes) {
            for (Node d : allNodes) {
                owl2Graph.createRelationship(n, d, OWL2Graph.DEPENDS_ON);
            }
        }
    }

    private Set<Node> getDependeeNodes(OWLObject axiom, Collection<Node> subNodes) {
        Set<Node> allNodes = Sets.newHashSet();
        allNodes.addAll(transform(axiom.getSignature(), entityToNodeFunction));
        allNodes.removeAll(subNodes);
        return allNodes;
    }

    private Collection<Node> getDependentNodes(Set<? extends OWLEntity> dependents) {
        Collection<Node> nodes = transform(dependents, entityToNodeFunction);
        return nodes;
    }

    @Override
    public void visit(OWLDataPropertyAssertionAxiom axiom) {
        Set<OWLNamedIndividual> subjects = axiom.getSubject().getIndividualsInSignature();
        Collection<Node> dependentNodes = getDependentNodes(subjects);
        Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
        addDependsOnRelations(dependentNodes, dependeeNodes);
    }

    @Override
    public void visit(OWLDataPropertyDomainAxiom axiom) {
        OWLDataPropertyExpression property = axiom.getProperty();
        OWLClassExpression domain = axiom.getDomain();
        if (!(property.isAnonymous() || domain.isAnonymous())) {
            Node pNode = owl2Graph.findOrCreateNode(property.toString(), OWL_OBJECT_PROPERTY.toString(), false);
            Node dNode = owl2Graph.findOrCreateNode(domain.toString(), OWL_CLASS.toString(), false);
            owl2Graph.createRelationship(pNode, dNode, RDFS_DOMAIN.toString());
        } else {
            Collection<Node> dependentNodes = getDependentNodes(axiom.getProperty().getDataPropertiesInSignature());
            Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
            addDependsOnRelations(dependentNodes, dependeeNodes);
        }
    }

    @Override
    public void visit(OWLDataPropertyRangeAxiom axiom) {
        OWLDataPropertyExpression property = axiom.getProperty();
        OWLDataRange range = axiom.getRange();
        if (!property.isAnonymous() && range.isDatatype()) {
            Node pNode = owl2Graph.findOrCreateNode(property.toString(), OWL_OBJECT_PROPERTY.toString(), false);
            Node rNode = owl2Graph.findOrCreateNode(range.toString(), OWL_CLASS.toString(), false);
            owl2Graph.createRelationship(pNode, rNode, RDFS_RANGE.toString());
        } else {
            Collection<Node> dependentNodes = getDependentNodes(property.getDataPropertiesInSignature());
            Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
            addDependsOnRelations(dependentNodes, dependeeNodes);
        }
    }

    @Override
    public void visit(OWLDifferentIndividualsAxiom axiom) {
        Collection<Node> nodes = transform(axiom.getIndividualsInSignature(), entityToNodeFunction);
        Node[] array = (Node[]) nodes.toArray();
        addPairwiseRelation(array);
    }

    private void addPairwiseRelation(Node[] array) {
        addPairwiseRelation(array, OWL2Graph.DEPENDS_ON);
    }

    private void addPairwiseRelation(Node[] array, String rel) {
        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                owl2Graph.createRelationship(array[i], array[j], rel);
                owl2Graph.createRelationship(array[j], array[i], rel);
            }
        }
    }

    @Override
    public void visit(OWLDisjointClassesAxiom axiom) {
        Set<OWLEntity> clsSig = Sets.newHashSet();
        for (OWLClassExpression ce : axiom.getClassExpressions()) {
            if (!ce.isAnonymous()) {
                clsSig.add(ce.asOWLClass());
            }
        }
        Collection<Node> sigNodes = getDependentNodes(clsSig);
        addPairwiseRelation((Node[]) sigNodes.toArray());
        Set<Node> dependeeNodes = getDependeeNodes(axiom, sigNodes);
        addDependsOnRelations(sigNodes, dependeeNodes);
    }

    @Override
    public void visit(OWLDisjointDataPropertiesAxiom axiom) {
        Collection<Node> dependentNodes = getDependentNodes(axiom.getDataPropertiesInSignature());
        Node[] array = (Node[]) dependentNodes.toArray();
        addPairwiseRelation(array, OWL_DISJOINT_WITH.toString());
    }

    @Override
    public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
        Collection<Node> dependentNodes = getDependentNodes(axiom.getObjectPropertiesInSignature());
        Node[] array = (Node[]) dependentNodes.toArray();
        addPairwiseRelation(array, OWL_DISJOINT_WITH.toString());
    }

    @Override
    public void visit(OWLDisjointUnionAxiom axiom) {
        Collection<Node> dependentNodes = getDependentNodes(Collections.singleton(axiom.getOWLClass()));
        Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
        addDependsOnRelations(dependentNodes, dependeeNodes);
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        Collection<Node> dependentNodes = getDependentNodes(axiom.getClassesInSignature());
        addPairwiseRelation((Node[]) dependentNodes.toArray());
        Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
        addDependsOnRelations(dependentNodes, dependeeNodes);
    }

    @Override
    public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
        Collection<Node> nodes = transform(axiom.getDataPropertiesInSignature(), entityToNodeFunction);
        Node[] array = (Node[]) nodes.toArray();
        addPairwiseRelation(array, OWL_EQUIVALENT_PROPERTY.toString());
    }

    @Override
    public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        Collection<Node> nodes = transform(axiom.getObjectPropertiesInSignature(), entityToNodeFunction);
        Node[] array = (Node[]) nodes.toArray();
        addPairwiseRelation(array, OWL_EQUIVALENT_PROPERTY.toString());
    }

    @Override
    public void visit(OWLFunctionalDataPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLHasKeyAxiom axiom) {
        OWLClassExpression expression = axiom.getClassExpression();
        if (!expression.isAnonymous()) {
            Node node = owl2Graph.findOrCreateNode(expression.asOWLClass().toStringID(), OWL_CLASS.toString(), false);
            Set<Node> dependeeNodes = getDependeeNodes(axiom, Collections.singleton(node));
            addDependsOnRelations(Collections.singleton(node), dependeeNodes);
        }
    }

    @Override
    public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLInverseObjectPropertiesAxiom axiom) {
        Collection<Node> nodes = transform(axiom.getObjectPropertiesInSignature(), entityToNodeFunction);
        Node[] array = (Node[]) nodes.toArray();
        addPairwiseRelation(array, OWL_INVERSE_OF.toString());
    }

    @Override
    public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        Set<OWLNamedIndividual> subjects = axiom.getSubject().getIndividualsInSignature();
        Collection<Node> dependentNodes = getDependentNodes(subjects);
        Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
        addDependsOnRelations(dependentNodes, dependeeNodes);
    }

    @Override
    public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        Set<OWLNamedIndividual> subjects = axiom.getSubject().getIndividualsInSignature();
        Collection<Node> dependentNodes = getDependentNodes(subjects);
        Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
        addDependsOnRelations(dependentNodes, dependeeNodes);
    }

    @Override
    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        Set<OWLNamedIndividual> subjects = axiom.getSubject().getIndividualsInSignature();
        Collection<Node> dependentNodes = getDependentNodes(subjects);
        Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
        addDependsOnRelations(dependentNodes, dependeeNodes);
    }

    @Override
    public void visit(OWLSubPropertyChainOfAxiom axiom) {
        Set<OWLObjectProperty> superProperties = axiom.getSuperProperty().getObjectPropertiesInSignature();
        Collection<Node> dependentNodes = getDependentNodes(superProperties);
        Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
        addDependsOnRelations(dependentNodes, dependeeNodes);
    }

    @Override
    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        OWLObjectPropertyExpression property = axiom.getProperty();
        OWLClassExpression domain = axiom.getDomain();
        if (!(property.isAnonymous() || domain.isAnonymous())) {
            Node pNode = owl2Graph.findOrCreateNode(property.toString(), OWL_OBJECT_PROPERTY.toString(), false);
            Node dNode = owl2Graph.findOrCreateNode(domain.toString(), OWL_CLASS.toString(), false);
            owl2Graph.createRelationship(pNode, dNode, RDFS_DOMAIN.toString());
        } else {
            Collection<Node> dependentNodes = getDependentNodes(property.getObjectPropertiesInSignature());
            Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
            addDependsOnRelations(dependentNodes, dependeeNodes);
        }
    }

    @Override
    public void visit(OWLObjectPropertyRangeAxiom axiom) {
        OWLObjectPropertyExpression property = axiom.getProperty();
        OWLClassExpression range = axiom.getRange();
        if (!(property.isAnonymous() || range.isAnonymous())) {
            Node pNode = owl2Graph.findOrCreateNode(property.toString(), OWL_OBJECT_PROPERTY.toString(), false);
            Node dNode = owl2Graph.findOrCreateNode(range.toString(), OWL_CLASS.toString(), false);
            owl2Graph.createRelationship(pNode, dNode, RDFS_RANGE.toString());
        } else {
            Collection<Node> dependentNodes = getDependentNodes(property.getObjectPropertiesInSignature());
            Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
            addDependsOnRelations(dependentNodes, dependeeNodes);
        }
    }

    @Override
    public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLSameIndividualAxiom axiom) {
        Collection<Node> nodes = transform(axiom.getIndividualsInSignature(), entityToNodeFunction);
        Node[] array = (Node[]) nodes.toArray();
        addPairwiseRelation(array);
    }

    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        if (!axiom.isGCI()) {
            OWLClassExpression subClass = axiom.getSubClass();
            OWLClassExpression superClass = axiom.getSuperClass();
            if (!subClass.isAnonymous() && !superClass.isAnonymous()) {
                Node subNode = owl2Graph.findOrCreateClass(subClass.toString(), OWL_CLASS.toString(), false);
                Node supNode = owl2Graph.findOrCreateClass(superClass.toString(), OWL_CLASS.toString(), false);
                owl2Graph.addSubclassRel(subNode, supNode);
            } else {
                Collection<Node> nodes = transform(Collections.singleton(subClass.asOWLClass()), entityToNodeFunction);
                Set<Node> dependeeNodes = getDependeeNodes(axiom, nodes);
                addDependsOnRelations(nodes, dependeeNodes);
            }
        }
    }

    @Override
    public void visit(OWLSubDataPropertyOfAxiom axiom) {
        OWLDataPropertyExpression subProperty = axiom.getSubProperty();
        OWLDataPropertyExpression superProperty = axiom.getSuperProperty();
        if (!(subProperty.isAnonymous() || superProperty.isAnonymous())) {
            Node subNode = owl2Graph.findOrCreateNode(subProperty.toString(), subProperty.asOWLDataProperty().getEntityType().toString(), false);
            Node supNode = owl2Graph.findOrCreateNode(super.toString(), superProperty.asOWLDataProperty().getEntityType().toString(), false);
            owl2Graph.createRelationship(subNode, supNode, OWLRDFVocabulary.RDFS_SUB_PROPERTY_OF.toString());
        } else {
            Collection<Node> dependentNodes = getDependentNodes(subProperty.getDataPropertiesInSignature());
            Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
            addDependsOnRelations(dependentNodes, dependeeNodes);
        }
    }

    @Override
    public void visit(OWLSubObjectPropertyOfAxiom axiom) {
        OWLObjectPropertyExpression subProperty = axiom.getSubProperty();
        OWLObjectPropertyExpression superProperty = axiom.getSuperProperty();
        if (!(subProperty.isAnonymous() || superProperty.isAnonymous())) {
            Node subNode = owl2Graph.findOrCreateNode(subProperty.toString(), subProperty.asOWLObjectProperty().getEntityType().toString(), false);
            Node supNode = owl2Graph.findOrCreateNode(super.toString(), superProperty.asOWLObjectProperty().getEntityType().toString(), false);
            owl2Graph.createRelationship(subNode, supNode, OWLRDFVocabulary.RDFS_SUB_PROPERTY_OF.toString());
        } else {
            Collection<Node> dependentNodes = getDependentNodes(axiom.getSubProperty().getObjectPropertiesInSignature());
            Set<Node> dependeeNodes = getDependeeNodes(axiom, dependentNodes);
            addDependsOnRelations(dependentNodes, dependeeNodes);
        }
    }

    @Override
    public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }

    @Override
    public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
        owl2Graph.handleUnaryPropAxiom(axiom);
    }
}
