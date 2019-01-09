package edu.monash.infotech.r2o2.prediction;

import weka.core.Instance;
import weka.core.Instances;


public abstract class Model {

    public abstract double evaluate(String arff_name) throws Exception;
    public abstract double evaluate(Instances data) throws Exception;
    public abstract double evaluate(Instances data, int max_depth) throws Exception;
    public abstract double evaluate(String arff_name, int max_depth) throws Exception;
    public abstract double evaluateClassification(Instances data) throws Exception;

    public abstract void buildClassifier(String arff_name) throws Exception;
    public abstract void buildClassifier(Instances data) throws Exception;
    public abstract void buildClassifier(String arff_name, int max_depth, String objective) throws Exception;
    public abstract void buildClassifier(Instances data, int max_depth, String objective) throws Exception;

    public abstract String getReasonerName(String fileName) throws Exception;

    public abstract void saveModel(String fname) throws Exception;

    public abstract Object loadModel(String fname) throws Exception;

    public abstract double classifyInstance (Instance instance) throws Exception;
}