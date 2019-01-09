package edu.monash.infotech.r2o2.prediction;

import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;

public class StackRegressionModel extends StackClassifierBase implements Serializable {

    private static final long serialVersionUID = -5118844956163390431L;

    private RandomForest _classifier;

    public StackRegressionModel()  {
        super();
    }


    public double evaluate2(Instances data) throws Exception {

        _classifier = new RandomForest();
        double eval_result = super.evaluate2(_classifier, data);
        return eval_result;
    }

    public double evaluate(Instances data) throws Exception {

        _classifier = new RandomForest();
        double eval_result = super.evaluate(_classifier, data);
        return eval_result;
    }

    public void buildClassifier(String arff_name) throws Exception {
        Instances data = new Instances(new BufferedReader(new FileReader(arff_name)));
        data.setClassIndex(data.numAttributes() - 1);

        _classifier = new RandomForest();
        _classifier.buildClassifier(data);
    }

    public void buildClassifier(Instances data) throws Exception {

        //data.setClassIndex(data.numAttributes() - 1);
        _classifier = new RandomForest();
        _classifier.buildClassifier(data);
    }

    public String getReasonerName(String fileName) {
        return fileName.split("\\.")[1];
    }

    public void saveClassifier(String fname) throws Exception {
        FileOutputStream file = new FileOutputStream(fname);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(_classifier);
        save.close();
    }

    public RandomForest loadClassifier(String fname) throws Exception {
        FileInputStream file = new FileInputStream(fname);
        ObjectInputStream save = new ObjectInputStream(file);
        _classifier = (RandomForest) save.readObject();
        save.close();
        return _classifier;
    }

    public StackRegressionModel loadModel(String fname) throws Exception {
        FileInputStream file = new FileInputStream(fname);
        ObjectInputStream save = new ObjectInputStream(file);
        StackRegressionModel model  = (StackRegressionModel) save.readObject();
        save.close();
        return model;
    }

    public double[] classifyInstances(Instances test) throws Exception {

        double preds[] = super.classifyInstances(_classifier, test);
        return preds;
    }

    public double classifyInstance(Instance test) throws Exception {

        double preds = super.classifyInstance(_classifier, test);
        return preds;
    }

}
