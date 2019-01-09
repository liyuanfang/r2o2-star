package edu.monash.infotech.r2o2.reasoning;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.apache.log4j.Logger;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigExtension;
import org.semanticweb.ore.configuration.ConfigExtensionFactory;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.configuration.InitialConfigBaseFactory;
import org.semanticweb.ore.conversion.OntologyFormatDynamicConversionRedirector;
import org.semanticweb.ore.conversion.OntologyFormatRedirector;
import org.semanticweb.ore.execution.ReasonerQueryExecutionHandler;
import org.semanticweb.ore.interfacing.DefaultReasonerAdaptorFactory;
import org.semanticweb.ore.interfacing.DefaultReasonerDescriptionFactory;
import org.semanticweb.ore.interfacing.ReasonerAdaptorFactory;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.interfacing.ReasonerDescriptionManager;
import org.semanticweb.ore.querying.ClassificationQuery;
import org.semanticweb.ore.querying.DefaultQueryFactory;
import org.semanticweb.ore.querying.QueryFactory;
import org.semanticweb.ore.querying.QueryResponse;
import org.semanticweb.ore.querying.QueryResponseStoringHandler;
import org.semanticweb.ore.querying.TSVQueryResponseStoringHandler;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static edu.monash.infotech.r2o2.reasoning.ReasonerWrapper.REASONER_ID;


public class OntologyReasoningTimer {
    private final Logger logger = Logger.getLogger(getClass());

    // 10 minutes
    public static final long MAX_TIME_LIMIT = 10 * 60 * 1000L;

    // 10 GB
    public static final long MAX_MEM_LIMIT = 10737418240L;


    private int rounds;
    private long limit;
    private long memoryLimit;
    private boolean skip = true;

    private REASONER_ID reasonerId;

    private edu.monash.infotech.r2o2.reasoning.ReasonerWrapper reasonerWrapper;


    private Config config = null;
    private ReasonerQueryExecutionHandler reasonerExecutionHandler = null;

    private ReasonerAdaptorFactory reasonerAdaptorFactory = null;
    private OntologyFormatRedirector formatRedirector = null;
    private QueryResponseStoringHandler responseStoringHandler = null;
    private DefaultReasonerDescriptionFactory reasonerDescriptionFactory = null;
    private ReasonerDescriptionManager reasonerManager = null;

    public OntologyReasoningTimer() {
        this.rounds = 5;
        this.limit = MAX_TIME_LIMIT;
        this.memoryLimit = MAX_MEM_LIMIT;
    }

