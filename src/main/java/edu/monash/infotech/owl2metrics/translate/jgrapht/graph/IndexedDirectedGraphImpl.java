package edu.monash.infotech.owl2metrics.translate.jgrapht.graph;

import org.jgrapht.graph.DirectedMultigraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class IndexedDirectedGraphImpl extends DirectedMultigraph<NamedNode, NamedParamEdge> implements IndexedDirectedGraph<NamedNode, NamedParamEdge> {
    private Map<String, NamedNode> vertexMap;
    private Set<Integer> edgeSet;

    public IndexedDirectedGraphImpl(Class<NamedParamEdge> edgeClass, int size) {
        super(edgeClass);
        //super(new ClassBasedEdgeFactory<NamedNode, NamedParamEdge>(edgeClass));

        vertexMap = new HashMap<String, NamedNode>(size);
        edgeSet = new HashSet<Integer>(size * 3);
    }

    @Override
    public boolean containsVertex(NamedNode namedNode) {
        return vertexMap.containsKey(namedNode.getName());
    }

    @Override
    public NamedNode getVertex(String key) {
        return vertexMap.get(key);
    }

    @Override
    public boolean addVertex(NamedNode v) {
        boolean added = super.addVertex(v);
        if (added) {
            vertexMap.put(v.getName(), v);
        }
        return added;
    }

    @Override
    public boolean containsEdge(NamedNode source, NamedNode target, String edgeName) {
        String key = source.toString() + "->" + edgeName + "->" + target.toString();
        return edgeSet.contains(key.hashCode());
    }

    @Override
    public void addEdgeToSet(NamedParamEdge edge) {
        String key = edge.getSource().toString() + "->" + edge.getName() + "->" + edge.getTarget().toString();
        edgeSet.add(key.hashCode());
    }
}
