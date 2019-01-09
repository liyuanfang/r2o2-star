package edu.monash.infotech.r2o2.demo;

import com.csvreader.CsvReader;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericTransform;
import weka.filters.unsupervised.attribute.Remove;

import java.io.*;
import java.util.*;

/**
 * Created by ybkan on 15/02/2016.
 */
public class Preprocessor {

    private double _mins[];
    private double _maxs[];
    private String _dupOntFile;

    /**
     * Function for defining preprocessing steps
     * @param metricFile The input ontology metrics file
     * @param predefined_metric_file The pre-chosen metrics file
     * @return Ontology instances after the preprocessing steps
     */
    public Instances perform(String metricFile, File predefined_metric_file) throws Exception {

        // step 1: remove duplicated ones
        Instances instances = removeDuplicatedMetrics(metricFile);

        // step 2: normalisation
        Instances normedInstances = norm(instances);

        // step 3: remove the near-zero predictors & remove highly correlated predictors.
        // If the pre-predefined_metric_file is given, we skip this step.
        Instances finalInstances = null;
        if (predefined_metric_file == null)
            finalInstances = removeBadMetrics(normedInstances);
        else
            finalInstances = normedInstances;

        return finalInstances;
    }

    /**
     * Remove duplicated ontologies in terms of metric values of the ontologies in the input file
     * @param metricFile The input ontology metrics file
     * @return Ontology instances after removing the duplicated ontologies
     */
    private Instances removeDuplicatedMetrics(String metricFile) throws Exception {

        // key: all metric values of each instance, value: ontology name
        HashMap<String, String> keyMap = new HashMap<>();

        // key: ontology, value: the ontology name kept in the "keys" variable
        HashMap<String, String> ontNameMap = new HashMap<>();

        CsvReader csvReader = new CsvReader(metricFile);
        csvReader.readHeaders();
        String headers[] = csvReader.getHeaders();

        // create weka instances
        Instances instances = createWekaAttributes(headers);
        int N = 0;
        while (csvReader.readRecord()) {
            String data[] = csvReader.getValues();
            String ontName = data[0];

            // generate the key of each ontology instance.
            String key = generateKeyForRecord(data);

            if (!keyMap.containsKey(key)) {
                keyMap.put(key, ontName);
                ontNameMap.put(ontName, ontName);
                instances.add(new DenseInstance(1, stringToDouble(instances, data)));
            } else {
                N++;
                ontNameMap.put(ontName, keyMap.get(key));
            }
        }
        csvReader.close();
        System.out.println(N + " instances were deleted due to their duplication");

        writeDupOntSet(metricFile, ontNameMap);
        return instances;
    }

    private Instances removeDuplicatedInstances(Instances data, String range) throws Exception {

        Instances newData = new Instances (data, 0);

        // get used attribute indices to check duplication
        Set<Integer> indexset = new HashSet<>();
        String indices[] = range.split(",");
        for (String s: indices) {
            indexset.add(Integer.parseInt(s));
        }

        // key: hashcode of attribute values of specified indices, value: instance
        Set<Integer> keyMap = new HashSet();

        for (int i = 0; i < data.numInstances(); i++) {
            int key = generateKeyForInstance(data.instance(i), indexset);
            if (!keyMap.contains(key)) {
                keyMap.add(key);
                newData.add(data.instance(i));
            }
        }

        return newData;
    }

