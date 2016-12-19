package edu.monash.infotech.owl2metrics.translate.jgrapht.graph;

import org.jgrapht.DirectedGraph;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public interface IndexedDirectedGraph<V, E> extends DirectedGraph<V, E> {
    V getVertex(String key);

    boolean containsEdge(V source, V target, String edge);

    void addEdgeToSet(E edge);
}
