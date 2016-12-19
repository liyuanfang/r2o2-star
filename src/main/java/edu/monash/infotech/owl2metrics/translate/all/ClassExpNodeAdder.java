package edu.monash.infotech.owl2metrics.translate.all;

import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import org.semanticweb.owlapi.model.*;

import java.util.Set;

import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.*;

/**
 * @author Yuan-Fang Li
 * @version $Id: ClassExpNodeAdder.java 87 2012-10-18 05:59:56Z yli $
 */
public class ClassExpNodeAdder<Node, Relationship> implements OWLClassExpressionVisitorEx<Node> {

    private OWL2Graph<?, Node, Relationship> owl2Graph;
    
    public ClassExpNodeAdder(OWL2Graph<?, Node, Relationship> owl2Graph) {
        this.owl2Graph = owl2Graph;
    }

    public Node visit(OWLClass ce) {
        return owl2Graph.findOrCreateClass(ce.toString(), OWL_CLASS.toString(), false);
    }

    public Node visit(OWLObjectIntersectionOf ce) {
        return handleNaryClassExpression(ce);
    }

    public Node visit(OWLObjectUnionOf ce) {
        return handleNaryClassExpression(ce);
    }
    private Node handleNaryClassExpression(OWLNaryBooleanClassExpression ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_INTERSECTION_OF.toString(), true);
        for (OWLClassExpression operand : ce.getOperands()) {
            Node opNode = operand.accept(this);
            owl2Graph.createRelationship(anonNode, opNode, OWL2Graph.OPERAND);
        }
        return anonNode;
    }


    public Node visit(OWLObjectComplementOf ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_COMPLEMENT_OF.toString(), true);
        Node origNode = ce.getOperand().accept(this);
        owl2Graph.createRelationship(anonNode, origNode, OWL_COMPLEMENT_OF.toString());
        return anonNode;
    }

    public Node visit(OWLObjectSomeValuesFrom ce) {
        return owl2Graph.handleQuantifiedExp(ce, OWL_SOME_VALUES_FROM.toString());
    }

    public Node visit(OWLObjectAllValuesFrom ce) {
        return owl2Graph.handleQuantifiedExp(ce, OWL_ALL_VALUES_FROM.toString());
    }

    public Node visit(OWLObjectHasValue ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_HAS_VALUE.toString(), true);
        OWLIndividual individual = ce.getValue();
        Node indNode = owl2Graph.findOrCreateNode(individual.toString(), OWL_INDIVIDUAL.toString(), individual.isAnonymous());
        owl2Graph.createRelationship(anonNode, indNode, ce.getProperty());
        return anonNode;
    }

    public Node visit(OWLObjectMinCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_MIN_CARDINALITY.toString());
    }

    public Node visit(OWLObjectExactCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_CARDINALITY.toString());
    }

    public Node visit(OWLObjectMaxCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_MAX_CARDINALITY.toString());
    }

    public Node visit(OWLObjectHasSelf ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_HAS_SELF.toString(), true);
        Set<OWLNamedIndividual> individuals = ce.getIndividualsInSignature();
        for (OWLIndividual ind : individuals) {
            Node indNode = owl2Graph.findOrCreateNode(ind.toString(), OWL_INDIVIDUAL.toString(), ind.isAnonymous());
            owl2Graph.createRelationship(indNode, indNode, ce.getProperty());
            owl2Graph.createRelationship(anonNode, indNode, ce.getProperty());
        }
        return anonNode;
    }

    public Node visit(OWLObjectOneOf ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_ONE_OF.toString(), true);
        for (OWLIndividual ind : ce.getIndividuals()) {
            Node indNode = owl2Graph.findOrCreateNode(ind.toString(), OWL_INDIVIDUAL.toString(), ind.isAnonymous());
            owl2Graph.createRelationship(anonNode, indNode, OWL2Graph.OPERAND);
        }
        return anonNode;
    }

    public Node visit(OWLDataSomeValuesFrom ce) {
        return owl2Graph.handleQuantifiedExp(ce, OWL_SOME_VALUES_FROM.toString());
    }

    public Node visit(OWLDataAllValuesFrom ce) {
        return owl2Graph.handleQuantifiedExp(ce, OWL_ALL_VALUES_FROM.toString());
    }

    public Node visit(OWLDataHasValue ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_HAS_VALUE.toString(), true);
        OWLLiteral value = ce.getValue();
        OWLDatatype datatype = value.getDatatype();
        String literalString = "\"" + value.toString() + "\"";
        if (null != datatype) {
            literalString += "^^" + datatype.toString();
        }
        anonNode = owl2Graph.setProperty(anonNode, OWL2Graph.VALUE, literalString);
        return anonNode;
    }

    public Node visit(OWLDataMinCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_MIN_CARDINALITY.toString());
    }

    public Node visit(OWLDataExactCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_CARDINALITY.toString());
    }

    public Node visit(OWLDataMaxCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_MAX_CARDINALITY.toString());
    }
}
