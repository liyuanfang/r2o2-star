package edu.monash.infotech.r2o2.prediction;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.util.Random;

public class RegressionModel extends Model implements Serializable  {

    private static final long serialVersionUID = -1955527003227678686L;

    private RandomForest _randomForest;

    public double evaluate(String arff_name) throws Exception {

        // load arff file
        Instances data = new Instances(new BufferedReader(new FileReader(arff_name)));
        data.randomize(new Random(1));
        data.setClassIndex(data.numAttributes() - 1);

        double result = doEvaluate(data);
        return result;
    }

    public double evaluate(Instances data, int i) throws Exception {
        return -1;
    }

    public double evaluate(String arff_file, int i) throws Exception {
        return -1;
    }

    public double evaluate(Instances data) throws Exception {
        double result = doEvaluate(data);
        return result;
    }

    private double doEvaluate(Instances data) throws Exception {

        // perform cross-validation
        int foldNum = 10;
        double sumR2 = 0;
        for (int nFold = 0; nFold < foldNum; nFold++) {

            // Get the training and testing data
            Instances train = data.trainCV(foldNum, nFold);
            Instances test = data.testCV(foldNum, nFold);

            // build prediction model
            _randomForest = new RandomForest();
            _randomForest.buildClassifier(train);
            //classifyInstances(test);

            Evaluation evaluation = new Evaluation(train);
            evaluation.evaluateModel(_randomForest, test);
            sumR2 += evaluation.correlationCoefficient() * evaluation.correlationCoefficient();
        }
        return (double) sumR2/foldNum;
    }

    public double evaluateClassification(Instances data) throws Exception {

        // perform cross-validation
        int foldNum = 10;
        double sumR2 = 0;
        for (int nFold = 0; nFold < foldNum; nFold++) {

            // Get the training and testing data
            Instances train = data.trainCV(foldNum, nFold);
            Instances test = data.testCV(foldNum, nFold);

            // build prediction model
            _randomForest = new RandomForest();
            _randomForest.buildClassifier(train);
            //classifyInstances(test);

            Evaluation evaluation = new Evaluation(train);
            evaluation.evaluateModel(_randomForest, test);

            sumR2 += evaluation.pctCorrect();
        }
        return sumR2/foldNum;
    }

    public void buildClassifier(String arff_name) throws Exception {
        Instances data = new Instances(new BufferedReader(new FileReader(arff_name)));
        data.setClassIndex(data.numAttributes() - 1);

        _randomForest = new RandomForest();
        _randomForest.buildClassifier(data);
    }

    public void buildClassifier(Instances data) throws Exception {

        data.setClassIndex(data.numAttributes() - 1);
        _randomForest = new RandomForest();
        _randomForest.buildClassifier(data);
    }


    public void buildClassifier(String arff_name, int max_depth, String objective) throws Exception {

    }

    public void buildClassifier(Instances data, int max_depth, String objective) throws Exception {

    }

    public String getReasonerName(String fileName) {
        return fileName.split("\\.")[1];
    }

    public void saveClassifier(String fname) throws Exception {
        FileOutputStream file = new FileOutputStream(fname);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(_randomForest);
        save.close();
    }

    public void saveModel(String fname) throws Exception {
        FileOutputStream file = new FileOutputStream(fname);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(this);
        save.close();
    }

    public RandomForest loadClassifier(String fname) throws Exception {
        FileInputStream file = new FileInputStream(fname);
        ObjectInputStream save = new ObjectInputStream(file);
        _randomForest = (RandomForest) save.readObject();
        save.close();
        return _randomForest;
    }

    public RegressionModel loadModel(String fname) throws Exception {
        FileInputStream file = new FileInputStream(fname);
        ObjectInputStream save = new ObjectInputStream(file);
        RegressionModel model  = (RegressionModel) save.readObject();
        save.close();
        return model;
    }

    public double[] classifyInstances(Instances test) throws Exception {

        double preds[] = new double[test.numInstances()];

        for (int i = 0; i < test.numInstances(); i++) {
            Instance instance = test.instance(i);
            double actual = instance.classValue();
            double pred = classifyInstance(instance);
            preds[i] = pred;
            //System.out.println("actual:" + actual + ", pred:" + pred);
        }

        return preds;
    }

    public double classifyInstance (Instance instance) throws Exception {
        double pred = _randomForest.classifyInstance(instance);
        return pred;
    }
}
