package edu.monash.infotech.owl2metrics.module;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.opencsv.CSVWriter;
import dk.aaue.sna.alg.centrality.CentralityMeasure;
import dk.aaue.sna.alg.centrality.CentralityResult;
import dk.aaue.sna.alg.centrality.DegreeCentrality;
import dk.aaue.sna.alg.centrality.EigenvectorCentrality;
import edu.monash.infotech.owl2metrics.metrics.jgrapht.DirectoryProcessor;
import edu.monash.infotech.owl2metrics.metrics.jgrapht.EdgeTypeSensitiveDepthFirstIterator;
import edu.monash.infotech.owl2metrics.model.NamedEntityMetric;
import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import edu.monash.infotech.owl2metrics.translate.jgrapht.OWL2GraphJGraphTNamedImpl;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedNode;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedParamEdge;
import org.apache.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ObjectArrays.concat;
import static java.lang.Boolean.valueOf;
import static java.lang.Math.max;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.*;

/**
 * @author Yuan-Fang Li
 * @version $Id: NodeMetricsCollector.java 123 2012-11-23 15:06:33Z yli $
 */
public class NodeMetricsCollector {
    private Logger logger = Logger.getLogger(getClass());

    private Map<NamedNode, Set<NamedEntityMetric<? extends Comparable>>> entityMetricMap;

    public static final Function<NamedEntityMetric<?>, String> VALUE_FUNCTION = new Function<NamedEntityMetric<?>, String>() {
        public String apply(@Nullable NamedEntityMetric<?> input) {
            return input.getValue().toString();
        }
    };

    public Map<NamedNode, Set<NamedEntityMetric<? extends Comparable>>> collectMetrics(DirectedGraph<NamedNode, NamedParamEdge> graph, OWLOntology ontology) {
        entityMetricMap = Maps.newTreeMap();
        String ontologyIRI = "<" + ontology.getOntologyID().getOntologyIRI().toString() + ">";
        logger.info("Ontology IRI = " + ontologyIRI);

        // Eigen vector centrality
        logger.info("Calculating Eigen vector centrality measures.");
        CentralityMeasure<NamedNode> evcm = new EigenvectorCentrality<NamedNode, NamedParamEdge>(graph);
        CentralityResult<NamedNode> eResult = evcm.calculate();

        // Degree centrality
        logger.info("Calculating Degree centrality measures.");
        CentralityMeasure<NamedNode> dvcm = new DegreeCentrality<NamedNode, NamedParamEdge>(graph);
        CentralityResult<NamedNode> dResult = dvcm.calculate();

        //// Betweenness centrality
        //logger.info("Calculating Betweenness centrality");
        //CentralityMeasure<NamedNode> bvcm = new RandomWalkBetweennessCentrality<NamedNode, NamedParamEdge>(graph);
        //CentralityResult<NamedNode> bResult = bvcm.calculate();

        //// Clustering coefficient
        //logger.info("Calculating clustering coefficient");
        //CentralityMeasure<NamedNode> cluCoe = new ClusteringCoefficient<NamedNode, NamedParamEdge>(graph);
        //CentralityResult<NamedNode> cResult = cluCoe.calculate();

        Set<NamedNode> nodeSet = graph.vertexSet();
        logger.info("Handling " + nodeSet.size() + " nodes.");
        int i = 0;
        for (NamedNode node : nodeSet) {
            if (++i % 10000 == 0) {
                logger.info("Handled " + i + " nodes.");
            }
            if (valueOf(node.getProperties().get(OWL2Graph.NODE_ANON_TYPE_NAME).toString()) ||
                    node.getName().equals(OWLRDFVocabulary.OWL_THING.toString())) {
                continue;
            }
            Set<NamedEntityMetric<? extends Comparable>> metricSet = findOrCreateSet(node);
            SortedSet<String> types = node.getTypes();
            if (types.contains(OWL_CLASS.toString())) {
                handleClassNode(graph, node, metricSet);
            }
            /*else if (types.contains(OWL_OBJECT_PROPERTY.toString()) || types.contains(OWL_DATA_PROPERTY.toString())) {
                handlePropertyNode(graph, node, metricSet);
            } else if (types.contains(OWL_INDIVIDUAL.toString())) {
                handleIndividualNode(graph, node, metricSet);
            }
            */

            // Eigen vector centrality
            NamedEntityMetric<Double> evcMetric = new NamedEntityMetric<Double>("EVC", eResult.get(node));
            metricSet.add(evcMetric);

            // Degree centrality
            NamedEntityMetric<Double> dvcMetric = new NamedEntityMetric<Double>("DEC", dResult.get(node));
            metricSet.add(dvcMetric);

            //// Between centrality
            //NamedEntityMetric<Double> becMetric = new NamedEntityMetric<Double>("BEV", bResult.get(node));
            //metricSet.add(becMetric);

            //// Clustering coefficient
            //NamedEntityMetric<Double> cluMetric = new NamedEntityMetric<Double>("CLC", cResult.get(node));
            //metricSet.add(cluMetric);

            entityMetricMap.put(node, metricSet);
        }

        logger.info("All node metrics collected.");
        return entityMetricMap;
    }