    public OntologyReasoningTimer(long limit) {
        this.limit = limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getLimit() {
        return limit;
    }

    public double recordAverageReasoningTime(final OWLOntology ontology, REASONER_ID reasonerId, int rounds, long limit)
            throws Exception {
        int i = 1;
        double average = 0.0;
        while (i <= rounds && (!skip || average < limit)) {
            double time = recordOntologyReasoningTime(ontology, reasonerId, limit);
            average = (average * (i - 1) + time) / i;
            i++;
        }
        logger.info("Average reasoning time for ont " + ontology.getOntologyID() + " = " + average + "ms.");
        return average;
    }

    public double recordAverageReasoningTime(final OWLOntology ontology, REASONER_ID reasonerId, int rounds)
            throws Exception {
        return recordAverageReasoningTime(ontology, reasonerId, rounds, limit);
    }

    public double recordOntologyReasoningTime(final OWLOntology subOntology, final REASONER_ID reasonerId) throws Exception {
        return recordOntologyReasoningTime(subOntology, reasonerId, limit);
    }

    public double recordOntologyReasoningTime(final OWLOntology subOntology, final REASONER_ID reasonerId, long limit)
            throws Exception {
        TimeLimiter limiter = new SimpleTimeLimiter(Executors.newFixedThreadPool(1));
        double time = limit;
        try {
            final edu.monash.infotech.r2o2.reasoning.ReasonerWrapper.ReasonerBundle bundle = reasonerWrapper.getBundle(reasonerId);
            time = limiter.callWithTimeout(
                    new Callable<Double>() {
                        @Override
                        public Double call() throws Exception {
                            return timeOneOntology(subOntology, bundle.factory, bundle.config);
                        }
                    }, limit, TimeUnit.MILLISECONDS, true
            );
        } catch (UncheckedTimeoutException e) {
            logger.warn("Time out on ontology: " + subOntology.getOntologyID().toString(), e);
        }

        logger.info("Ontology reasoning took " + time + "ms.");
        return time;
    }

    private double timeOneOntology(OWLOntology subOntology, OWLReasonerFactory factory, OWLReasonerConfiguration configuration) {
        logger.info("Start reasoning on: " +subOntology.getOntologyID().toString() + " using " + factory.getReasonerName());
        long start = getUserTime();
        OWLReasoner reasoner = factory.createNonBufferingReasoner(subOntology, configuration);
        boolean consistent = reasoner.isConsistent();
        if (consistent) {
            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
            for (OWLClass cls : subOntology.getClassesInSignature(true)) {
                reasoner.getSuperClasses(cls, true);
            }
        } else {
            logger.warn("Ont inconsistent: " + subOntology.getOntologyID());
        }
        long end = getUserTime();
        reasoner.dispose();
        return (end - start) * 1.0 / (1000 * 1000);
    }

    public static long getUserTime() {
        ThreadMXBean tb = ManagementFactory.getThreadMXBean();
        return tb.getCurrentThreadUserTime();
    }

    public void initialise(Properties properties) throws MalformedURLException {
        limit = Long.parseLong(properties.getProperty("time.limit", Long.toString(MAX_TIME_LIMIT)));
        rounds = Integer.parseInt((properties.getProperty("reasoning.rounds", "3")));
        reasonerId = REASONER_ID.getEnum(Integer.parseInt(properties.getProperty("reasoner.idx", "2")));
        memoryLimit = Long.parseLong(properties.getProperty("memory.limit", Long.toString(MAX_MEM_LIMIT)));
        reasonerWrapper = new ReasonerWrapper();
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        String[] skipFiles = Arrays.copyOfRange(args, 1, args.length);
        //String skipFileName = args[1];

        Set<String> skipSet = getSkipFiles(skipFiles);
        System.out.println("Ontologies to skip: " + skipSet.size());

        OntologyReasoningTimer timer = new OntologyReasoningTimer();
        Properties properties = new Properties();
        properties.load(new FileReader(new File("ont.reasoning.timer.properties")));
        System.out.println(properties);
        timer.initialise(properties);

        String rName = timer.reasonerId.name();
        String csvFileName = createCsvFileName(dir.getName() + "_" + rName);
        System.out.println("Writing to CSV file: " + csvFileName);
        CSVWriter csvWriter = new CSVWriter(new FileWriter(new File(csvFileName)));
        csvWriter.writeNext(new String[]{"Ont name", "Classification time (ms)", "Reasoner"});

        timer.processDir(dir, csvWriter, skipSet);

        csvWriter.close();
        System.exit(0);
    }

    private static String createCsvFileName(String rName) {
        String name = rName + ".csv";
        File file = new File(name);
        int idx = 0;

        while (file.exists()) {
            name = rName + "_" + idx++ + ".csv";
            file = new File(name);
            System.out.println("CSV file already present, trying new name: " + name);
        }
        return name;
    }

    private static Set<String> getSkipFiles(String[] skipFiles) throws IOException {
        Set<String> skipSet = new HashSet<String>();
        if (skipFiles != null || skipFiles.length > 0) {
            for (String skipFileName : skipFiles) {
                File skipFile = new File(skipFileName);
                CSVReader csvReader = new CSVReader(new FileReader(skipFile));
                try {
                    String[] nextLine;
                    while ((nextLine = csvReader.readNext()) != null) {
                        skipSet.add(nextLine[0]);
                    }
                } finally {
                    csvReader.close();
                }
            }
        }
        return skipSet;
    }

    private void processDir(File dir, CSVWriter csvWriter, Set<String> skipFile) {
        File[] files = dir.listFiles();
        if (null != files) {
            for (File f : files) {
                String name = f.getName().toLowerCase();
                if (f.isFile() && (name.endsWith(".xml") || name.endsWith(".owl"))) {
                    if (skipFile.isEmpty() || !skipFile.contains(f.getName())) {
                        processOneFile(f, csvWriter);
                    } else {
                        logger.info("File completed already, skipping: " + f.getName() + ".");
                    }
                } else if (f.isDirectory()) {
                    processDir(f, csvWriter, skipFile);
                }
            }
        }
    }

    private void processOneFile(File f, CSVWriter csvWriter) {
        String name = f.getName();
        OWLOntology ontology = null;
        logger.info("Processing ontology: " + f.getAbsolutePath());

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        String reasonerName = reasonerId.name();
        try {
            OWLOntologyLoaderConfiguration loaderConfiguration = new OWLOntologyLoaderConfiguration();
            loaderConfiguration = loaderConfiguration.setLoadAnnotationAxioms(false);
            ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(f), loaderConfiguration);

            if (ontology.getLogicalAxiomCount() == 0 || ontology.getSignature(true).size() == 0) {
                logger.info("Skipping empty ontology: " + f.getAbsolutePath());
                return;
            }
            //preprocessOntology(manager, ontology);
            double time = recordAverageReasoningTime(f, rounds);
            csvWriter.writeNext(new String[]{name, Double.toString(time), reasonerName});
            csvWriter.flush();
            manager.removeOntology(ontology);
        } catch (Exception e) {
            logger.error("Error processing " + f.getAbsolutePath(), e);
            csvWriter.writeNext(new String[]{name, "", reasonerName});
        } finally {
            if (null != ontology) {
                manager.removeOntology(ontology);
            }
            try {
                csvWriter.flush();
            } catch (IOException e) {
                logger.error("Error flushing csv writer.", e);
                e.printStackTrace();
            }
            System.gc();
        }
    }

