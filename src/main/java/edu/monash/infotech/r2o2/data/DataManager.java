package edu.monash.infotech.r2o2.data;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import edu.monash.infotech.r2o2.prediction.Preprocessor;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;

import java.io.*;
import java.util.*;

/**
 * Created by ybkan on 15/02/2016.
 */
public class DataManager {

    private HashMap<String, Instances> _allInstancesMap;
    private static final double THRESHOLD_RATIO = 0.05;
    private static final double LOG_TIMEOUT = 15.096445;
    //private static final double LOG_TIMEOUT = 3600000;

    /**
     * Write the combined records of ontology metric values and reasoning time
     */
    public void genMetricAndClsTimeData(Instances metricInstances, String dupOntFile, String reasoningTimeDir) throws Exception {

        // key: ontology name, value: metric value vector
        HashMap<String, Integer> metricsHashMap = new HashMap();

        // Read metrics data
        for (int i = 0; i < metricInstances.numInstances(); i++) {
            Instance instance = metricInstances.instance(i);
            metricsHashMap.put(instance.stringValue(0), i);
        }
        System.out.println("Number of ontologies in the metrics instances:" + metricsHashMap.size());

        // Read duplicated ontology names
        HashMap<String, String> ontNameMap = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new FileReader(dupOntFile));
        String line = "";
        while ((line = reader.readLine()) != null) {
            String onts[] = line.split(",");
            ontNameMap.put(onts[0], onts[1]);
        }
        reader.close();

        // read files that contain reasoning time
        File reasoningTimeDirFile = new File(reasoningTimeDir);
        File files[] = reasoningTimeDirFile.listFiles();
        for (File f : files) {
            // file format to be handled: "string_reasoner.csv": ex) subset1_KONCLUDE.csv
            if (!f.getName().startsWith("subset")) continue;

            // read reasoner name
            String reasoner = f.getName().split("_")[1].split("\\.")[0].toLowerCase();

            // create the set (combinedInstances)
            Instances combinedInstances = createWekaAttributes(metricInstances);

            // read a reasoning file
            Set<String> keySet = new HashSet<>();
            Preprocessor preprocessor = new Preprocessor();

            CsvReader reasonerRT = new CsvReader(f.getAbsolutePath());
            reasonerRT.readHeaders();
            while (reasonerRT.readRecord()) {
                String ontName = reasonerRT.get("Ont name");
                double rt = Double.parseDouble(reasonerRT.get("Classification time (ms)"));
                if (rt == -1) continue; // reasoning time -1 means an error, which we ignore.

                Double rtDouble = Math.log1p(rt);
                //if (rtDouble == LOG_TIMEOUT) continue; // don't consider timeout ontologies
                //Double rtDouble = rt;

                // generate a string array that contain metrics+reasoning time
                String keyOntName = ontNameMap.get(ontName);
                if (keyOntName != null) {
                    //System.out.println(ontName + ", keyOntName:" + keyOntName);
                    Integer indexInMetricInstances = metricsHashMap.get(keyOntName);
                    Instance metricInstance = metricInstances.instance(indexInMetricInstances);
                    String key = preprocessor.generateKeyForInstance(metricInstance);
                    if (!keySet.contains(key)) {
                        keySet.add(key);
                        Instance newInstance = createInstance(combinedInstances, metricInstance, rtDouble);
                        combinedInstances.add(newInstance);
                    }
                }
            }
            reasonerRT.close();

            System.out.println(reasoner + "'s instance #:" + combinedInstances.numInstances());

            ArffSaver writer = new ArffSaver();
            writer.setInstances(combinedInstances);
            writer.setFile(new File(reasoningTimeDir + "/" + "r." + reasoner + ".arff"));
            writer.writeBatch();
        }
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

    public String genMetricData(String metricsFile, String reasoningTimeDir) throws Exception {

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
            if (!f.getName().startsWith("subset")) continue;

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
        // create a writer for each reasoner that will contain "metrics+reasoningtime"
        String outFilename = metricsFile + ".filter.csv";
        CsvWriter csvOutput = new CsvWriter(new FileWriter(outFilename), ',');
        csvOutput.writeRecord(headers);
        for (String ontName : reasonedOntSet) {
            // write a record
            csvOutput.writeRecord(metricsHashMap.get(ontName));
        }
        csvOutput.close();

        return outFilename;
    }