    private void handleClassNode(DirectedGraph<NamedNode, NamedParamEdge> graph,
                                 NamedNode node, Set<NamedEntityMetric<? extends Comparable>> metricSet) {
        // NOC
        int nocC = 0;
        NamedEntityMetric<Integer> noc = new NamedEntityMetric<Integer>("NOC", 0);

        // NOP
        int nopC = 0;
        NamedEntityMetric<Integer> nop = new NamedEntityMetric<Integer>("NOP", 0);

        // Node in-degree
        int nidC = 0;
        NamedEntityMetric<Integer> nid = new NamedEntityMetric<Integer>("NID", 0);

        // Node out-degree
        int nodC = 0;
        NamedEntityMetric<Integer> nod = new NamedEntityMetric<Integer>("NOD", 0);

        // DIT
        int ditC = calculateDITForNode(node, graph);
        NamedEntityMetric<Integer> dit = new NamedEntityMetric<Integer>("DIT", 0);
        dit.setValue(ditC);

        for (NamedParamEdge e : graph.outgoingEdgesOf(node)) {
            nodC++;
            if (e.getName().equals(RDFS_SUBCLASS_OF.toString())) {
                nopC++;
            }
        }
        nod.setValue(nodC);
        nop.setValue(nopC);

        for (NamedParamEdge e : graph.incomingEdgesOf(node)) {
            nidC++;
            if (e.getName().equals(RDFS_SUBCLASS_OF.toString())) {
                nocC++;
            }
        }
        nid.setValue(nidC);
        noc.setValue(nocC);

        metricSet.add(noc);
        metricSet.add(nop);
        metricSet.add(nid);
        metricSet.add(nod);
        metricSet.add(dit);
    }

    public String[] getClassMetricsHeader() {
        return new String[]{"Name", "Type", "NOC", "NOP", "NID", "NOD", "DIT", "EVC"};
    }

    public String[] getPropertyMetricsHeader() {
        return new String[]{"Name", "Type", "NOC", "NOP", "NID", "NOD", "EVC", "DVC"};
    }

    public String[] getIndividualMetricsHeader() {
        return new String[]{"Name", "Type", "SAMEAS", "DIFF", "NID", "NOD", "EVC", "DVC"};
    }

    private void handlePropertyNode(DirectedGraph<NamedNode, NamedParamEdge> graph,
                                    NamedNode node, Set<NamedEntityMetric<? extends Comparable>> metricSet) {
        // NOC
        int nocP = 0;
        NamedEntityMetric<Integer> noc = new NamedEntityMetric<Integer>("NOC", 0);

        // NOP
        int nopP = 0;
        NamedEntityMetric<Integer> nop = new NamedEntityMetric<Integer>("NOP", 0);

        // Node in-degree
        int nidC = 0;
        NamedEntityMetric<Integer> nid = new NamedEntityMetric<Integer>("NID", 0);

        // Node out-degree
        int nodC = 0;
        NamedEntityMetric<Integer> nod = new NamedEntityMetric<Integer>("NOD", 0);

        for (NamedParamEdge e : graph.outgoingEdgesOf(node)) {
            nodC++;
            if (e.getName().equals(RDFS_SUB_PROPERTY_OF.toString())) {
                nopP++;
            }
        }
        nod.setValue(nodC);
        nop.setValue(nopP);

        for (NamedParamEdge e : graph.incomingEdgesOf(node)) {
            nidC++;
            if (e.getName().equals(RDFS_SUBCLASS_OF.toString())) {
                nocP++;
            }
        }
        nid.setValue(nidC);
        noc.setValue(nocP);

        metricSet.add(noc);
        metricSet.add(nop);
        metricSet.add(nid);
        metricSet.add(nod);
    }

