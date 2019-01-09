
package edu.monash.infotech.r2o2.demo;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.apache.commons.math3.util.Pair;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;

import java.io.*;
import java.net.URL;
import java.util.*;

@SuppressWarnings("Duplicates")

/**
 * This class include functions that are used to generate training and testing instances for the meta-reasoning framework R2O2*.
 *
 *  @author Yong-Bin Kang
 *  Date:   01/11/2018
 */
public class DataGeneration {

    private static final double TIMEOUT = 1800000;

    /**
     *
     * @param metricsFile
     * @param reasoningTimeDir
     * @return
     * @throws Exception
     */
    public String genMetricData(String metricsFile, String reasoningTimeDir) throws Exception {

        System.out.println("\n# Generate ontologies in the metrics data that are only used in measurement of reasoning time:");

        // key: ontology name, value: metric value vector
        HashMap<String, String[]> metricsHashMap = new HashMap<String, String[]>();
        HashSet<String> reasonedOntSet = new HashSet();

        // Read metrics data
        CsvReader csvReader = new CsvReader(metricsFile);
        csvReader.readHeaders();
        String headers[] = csvReader.getHeaders();
        while (csvReader.readRecord()) {
            String ontName = csvReader.get("Ontology");
            String metricData[] = csvReader.getValues();
            metricsHashMap.put(ontName, metricData);
        }
        System.out.println("Number of ontologies in the metrics data:" + metricsHashMap.size());

        // read files that contain reasoning time
        File reasoningTimeDirFile = new File(reasoningTimeDir);
        File files[] = reasoningTimeDirFile.listFiles();
        for (File f : files) {
            // file format to be handled: "string_reasoner.csv": ex) subset1_KONCLUDE.csv
            if (!f.getName().startsWith("files")) continue;

            // read a reasoning file
            CsvReader reasonerRT = new CsvReader(f.getAbsolutePath());
            reasonerRT.readHeaders();
            while (reasonerRT.readRecord()) {
                double reasoningTime = Double.parseDouble(reasonerRT.get("Classification time (ms)"));

                // ignore error ontologies
                if (reasoningTime == -1d) continue;

                String ontName = reasonerRT.get("Ont name");
                reasonedOntSet.add(ontName);
            }
            csvReader.close();
        }

        // write only metrics data that used by any of reasoners
        String outFilename = metricsFile + ".filter.csv";
        CsvWriter csvOutput = new CsvWriter(new FileWriter(outFilename), ',');
        csvOutput.writeRecord(headers);
        for (String ontName : reasonedOntSet) {
            csvOutput.writeRecord(metricsHashMap.get(ontName));
        }
        csvOutput.close();

        return outFilename;
    }

    /**
     * Apply the preprocessing steps, following the steps in : "Y.-B. Kang, J. Z. Pan, S. Krishnaswamy, W. Sawangphol, Y.-F. Li, How long will it take?, aaai 2014"
     * @param newMetricsFile This is metrics file that represents metrics values of the ontologies in the corpus
     * @param predefined_metric_file This is pre-chosen metrics file. If this file is Null, we apply the 3rd step of the preprocessing technique in the givne corpus.
     * @return Pair<String, Instances> String: duplicated ontology file name showing the pairs of duplicated ontologies, Instances that excluded duplicated ontologies.
     */
    public Pair<String, Instances> preprocess(String newMetricsFile, File predefined_metric_file) throws Exception {
        System.out.println("\n# Apply preprocessing steps:");

        Preprocessor preprocessor = new Preprocessor();
        Instances data = preprocessor.perform(newMetricsFile, predefined_metric_file);
        String dupOntFileName = preprocessor.getDupOntFileName();

        Pair<String, Instances> pair = new Pair<String, Instances>(dupOntFileName, data);
        return pair;
    }


    public Instances getInstancesByMetrics(Instances data, File metricFile) throws Exception {
        System.out.println("\n# Generate instances with only metrics in the given file:" + metricFile.toPath());

        Preprocessor preprocessor = new Preprocessor();
        data = preprocessor.getInstancesByMetrics(data, metricFile);

        return data;
    }

    private Instances createWekaAttributes(Instances metricInstances) throws Exception {

        ArrayList<Attribute> atts = new ArrayList<>();

        // set numeric values for features (we exclude ontologyname stored in the first position in metricInstances)
        for (int i = 0; i < metricInstances.numAttributes(); i++) {
            atts.add(metricInstances.attribute(i));
        }
        atts.add(new Attribute("reasoning_time"));

        // define relation name
        String rel = "metrics+reasoning_time";

        // create instances
        Instances instances = new Instances(rel, atts, 0);
        return instances;
    }