    private void preprocessOntology(OWLOntologyManager manager, OWLOntology ontology) {
        removeAnnotationProperties(manager, ontology);
    }

    private double recordAverageReasoningTime(OWLOntology ontology) throws Exception {
        return recordAverageReasoningTime(ontology, reasonerId, rounds);
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    private void removeAnnotationProperties(OWLOntologyManager manager, OWLOntology ontology) {
        int size = ontology.getAnnotations().size();
        logger.info("Removing annotations from ontology: " + ontology.getOntologyID() + " with " + size + " annotations");
        Set<OWLAnnotationProperty> annotationProperties = ontology.getAnnotationPropertiesInSignature();
        for (OWLAnnotationProperty p : annotationProperties) {
            Set<OWLAnnotationAxiom> axioms = ontology.getAxioms(p);
            manager.removeAxioms(ontology, axioms);
            Set<OWLAnnotation> annotations = p.getAnnotations(ontology);
            ArrayList<OWLOntologyChange> list = Lists.newArrayList();
            for (OWLAnnotation a : annotations) {
                list.add(new RemoveOntologyAnnotation(ontology, a));
            }
            manager.applyChanges(list);
        }
        size = ontology.getAnnotations().size();
        logger.info("Removed annotations from ontology: " + ontology.getOntologyID() + " with " + size + " annotations");
    }

    public double recordAverageReasoningTime(final File file, int rounds, long limit)
            throws Exception {
        int i = 1;
        double average = 0.0;
        while (i <= rounds && (!skip || (average < limit && average >= 0.0))) {

            double time = recordOntologyReasoningTime(file, limit);
            average = (average * (i - 1) + time) / i;
            i++;

        }
        logger.info("Average reasoning time for ont " + file + " = " + average + "ms.");
        return average;
    }

    public double recordAverageReasoningTime(final File file, int rounds)
            throws Exception {
        return recordAverageReasoningTime(file, rounds, limit);
    }

    public double recordOntologyReasoningTime(final File file, long limit) throws Exception {
        // some configuration and initialising
        if (config == null) {
            InitialConfigBaseFactory initConfigFac = new InitialConfigBaseFactory();
            config = new ConfigExtension(initConfigFac.createConfig());
            ConfigExtensionFactory configExtFac = new ConfigExtensionFactory(config);

            // configure to not load results from file
            configExtFac.addConfigValue(config, ConfigType.CONFIG_TYPE_SAVE_LOAD_RESULTS_CODES.getConfigTypeString(), configExtFac.createConfigValueString(config, ConfigType.CONFIG_TYPE_SAVE_LOAD_RESULTS_CODES.getConfigTypeString(), "FALSE"));

            // pass time limit and memory limit to reasoner's starter script
            configExtFac.addConfigValue(config, ConfigType.CONFIG_TYPE_EXECUTION_ADD_TIMEOUT_AS_ARGUMENT.getConfigTypeString(), configExtFac.createConfigValueString(config, ConfigType.CONFIG_TYPE_EXECUTION_ADD_TIMEOUT_AS_ARGUMENT.getConfigTypeString(), "TRUE"));
            configExtFac.addConfigValue(config, ConfigType.CONFIG_TYPE_EXECUTION_ADD_MEMORY_LIMIT_AS_ARGUMENT.getConfigTypeString(), configExtFac.createConfigValueString(config, ConfigType.CONFIG_TYPE_EXECUTION_ADD_MEMORY_LIMIT_AS_ARGUMENT.getConfigTypeString(), "TRUE"));
        }
        if (reasonerAdaptorFactory ==  null) {
            reasonerAdaptorFactory = new DefaultReasonerAdaptorFactory();
        }
        if (formatRedirector ==  null) {
            // use if all ontologies are already in OWL Functional Style format
            //formatRedirector = new OntologyFormatNoRedictionRedirector();

            // use if conversion to OWL Functional Style format is potentially required
            formatRedirector = new OntologyFormatDynamicConversionRedirector(config);
        }
        if (responseStoringHandler ==  null) {
            responseStoringHandler = new TSVQueryResponseStoringHandler(config);
        }
        if (reasonerExecutionHandler ==  null) {
            reasonerExecutionHandler = new ReasonerQueryExecutionHandler(reasonerAdaptorFactory, formatRedirector, responseStoringHandler, config);
        }
        if (reasonerDescriptionFactory == null) {
            reasonerDescriptionFactory = new DefaultReasonerDescriptionFactory();
        }
        if (reasonerManager == null) {
            reasonerManager =  new ReasonerDescriptionManager(reasonerDescriptionFactory,config);
        }

        String reasonerPack = "";
        switch(reasonerId) {
            case CHAINSAW:
                reasonerPack = "chainsaw";
                break;
            case FACT:
                reasonerPack = "fact++";
                break;
            case HERMIT:
                reasonerPack = "hermit-owlapiv4";
                break;
            case JFACT:
                reasonerPack = "jfact";
                break;
            case KONCLUDE:
                reasonerPack = "konclude";
                break;
            case MORE:
                reasonerPack = "more-hermit";
                break;
            case PELLET:
                reasonerPack = "pellet";
                break;
            case TROWL:
                reasonerPack = "trowl";
                break;
            case ELK:
                reasonerPack = "elk";
                break;
            case RACER:
                reasonerPack = "racer";
                break;
            default:
                throw new RuntimeException("Unsupported reasoner: " + reasonerId.name());
        }

        // create a classification query
        QueryFactory queryFactory = new DefaultQueryFactory();
        ClassificationQuery query = queryFactory.createClassificationQuery(new FilePathString(file.getAbsoluteFile().toString()), new FilePathString(file.getAbsoluteFile().toString()), null);


        // load the reasoner specification (must be in './data/reasoners/' directory), alternatively a full path to the directory/file of the reasoner can be specified
        // TODO: adapt reasoner directory w.r.t. to your platform
        ReasonerDescription reasoner = reasonerManager.loadReasonerDescription(reasonerPack);

        // specify the temporary response directory
        // each reasoner gets a separate dir
        String responseDir = ConfigDataValueReader.getConfigDataValueString(config, ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY) +"tmp"+File.separator + reasonerPack + File.separator;

        // execute the query
        QueryResponse queryResponse = reasonerExecutionHandler.executeReasonerQuery(reasoner, query, responseDir, limit, memoryLimit);

        // only processing time reported by reasoner
        //double time = queryResponse.getReasonerQueryProcessingTime();

        // total execution time
        double time = queryResponse.getExecutionTime();
        time = Math.min(time, limit);

        if (queryResponse.hasTimedOut()) {
            logger.warn("Time out on ontology: " + file.toString());
        } else if (queryResponse.hasExecutionError()) {
            logger.warn("Execution failure on ontology: " + file.toString());
            time = -1.0;
        }

        logger.info("Ontology reasoning took " + time + "ms.");
        return time;
    }
}