    private void handleIndividualNode(DirectedGraph<NamedNode, NamedParamEdge> graph,
                                      NamedNode node, Set<NamedEntityMetric<? extends Comparable>> metricSet) {
        // Same as
        int sameC = 0;
        NamedEntityMetric<Integer> sameAs = new NamedEntityMetric<Integer>("SAMEAS", 0);

        // Different
        int diffC = 0;
        NamedEntityMetric<Integer> diffFrom = new NamedEntityMetric<Integer>("DIFF", 0);

        // Node in-degree
        NamedEntityMetric<Integer> nid = new NamedEntityMetric<Integer>("NID", 0);

        // Node out-degree
        int nodC = 0;
        NamedEntityMetric<Integer> nod = new NamedEntityMetric<Integer>("NOD", 0);

        for (NamedParamEdge e : graph.outgoingEdgesOf(node)) {
            nodC++;
            if (e.getName().equals(OWL_DIFFERENT_FROM.toString())) {
                diffC++;
            } else if (e.getName().equals(OWL_SAME_AS.toString())) {
                sameC++;
            }
        }
        nod.setValue(nodC);
        diffFrom.setValue(diffC);

        nid.setValue(graph.incomingEdgesOf(node).size());
        sameAs.setValue(sameC);

        metricSet.add(sameAs);
        metricSet.add(diffFrom);
        metricSet.add(nid);
        metricSet.add(nod);
    }

    public void processDir(File[] dir, File outputDir, OWL2Graph<DirectedGraph<NamedNode, NamedParamEdge>, NamedNode, NamedParamEdge> owl2Graph)
            throws IOException, OWLOntologyCreationException {
        for (File f : dir) {
            if (f.isDirectory()) {
                logger.info("Processing dir: " + f.getAbsolutePath());
                processDir(f.listFiles(), outputDir, owl2Graph);
                logger.info("Completed dir: " + f.getAbsolutePath());
            } else if (f.isFile() && DirectoryProcessor.isValidOntology(f)) {
                logger.info("Processing file: " + f.getAbsolutePath());
                handleOneOntology(outputDir, owl2Graph, f);
            }
        }
    }

    private void handleOneOntology(File outputDir, OWL2Graph<DirectedGraph<NamedNode, NamedParamEdge>, NamedNode, NamedParamEdge> owl2Graph, File f)
            throws IOException, OWLOntologyCreationException {
        String fName = f.getName();
        CSVWriter writer = new CSVWriter(new FileWriter(new File(outputDir, fName + ".c.csv")));
        try {
            writer.writeNext(getClassMetricsHeader());
            OWLOntologyDocumentSource source = new FileDocumentSource(f);
            DirectedGraph<NamedNode, NamedParamEdge> graph = owl2Graph.loadOWLOntology(source, true);
            Map<NamedNode, Set<NamedEntityMetric<? extends Comparable>>> map = collectMetrics(graph, owl2Graph.getOntology());
            for (NamedNode node : map.keySet()) {
                if (valueOf(node.getProperties().get(OWL2Graph.NODE_ANON_TYPE_NAME).toString())) {
                    continue;
                }
                Set<NamedEntityMetric<? extends Comparable>> metricSet = map.get(node);

                SortedSet<String> types = node.getTypes();
                if (types.contains(OWL_CLASS.toString())) {
                    String[] concat = concat(node.getName(),
                            concat(types.toString(), transform(metricSet, VALUE_FUNCTION).toArray(new String[metricSet.size()])));
                    writer.writeNext(concat);
                }
                writer.flush();
            }
        } finally {
            reset();
            writer.close();
            owl2Graph.shutdown();
        }
    }

    private Set<NamedEntityMetric<? extends Comparable>> findOrCreateSet(NamedNode node) {
        Set<NamedEntityMetric<? extends Comparable>> metricSet;
        if (entityMetricMap.containsKey(node)) {
            metricSet = entityMetricMap.get(node);
        } else {
            metricSet = Sets.newLinkedHashSet();
        }
        return metricSet;
    }

    private int calculateDITForNode(NamedNode node, DirectedGraph<NamedNode, NamedParamEdge> graph) {
        int maxDIT = 0;
        boolean isolatedSub = true;
        for (NamedParamEdge e : graph.outgoingEdgesOf(node)) {
            if (e.getName().equals(RDFS_SUBCLASS_OF.toString())) {
                isolatedSub = false;
                break;
            }
        }
        if (isolatedSub) {
            return 0;
        }

        DepthFirstIterator<NamedNode, NamedParamEdge> iterator =
                new EdgeTypeSensitiveDepthFirstIterator(graph, node, RDFS_SUBCLASS_OF.toString());

        int curDit = 0;
        while (iterator.hasNext()) {
            NamedNode next = iterator.next();
            if (next.getName().equals(OWL_THING.toString())) {
                maxDIT = max(maxDIT, curDit);
                curDit = 0;
            } else {
                curDit++;
            }
        }
        return maxDIT;
    }