    private double handle_err_ont (double rt, int how_to_handle_err_onts) {

        double new_rt = -1;

        // handling error ontologies
        if (rt == -1) {
            if (how_to_handle_err_onts == 0) {
                new_rt = rt;
            } else if (how_to_handle_err_onts == 1) {
                new_rt = TIMEOUT; // timeout
            } else if (how_to_handle_err_onts == 2) {
                new_rt = TIMEOUT*2; // penalty
            }
        } else {
            new_rt = rt;
        }
        return new_rt;
    }

    private Instance createInstance(Instances combinedInstances, Instance metricInstance, double reasoningTime) throws Exception {
        double vals[] = new double[combinedInstances.numAttributes()];
        for (int i = 0; i < metricInstance.numAttributes(); i++) {
            if (i == 0)
                vals[i] = combinedInstances.attribute(0).addStringValue(metricInstance.stringValue(i));
            else
                vals[i] = metricInstance.value(i);
        }
        vals[combinedInstances.numAttributes() - 1] = reasoningTime;

        return new DenseInstance(1, vals);
    }

    /**
     * Given the instances of ontologies, we combine their metric values and reasoning time. The combined instances will be used for R2O2*.
     * @param data Ontologies represented by their metric values
     * @param dupOntFileName The duplicated ontology file showing pairs of duplicated ontology names
     * @param reasoningTimeDir The data directory
     * @param how_to_handle_err_onts This value is 0, if we ignore errored ontologies, otherwise errored ontologies' reasoning time will be timeout (30 mins in our exp).
     * @throws Exception
     */
    public void genMetricAndClsTimeData(Instances data, String dupOntFileName, String reasoningTimeDir, int how_to_handle_err_onts) throws Exception {
        System.out.println("\n# Generate combined form of metrics and reasoning time data:");

        // key: ontology name, value: its index
        HashMap<String, Integer> metricsHashMap = new HashMap();

        // Read metrics data
        for (int i = 0; i < data.numInstances(); i++) {
            Instance instance = data.instance(i);
            metricsHashMap.put(instance.stringValue(0), i);
        }
        System.out.println("Number of ontologies in the metrics instances:" + metricsHashMap.size());

        // Read duplicated ontology names
        HashMap<String, String> ontNameMap = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new FileReader(dupOntFileName));
        String line = "";
        while ((line = reader.readLine()) != null) {
            String onts[] = line.split(",");
            ontNameMap.put(onts[0], onts[1]);
        }
        reader.close();


        // -------------------------------
        // ELK reasoner dataset generation
        {
            // The following file shows reasoning time of ELK on the ORE2015 ontologies
            File file = new File(reasoningTimeDir + "/ELK_reasoning_time.csv");
            if (!file.exists()) {
                throw new Exception("ELK reasoning file doesn't exist!");
            }
            // read reasoner name
            String reasoner = file.getName().split("_")[0].toLowerCase();
            Instances combinedInstances = createWekaAttributes(data);

            CsvReader reasonerRT = new CsvReader(file.getAbsolutePath());
            reasonerRT.readHeaders();
            while (reasonerRT.readRecord()) {
                String ontName = reasonerRT.get("Ont name");
                double rt = Double.parseDouble(reasonerRT.get("Classification time (ms)"));
                if (rt == -1) {
                    rt = TIMEOUT;
                }
                Double rtDouble = Math.log1p(rt);

                // generate a string array that contain metrics+reasoning time
                String keyOntName = ontNameMap.get(ontName);
                if (ontName.equalsIgnoreCase(keyOntName)) {
                    Integer ontIndex = metricsHashMap.get(keyOntName);
                    Instance metricInstance = data.instance(ontIndex);
                    Instance newInstance = createInstance(combinedInstances, metricInstance, rtDouble);
                    combinedInstances.add(newInstance);
                }
            }
            reasonerRT.close();
            System.out.println(reasoner + "'s instance #:" + combinedInstances.numInstances());
            File elk_dir = new File(reasoningTimeDir + "/elk");
            createArffFile(elk_dir.getAbsoluteFile() + "/r." + reasoner + ".arff", combinedInstances);
        }
        // -------------------------------

