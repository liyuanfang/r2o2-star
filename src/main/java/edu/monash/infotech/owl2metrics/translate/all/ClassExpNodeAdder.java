package edu.monash.infotech.owl2metrics.translate.all;

import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import java.util.Set;

import static edu.monash.infotech.owl2metrics.translate.OWL2Graph.OPERAND;
import static edu.monash.infotech.owl2metrics.translate.OWL2Graph.VALUE;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_ALL_VALUES_FROM;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_CARDINALITY;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_CLASS;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_COMPLEMENT_OF;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_HAS_SELF;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_HAS_VALUE;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_INDIVIDUAL;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_INTERSECTION_OF;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_MAX_CARDINALITY;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_MIN_CARDINALITY;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_ONE_OF;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_SOME_VALUES_FROM;

/**
 * @author Yuan-Fang Li
 * @version $Id: ClassExpNodeAdder.java 87 2012-10-18 05:59:56Z yli $
 */
public class ClassExpNodeAdder<Node, Relationship> implements OWLClassExpressionVisitorEx<Node> {

    private OWL2Graph<?, Node, Relationship> owl2Graph;
    
    public ClassExpNodeAdder(OWL2Graph<?, Node, Relationship> owl2Graph) {
        this.owl2Graph = owl2Graph;
    }

    @Override
    public Node visit(OWLClass ce) {
        return owl2Graph.findOrCreateClass(ce.toString(), OWL_CLASS.toString(), false);
    }

    @Override
    public Node visit(OWLObjectIntersectionOf ce) {
        return handleNaryClassExpression(ce);
    }

    @Override
    public Node visit(OWLObjectUnionOf ce) {
        return handleNaryClassExpression(ce);
    }
    private Node handleNaryClassExpression(OWLNaryBooleanClassExpression ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_INTERSECTION_OF.toString(), true);
        for (OWLClassExpression operand : ce.getOperands()) {
            Node opNode = operand.accept(this);
            owl2Graph.createRelationship(anonNode, opNode, OPERAND);
        }
        return anonNode;
    }


    @Override
    public Node visit(OWLObjectComplementOf ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_COMPLEMENT_OF.toString(), true);
        Node origNode = ce.getOperand().accept(this);
        owl2Graph.createRelationship(anonNode, origNode, OWL_COMPLEMENT_OF.toString());
        return anonNode;
    }

    @Override
    public Node visit(OWLObjectSomeValuesFrom ce) {
        return owl2Graph.handleQuantifiedExp(ce, OWL_SOME_VALUES_FROM.toString());
    }

    @Override
    public Node visit(OWLObjectAllValuesFrom ce) {
        return owl2Graph.handleQuantifiedExp(ce, OWL_ALL_VALUES_FROM.toString());
    }

    @Override
    public Node visit(OWLObjectHasValue ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_HAS_VALUE.toString(), true);
        OWLIndividual individual = ce.getValue();
        Node indNode = owl2Graph.findOrCreateNode(individual.toString(), OWL_INDIVIDUAL.toString(), individual.isAnonymous());
        owl2Graph.createRelationship(anonNode, indNode, ce.getProperty());
        return anonNode;
    }

    @Override
    public Node visit(OWLObjectMinCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_MIN_CARDINALITY.toString());
    }

    @Override
    public Node visit(OWLObjectExactCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_CARDINALITY.toString());
    }

    @Override
    public Node visit(OWLObjectMaxCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_MAX_CARDINALITY.toString());
    }

    @Override
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

    @Override
    public Node visit(OWLObjectOneOf ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_ONE_OF.toString(), true);
        for (OWLIndividual ind : ce.getIndividuals()) {
            Node indNode = owl2Graph.findOrCreateNode(ind.toString(), OWL_INDIVIDUAL.toString(), ind.isAnonymous());
            owl2Graph.createRelationship(anonNode, indNode, OPERAND);
        }
        return anonNode;
    }

    @Override
    public Node visit(OWLDataSomeValuesFrom ce) {
        return owl2Graph.handleQuantifiedExp(ce, OWL_SOME_VALUES_FROM.toString());
    }

    @Override
    public Node visit(OWLDataAllValuesFrom ce) {
        return owl2Graph.handleQuantifiedExp(ce, OWL_ALL_VALUES_FROM.toString());
    }

    @Override
    public Node visit(OWLDataHasValue ce) {
        Node anonNode = owl2Graph.findOrCreateClass(ce.toString(), OWL_HAS_VALUE.toString(), true);
        OWLLiteral value = ce.getValue();
        OWLDatatype datatype = value.getDatatype();
        String literalString = "\"" + value.toString() + "\"";
        if (null != datatype) {
            literalString += "^^" + datatype.toString();
        }
        anonNode = owl2Graph.setProperty(anonNode, VALUE, literalString);
        return anonNode;
    }

    @Override
    public Node visit(OWLDataMinCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_MIN_CARDINALITY.toString());
    }

    @Override
    public Node visit(OWLDataExactCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_CARDINALITY.toString());
    }

    @Override
    public Node visit(OWLDataMaxCardinality ce) {
        return owl2Graph.handleCardExpression(ce, OWL_MAX_CARDINALITY.toString());
    }
}
