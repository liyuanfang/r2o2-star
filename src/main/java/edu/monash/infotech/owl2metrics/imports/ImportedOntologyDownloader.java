package edu.monash.infotech.owl2metrics.imports;

import edu.monash.infotech.owl2metrics.metrics.jgrapht.DirectoryProcessor;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * $Id$ $Revision$ $Date$ $Author$
 */
public class ImportedOntologyDownloader {
    private Logger LOGGER = Logger.getLogger(ImportedOntologyDownloader.class);
    
    private static final OWLOntologyManager MANAGER = OWLManager.createOWLOntologyManager();
    private Set<IRI> processedOntSet;

    public ImportedOntologyDownloader() {
        processedOntSet = new HashSet<IRI>();
    }
    
    public void processDirectory(File file, File targetDir) {
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        LOGGER.info("Processing directory: " + file.getAbsolutePath());
        for (File f : file.listFiles()) {
            if (DirectoryProcessor.isValidOntology(f)) {
                processOntology(f, targetDir);
            } else if (f.isDirectory()) {
                processDirectory(f, targetDir);
            }
        }
        LOGGER.info("Done.");
    }
    
    public static void main(String[] args) {
        URL url = DirectoryProcessor.class.getClassLoader().getResource("log4j.properties");
        PropertyConfigurator.configure(url);

        String dirName = args[0];
        String targetName = args[1];

        File dir = new File(dirName);
        File targetDir = new File(targetName);
        ImportedOntologyDownloader downloader = new ImportedOntologyDownloader();
        downloader.processDirectory(dir, targetDir);
    }

    private void processOntology(File f, File targetDir) {
        LOGGER.info("Processing ontology: " + f.getAbsolutePath());

        try {
            FileInputStream stream = new FileInputStream(f);
            ReaderDocumentSource source = new ReaderDocumentSource(new InputStreamReader(stream, "UTF-8"));
            OWLOntology ontology = MANAGER.loadOntologyFromOntologyDocument(source);
            processImports(ontology, targetDir, true);
        } catch (Exception e) {
            LOGGER.error("Error loading ontology.", e);
        } 
    }

    private void processImports(OWLOntology ontology, File targetDir, boolean existing) {
        if (processedOntSet.contains(ontology.getOntologyID().getOntologyIRI())) {
            LOGGER.info("Ontology already processed: " + ontology.getOntologyID().getOntologyIRI());
            MANAGER.removeOntology(ontology);
            return;
        }
        if (!existing) {
            writeOntology(ontology, targetDir);
        }
        processedOntSet.add(ontology.getOntologyID().getOntologyIRI());
        Set<OWLOntology> imports = ontology.getDirectImports();
        for (OWLOntology o : imports) {
            LOGGER.debug("Importing ontology: " + o.getOntologyID());
            processImports(o, targetDir, false);
        }
        MANAGER.removeOntology(ontology);
    }

    private void writeOntology(OWLOntology ontology, File targetDir) {
        try {
            String fragment = ontology.getOntologyID().getOntologyIRI().getFragment();
            if (null == fragment) {
                fragment = "ontology_" + System.nanoTime() + ".owl";
            } else {
                if (!fragment.toLowerCase().endsWith(".owl")) {
                    fragment = fragment + ".owl";
                }
            }
            File newOntFile = new File(targetDir, fragment);
            LOGGER.info("Writing ontology: " + ontology.getOntologyID().getOntologyIRI() + " to file " + newOntFile.getAbsolutePath());
            OWLXMLOntologyFormat owlxmlFormat = new OWLXMLOntologyFormat();
            MANAGER.saveOntology(ontology, owlxmlFormat, IRI.create(newOntFile.toURI()));
        } catch (Exception e) {
            LOGGER.error("Error writing ontology: " + ontology.getOntologyID().getOntologyIRI(), e);
        }
    }
}
