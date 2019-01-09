package edu.monash.infotech.r2o2.prediction;


import weka.classifiers.trees.XGBoost;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.io.*;


public class StackXGBoostModel extends StackClassifierBase implements Serializable {

    private static final long serialVersionUID = 8764574022917787948L;

    private XGBoost _classifier;
    private String OPTION = "-silent 1 -num_round 50 -eta 0.1 -colsample_bylevel 0.67 -subsample 0.5";

    public StackXGBoostModel()  {
        super();
    }

    public double evaluate2(Instances data, int max_depth)throws Exception {

        _classifier = new XGBoost();
        String max_depth_str = OPTION + " -max_depth " + max_depth;
        _classifier.setOptions(Utils.splitOptions(max_depth_str));
        double eval_result = super.evaluate2(_classifier, data);
        return eval_result;
    }

    public double evaluate(Instances data, int max_depth) throws Exception {

        _classifier = new XGBoost();
        String max_depth_str = OPTION + " -max_depth " + max_depth;
        _classifier.setOptions(Utils.splitOptions(max_depth_str));
        double eval_result = super.evaluate(_classifier, data);
        return eval_result;
    }

    public void buildClassifier(String arff_name, int max_depth) throws Exception {
        Instances data = new Instances(new BufferedReader(new FileReader(arff_name)));
        data.setClassIndex(data.numAttributes() - 1);

        _classifier = new XGBoost();
        String max_depth_str = OPTION + " -max_depth " + max_depth;
        _classifier.setOptions(Utils.splitOptions(max_depth_str));
        _classifier.buildClassifier(data);
    }

    public void buildClassifier(Instances data, int max_depth) throws Exception {

        //data.setClassIndex(data.numAttributes() - 1);
        _classifier = new XGBoost();
        String max_depth_str = OPTION + " -max_depth " + max_depth;
        _classifier.setOptions(Utils.splitOptions(max_depth_str));
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

    public XGBoost loadClassifier(String fname) throws Exception {
        FileInputStream file = new FileInputStream(fname);
        ObjectInputStream save = new ObjectInputStream(file);
        _classifier = (XGBoost) save.readObject();
        save.close();
        return _classifier;
    }

    public StackXGBoostModel loadModel(String fname) throws Exception {
        FileInputStream file = new FileInputStream(fname);
        ObjectInputStream save = new ObjectInputStream(file);
        StackXGBoostModel model  = (StackXGBoostModel) save.readObject();
        save.close();
        return model;
    }


    public double[] classifyInstances(Instances test) throws Exception {
        double preds[] = super.classifyInstances(_classifier, test);
        return preds;
    }

    public double classifyInstance(Instance t) throws Exception {
        double preds = super.classifyInstance(_classifier, t);
        return preds;
    }
}

