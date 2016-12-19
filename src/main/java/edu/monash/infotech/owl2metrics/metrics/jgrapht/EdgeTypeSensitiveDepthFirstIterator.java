package edu.monash.infotech.owl2metrics.metrics.jgrapht;

import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedNode;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedParamEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class EdgeTypeSensitiveDepthFirstIterator extends DepthFirstIterator<NamedNode, NamedParamEdge> {
    private String edgeType;
    private int maxDepth;
    private int encounteredCount;

    public EdgeTypeSensitiveDepthFirstIterator(DirectedGraph<NamedNode, NamedParamEdge> namedNodeNamedParamEdgeGraph, NamedNode namedNode, String type) {
        super(namedNodeNamedParamEdgeGraph, namedNode);
        this.edgeType = type;
        this.maxDepth = namedNodeNamedParamEdgeGraph.vertexSet().size();
        this.encounteredCount = 0;
    }

    @Override
    protected void encounterVertex(NamedNode namedNode, NamedParamEdge namedParamEdge) {
        if (((DirectedGraph<NamedNode, NamedParamEdge>) this.getGraph()).outDegreeOf(namedNode) == 0) {
        }
        if (null == namedParamEdge || namedParamEdge.getName().equals(edgeType)) {
            super.encounterVertex(namedNode, namedParamEdge);
        }
    }

    @Override
    protected void encounterVertexAgain(NamedNode namedNode, NamedParamEdge namedParamEdge) {
        encounteredCount++;
        if (encounteredCount >= maxDepth) {
            super.encounterVertexAgain(namedNode, namedParamEdge);
        } else {
            encounterVertex(namedNode, namedParamEdge);
        }
    }
}
