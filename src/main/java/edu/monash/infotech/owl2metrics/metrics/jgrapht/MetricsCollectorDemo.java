package edu.monash.infotech.owl2metrics.metrics.jgrapht;

import edu.monash.infotech.owl2metrics.metrics.writer.MetricsWriter;
import edu.monash.infotech.owl2metrics.model.OntMetrics;
import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import edu.monash.infotech.owl2metrics.translate.jgrapht.OWL2GraphJGraphTImpl;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedNode;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedParamEdge;
import org.apache.log4j.PropertyConfigurator;
import org.jgrapht.DirectedGraph;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by ybkan on 4/08/2016.
 */
public class MetricsCollectorDemo {

    public static void main(String[] args) {
        String url = "resources/log4j.properties";
        PropertyConfigurator.configure(url);

        String inputDir = "./data/test_mm";

        try {
            for (File f : new File(inputDir).listFiles()) {
                if (f.isFile() && edu.monash.infotech.owl2metrics.metrics.jgrapht.DirectoryProcessor.isValidOntology(f)) {

                    //if (f.getName().equalsIgnoreCase("00001.owl_functional.owl")) continue;;
                    System.out.println(f.getName());

                    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                    OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(f));
                    OWL2Graph<DirectedGraph<NamedNode, NamedParamEdge>, NamedNode, NamedParamEdge> owl2Graph = new OWL2GraphJGraphTImpl();
                    DirectedGraph<NamedNode, NamedParamEdge> graph = owl2Graph.loadOWLOntology(ontology, true);

                    edu.monash.infotech.owl2metrics.metrics.jgrapht.MetricsCollector collector = new edu.monash.infotech.owl2metrics.metrics.jgrapht.MetricsCollector(graph, ontology, false);
                    String ontName = ontology.getOntologyID().getOntologyIRI().toString();
                    OntMetrics ontMetrics = collector.collectMetrics(ontName);

                    Writer stringWriter = new StringWriter();
                    MetricsWriter mWriter = new MetricsWriter();
                    mWriter.writeHeader(stringWriter, ',');
                    mWriter.writeMetrics(stringWriter, ',', ontName, ontMetrics, false);
                    String metricValues = stringWriter.toString().replace("\"", "");
                    System.out.println("Ont Name:" + ontName + "'s metrics:\n" + metricValues);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