        // read files that contain reasoning time
        {
            File reasoningTimeDirFile = new File(reasoningTimeDir);
            File files[] = reasoningTimeDirFile.listFiles();

            for (File f : files) {
                // file format to be handled: "string_reasoner.csv": ex) subset1_KONCLUDE.csv
                if (!f.getName().startsWith("files")) continue;

                // read reasoner name
                String reasoner = f.getName().split("_")[1].split("\\.")[0].toLowerCase();

                // create the set (combinedInstances)
                Instances combinedInstances = createWekaAttributes(data);

                CsvReader reasonerRT = new CsvReader(f.getAbsolutePath());
                reasonerRT.readHeaders();
                while (reasonerRT.readRecord()) {
                    String ontName = reasonerRT.get("Ont name");
                    double rt = Double.parseDouble(reasonerRT.get("Classification time (ms)"));
                    rt = handle_err_ont(rt, how_to_handle_err_onts);
                    if (rt == -1) continue;

                    Double rtDouble = Math.log1p(rt);

                    // generate a string array that contain metrics+reasoning time
                    String keyOntName = ontNameMap.get(ontName);
                    if (ontName.equalsIgnoreCase(keyOntName)) {
                        //System.out.println(ontName + ", keyOntName:" + keyOntName);
                        Integer ontIndex = metricsHashMap.get(keyOntName);
                        Instance metricInstance = data.instance(ontIndex);
                        Instance newInstance = createInstance(combinedInstances, metricInstance, rtDouble);
                        combinedInstances.add(newInstance);
                    }
                }
                reasonerRT.close();

                System.out.println(reasoner + "'s instance #:" + combinedInstances.numInstances());
                createArffFile(reasoningTimeDir + "/" + "r." + reasoner + ".arff", combinedInstances);
            }
        }
    }

    private Set<String> findCommonOntSet(HashMap<String, Set<String>> allReasonerOntMap) throws Exception {

        Set<String> totalSet = new LinkedHashSet<>();
        for (Set<String> set : allReasonerOntMap.values()) {
            totalSet.addAll(set);
        }

        for (Set<String> set : allReasonerOntMap.values()) {
            totalSet.retainAll(set);
        }
        return totalSet;
    }

    private void write_common_onts (HashMap<String, Instances> allInstancesMap, String dir, List<String> common_onts, HashMap<String, HashMap<String, Integer>>reasonerOntMap) throws Exception {

        List<String> reasonerNames = new ArrayList(allInstancesMap.keySet());

        BufferedWriter w = new BufferedWriter(new FileWriter(dir + "/ont_names_metrics.csv"));
        w.write("name,metrics\n");

        for (int i = 0; i < common_onts.size(); i++) {

            String ontName = common_onts.get(i);
            String reasonerName = reasonerNames.get(0);
            Instances instances = allInstancesMap.get(reasonerNames.get(0));
            Instance instance = instances.get(reasonerOntMap.get(reasonerName).get(ontName));

            // write a row data
            StringBuilder key = new StringBuilder();
            for (int a = 1; a < instance.numAttributes() - 1; a++) {
                key.append(instance.value(a));
            }
            w.write(ontName + "," + key.toString() + "\n");
        }
        w.close();
    }

    public List<String> getTrainCV(List<String> data, int fold, int i) {

        int numInstForFold, first, offset;

        numInstForFold = data.size() / fold;
        if (i < data.size() % fold) {
            numInstForFold++;
            offset = i;
        } else {
            offset = data.size() % fold;
        }

        List<String> train = new ArrayList<>();
        first = i * (data.size() / fold) + offset;
        copyInstances(0, data, train, first);
        copyInstances(first + numInstForFold, data, train, data.size() - first - numInstForFold);

        return train;
    }

    public List<String> getTestCV(List<String> data, int fold, int i) {

        int numInstForFold, first, offset;

        numInstForFold = data.size() / fold;
        if (i < data.size() % fold) {
            numInstForFold++;
            offset = i;
        } else {
            offset = data.size() % fold;
        }

        List<String> test = new ArrayList<>();
        first = i * (data.size() / fold) + offset;
        copyInstances(first, data, test, numInstForFold);

        return test;
    }

    public void copyInstances(int from, List<String> data, List<String> dest, int num) {

        for (int i = 0; i < num; i++) {
            dest.add(data.get(from + i));
        }
    }

    /**
     * This function generate cross-validation datasets, where each dataset contains the training data and testing data
     * @param reasoningTimeDataDir The data directory
     * @param nFold N-fold cross-validation, where N is given
     * @param data_gen_seed The random seed to generate the cross-validation datasets.
     */
    public void genMetaData(String reasoningTimeDataDir, int nFold, int data_gen_seed) throws Exception {
        System.out.println("\n# Generate datasets:");

        HashMap<String, Instances> allInstancesMap = new LinkedHashMap<>();

        // ontology sets for all reasoners
        // 1st key: reasoner name, 2nd map's key: ont name, 2nd map's value: instance index
        HashMap<String, HashMap<String, Integer>> reasonerOntMap = new HashMap<>();

        // non-common ontology sets for all reasoners
        // key: reasoner name, value: ont name set
        HashMap<String, Set<String>> allReasonerOntMap = new LinkedHashMap<>();

        // add all reasoning times of all reasoners
        HashMap<String, Double> totalReasoningTimeMap = new HashMap<>();

        File reasoningTimeDirFile = new File(reasoningTimeDataDir);
        File files[] = reasoningTimeDirFile.listFiles();

        for (File file : files) {
            if (!(file.getName().startsWith("r.") && file.getName().endsWith(".arff")))
                continue;

            // set the reasoner name
            String reasoner = file.getName().split("\\.")[1];

            // load arff file and convert it into instances
            Instances data = loadInstances(file.getAbsolutePath());
            allInstancesMap.put(reasoner, data);

            // create Set<String> that contains ont names for each reasoner
            Set<String> ontSet = new LinkedHashSet<>();

            // create Set<String> that contains ont names for each reasoner
            HashMap<String, Integer> instanceIndexMap = new HashMap<>();

            // set common & non-common ontologies
            for (int i = 0; i < data.numInstances(); i++) {
                String ontName = data.instance(i).stringValue(0);
                double reasoningTime = data.instance(i).classValue();

                // add the ont name to the reasoner's ont name set
                ontSet.add(ontName);

                // remember the instance index of the ont name in the reasoner
                instanceIndexMap.put(ontName, i);

                // add the reasoning time
                if (totalReasoningTimeMap.containsKey(ontName)) {
                    totalReasoningTimeMap.put(ontName, totalReasoningTimeMap.get(ontName) + reasoningTime);
                } else {
                    totalReasoningTimeMap.put(ontName, reasoningTime);
                }
            }
            allReasonerOntMap.put(reasoner, ontSet);
            reasonerOntMap.put(reasoner, instanceIndexMap);
        }

        // Find common ontology set
        Set<String> commonOntSet = findCommonOntSet(allReasonerOntMap);
        List<String> commonOntList = new ArrayList(commonOntSet);
        Collections.shuffle(commonOntList, new Random(data_gen_seed));
        write_common_onts(allInstancesMap, reasoningTimeDataDir, commonOntList, reasonerOntMap);
        System.out.println("# Common ontology #:" + commonOntSet.size());

        // generate different datasets in each fold
        for (int fold = 0; fold < nFold; fold++) {

            // create each fold's directory
            File dir = new File(reasoningTimeDataDir + "/" + (fold));
            if (!dir.exists()) dir.mkdir();
            System.out.print("\tfold:" + fold + ",");

            List<String> train = getTrainCV(commonOntList, nFold, fold);
            List<String> test = getTestCV(commonOntList, nFold, fold);

            // Generate datasets for different meta-reasoners in our framework.

            // (1) Generate datasets for building prediction models used in R2O2*_pt
            generateDataForR2O2_pt(allInstancesMap, dir.getAbsolutePath(), train, reasonerOntMap);

            // (2) Geneate datasets for building rankers used in R2O2*_rk
            generateDataForR2O2_rk(allInstancesMap, dir.getAbsolutePath(), train, test, reasonerOntMap, "arff");

            // (3) Generate datasets for building the rank-based prediction model for R2O2*_mc
            generateDataForR2O2_mc(allInstancesMap, dir.getAbsolutePath(), train, test, reasonerOntMap, "arff");

            System.out.println("\tgenerating data is done:" + "train #:" + train.size() +  ", test #:" + test.size());
        }

        // Generate the data for buliding the prediction model of ELK
        genELKMetaData(reasoningTimeDataDir, nFold, data_gen_seed, commonOntList);
    }

    /**
     * Generate dataset for building the prediction model of ELK
     */
    public void genELKMetaData(String reasoningTimeDataDir, int nFold, int data_gen_seed, List<String> commonOntList) throws Exception {

        System.out.println("\n# Generate datasets (ELK):");

        // ontology sets for all reasoners
        // 1st key: reasoner name, 2nd map's key: ont name, 2nd map's value: instance index
        HashMap<String, HashMap<String, Integer>> reasonerOntMap = new HashMap<>();

        // non-common ontology sets for all reasoners
        // key: reasoner name, value: ont name set
        HashMap<String, Set<String>> allReasonerOntMap = new LinkedHashMap<>();

        File elk_file = new File(reasoningTimeDataDir + "/elk/r.elk.arff");
        if (!elk_file.exists()) {
            throw new Exception("ELK reasoner arff file doesn't exist!");
        }

        // set the reasoner name
        String reasoner = elk_file.getName().split("\\.")[1];

        // load arff file and convert it into instances
        Instances data = loadInstances(elk_file.getAbsolutePath());

        // create Set<String> that contains ont names for each reasoner
        Set<String> ontSet = new LinkedHashSet<>();

        // create Set<String> that contains ont names for each reasoner
        HashMap<String, Integer> instanceIndexMap = new HashMap<>();

        // set common & non-common ontologies
        for (int i = 0; i < data.numInstances(); i++) {
            String ontName = data.instance(i).stringValue(0);
            ontSet.add(ontName);
            instanceIndexMap.put(ontName, i);
        }
        allReasonerOntMap.put(reasoner, ontSet);
        reasonerOntMap.put(reasoner, instanceIndexMap);

        // common ontology set
        System.out.println("# Common ontology #:" + commonOntList.size());

        // generate different datasets in each fold
        for (int fold = 0; fold < nFold; fold++) {

            // create each fold's directory
            File dir = new File(reasoningTimeDataDir + "/" + (fold));
            if (!dir.exists()) dir.mkdir();
            System.out.print("\tfold:" + fold + ",");

            List<String> train = getTrainCV(commonOntList, nFold, fold);
            List<String> test = getTestCV(commonOntList, nFold, fold);
            System.out.println("\ttrain size:" + train.size());

            gen_elk_data(dir.getAbsolutePath(), data, train, reasonerOntMap);
        }
    }

    private void gen_elk_data(String foldDir, Instances data, List<String> candidateOntNameList,
                              HashMap<String, HashMap<String, Integer>> reasonerOntMap) throws Exception {

        File pmDir = new File(foldDir + "/elk");
        if (!pmDir.exists()) pmDir.mkdir();

        String reasonerName = "elk";

        // the file to be used for input to the prediction models
        BufferedWriter inputPMWriter = new BufferedWriter(new FileWriter(pmDir.toPath() + "/r." + reasonerName + ".arff"));
        inputPMWriter.write("@relation input-for-prediction-models\n\n");

        // write attribute declaration
        for (int i = 0; i < data.numAttributes() - 1; i++) {
            // copy attributes from the original instances
            Attribute attr = data.attribute(i);
            if (attr.isNumeric()) {
                inputPMWriter.write("@attribute " + attr.name().trim() + " numeric\n");
            } else if (attr.isNominal()) {
                inputPMWriter.write("@attribute " + attr.name().trim() + " {" + print(attr.enumerateValues()) + "}\n");
            }
        }
        inputPMWriter.write("@attribute reasoning_time numeric\n\n");
        inputPMWriter.write("@data\n");

        // write data
        for (int i = 0; i < candidateOntNameList.size(); i++) {
            String ontName = candidateOntNameList.get(i);

            // find attributes and reasoning times of reasoners
            StringBuilder rowForInputPredWriter = new StringBuilder();
            Instance instance = data.get(reasonerOntMap.get(reasonerName).get(ontName));
            for (int a = 1; a < instance.numAttributes()-1; a++) {
                rowForInputPredWriter.append(instance.value(a) + ",");
            }
            double reasoningTime = instance.value(instance.numAttributes()-1);
            rowForInputPredWriter.append(reasoningTime);
            inputPMWriter.write(rowForInputPredWriter.toString() + "\n");
        }
        inputPMWriter.close();
    }

    private void generateMetaDataForRankPredictionModel(HashMap<String, Instances> allInstancesMap,
                                                        List<String> candidateOntNameList,
                                                        HashMap<String, HashMap<String, Integer>> reasonerOntMap,
                                                        String directory,
                                                        String extension) throws Exception {

        // Read each reasoner's instances
        List<String> reasonerNames = new ArrayList(allInstancesMap.keySet());

        // create two writers: class dimension is different: (1) ranking of reasoners, (2) actual reasoning time
        BufferedWriter rankWriter = new BufferedWriter(new FileWriter(directory + "/pm-rank." + extension));

        // write relation name
        rankWriter.write("@relation meta-pm-rank-dataset\n\n");

        // write attribute declaration
        Instances data = allInstancesMap.get(reasonerNames.get(0));
        for (int i = 0; i < data.numAttributes() - 1; i++) {
            // copy attributes from the original instances
            Attribute attr = data.attribute(i);
            if (attr.isNumeric()) {
                rankWriter.write("@attribute " + attr.name().trim() + " numeric\n");
            } else if (attr.isNominal()) {
                rankWriter.write("@attribute " + attr.name().trim() + " {" + print(attr.enumerateValues()) + "}\n");
            }
        }
        rankWriter.write("@attribute reasoner {");
        for (int i = 0; i < reasonerNames.size(); i++) {
            if (i == reasonerNames.size()-1)
                rankWriter.write(reasonerNames.get(i) + "}\n\n");
            else
                rankWriter.write(reasonerNames.get(i) + ",");
        }

        rankWriter.write("@data\n");

        // write data
        for (int i = 0; i < candidateOntNameList.size(); i++) {

            String ontName = candidateOntNameList.get(i);

            // find attributes and reasoning times of reasoners
            StringBuilder rowForRankWriter = new StringBuilder();

            Instance instance = null;
            double reasoningTime[] = new double[reasonerNames.size()];
            for (int r = 0; r < reasonerNames.size(); r++) {
                String reasonerName = reasonerNames.get(r);
                Instances instances = allInstancesMap.get(reasonerName);
                instance = instances.get(reasonerOntMap.get(reasonerName).get(ontName));
                reasoningTime[r] = instance.value(instance.numAttributes()-1);
            }

            // write a row data
            int rank[] = rankLowFirst(reasoningTime);
            for (int r = 0; r < reasonerNames.size(); r++) {
                if (rank[r] == 1) {
                    // generate attribute dimension by looking at the first reasoner's metrics
                    for (int a = 1; a < instance.numAttributes()-1; a++) {
                        rowForRankWriter.append(instance.value(a) + ",");
                    }
                    rowForRankWriter.append(reasonerNames.get(r) + "\n");
                }
            }

            rankWriter.write(rowForRankWriter.toString());
        }
        rankWriter.close();
    }

    /**
     * Generate datasets for building the rank-based prediction model used in R2O2*_mc
     */
    private void generateDataForR2O2_mc(HashMap<String, Instances> allInstancesMap,
                                        String foldDir,
                                        List<String> trainForRankers,
                                        List<String> testForRankers,
                                        HashMap<String, HashMap<String, Integer>> reasonerOntMap,
                                        String extension) throws Exception {

        // Check if there is the top_dir, otherwise create it.
        String trainTopDir = foldDir + "/train_mm";
        String testTopDir = foldDir + "/test_mm";
        File dir = new File(trainTopDir);
        if (!dir.exists()) dir.mkdir();
        dir = new File(testTopDir);
        if (!dir.exists()) dir.mkdir();

        // write metadata for training rank prediction models
        generateMetaDataForRankPredictionModel(allInstancesMap, trainForRankers, reasonerOntMap, trainTopDir, extension);

        // write metadata for testing rank prediction models
        generateMetaDataForRankPredictionModel(allInstancesMap, testForRankers, reasonerOntMap, testTopDir, extension);
    }

    private void generateMetaData(HashMap<String, Instances> allInstancesMap,
                                  List<String> candidateOntNameList,
                                  HashMap<String, HashMap<String, Integer>> reasonerOntMap,
                                  String directory,
                                  String extension) throws Exception {

        // Read each reasoner's instances
        List<String> reasonerNames = new ArrayList(allInstancesMap.keySet());

        // create two writers: class dimension is different: (1) ranking of reasoners, (2) actual reasoning time
        BufferedWriter rankWriter = new BufferedWriter(new FileWriter(directory + "/meta-rank-actual." + extension));
        BufferedWriter timeWriter = new BufferedWriter(new FileWriter(directory + "/meta-time-actual." + extension));

        // the file to be used for input to the prediction models
        BufferedWriter inputPMWriter = new BufferedWriter(new FileWriter(directory + "/input-pred." + extension));
        BufferedWriter ont_name_writer = new BufferedWriter(new FileWriter(directory + "/input-pred-name.csv"));

        // write relation name
        rankWriter.write("@relation meta-dataset\n\n");
        timeWriter.write("@relation meta-dataset\n\n");
        inputPMWriter.write("@relation input-for-prediction-models\n\n");

        // write attribute declaration
        Instances data = allInstancesMap.get(reasonerNames.get(0));
        for (int i = 0; i < data.numAttributes() - 1; i++) {
            // copy attributes from the original instances
            Attribute attr = data.attribute(i);
            if (attr.isNumeric()) {
                rankWriter.write("@attribute " + attr.name().trim() + " numeric\n");
                timeWriter.write("@attribute " + attr.name().trim() + " numeric\n");
                inputPMWriter.write("@attribute " + attr.name().trim() + " numeric\n");
            } else if (attr.isNominal()) {
                rankWriter.write("@attribute " + attr.name().trim() + " {" + print(attr.enumerateValues()) + "}\n");
                timeWriter.write("@attribute " + attr.name().trim() + " {" + print(attr.enumerateValues()) + "}\n");
                inputPMWriter.write("@attribute " + attr.name().trim() + " {" + print(attr.enumerateValues()) + "}\n");
            }
        }
        rankWriter.write("@attribute targets relational\n");
        timeWriter.write("@attribute targets relational\n");
        for (int i = 0; i < reasonerNames.size(); i++) {
            rankWriter.write("@attribute " + reasonerNames.get(i) + " numeric\n");
            timeWriter.write("@attribute " + reasonerNames.get(i) + " numeric\n");
        }
        rankWriter.write("@end targets\n\n");
        timeWriter.write("@end targets\n\n");
        inputPMWriter.write("@attribute reasoning_time numeric\n\n");

        rankWriter.write("@data\n");
        timeWriter.write("@data\n");
        inputPMWriter.write("@data\n");

        // write data
        for (int i = 0; i < candidateOntNameList.size(); i++) {

            String ontName = candidateOntNameList.get(i);
            ont_name_writer.write(i + "," + ontName + "\n");

            // find attributes and reasoning times of reasoners
            StringBuilder rowForRankWriter = new StringBuilder();
            StringBuilder rowForTimeWriter = new StringBuilder();
            StringBuilder rowForInputPredWriter = new StringBuilder();

            double reasoningTime[] = new double[reasonerNames.size()];
            for (int r = 0; r < reasonerNames.size(); r++) {
                String reasonerName = reasonerNames.get(r);
                Instances instances = allInstancesMap.get(reasonerName);
                Instance instance = instances.get(reasonerOntMap.get(reasonerName).get(ontName));

                if (r == 0) {
                    // generate attribute dimension by looking at the first reasoner's metrics
                    for (int a = 1; a < instance.numAttributes()-1; a++) {
                        rowForRankWriter.append(instance.value(a) + ",");
                        rowForTimeWriter.append(instance.value(a) + ",");
                        rowForInputPredWriter.append(instance.value(a) + ",");
                    }
                }
                reasoningTime[r] = instance.value(instance.numAttributes()-1);
            }

            // write a row data
            int rank[] = rankLowFirst(reasoningTime);
            for (int r = 0; r < reasonerNames.size(); r++) {
                if (r == 0) {
                    rowForRankWriter.append("'" + rank[r] + ",");
                    rowForTimeWriter.append("'" + reasoningTime[r] + ",");
                } else if (r < reasonerNames.size()-1) {
                    rowForRankWriter.append(rank[r] + ",");
                    rowForTimeWriter.append(reasoningTime[r] + ",");
                } else {
                    rowForRankWriter.append(rank[r] + "'");
                    rowForTimeWriter.append(reasoningTime[r] + "'");
                }
            }
            rowForInputPredWriter.append("-1"); // default: nothing

            rankWriter.write(rowForRankWriter.toString() + "\n");
            timeWriter.write(rowForTimeWriter.toString() + "\n");
            inputPMWriter.write(rowForInputPredWriter.toString() + "\n");
        }
        rankWriter.close();
        timeWriter.close();
        inputPMWriter.close();
        ont_name_writer.close();
    }

    private int[] rankLowFirst(double[] v) {
        double rank[] = new NaturalRanking(TiesStrategy.MINIMUM).rank(v);
        int rankInt[] = Ints.toArray(Doubles.asList(rank));
        return rankInt;
    }

    /**
     * Generate datasets for rankers used in R2O2*_rk
     */
    private void generateDataForR2O2_rk(HashMap<String, Instances> allInstancesMap,
                                        String foldDir,
                                        List<String> trainForRankers,
                                        List<String> testForRankers,
                                        HashMap<String, HashMap<String, Integer>> reasonerOntMap,
                                        String extension) throws Exception {

        // Check if there is the top_dir, otherwise create it.
        String trainTopDir = foldDir + "/train_mm";
        String testTopDir = foldDir + "/test_mm";
        File dir = new File(trainTopDir);
        if (!dir.exists()) dir.mkdir();
        dir = new File(testTopDir);
        if (!dir.exists()) dir.mkdir();

        // write metadata for training
        generateMetaData(allInstancesMap, trainForRankers, reasonerOntMap, trainTopDir, extension);

        // write metadata for testing
        generateMetaData(allInstancesMap, testForRankers, reasonerOntMap, testTopDir, extension);
    }

    /**
     * Generate datasets for building prediction models used in R2O2*_pt. The datasets will be created under "~/pm/*".
     */
    private void generateDataForR2O2_pt(HashMap<String, Instances> allInstancesMap, String foldDir, List<String> candidateOntNameList,
                                        HashMap<String, HashMap<String, Integer>> reasonerOntMap) throws Exception {

        File pmDir = new File(foldDir + "/pm");
        if (!pmDir.exists())
            pmDir.mkdir();

        // Read each reasoner's instances
        List<String> reasonerNames = new ArrayList(allInstancesMap.keySet());

        for (int r = 0; r < reasonerNames.size(); r++) {

            String reasonerName = reasonerNames.get(r);

            // the file to be used for input to the prediction models
            BufferedWriter inputPMWriter = new BufferedWriter(new FileWriter(pmDir.toPath() + "/r." + reasonerName + ".arff"));
            inputPMWriter.write("@relation input-for-prediction-models\n\n");

            // write attribute declaration
            Instances data = allInstancesMap.get(reasonerName);
            for (int i = 0; i < data.numAttributes() - 1; i++) {
                // copy attributes from the original instances
                Attribute attr = data.attribute(i);
                if (attr.isNumeric()) {
                    inputPMWriter.write("@attribute " + attr.name().trim() + " numeric\n");
                } else if (attr.isNominal()) {
                    inputPMWriter.write("@attribute " + attr.name().trim() + " {" + print(attr.enumerateValues()) + "}\n");
                }
            }
            inputPMWriter.write("@attribute reasoning_time numeric\n\n");
            inputPMWriter.write("@data\n");

            // write data
            Instances instances = allInstancesMap.get(reasonerName);
            for (int i = 0; i < candidateOntNameList.size(); i++) {
                String ontName = candidateOntNameList.get(i);

                // find attributes and reasoning times of reasoners
                StringBuilder rowForInputPredWriter = new StringBuilder();
                Instance instance = instances.get(reasonerOntMap.get(reasonerName).get(ontName));
                for (int a = 1; a < instance.numAttributes()-1; a++) {
                    rowForInputPredWriter.append(instance.value(a) + ",");
                }
                double reasoningTime = instance.value(instance.numAttributes()-1);
                rowForInputPredWriter.append(reasoningTime);
                inputPMWriter.write(rowForInputPredWriter.toString() + "\n");
            }
            inputPMWriter.close();
        }
    }

    private String print(Enumeration obj) {
        String res = "";
        while (obj.hasMoreElements()) {
            res += obj.nextElement() + ",";
        }
        return res.substring(0, res.length() - 1);
    }


    public Instances loadInstances(String arff_file) throws Exception {
        ArffLoader.ArffReader arff = new ArffLoader.ArffReader(new BufferedReader(new FileReader(arff_file)));
        Instances data = arff.getData();
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    public void createArffFile(String fileName, Instances data) throws Exception {
        ArffSaver writer = new ArffSaver();
        writer.setInstances(data);
        writer.setFile(new File(fileName));
        writer.writeBatch();
    }


    public double[] rank(double values[]) {
        return new NaturalRanking(TiesStrategy.MINIMUM).rank(values);
    }

    /**
     * The main function that is able to generate two different datasets used in R2O2*: (1) ErrorsRemoved and (2) ErrorsReplaced.
     * To know more about these datasets, please refer to our paper.
     * @param args N/A
     */
    public static void main(String args[]) throws Exception {

        // To generate the dataset ErrorsRemoved, set how_to_handle_err_onts to be 0;
        // To generate the dataset ErrorsReplaced, set how_to_handle_err_onts to be 1.
        int how_to_handle_err_onts = 0;

        DataGeneration dataGeneration = new DataGeneration();

        // Indicate the directory name. For example, if how_to_handle_err_onts = 1, then set dir_name to be ErrorsRemoved, otherwise ErrorsReplaced
        // in the provided demo package.
//        String dir = "data/ErrorsRemoved/"; //or reasoningTimeDataDir = "./data/ErrorsReplaced"
        URL erDirURL = dataGeneration.getClass().getClassLoader().getResource("Data/ErrorsRemoved");
        String dir = new File(erDirURL.getPath()).getPath();

        // Give a random seed for shuffling of the entire corpus (for reproducibility)
        int data_gen_seed = 9;

        // Cross-validation (10 means 10-fold cross-validation). We will generate 10 different datasets where each dataset has the different training
        // and testing data
        int nFold = 10;

        // Ontology metrics file
        String metricsFile = dir + "/ore2015.csv";

        // Pre-chosen ontology metrics from the 91 metrics. The 29 metrics have been chosen after preprocessing on ORE2014 dataset that is a larger dataset.
        // If this file is not given, we will apply the preprocessing technique on the current ontology metrics files (e.g. ore2015.csv).
        File predefined_metric_file = new File(dir + "/29_metrics.txt");

        // Generate metrics data that are used by any of reasoners successfully. That is, the metrics data don't include error ontologies.
        String newMetricsFile = dataGeneration.genMetricData(metricsFile, dir);

        // perform pre-processing on the metrics data
        Pair<String, Instances> pair = dataGeneration.preprocess(newMetricsFile, predefined_metric_file);
        String dupOntFileName = pair.getKey();
        Instances data = pair.getValue();

        // If predefined_metric_file is given, we only keep the ontologies in the file.
        if (predefined_metric_file != null)
            data = dataGeneration.getInstancesByMetrics(data, predefined_metric_file);

        // For each reasoner, generate its training example by combining metric values and reasoning times of the sample ontologies
        dataGeneration.genMetricAndClsTimeData(data, dupOntFileName, dir, how_to_handle_err_onts);

        // Divide the data for each reasoner into three different parts:
        // 1) for training prediction model, 2) for training meta-reasoner (ranking matrix), and 3) testing meta-reasoner
        dataGeneration.genMetaData(dir, nFold, data_gen_seed);
    }
}
