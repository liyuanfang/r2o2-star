package edu.monash.infotech.r2o2.prediction;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class StackClassifierBase implements Serializable {

    private static final long serialVersionUID = -4838358958207939916L;

    private HashMap<String, Object> _classifiers;

    public StackClassifierBase()  {
        _classifiers = new LinkedHashMap<>();
    }


    public double evaluate2(Classifier classifier, Instances data) throws Exception {

        // perform cross-validation
        int foldNum = 10;
        double sumR2 = 0;
        for (int nFold = 0; nFold < foldNum; nFold++) {

            // Get the training and testing data
            Instances train = data.trainCV(foldNum, nFold);
            Instances test = data.testCV(foldNum, nFold);

            // build prediction model
            classifier.buildClassifier(train);
            Evaluation evaluation = new Evaluation(train);
            evaluation.evaluateModel(classifier, test);
            sumR2 += evaluation.correlationCoefficient() * evaluation.correlationCoefficient();
        }
        return (double) sumR2/foldNum;
    }


    public double evaluate(Classifier classifier, Instances data) throws Exception {
        // perform cross-validation
        int foldNum = 10;
        double sumR2 = 0;
        for (int nFold = 0; nFold < foldNum; nFold++) {

            // Get the training and testing data
            Instances train = data.trainCV(foldNum, nFold);
            Instances test = data.testCV(foldNum, nFold);

            // measure the predicted values on the original training data using base classifiers
            HashMap<String, double[]> predicted_val_map = predictValuesOnOriginalData(train, train);
            Instances newTrain = initStackInstances();
            newTrain.setClassIndex(newTrain.numAttributes()-1);
            newTrain = addNewInstances(train, newTrain, predicted_val_map);

            // create the new test data
            predicted_val_map = predictValuesOnOriginalData(train, test);
            Instances newTest = initStackInstances();
            newTest.setClassIndex(newTest.numAttributes()-1);
            newTest = addNewInstances(test, newTest, predicted_val_map);

            classifier.buildClassifier(newTrain);
            Evaluation evaluation = new Evaluation(newTrain);
            evaluation.evaluateModel(classifier, newTest);
            sumR2 += evaluation.correlationCoefficient() * evaluation.correlationCoefficient();
        }

        return (double) sumR2/foldNum;
    }


    private Instances addNewInstances(Instances originalData, Instances newData, HashMap<String, double[]> predicted_val_map) {
        // generate 2nd-level instances
        for (int i = 0; i < originalData.numInstances(); i++) {
            Instance instance = originalData.instance(i);
            double vals[] = new double[_classifiers.keySet().size() + 1];

            int k = 0;
            for (String ensemble_name: _classifiers.keySet()) {
                vals[k++] = predicted_val_map.get(ensemble_name)[i];
            }
            vals[vals.length - 1] = instance.value(instance.classIndex());
            newData.add(new DenseInstance(1, vals));
        }

        return newData;
    }

    private HashMap<String, double[]> predictValuesOnOriginalData(Instances train, Instances test) throws Exception {

        HashMap<String, double[]> predicted_val_map = new LinkedHashMap<>();

        for (String ensemble_name: _classifiers.keySet()) {
            if (ensemble_name.equalsIgnoreCase("rf")) {
                RegressionModel classifier = (RegressionModel) _classifiers.get("rf");
                classifier.buildClassifier(train);

                double vals[] = new double[test.numInstances()];
                for (int i = 0; i < test.numInstances(); i++) {
                    vals[i] = classifier.classifyInstance(test.instance(i));
                }
                predicted_val_map.put(ensemble_name, vals);

            } else if (ensemble_name.equalsIgnoreCase("xgb")) {
                XGBoostModel classifier = (XGBoostModel) _classifiers.get("xgb");
                classifier.buildClassifier(train);

                double vals[] = new double[test.numInstances()];
                for (int i = 0; i < test.numInstances(); i++) {
                    vals[i] = classifier.classifyInstance(test.instance(i));
                }
                predicted_val_map.put(ensemble_name, vals);
            }
        }

        return predicted_val_map;
    }

    private Instances initStackInstances() {

        String rel = "instances_for_stacking_prediction_model";
        ArrayList<Attribute> atts = new ArrayList<>();

        // define attributes
        for (String ensemble_name: _classifiers.keySet()) {
            atts.add(new Attribute(ensemble_name));
        }
        atts.add(new Attribute("reasoning_time"));

        // create the stacking instance structure
        Instances newInstances = new Instances(rel, atts, 0);

        return newInstances;
    }

    public String getReasonerName(String fileName) {
        return fileName.split("\\.")[1];
    }

    public double[] classifyInstances(Classifier classifier, Instances test) throws Exception {

        double preds[] = new double[test.numInstances()];

        for (int i = 0; i < test.numInstances(); i++) {
            Instance instance = test.instance(i);
            double actual = instance.classValue();
            double pred = classifyInstance(classifier,instance);
            preds[i] = pred;
            //System.out.println("actual:" + actual + ", pred:" + pred);
        }

        return preds;
    }

    public double classifyInstance (Classifier classifier, Instance instance) throws Exception {
        double pred = classifier.classifyInstance(instance);
        return pred;
    }

    public void addClassifier(String name, Object classifier) {
        _classifiers.put(name, classifier);
    }

    public HashMap<String, Object> getClassifiers() {
        return _classifiers;
    }

    public Instances createStackingInstances(Instances originalInstances) throws Exception {

        Instances newInstances = initStackInstances();

        HashMap<String, double[]> predicted_val_map = new LinkedHashMap<>();

        for (String ensemble_name: _classifiers.keySet()) {
            if (ensemble_name.equalsIgnoreCase("rf")) {
                // stacking random forest prediction model
                RegressionModel classifier = (RegressionModel) _classifiers.get("rf");
                double vals[] = new double[originalInstances.numInstances()];
                for (int i = 0; i < originalInstances.numInstances(); i++) {
                    vals[i] = classifier.classifyInstance(originalInstances.instance(i));
                }
                predicted_val_map.put(ensemble_name, vals);

            } else if (ensemble_name.equalsIgnoreCase("xgb")) {
                // stacking xgboost prediction model
                XGBoostModel classifier = (XGBoostModel) _classifiers.get("xgb");
                double vals[] = new double[originalInstances.numInstances()];
                for (int i = 0; i < originalInstances.numInstances(); i++) {
                    vals[i] = classifier.classifyInstance(originalInstances.instance(i));
                }
                predicted_val_map.put(ensemble_name, vals);
            }
        }

        for (int i = 0; i < originalInstances.numInstances(); i++) {
            Instance instance = originalInstances.instance(i);
            double vals[] = new double[_classifiers.keySet().size() + 1];

            int k = 0;
            for (String ensemble_name: _classifiers.keySet()) {
                vals[k++] = predicted_val_map.get(ensemble_name)[i];
            }
            vals[vals.length - 1] = instance.value(instance.classIndex());
            newInstances.add(new DenseInstance(1, vals));
        }

        newInstances.setClassIndex(newInstances.numAttributes()-1);
        return newInstances;
    }

    public void saveData(String fileName, Instances data) throws Exception {

        ArffSaver writer = new ArffSaver();
        writer.setInstances(data);
        writer.setFile(new File(fileName));
        writer.writeBatch();
    }

    public void saveModel(String fname) throws Exception {
        FileOutputStream file = new FileOutputStream(fname);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(this);
        save.close();
    }
}
