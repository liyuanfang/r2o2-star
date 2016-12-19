package edu.monash.infotech.owl2metrics.translate.jgrapht;

import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedNode;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedParamEdge;
import edu.monash.infotech.owl2metrics.translate.named.NamedNodeRelAddingVisitor;
import org.jgrapht.DirectedGraph;

/**
 * @author Yuan-Fang Li
 * @version $Id: OWL2GraphJGraphTNamedImpl.java 89 2012-10-18 12:29:17Z yli $
 */
public class OWL2GraphJGraphTNamedImpl extends OWL2GraphJGraphTImpl implements
        OWL2Graph<DirectedGraph<NamedNode, NamedParamEdge>, NamedNode, NamedParamEdge> {

    @Override
    protected void createNodeRelAdder() {
        adder = new NamedNodeRelAddingVisitor<NamedNode, NamedParamEdge>(this);
    }
}