    public Set<OWLClass> getTopClasses(Map<NamedNode, Set<NamedEntityMetric<? extends Comparable>>> metrics,
                                       Set<String> metricNameSet, OWLOntology ontology, boolean localOnly, int length) {
        logger.info("Calculating top " + length + " classes as signature for metrics: " + metricNameSet);
        // 0: NOC, 1: NOP, 2: NID, 3: NOD, 4: DIT, 5: EVC, 6: DEC
        Set<OWLClass> classesInSignature = ontology.getClassesInSignature(!localOnly);
        Set<OWLClass> clsSet = Sets.newLinkedHashSet();
        OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();

        Map<String, SortedSetMultimap<Comparable, String>> maps = Maps.newHashMap();
        Map<String, SortedSetMultimap<Integer, String>> rankMaps = Maps.newHashMap();
        for (String n : metricNameSet) {
            maps.put(n, TreeMultimap.<Comparable, String>create(Collections.reverseOrder(), Collections.reverseOrder()));
            rankMaps.put(n, TreeMultimap.<Integer, String>create());
        }

        for (NamedNode n : metrics.keySet()) {
            if (n.getTypes().contains(OWL_CLASS.toString()) &&
                    !valueOf(n.getProperties().getProperty(OWL2Graph.NODE_ANON_TYPE_NAME))) {
                String clsIri = n.getName().substring(1, n.getName().length() - 1);
                if (!classesInSignature.contains(factory.getOWLClass(IRI.create(clsIri)))) {
                    continue;
                }
                for (NamedEntityMetric<? extends Comparable> m : metrics.get(n)) {
                    String name = m.getName();
                    if (metricNameSet.contains(name)) {
                        //logger.info("Adding to: " + name + " map: " + substring);
                        maps.get(name).put(m.getValue(), clsIri);
                    }
                }
            }
        }
        //System.out.println(maps);

        for (String m : metricNameSet) {
            int rank = 0;
            SortedSetMultimap<Comparable, String> valueMap = maps.get(m);
            Map<Comparable, Collection<String>> map = valueMap.asMap();
            for (Comparable key : map.keySet()) {
                rankMaps.get(m).putAll(rank, map.get(key));
                rank += map.get(key).size();
            }
        }
        //System.out.println("Rank maps = " + rankMaps);

        Map<String, Integer> clsRankMap = Maps.newLinkedHashMap();
        for (SortedSetMultimap<Integer, String> multimap : rankMaps.values()) {
            Map<Integer, Collection<String>> map = multimap.asMap();
            for (Integer rank : map.keySet()) {
                Collection<String> strings = map.get(rank);
                //System.out.println("rank: " + rank + " = " + strings);
                for (String iri : strings) {
                    if (clsRankMap.containsKey(iri)) {
                        clsRankMap.put(iri, clsRankMap.get(iri) + rank);
                    } else {
                        clsRankMap.put(iri, rank);
                    }
                }
            }
        }

        //System.out.println("clsRank map: " + clsRankMap);

        SortedSetMultimap<Integer, String> rankMap = TreeMultimap.create();
        for (String iri : clsRankMap.keySet()) {
            rankMap.put(clsRankMap.get(iri), iri);
        }
        //System.out.println("rank map: " + rankMap);

        loop:
        for (Integer i : rankMap.keys()) {
            for (String iri : rankMap.get(i)) {
                OWLClass c = factory.getOWLClass(IRI.create(iri));
                if (clsSet.size() == length) {
                    break loop;
                } else {
                    clsSet.add(c);
                }
            }
        }

        return clsSet;
    }

    public void reset() {
        if (null != entityMetricMap) {
            entityMetricMap.clear();
        }
    }

    public static void main(String[] args) throws IOException, OWLOntologyCreationException {
        NodeMetricsCollector collector = new NodeMetricsCollector();
        OWL2Graph<DirectedGraph<NamedNode, NamedParamEdge>, NamedNode, NamedParamEdge> owl2Graph =
                new OWL2GraphJGraphTNamedImpl();
        String inputDir = args[0];
        String outputDir = args[1];
        File output = new File(outputDir);
        if (!output.exists()) {
            output.mkdirs();
        }
        collector.processDir(new File[]{new File(inputDir)}, output, owl2Graph);
    }
}
