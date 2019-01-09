package edu.monash.infotech.owl2metrics.metrics.jgrapht;

import com.google.common.collect.Sets;
import com.sun.org.apache.xerces.internal.impl.io.MalformedByteSequenceException;
import edu.monash.infotech.owl2metrics.metrics.writer.MetricsWriter;
import edu.monash.infotech.owl2metrics.model.OntMetrics;
import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import edu.monash.infotech.owl2metrics.translate.jgrapht.OWL2GraphJGraphTImpl;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedNode;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedParamEdge;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jgrapht.DirectedGraph;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Set;

import static au.com.bytecode.opencsv.CSVWriter.DEFAULT_SEPARATOR;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class DirectoryProcessor {
    private static final Logger LOGGER = Logger.getLogger(DirectoryProcessor.class);

    private char delimiter = DEFAULT_SEPARATOR;
    private String csvFileName;
    private boolean measureExpressivity;
    private Set<String> processNames;

    private MetricsWriter writer;
    private OWL2Graph<DirectedGraph<NamedNode, NamedParamEdge>, NamedNode, NamedParamEdge> owl2Graph;


    public DirectoryProcessor(String csvFileName, boolean measureExpressivity) {
        this.csvFileName = csvFileName;
        this.writer = new MetricsWriter();
        this.owl2Graph = new OWL2GraphJGraphTImpl();
        this.measureExpressivity = measureExpressivity;
        this.processNames = Sets.newLinkedHashSet();
    }

    public DirectoryProcessor(String csvFileName, char delimiter, boolean measureExpressivity) {
        this(csvFileName, measureExpressivity);
        this.delimiter = delimiter;
    }

    public DirectoryProcessor(String csvFileName, char delimiter, boolean measureExpressivity, File processFile) throws IOException {
        this(csvFileName, delimiter, measureExpressivity);
        if (processFile != null) {
            FileReader fr = new FileReader(processFile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                processNames.add(line.trim());
            }
            br.close();
        }
    }

    public void writeHeader() throws IOException {
        writer.writeHeader(csvFileName, delimiter, measureExpressivity);
        //writer.writeHeader(csvFileName, delimiter, new String[] {"Ontology", "Expressivity", "EL", "QL", "RL", "DL", "FULL"});
    }

    public void processDirectory(String dirName) throws IOException {
//        URL resource = this.getClass().getClassLoader().getResource(dirName);
//        File dir = new File(resource.getPath());
        File dir = new File(dirName);

        processDirectory(new File[]{dir});
    }

    public void processDirectory(File[] dirs) throws IOException {
        for (File f : dirs) {
            if (isValidOntology(f) && f.isFile() && needToProcessFile(f)) {
                processOntology(f);
            } else if (f.isDirectory()) {
                LOGGER.info("Processing dir: " + f.getAbsolutePath());
                processDirectory(f.listFiles());
            }
        }
    }

    private boolean needToProcessFile(File f) {
        return processNames.isEmpty() || processNames.contains(f.getName());
    }

    public static boolean isValidOntology(File f) {
        String fileName = f.getName().toLowerCase();
        return fileName.endsWith(".owl") ||
                fileName.endsWith(".obo") ||
                fileName.endsWith(".txt") ||
                fileName.endsWith(".xml") ||
                fileName.endsWith(".rdf") ||
                fileName.endsWith(".owl.xml");
    }

    private void processOntology(File ontFile) {
        LOGGER.info("Processing ontology: " + ontFile.getAbsolutePath());

        String ontologyName = ontFile.getName();
        double fileSize = (double) ontFile.length() / 1024;
        try {
            processOneOntology(new FileDocumentSource(ontFile), ontologyName, fileSize);
        } catch (MalformedByteSequenceException e) {
            LOGGER.warn("Error reading ontology as a file", e);
            try {
                FileInputStream stream = new FileInputStream(ontFile);
                ReaderDocumentSource source = new ReaderDocumentSource(new InputStreamReader(stream, "UTF-8"));
                processOneOntology(source, ontologyName, fileSize);
            } catch (Exception e1) {
                LOGGER.error("Error processing ontology as a stream.", e1);
            }
        } catch (Exception e) {
            LOGGER.error("Eorror!", e);
        } finally {
            owl2Graph.shutdown();
        }
    }

    private void processOneOntology(OWLOntologyDocumentSource source, String ontologyName, double fileSize)
            throws OWLOntologyCreationException, IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        long start = getUserTime();
        DirectedGraph<NamedNode, NamedParamEdge> graph = owl2Graph.loadOWLOntology(source, true);
        MetricsCollector collector = new MetricsCollector(graph, owl2Graph.getOntology(), measureExpressivity);
        OntMetrics metrics = collector.collectMetrics(ontologyName);

        metrics.setSze(fileSize);

        long end = getUserTime();

        metrics.setCalculationTime((end - start) / 1000000);

        writer.writeMetrics(csvFileName, delimiter, ontologyName, metrics, false, true);

        LOGGER.info("Done.");
    }

    public static void main(String[] args) throws IOException {
        URL url = DirectoryProcessor.class.getClassLoader().getResource("log4j.properties");
        PropertyConfigurator.configure(url);
        //PropertyConfigurator.configure("log4j.properties");
        String dirName = args[0];
        String csvName = args[1];
        char delimiter = DEFAULT_SEPARATOR;
        if (args.length > 2) {
            delimiter = args[2].charAt(0);
        }
        File processFile = null;
        if (args.length > 3) {
            processFile = new File(args[3]);
        }
        File dir = new File(dirName);
        LOGGER.info("Parameters: ontsDir = " + dirName + ", csvName = " + csvName + ", delimiter = " + delimiter + ", to process onts in file: " + processFile);
        DirectoryProcessor processor = new DirectoryProcessor(csvName, delimiter, true, processFile);
        processor.writeHeader();
        processor.processDirectory(new File[]{dir});
        LOGGER.info("All ontologies processed!");
    }

    public static long getUserTime() {
        ThreadMXBean tb = ManagementFactory.getThreadMXBean();
        return tb.getCurrentThreadUserTime();
    }
}