    // This function generates ranking matrix using all common ontologies from the given data.
    public void genData(String reasoningTimeDataDir, int nFold) throws Exception {

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
        _allInstancesMap = new LinkedHashMap<>();
        for (File file : files) {
            if (!(file.getName().startsWith("r.") && file.getName().endsWith(".arff"))) continue;

            // set the reasoner name
            String reasoner = file.getName().split("\\.")[1];

            // load arff file and convert it into instances
            ArffLoader.ArffReader arff = new ArffLoader.ArffReader(new BufferedReader(new FileReader(file)));
            Instances data = arff.getData();
            data.setClassIndex(data.numAttributes() - 1);
            _allInstancesMap.put(reasoner, data);

            // create Set<String> that contains ont names for each reasoner
            Set<String> ontSet = new HashSet<>();

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

        // common ontology set
        Set<String> commonOntSet = findCommonOntSet(allReasonerOntMap);
        List<String> commonOntList = new ArrayList(commonOntSet);
        System.out.println("Common ontology #:" + commonOntSet.size());

        // generate common ontology instances where the class is the averaged reasoning time over all reasoners
        int reasonerNum = reasonerOntMap.size();

        // generate different datasets in each fold
        for (int fold = 0; fold < nFold; fold++) {

            // create each fold's directory
            File dir = new File(reasoningTimeDataDir + "/" + (fold + 1));
            if (!dir.exists()) dir.mkdir();

            System.out.println("fold:" + fold);

            // randomize the data
            Collections.shuffle(commonOntList);
            int trainForPMSize = (int) (double) (commonOntList.size() * 0.9); // 90%: used for building prediction models
            List<String> trainForPM = new ArrayList(commonOntList.subList(0, trainForPMSize));
            List<String> others = new ArrayList(commonOntList.subList(trainForPMSize, commonOntList.size()));

            List<String> timeoutOnts = generatePMTrainInstances(dir.getAbsolutePath(), reasoningTimeDataDir, new HashSet(trainForPM), new HashSet(others), "arff");
            System.out.println("\ttime out onts #:" + timeoutOnts.size());

            // generate metadata for r2o2
            Collections.shuffle(timeoutOnts);
            others.addAll(timeoutOnts.subList(0, (int) (timeoutOnts.size() * 0.1)));

            List<String> trainForRankers = trainForPM;
            List<String> testForRankers = others;
            generateR2O2Instances(dir.getAbsolutePath(), trainForRankers, testForRankers, reasonerOntMap, "arff");

            System.out.println("\tgenerating data is done:" +
                    "train # for PM:" + trainForPM.size() +
                    ", train # for Rankers:" + trainForRankers.size() +
                    ", test # for Rankers:" + testForRankers.size());
        }
    }

    private Set<String> findCommonOntSet(HashMap<String, Set<String>> allReasonerOntMap) throws Exception {

        Set<String> commonSet = new HashSet<>();
        for (Set<String> set : allReasonerOntMap.values()) {
            if (commonSet.size() == 0) {
                commonSet = set;
            } else {
                commonSet.retainAll(set);
            }
        }
        return commonSet;
    }

    private void generateR2O2Instances(String foldDir,
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
        generateMetaData(trainForRankers, reasonerOntMap, trainTopDir, extension);

        // write metadata for testing
        generateMetaData(testForRankers, reasonerOntMap, testTopDir, extension);
    }


    private void generateMetaData(List<String> candidateOntNameList,
                                  HashMap<String, HashMap<String, Integer>> reasonerOntMap,
                                  String directory,
                                  String extension) throws Exception {

        // Read each reasoner's instances
        List<String> reasonerNames = new ArrayList(_allInstancesMap.keySet());

        // create two writers: class dimension is different: (1) ranking of reasoners, (2) actual reasoning time
        BufferedWriter rankWriter = new BufferedWriter(new FileWriter(directory + "/meta-rank-actual." + extension));
        BufferedWriter timeWriter = new BufferedWriter(new FileWriter(directory + "/meta-time-actual." + extension));

        // the file to be used for input to the prediction models
        BufferedWriter inputPMWriter = new BufferedWriter(new FileWriter(directory + "/input-pred." + extension));

        // write relation name
        rankWriter.write("@relation meta-dataset\n\n");
        timeWriter.write("@relation meta-dataset\n\n");
        inputPMWriter.write("@relation input-for-prediction-models\n\n");

        // write attribute declaration
        Instances data = _allInstancesMap.get(reasonerNames.get(0));
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

            // find attributes and reasoning times of reasoners
            StringBuilder rowForRankWriter = new StringBuilder();
            StringBuilder rowForTimeWriter = new StringBuilder();
            StringBuilder rowForInputPredWriter = new StringBuilder();

            double reasoningTime[] = new double[reasonerNames.size()];
            for (int r = 0; r < reasonerNames.size(); r++) {
                String reasonerName = reasonerNames.get(r);
                Instances instances = _allInstancesMap.get(reasonerName);
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
    }

    public Instances generatePMInstances(Instances testRawInstances) throws Exception {

        // Remove the first attribute - ontology name
        Remove remove = new Remove();
        remove.setAttributeIndices("1");
        remove.setInputFormat(testRawInstances);
        Instances newData = Filter.useFilter(testRawInstances, remove);

        // Add a numeric attribute
        Add filter = new Add();
        filter.setAttributeIndex("last");
        filter.setAttributeName("reasoning_time");
        filter.setInputFormat(newData);
        Instances newData2 = Filter.useFilter(newData, filter);

        for (int i = 0; i < newData2.numInstances(); i++) {
            newData2.instance(i).setValue(newData2.numAttributes() - 1, -1); // default reasoning time = -1
        }

        return newData2;
    }

    public Instances generateMetaInstances(Instances testRawInstances, String[] reasonerNames) throws Exception {

        // create a stringBuilder for meta-data
        StringBuilder metaDataStr = new StringBuilder();

        // write relation name
        metaDataStr.append("@relation meta-dataset\n\n");

        // write attribute declaration
        for (int i = 1; i < testRawInstances.numAttributes(); i++) { // ignore 0 index as it has ontology names
            // copy attributes from the original instances
            Attribute attr = testRawInstances.attribute(i);
            if (attr.isNumeric()) {
                metaDataStr.append("@attribute " + attr.name().trim() + " numeric\n");
            } else if (attr.isNominal()) {
                metaDataStr.append("@attribute " + attr.name().trim() + " {" + print(attr.enumerateValues()) + "}\n");
            }
        }
        metaDataStr.append("@attribute targets relational\n");
        for (int i = 0; i < reasonerNames.length; i++) {
            metaDataStr.append("@attribute " + reasonerNames[i] + " numeric\n");
        }
        metaDataStr.append("@end targets\n\n");
        metaDataStr.append("@data\n");

        // write data
        for (int i = 0; i < testRawInstances.size(); i++) {

            Instance inst = testRawInstances.instance(i);

            for (int j = 1; j < inst.numAttributes(); j++) {
                metaDataStr.append(inst.value(j) + ",");
            }

            // write a dummy ranking
            metaDataStr.append("'");
            for (int k = 0; k < reasonerNames.length-1; k++) {
                metaDataStr.append("-1,");
            }
            metaDataStr.append("-1'\n");
        }

        ArffLoader.ArffReader arff = new ArffLoader.ArffReader(new BufferedReader(new StringReader(metaDataStr.toString())));
        Instances testMetaInstances = arff.getData();
        testMetaInstances.setClassIndex(testMetaInstances.numAttributes() - 1);

        return testMetaInstances;
    }

    private int[] rankLowFirst(double[] v) {
        double rank[] = new NaturalRanking(TiesStrategy.MINIMUM).rank(v);
        int rankInt[] = Ints.toArray(Doubles.asList(rank));
        return rankInt;
    }

    private int[] rankLowFirst(int[] v) {
        double vDouble[] = Doubles.toArray(Ints.asList(v));
        double rank[] = new NaturalRanking(TiesStrategy.MINIMUM).rank(vDouble);
        int rankInt[] = Ints.toArray(Doubles.asList(rank));
        return rankInt;
    }

    private int[] rankHighFirst(double[] v) {
        double rank[] = new NaturalRanking().rank(v);
        for (int i = 0; i < rank.length; i++) {
            rank[i] = rank.length+1 - rank[i];
        }
        int rankInt[] = Ints.toArray(Doubles.asList(rank));
        return rankInt;
    }

    private int[] rankHighFirst(int[] v) {
        double vDouble[] = Doubles.toArray(Ints.asList(v));
        double rank[] = new NaturalRanking().rank(vDouble);
        for (int i = 0; i < rank.length; i++) {
            rank[i] = rank.length+1 - rank[i];
        }
        int rankInt[] = Ints.toArray(Doubles.asList(rank));
        return rankInt;
    }

    private String print(Enumeration obj) {
        String res = "";
        while (obj.hasMoreElements()) {
            res += obj.nextElement() + ",";
        }
        return res.substring(0, res.length() - 1);
    }

    private List<String> generatePMTrainInstances(String foldDir,
                                          String reasoningTimeDataDir,
                                          Set<String> commonOntSet,
                                          Set<String> nonPMOntSet,
                                          String fileExtension) throws Exception {

        Set<String> timeoutOnts = new HashSet<>();

        File reasoningTimeDataDirFile = new File(reasoningTimeDataDir);
        File files[] = reasoningTimeDataDirFile.listFiles();
        for (File file : files) {
            if (file.getName().startsWith("r.") && file.getName().endsWith(".arff")) {

                // set the reasoner name
                String reasoner = file.getName().split("\\.")[1];

                // load arff file and convert it into instances
                ArffLoader.ArffReader arff = new ArffLoader.ArffReader(new BufferedReader(new FileReader(file)));
                Instances data = arff.getData();
                data.setClassIndex(data.numAttributes() - 1);
                int orgInstanceNum = data.numInstances();

                int deletedNum = 0;
                for (int i = data.numInstances() - 1; i >= 0; i--) {
                    Instance instance = data.get(i);
                    String ontName = instance.stringValue(0);

                    // delete an ontology that does belong to the other ontologies
                    // that will be used for training and testing for R2O2.
                    if (nonPMOntSet.contains(ontName)) {
                        data.delete(i);
                        deletedNum++;
                    } else {
                        // delete a time out ontology because we don't include it in training regression models.
                        if (instance.classValue() == LOG_TIMEOUT) {
                            if (commonOntSet.contains(ontName))
                                timeoutOnts.add(ontName);

                            data.delete(i);
                            deletedNum++;
                        }
                    }
                }

                ArffSaver writer = new ArffSaver();
                writer.setInstances(data);
                writer.setFile(new File(foldDir + "/" + "r." + reasoner + "_reg." + fileExtension));
                writer.writeBatch();

                System.out.println("\treasoner:" + reasoner + ", org instance #:" + orgInstanceNum +
                        ", " + "deleted instance #:" + deletedNum + ", train data #:" + data.numInstances());
            }
        }
        return new ArrayList<String>(timeoutOnts);
    }

    public static void createArffFile(String fileName, Instances data) throws Exception {
        ArffSaver writer = new ArffSaver();
        writer.setInstances(data);
        writer.setFile(new File(fileName));
        writer.writeBatch();
    }

    public static void addPredictedOutput(double scores[], Instance instance, Instances predInstances) {

        double values[] = new double[instance.numAttributes()];

        // copy metric values
        for (int i = 0; i < instance.numAttributes()-1; i++) {
            values[i] = instance.value(i);
        }

        // set predicted ranking
        Instances bag = new Instances(instance.classAttribute().relation(), 0);
        double classValues[] = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            classValues[i] = scores[i];
        }
        bag.add(new DenseInstance(1.0, classValues));

        values[instance.numAttributes()-1] = predInstances.classAttribute().addRelation(bag);
        predInstances.add(new DenseInstance(1.0, values));

        /*System.out.println("class values:" + Arrays.toString(classValues));
        System.out.println("bag:" + bag);
        System.out.println("instance:" + new DenseInstance(1.0, values));
        System.out.println("instances:" +predInstances);*/
    }

    public static double[] rank(double values[]) {
        return new NaturalRanking(TiesStrategy.MINIMUM).rank(values);
    }

    public static double[] adjustRanking (double ranking[], double preds[]) {

        int sortedIndexSet[] = indexSort(preds);
        double newRanking[] = new double[ranking.length];

        HashMap<Integer, Integer> map = new HashMap<>();
        double higherRankPrediction = 0;
        for (int i = 0; i < ranking.length; i++) {
            double rank = ranking[sortedIndexSet[i]];
            double prediction = preds[sortedIndexSet[i]];

            if (i == 0) {
                higherRankPrediction = prediction;
            } else {
                higherRankPrediction = preds[map.get(sortedIndexSet[i-1])];
            }

            if (Math.abs(higherRankPrediction - prediction) <= (higherRankPrediction * THRESHOLD_RATIO)) {
                if (i != 0) {
                    newRanking[sortedIndexSet[i]] = newRanking[sortedIndexSet[i - 1]];
                    map.put(sortedIndexSet[i], map.get(sortedIndexSet[i-1]));
                } else {
                    newRanking[sortedIndexSet[i]] = rank;
                    map.put(sortedIndexSet[i], sortedIndexSet[i]);
                }
            } else {
                newRanking[sortedIndexSet[i]] = rank;
                map.put(sortedIndexSet[i], sortedIndexSet[i]);
            }
        }

        return newRanking;
    }

    public static int[] indexSort(final double[] v) {
        //System.out.println("raw:"+ Arrays.toString(v));
        final Integer[] II = new Integer[v.length];
        for (int i = 0; i < v.length; i++)
            II[i] = i;

        Arrays.sort(II, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (v[o1] < v[o2]) return -1;
                else if (v[o1] > v[o2]) return 1;
                else return 0;
            }
        });
        int[] ii = new int[v.length];
        for (int i = 0; i < v.length; i++)
            ii[i] = II[i];

        //System.out.println("sorted:" + Arrays.toString(ii));
        return ii;
    }

    public static double[] summarizePrediction (HashMap<String, Double> preds, String reasonerNames[]) {
        double predictions[] = new double[reasonerNames.length];
        for (int i = 0; i < reasonerNames.length; i++) {
            predictions[i] = preds.get(reasonerNames[i]);
        }
        return predictions;
    }
}