    /**
     * Write pairs of ontologies names that indicate which ontologies have duplicated ones. In a pair, if the first ontology name is the same
     * with the second name, it means the first name doesn't have any duplicated one. Otherwise, the second name indicates the duplicated one with the
     * first name.
     * @param metricFile The ontology metrics file name
     * @param ontNameMap The set of pairs showing which ontologies have duplcated ones.
     * @throws Exception
     */
    private void writeDupOntSet(String metricFile, HashMap<String,String> ontNameMap) throws Exception {

        _dupOntFile = metricFile + ".dup";
        BufferedWriter writer = new BufferedWriter(new FileWriter(metricFile + ".dup"));
        Iterator it = ontNameMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String,String>) it.next();
            writer.write(entry.getKey() + "," + entry.getValue() + "\n");
        }
        writer.close();
    }

    /**
     * Remove nearzero variance metrics as well as highly-correlated metrics
     * @param data The set of ontologies where each is represented its log-scaled metric values
     * @return Instances after applying this function
     */
    private Instances removeBadMetrics (Instances data) throws Exception {

        // step 1: remove near-zero variance metrics
        Instances data1 = removeByNearZeroVar(data);

        // step 2: remove metrics that are highly correlated (threshold correlation coef = 0.9)
        Instances data2 = removeByCorrelation(data1);

        return data2;
    }

    public Instances getInstancesByMetrics (Instances data, File metricFile) throws Exception {

        BufferedReader reader = new BufferedReader(new FileReader(metricFile));
        String line = "";
        Set<String> metricNames = new HashSet<String>();
        while ((line = reader.readLine()) != null) {
            String metricName = line.split(" ")[1];
            metricNames.add(metricName.toLowerCase());
        }
        reader.close();
        System.out.println("# of important metrics given:" + metricNames.size());
        System.out.println("# of data metrics:" + data.numAttributes());

        HashSet<Integer> removalMetricIndices = new HashSet<>();
        for (int i = 1; i < data.numAttributes(); i++) {
            String metricName = data.attribute(i).name().toLowerCase();
            if (!metricNames.contains(metricName)) {
                removalMetricIndices.add(i);
            }
        }

        data = removeMetrics(data, removalMetricIndices);
        System.out.println("after removing - # of data metrics:" + data.numAttributes());

        return data;
    }

    private Instances removeByNearZeroVar (Instances data) throws Exception {
        Instances newData = new Instances (data);

        HashSet<Integer> removalMetricIndices = new HashSet<>();

        for (int i = 1; i < newData.numAttributes(); i++) {

            // step 1: remove metrics whose value variance is zero
            double values[] = newData.attributeToDoubleArray(i);
            double variance = StatUtils.variance(values);
            if (Double.isNaN(variance)) variance = 0;
            //System.out.println("attribute:" + newData.attribute(i).name() + ", variance=" + variance);
            if (variance == 0) {
                //System.out.println("zero variance attribute:" + newData.attribute(i).name() + ", variance=" + variance);
                removalMetricIndices.add(i);
                continue;
            }

            // step 2: remove metrics that have few unique values related to the number of samples (< 0.1)
            double uniqueValueNum = calUniqueValueNum(values);
            double uniqueRatio = uniqueValueNum/values.length;
            if (uniqueRatio < 0.1) {
                //System.out.println("few unique attribute:" + newData.attribute(i).name() + ", uniqueRatio=" + uniqueRatio + ": unique value num=" + uniqueValueNum + ", total num=" + values.length);
                removalMetricIndices.add(i);
                continue;
            }

            // step 3: remove metrics where the ratio of frequency of the most common values to the frequency of
            // the second most common values is large (> 19).
            //System.out.println("attribute:" + newData.attribute(i).name() + ", values=" + Arrays.toString(values));
            if (isLargeFreqDiff(values)) {
                //removalMetricIndices.add(i);
                continue;
            }
        }
        System.out.println("Metrics to be removed by nearZeroVar: #" + removalMetricIndices.size());

        // remove attributes
        newData = removeMetrics (newData, removalMetricIndices);

        return newData;
    }

    private Instances removeMetrics(Instances data, HashSet<Integer> removalMetricIndices) throws Exception {
        if (removalMetricIndices.size() > 0) {
            Remove remove = new Remove();
            remove.setOptions(Utils.splitOptions("-R " + getClassRange(new ArrayList(removalMetricIndices))));
            remove.setInputFormat(data);
            data = Filter.useFilter(data, remove);
        }
        return data;
    }

    private boolean isLargeFreqDiff (double[] values) {

        HashMap<Double, Integer> map = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            Integer count = map.get(values[i]);
            if (count == null) {
                map.put(values[i], 1);
            } else {
                map.put(values[i], count+1);
            }
        }

        List<Integer> freqs = new ArrayList(map.values());
        Collections.sort(freqs, Collections.reverseOrder());

        double mostFreq = freqs.get(0);
        double secondMostFreq = freqs.get(1);
        if (mostFreq/secondMostFreq > 19)
            return true;
        else
            return false;
    }

    private double calUniqueValueNum (double[] values) {

        HashSet<Double> set = new HashSet();
        for (double v: values) {
            set.add(v);
        }
        return set.size();
    }

    private Instances removeByCorrelation (Instances data) throws Exception {

        PearsonsCorrelation pc = new PearsonsCorrelation();

        // create a copy of the instances
        Instances newData = new Instances (data);

        // the variable that contains the correlation coefficients between metrics.
        double cc[][] = new double[newData.numAttributes()-1][newData.numAttributes()-1];

        for (int i = 1; i < newData.numAttributes()-1; i++) {
            cc[i-1][i-1] = 0; // correlation for itself
            double v1[] = newData.attributeToDoubleArray(i);

            for (int j = i+1; j < newData.numAttributes(); j++) {
                double v2[] = newData.attributeToDoubleArray(j);
                double correlation = pc.correlation(v1, v2);
                if (Double.isNaN(correlation)) correlation = 0;
                cc[i-1][j-1] = cc[j-1][i-1] = correlation;
            }
        }

        // calculate the mean correlation of each attribute
        double meanCC[] = new double[cc.length];
        for (int i = 0; i < cc.length; i++) {
            meanCC[i] = StatUtils.mean(cc[i]);
            //System.out.println("The mean absolute correlation:" + meanCC[i]);
        }

        // find removal metric indices
        HashSet<Integer> removalMetricIndices = new HashSet<>();
        for (int i = 0; i < cc.length; i++) {

            if (removalMetricIndices.contains(i+1)) continue;

            HashSet<Integer> candidates = new HashSet<>();
            candidates.add(i+1);
            for (int j = i+1; j < cc.length; j++) {
                if (removalMetricIndices.contains(j+1)) continue;
                if (cc[i][j] > 0.9) {
                    candidates.add(j+1);
                }
            }
            removalMetricIndices.addAll(findHighlyCorrelatedMetrics(candidates, meanCC));
        }
        System.out.println("Metrics to be removed by correlation: #" + removalMetricIndices.size() + ":" + removalMetricIndices);

        // remove attributes
        newData = removeMetrics(newData, removalMetricIndices);

        return newData;
    }

    private HashSet<Integer> findHighlyCorrelatedMetrics(HashSet<Integer> candidates, double meanCC[]) {

        // if the size of candidates is 1, then we do not remove it.
        if (candidates.size() == 1)
            return new HashSet<>(0);

        HashSet<Integer> result = new HashSet<>(candidates);

        int minMeanCCMetricIndex = 0;
        double minMeanCC = Double.MAX_VALUE;
        Iterator<Integer> iterator = result.iterator();
        while (iterator.hasNext()) {
            Integer metricIndex = iterator.next();
            if (minMeanCC > meanCC[metricIndex-1]) {
                minMeanCC = meanCC[metricIndex-1];
                minMeanCCMetricIndex = metricIndex;
            }
        }

        iterator = result.iterator();
        while (iterator.hasNext()) {
            Integer metricIndex = iterator.next();
            if (minMeanCCMetricIndex == metricIndex) {
                iterator.remove();
            }
        }

        return result;
    }

    private void printDoubleArray(double[][] array) {

        StringBuilder sb = new StringBuilder();
        sb.append("Print the arrayh\n");
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                //sb.append(String.format("%.0f", Integer.toString(_graph[i][j].freq)) + ",");
                sb.append(String.format("%2s", array[i][j]) + ",");
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

    private String getClassRange(List<Integer> indices) throws Exception {

        List<Integer> list = new ArrayList<>(indices);
        String range = "";
        for (int i = 0; i < list.size()-1; i++) {
            range += (list.get(i)+1) + ",";
        }
        range += (list.get(indices.size()-1) + 1);
        //System.out.println(range);
        return range;
    }

    public Instances createWekaAttributes(String headers[]) throws Exception {

        ArrayList<Attribute> atts = new ArrayList<>();

        // set numeric values for features
        for (int i = 0; i < headers.length; i++) {
            if (i == 0)
                atts.add(new Attribute("ontology",(ArrayList<String>)null));
            else
                atts.add(new Attribute(headers[i]));
        }

        // define relation name
        String rel = "metric_length " + (headers.length-1);

        // create instances
        Instances instances = new Instances(rel, atts, 0);
        return instances;
    }

    /**
     * Normalise the metric values in log-scale
     * @param data The set of instances where each instance is represented by its metric values
     * @return The instances normalised
     */
    private Instances norm(Instances data) throws Exception {

        // step 1: find metrics whose max value is above 10. Such metrics are the candidates for log-transformation
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < data.numAttributes(); i++) {
            double values[] = data.attributeToDoubleArray(i);
            double maxValue = StatUtils.max(values);
            if (maxValue > 10) {
                candidates.add(i);
            }
        }

        // step 2: apply log-transformation
        Instances newData = data;
        if (candidates.size() > 0) {
            NumericTransform filter = new NumericTransform();
            String[] params = new String[6];
            params[0] = "-R"; // range of attributes to make numeric
            params[1] = getClassRange(candidates);
            params[2] = "-C";
            params[3] = "java.lang.Math";
            params[4] = "-M";
            params[5] = "log1p";
            //System.out.println(Arrays.toString(params));
            filter.setOptions(params);
            filter.setInputFormat(data);
            newData = Filter.useFilter(data, filter);
        }

        // step 3: scaling and centering
        _mins = new double[data.numAttributes()-1];
        _maxs = new double[data.numAttributes()-1];
        for (int i = 1; i < newData.numAttributes(); i++) {
            double values[] = newData.attributeToDoubleArray(i);
            _mins[i-1] = StatUtils.min(values);
            _maxs[i-1] = StatUtils.max(values);
        }

        for (int i = 0; i < newData.numInstances(); i++) {
            Instance instance = newData.instance(i);
            for (int k = 1; k < instance.numAttributes(); k++) {
                double v = instance.value(k);
                double norm = 0;
                if (_maxs[k-1]-_mins[k-1] != 0)
                    norm = (v-_mins[k-1])/(_maxs[k-1]-_mins[k-1]);
                instance.setValue(k, norm);
            }
        }
        return newData;
    }

    public double[] getMinAttributeValues() {
        return _mins;
    }

    public double[] getMaxAttributeValues() {
        return _maxs;
    }

    public String getDupOntFileName() {
        return _dupOntFile;
    }

    private double[] stringToDouble (Instances instances, String data[]) {
        double vals[] = new double[data.length];
        vals[0] = instances.attribute(0).addStringValue(data[0]);
        for (int i = 1; i < data.length; i++) {
            vals[i] = Double.parseDouble(data[i]);
        }
        return vals;
    }

    public <T> String generateKeyForRecord (T data[]) throws Exception {

        StringBuilder key = new StringBuilder();

        // data[]: 1st index: ontology name, last index: reasoning time
        // key = data[1] - data[last index]
        for (int i = 1; i < data.length; i++) {
            key.append(data[i]);
        }
        //return result.toString().hashCode();
        return Hashing.md5().hashString(key.toString().trim().toLowerCase(), Charsets.UTF_8).toString();
    }

    public String generateKeyForInstance (Instance instance) throws Exception {

        StringBuilder key = new StringBuilder();

        for (int i = 1; i < instance.numAttributes(); i++) {
            key.append(instance.value(i));
        }
        //return key.toString().hashCode();
        return key.toString();
    }

    public int generateKeyForInstance (Instance instance, Set<Integer> attrIndices) throws Exception {

        StringBuilder key = new StringBuilder();

        for (int i = 0; i < instance.numAttributes(); i++) {
            if (attrIndices.contains(i)) {
                key.append(instance.value(i));
            }
        }
        return key.toString().hashCode();
    }
}
