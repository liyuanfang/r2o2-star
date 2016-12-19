package edu.monash.infotech.owl2metrics.translate.named;

import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import org.semanticweb.owlapi.model.*;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.OWL_CLASS;

/**
 * @author Yuan-Fang Li
 * @version $Id: ClassExpNodeAggregator.java 87 2012-10-18 05:59:56Z yli $
 */
public class ClassExpNodeAggregator<Node, Relationship> implements OWLClassExpressionVisitorEx<Set<Node>> {
    private OWL2Graph<?, Node, Relationship> owl2Graph;

    public ClassExpNodeAggregator(OWL2Graph<?, Node, Relationship> owl2Graph) {
        this.owl2Graph = owl2Graph;
    }

    @Override
    public Set<Node> visit(OWLClass ce) {
        return newHashSet(owl2Graph.findOrCreateClass(ce.toString(), OWL_CLASS.toString(), false));
    }

    @Override
    public Set<Node> visit(OWLObjectIntersectionOf ce) {
        return handleNaryClassExpression(ce);
    }

    @Override
    public Set<Node> visit(OWLObjectUnionOf ce) {
        return handleNaryClassExpression(ce);    }

    private Set<Node> handleNaryClassExpression(OWLNaryBooleanClassExpression ce) {
        Set<Node> set = newHashSet();
        for (OWLClassExpression operand : ce.getOperands()) {
            Set<Node> opNode = operand.accept(this);
            set.addAll(opNode);
        }
        return set;
    }

    @Override
    public Set<Node> visit(OWLObjectComplementOf ce) {
        return ce.getOperand().accept(this);
    }

    @Override
    public Set<Node> visit(OWLObjectSomeValuesFrom ce) {
        Set<OWLObjectProperty> properties = ce.getObjectPropertiesInSignature();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLObjectAllValuesFrom ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLObjectHasValue ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLObjectMinCardinality ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLObjectExactCardinality ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLObjectMaxCardinality ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLObjectHasSelf ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLObjectOneOf ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLDataSomeValuesFrom ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLDataAllValuesFrom ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLDataHasValue ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLDataMinCardinality ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLDataExactCardinality ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Node> visit(OWLDataMaxCardinality ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
