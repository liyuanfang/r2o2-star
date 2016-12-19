package edu.monash.infotech.r2o2.demo;

import org.apache.commons.math3.util.Pair;
import weka.core.Instances;

/**
 * Created by ybkan on 15/02/2016.
 */
public class R2O2EvaluationDemo {

    /**
     * This demo shows the overall procedure for evaluating the meta-reasoner, R2O2, from the raw reasoning training data.
     * Especially, this r2o2 shows how to evaluate R2O2 from the ranking matrix where rankings are determined by the original reasoning performance.
     */

    public static void main(String args[]) throws Exception {

        DemoCoreFunction demoCoreFunction = new DemoCoreFunction();

        ClassLoader classLoader = R2O2EvaluationDemo.class.getClassLoader();
        String dir = classLoader.getResource("r2o2/evaluation/").getPath();

        // metrics file for sample ontologies
        String metricsFile = dir + "/ore14_all.csv";

        // directory that contains reasoning times for reasoners
        String reasoningTimeDataDir = dir;

        // step 1: generate metrics data that are used by any of reasoners
        String newMetricsFile = demoCoreFunction.genMetricData(metricsFile, reasoningTimeDataDir);

        // step 2: perform pre-processing on the metrics data
        Pair<String, Instances> pair = demoCoreFunction.preprocess(newMetricsFile);
        String dupOntFileName = pair.getKey();
        Instances data = pair.getValue();

        // step 3: for each reasoner, generate its training example by combining metric values and reasoning times of the sample ontologies
        demoCoreFunction.genMetricAndClsTimeData(data, dupOntFileName, reasoningTimeDataDir);

        // step 4: we divide the data for each reasoner into three different parts:
        // 1) for training prediction model, 2) for training meta-reasoner (ranking matrix), and 3) testing meta-reasoner
        int nFold = 10;
        demoCoreFunction.genMetaData(reasoningTimeDataDir, nFold);

        // step 5: create prediction models of reasoners and evaluatePredictedRanking their performance
        int cvFoldForPrediction = 10;
        demoCoreFunction.buildPredictionModel(reasoningTimeDataDir, nFold, cvFoldForPrediction, "arff");

        // step 6: building the trainig and testing ranking matrix
        System.out.println("\n#Generate the training ranking matrix:");
        demoCoreFunction.buildRankingMatrix(reasoningTimeDataDir, nFold, "train_mm", "arff", "reg");
        System.out.println("\n#Generate the testing ranking matrix:");
        demoCoreFunction.buildRankingMatrix(reasoningTimeDataDir, nFold, "test_mm", "arff", "reg");

        // step 7: build rankers on the ranking matrix
        demoCoreFunction.buildRankerOnAcutal(reasoningTimeDataDir, nFold);
        demoCoreFunction.measurePortfolioPerformance(reasoningTimeDataDir, nFold, "arff");

        // step 8: evaluate the predicted ranking result using P@1
        String resultFileName = "r2o2_eval.csv";
        demoCoreFunction.evaluateR2O2(reasoningTimeDataDir, nFold, resultFileName);
    }
}
