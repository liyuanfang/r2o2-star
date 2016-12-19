package edu.monash.infotech.r2o2.prediction;

import org.apache.commons.math3.util.Pair;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.*;
import java.util.*;

public class RegressionModel {

    private RandomForest _randomForest;

    public Pair<Double,Double> evaluate(String arff_name, int foldNum) throws Exception {

        // parent dir
        String parentDir = new File(arff_name).getParent();
        File file = new File (arff_name);
        String reasonerName = file.getName().split("\\.")[1];

        // load arff file
        Instances data = new Instances(new BufferedReader(new FileReader(file)));
        data.randomize(new Random(1));

        // remove the first attribute - ontology name
        Remove remove = new Remove();
        remove.setOptions(Utils.splitOptions("-R 1"));
        remove.setInputFormat(data);
        data = Filter.useFilter(data, remove);
        data.setClassIndex(data.numAttributes() - 1);

        // perform cross-validation
        double sumR2 = 0;
        double sumRMSE = 0;
        for (int nFold = 0; nFold < foldNum; nFold++) {

            // Get the training and testing data
            Instances train = data.trainCV(foldNum, nFold);
            Instances test = data.testCV(foldNum, nFold);

            // build prediction model
            _randomForest = new RandomForest();
            _randomForest.buildClassifier(train);

            Evaluation evaluation = new Evaluation(train);
            evaluation.evaluateModel(_randomForest, test);
            System.out.println("\tfold:" + nFold + "=" + evaluation.correlationCoefficient()*evaluation.correlationCoefficient() + "," + evaluation.rootMeanSquaredError());
            sumR2 += evaluation.correlationCoefficient() * evaluation.correlationCoefficient();
            sumRMSE += evaluation.rootMeanSquaredError();

            // retrieve prediction results
            File curretDir = new File(parentDir + "/" + nFold);
            if (!curretDir.exists()) curretDir.mkdir();

            BufferedWriter writer = new BufferedWriter(new FileWriter(curretDir.getAbsolutePath() + "/" + reasonerName + "_eval.csv"));
            writer.write("pred, actual\n");
            List<Prediction> predictions = evaluation.predictions();
            for (Prediction p : predictions) {
                writer.write(p.predicted() + ", " + p.actual() + "\n");
            }
            writer.close();
        }

        return new Pair(sumR2/foldNum, sumRMSE/foldNum);
    }

     public void buildClassifier(String arff_name) throws Exception {
        // load arff file
        Instances data = new Instances(new BufferedReader(new FileReader(arff_name)));

        // remove the first attribute - ontology name
        Remove remove = new Remove();
        remove.setOptions(Utils.splitOptions("-R 1"));
        remove.setInputFormat(data);
        data = Filter.useFilter(data, remove);
        data.setClassIndex(data.numAttributes() - 1);

        _randomForest = new RandomForest();
        _randomForest.setNumTrees(100);
        _randomForest.buildClassifier(data);
    }

    public String getReasonerName(String fileName) {
        return fileName.split("\\.")[1].split("_")[0];
    }

    public void saveClassifier(String fname) throws Exception {
        FileOutputStream file = new FileOutputStream(fname);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(_randomForest);
        save.close();
    }

    public RandomForest loadClassifier(String fname) throws Exception {
        FileInputStream file = new FileInputStream(fname);
        ObjectInputStream save = new ObjectInputStream(file);
        _randomForest = (RandomForest) save.readObject();
        save.close();
        return _randomForest;
    }

    public double classifyInstance (Instance instance) throws Exception {
        double pred = _randomForest.classifyInstance(instance);
        return pred;
    }
}
