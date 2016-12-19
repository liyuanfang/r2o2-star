package edu.monash.infotech.r2o2.prediction;

import com.csvreader.CsvReader;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.util.Pair;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericTransform;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by ybkan on 15/02/2016.
 */
public class Preprocessor {

    private double _mins[];
    private double _maxs[];
    private String _dupOntFile;

    public Instances transform(String metricFile, String trainMetricPropertyFile, String metricValues) throws Exception {

        // step 1: read the properties generated from the training data regarding what transformation has been applied to the training data.
        String logAttributes = "";
        List<String> normInfo = new ArrayList();
        String finalAttributes = "";

        BufferedReader reader = new BufferedReader(new FileReader(trainMetricPropertyFile));
        String line = "";
        boolean isNormInfoAvailable = false;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#log")) {
                int index = line.indexOf(":");
                logAttributes = line.substring(index + 1).trim();
            } else if (line.startsWith("#norm")) {
                isNormInfoAvailable = true;
            } else if (line.startsWith("#final metrics")) {
                //System.out.println(line);
                isNormInfoAvailable = false;
                int index = line.indexOf(":");
                finalAttributes = line.substring(index + 1).trim();
            } else if (isNormInfoAvailable == true) {
                normInfo.add(line);
            }
        }

        // step 2: read metric values of the test ontologies and transform them into Weka instances
        CsvReader csvReader = new CsvReader(metricFile);
        csvReader.readHeaders();
        String headers[] = csvReader.getHeaders();

        Instances instances = createWekaAttributes(headers);

        if (metricValues.length() > 0) {
            // if metrics values are given then we use them
            String values[] = metricValues.split(",");
            instances.add(new DenseInstance(1, stringToDouble(instances, values)));
        } else {
            //otherwise such values are provided in the metric file
            while (csvReader.readRecord()) {
                String data[] = csvReader.getValues();
                instances.add(new DenseInstance(1, stringToDouble(instances, data)));
            }
        }
        csvReader.close();

        // step 3: apply log-transformation
        Instances newData = new Instances(instances);
        if (logAttributes.length() > 0) {
            NumericTransform filter = new NumericTransform();
            String[] params = new String[6];
            params[0] = "-R"; // range of attributes to make numeric
            params[1] = logAttributes;
            params[2] = "-C";
            params[3] = "java.lang.Math";
            params[4] = "-M";
            params[5] = "log1p";
            filter.setOptions(params);
            filter.setInputFormat(instances);
            newData = Filter.useFilter(instances, filter);
        }

        // step 5: normalisation
        for (int i = 0; i < newData.numInstances(); i++) {
            Instance instance = newData.instance(i);
            for (int k = 1; k < instance.numAttributes(); k++) {
                double v = instance.value(k);
                double norm = 0;

                String info[] = normInfo.get(k - 1).split(",");
                double min = Double.parseDouble(info[0].substring(info[0].indexOf("=") + 1));
                double max = Double.parseDouble(info[1].substring(info[1].indexOf("=") + 1));

                if (max - min != 0) {
                    norm = (v - min) / (max - min);
                }
                instance.setValue(k, norm);
            }
        }

        // step 6: leave only metrics that are the ones finally used in the training data after removing bad metrics
        HashSet<Integer> removalMetricIndices = new HashSet<>();
        Set<String> finalMetrics = new HashSet(Arrays.asList(finalAttributes.split(",")));
        for (int k = 0; k < newData.numAttributes(); k++) {
            if (!finalMetrics.contains(newData.attribute(k).name())) {
                removalMetricIndices.add(k);
            }
        }

        Instances finaData = removeMetrics(newData, removalMetricIndices);
        return finaData;
    }

    public Instances perform(String metricFile) throws Exception {

        // step 1: remove duplicated ones
        Instances instances = removeDuplicatedMetrics(metricFile);

        // step 2: normalisation
        Pair<Instances, StringBuilder> normedInstanceResult = norm(instances);
        //System.out.println(normedInstances);

        // step 3: remove the near-zero predictors & remove highly correlated predictors
        Instances finalInstances = removeBadMetrics(normedInstanceResult.getKey());

        // step 4: write the results into a file for the use to testing data:
        // 1) write on which metrics the log-transformation has been applied,
        // 2) write on which metrics we've chosen finally
        writeChosenMetricProperty(metricFile + ".properties", normedInstanceResult.getValue(), finalInstances);

        return finalInstances;
    }

    private void writeChosenMetricProperty(String outFileName, StringBuilder propertyInfo, Instances finalInstances) throws Exception {

        BufferedWriter writer = new BufferedWriter(new FileWriter(outFileName));

        // first write metrics that log-transformation has been applied & normalisation info
        writer.write(propertyInfo.toString());

        // second write metrics after removing the bad metrics
        writer.write("#final metrics:");
        for (int i = 0; i < finalInstances.numAttributes() - 1; i++) {
            writer.write(finalInstances.attribute(i).name() + ",");
        }
        writer.write(finalInstances.attribute(finalInstances.numAttributes() - 1).name() + "\n");
        writer.close();
    }

    private Instances removeDuplicatedMetrics(String metricFile) throws Exception {

        // key: hashcode of all metric values of each instance, value: ontology name
        HashMap<String, String> keyMap = new HashMap<>();

        // key: onotolgy, value: the ontology name kept in the "keys" variable
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

    private void writeDupOntSet(String metricFile, HashMap<String, String> ontNameMap) throws Exception {

        _dupOntFile = metricFile + ".dup";
        BufferedWriter writer = new BufferedWriter(new FileWriter(metricFile + ".dup"));
        Iterator it = ontNameMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
            writer.write(entry.getKey() + "," + entry.getValue() + "\n");
        }
        writer.close();
    }

    private Instances removeBadMetrics(Instances data) throws Exception {

        // step 1: remove near-zero variance metrics
        Instances data1 = removeByNearZeroVar(data);

        // step 2: remove metrics that are highly correlated (threshold correlation coef = 0.9)
        Instances data2 = removeByCorrelation(data1);

        return data2;
    }

    private Instances removeByNearZeroVar(Instances data) throws Exception {
        Instances newData = new Instances(data);

        HashSet<Integer> removalMetricIndices = new HashSet<>();

        for (int i = 1; i < newData.numAttributes(); i++) {

            // step 1: remove metrics whose value variance is zero
            double values[] = newData.attributeToDoubleArray(i);
            double variance = StatUtils.variance(values);
            if (Double.isNaN(variance)) variance = 0;
            if (variance == 0) {
                removalMetricIndices.add(i);
                continue;
            }

            // step 2: remove metrics that have few unique values related to the number of samples (< 0.1)
            double uniqueValueNum = calUniqueValueNum(values);
            double uniqueRatio = uniqueValueNum / values.length;
            if (uniqueRatio < 0.1) {
                removalMetricIndices.add(i);
                continue;
            }

            // step 3: remove metrics where the ratio of frequency of the most common values to the frequency of
            // the second most common values is large (> 19).
            if (isLargeFreqDiff(values)) {
                continue;
            }
        }
        System.out.println("Metrics to be removed by nearZeroVar: #" + removalMetricIndices.size());

        // remove attributes
        newData = removeMetrics(newData, removalMetricIndices);

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

    private boolean isLargeFreqDiff(double[] values) {

        HashMap<Double, Integer> map = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            Integer count = map.get(values[i]);
            if (count == null) {
                map.put(values[i], 1);
            } else {
                map.put(values[i], count + 1);
            }
        }

        List<Integer> freqs = new ArrayList(map.values());
        Collections.sort(freqs, Collections.reverseOrder());

        double mostFreq = freqs.get(0);
        double secondMostFreq = freqs.get(1);
        if (mostFreq / secondMostFreq > 19)
            return true;
        else
            return false;
    }

    private double calUniqueValueNum(double[] values) {

        HashSet<Double> set = new HashSet();
        for (double v : values) {
            set.add(v);
        }
        return set.size();
    }

    private Instances removeByCorrelation(Instances data) throws Exception {

        PearsonsCorrelation pc = new PearsonsCorrelation();

        // create a copy of the instances
        Instances newData = new Instances(data);

        // the variable that contains the correlation coefficients between metrics.
        double cc[][] = new double[newData.numAttributes() - 1][newData.numAttributes() - 1];

        for (int i = 1; i < newData.numAttributes() - 1; i++) {
            cc[i - 1][i - 1] = 0; // correlation for itself
            double v1[] = newData.attributeToDoubleArray(i);

            for (int j = i + 1; j < newData.numAttributes(); j++) {
                double v2[] = newData.attributeToDoubleArray(j);
                double correlation = pc.correlation(v1, v2);
                if (Double.isNaN(correlation)) correlation = 0;
                cc[i - 1][j - 1] = cc[j - 1][i - 1] = correlation;
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

            if (removalMetricIndices.contains(i + 1)) continue;

            HashSet<Integer> candidates = new HashSet<>();
            candidates.add(i + 1);
            for (int j = i + 1; j < cc.length; j++) {
                if (removalMetricIndices.contains(j + 1)) continue;
                if (cc[i][j] > 0.9) {
                    candidates.add(j + 1);
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
        if (candidates.size() == 1) return new HashSet<>(0);

        HashSet<Integer> result = new HashSet<>(candidates);

        int minMeanCCMetricIndex = 0;
        double minMeanCC = Double.MAX_VALUE;
        Iterator<Integer> iterator = result.iterator();
        while (iterator.hasNext()) {
            Integer metricIndex = iterator.next();
            if (minMeanCC > meanCC[metricIndex - 1]) {
                minMeanCC = meanCC[metricIndex - 1];
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

    private String getClassRange(List<Integer> indices) throws Exception {

        List<Integer> list = new ArrayList<>(indices);
        String range = "";
        for (int i = 0; i < list.size() - 1; i++) {
            range += (list.get(i) + 1) + ",";
        }
        range += (list.get(indices.size() - 1) + 1);
        return range;
    }

    private Instances createWekaAttributes(String headers[]) throws Exception {

        ArrayList<Attribute> atts = new ArrayList<>();

        // set numeric values for features
        for (int i = 0; i < headers.length; i++) {
            if (i == 0)
                atts.add(new Attribute("ontology", (ArrayList<String>) null));
            else
                atts.add(new Attribute(headers[i]));
        }

        // define relation name
        String rel = "metric_length " + (headers.length - 1);

        // create instances
        Instances instances = new Instances(rel, atts, 0);
        return instances;
    }

    private Pair<Instances, StringBuilder> norm(Instances data) throws Exception {

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
            filter.setOptions(params);
            filter.setInputFormat(data);
            newData = Filter.useFilter(data, filter);
        }

        // step 3: scaling and centering
        _mins = new double[data.numAttributes() - 1];
        _maxs = new double[data.numAttributes() - 1];
        for (int i = 1; i < newData.numAttributes(); i++) {
            double values[] = newData.attributeToDoubleArray(i);
            _mins[i - 1] = StatUtils.min(values);
            _maxs[i - 1] = StatUtils.max(values);
        }

        StringBuilder dataPropertyBuilder = new StringBuilder();
        dataPropertyBuilder.append("#log:");
        for (int i = 0; i < candidates.size() - 1; i++) {
            dataPropertyBuilder.append(candidates.get(i) + ",");
        }
        dataPropertyBuilder.append(candidates.get(candidates.size() - 1) + "\n\n");

        dataPropertyBuilder.append("#norm:\n");
        for (int k = 1; k < newData.numAttributes(); k++) {
            if (_maxs[k - 1] - _mins[k - 1] != 0) {
                dataPropertyBuilder.append("min=" + _mins[k - 1] + ",max=" + _maxs[k - 1] + "\n");
            } else {
                dataPropertyBuilder.append("min=-1,max=-1\n");
            }
        }

        for (int i = 0; i < newData.numInstances(); i++) {
            Instance instance = newData.instance(i);
            for (int k = 1; k < instance.numAttributes(); k++) {
                double v = instance.value(k);
                double norm = 0;

                if (_maxs[k - 1] - _mins[k - 1] != 0) {
                    norm = (v - _mins[k - 1]) / (_maxs[k - 1] - _mins[k - 1]);
                }
                instance.setValue(k, norm);
            }
        }

        return new Pair<Instances, StringBuilder>(newData, dataPropertyBuilder);
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

    private double[] stringToDouble(Instances instances, String data[]) {
        double vals[] = new double[data.length];
        vals[0] = instances.attribute(0).addStringValue(data[0]);
        for (int i = 1; i < data.length; i++) {
            vals[i] = Double.parseDouble(data[i]);
        }
        return vals;
    }

    public <T> String generateKeyForRecord(T data[]) throws Exception {

        StringBuilder key = new StringBuilder();

        // data[]: 1st index: ontology name, last index: reasoning time
        // key = data[1] - data[last index]
        for (int i = 1; i < data.length; i++) {
            key.append(data[i]);
        }
        return key.toString();
    }

    public String generateKeyForInstance(Instance instance) throws Exception {

        StringBuilder key = new StringBuilder();

        for (int i = 1; i < instance.numAttributes(); i++) {
            key.append(instance.value(i));
        }
        return key.toString();
    }
}
