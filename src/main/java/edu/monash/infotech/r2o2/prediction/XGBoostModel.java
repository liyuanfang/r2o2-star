package edu.monash.infotech.r2o2.prediction;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.XGBoost;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.io.*;
import java.util.Random;

public class XGBoostModel extends Model implements Serializable {

    private static final long serialVersionUID = -9018159094254830983L;

    private XGBoost _classifier;
    private String OPTION = "-silent 1 -num_round 50 -eta 0.1 -colsample_bylevel 0.67 -subsample 0.5";
    //private String OPTION = "-silent 1 -num_round 50";

    public double evaluate(String arff_name) throws Exception {

        return -1;
    }

    public double evaluate(Instances data) throws Exception {
        return -1;
    }

    public double evaluate(String arff_name, int max_depth) throws Exception {

        // load arff file
        Instances data = new Instances(new BufferedReader(new FileReader(arff_name)));
        data.setClassIndex(data.numAttributes() - 1);
        data.randomize(new Random(1));

        return doEvaluate(data, max_depth);
    }

    public double evaluate(Instances data, int max_depth) throws Exception {

        return doEvaluate(data, max_depth);
    }

    public double doEvaluate(Instances data, int max_depth) throws Exception {

        // perform cross-validation
        int foldNum = 10;
        double sumR2 = 0;
        for (int nFold = 0; nFold < foldNum; nFold++) {

            // Get the training and testing data
            Instances train = data.trainCV(foldNum, nFold);
            Instances test = data.testCV(foldNum, nFold);

            // build prediction model
            _classifier = new XGBoost();
            _classifier.setOptions(Utils.splitOptions(OPTION + " -max_depth " + max_depth));
            _classifier.buildClassifier(train);

            Evaluation evaluation = new Evaluation(train);
            evaluation.evaluateModel(_classifier, test);
            //System.out.println("\t" + evaluation.toSummaryString("\nResults\n======\n", false));
            //System.out.println("\tfold:" + nFold + "=" + evaluation.correlationCoefficient()*evaluation.correlationCoefficient());
            sumR2 += evaluation.correlationCoefficient() * evaluation.correlationCoefficient();
        }
        return (double) sumR2/foldNum;
    }

    public double evaluateClassification(Instances data, int max_depth, String objective) throws Exception {

        // perform cross-validation
        int foldNum = 10;
        double sumR2 = 0;
        for (int nFold = 0; nFold < foldNum; nFold++) {

            // Get the training and testing data
            Instances train = data.trainCV(foldNum, nFold);
            Instances test = data.testCV(foldNum, nFold);

            // build prediction model
            _classifier = new XGBoost();
            String max_depth_str = "";
            if (objective.length() > 0) {
                max_depth_str = OPTION + " -max_depth " + max_depth + " -objective " + objective;
            } else {
                max_depth_str = OPTION + " -max_depth " + max_depth;
            }

            _classifier.setOptions(Utils.splitOptions(max_depth_str));
            _classifier.buildClassifier(train);

            Evaluation evaluation = new Evaluation(train);
            evaluation.evaluateModel(_classifier, test);
            //System.out.println("\t" + evaluation.toSummaryString("\nResults\n======\n", false));
            //System.out.println("\tfold:" + nFold + "=" + evaluation.correlationCoefficient()*evaluation.correlationCoefficient());
            sumR2 += evaluation.pctCorrect();
        }
        return (double) sumR2/foldNum;
    }

    public double evaluateClassification(Instances data) throws Exception {

        return -1;
    }

    public void buildClassifier(String arff_name, int max_depth, String objective) throws Exception {
        // load arff file
        Instances data = new Instances(new BufferedReader(new FileReader(arff_name)));
        data.setClassIndex(data.numAttributes() - 1);

        String max_depth_str = "";
        if (objective.length() > 0) {
            max_depth_str = OPTION + " -max_depth " + max_depth + " -objective " + objective;
        } else {
            max_depth_str = OPTION + " -max_depth " + max_depth;
        }

        _classifier = new XGBoost();
        _classifier.setOptions(Utils.splitOptions(max_depth_str));
        _classifier.buildClassifier(data);
    }

    public void buildClassifier(Instances data, int max_depth, String objective) throws Exception {

        String max_depth_str = "";
        if (objective.length() > 0) {
            max_depth_str = OPTION + " -max_depth " + max_depth + " -objective " + objective;
        } else {
            max_depth_str = OPTION + " -max_depth " + max_depth;
        }

        _classifier = new XGBoost();
        _classifier.setOptions(Utils.splitOptions(max_depth_str));
        _classifier.buildClassifier(data);
    }

    public void buildClassifier(Instances data) throws Exception {
        String max_depth_str = OPTION + " -max_depth 10";
        _classifier = new XGBoost();
        _classifier.setOptions(Utils.splitOptions(max_depth_str));
        _classifier.buildClassifier(data);
    }

    public void buildClassifier(String arff_file) throws Exception {

    }

    public String getReasonerName(String fileName) {
        return fileName.split("\\.")[1].split("_")[0];
    }

    public void saveClassifier(String fname) throws Exception {
        FileOutputStream file = new FileOutputStream(fname);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(_classifier);
        save.close();
    }

    public XGBoost loadClassifier(String fname) throws Exception {
        FileInputStream file = new FileInputStream(fname);
        ObjectInputStream save = new ObjectInputStream(file);
        _classifier = (XGBoost) save.readObject();
        save.close();
        return _classifier;
    }

    public void saveModel(String fname) throws Exception {
        FileOutputStream file = new FileOutputStream(fname);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(this);
        save.close();
    }

    public XGBoostModel loadModel(String fname) throws Exception {
        FileInputStream file;
        if (fname.startsWith("/")) {
            file = new FileInputStream(fname);
        } else {
            file = new FileInputStream(this.getClass().getClassLoader().getResource(fname).getPath());
        }
        ObjectInputStream save = new ObjectInputStream(file);
        XGBoostModel model  = (XGBoostModel) save.readObject();
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
        double pred = _classifier.classifyInstance(instance);
        return pred;
    }
}
