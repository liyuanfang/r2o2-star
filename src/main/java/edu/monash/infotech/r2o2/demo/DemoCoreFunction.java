package edu.monash.infotech.r2o2.demo;

import edu.monash.infotech.r2o2.data.DataManager;
import edu.monash.infotech.r2o2.evaluation.Evaluation;
import edu.monash.infotech.r2o2.evaluation.RankingEvalResult;
import edu.monash.infotech.r2o2.evaluation.ReasonerComponent;
import edu.monash.infotech.r2o2.prediction.Preprocessor;
import edu.monash.infotech.r2o2.prediction.RegressionModel;
import edu.monash.infotech.r2o2.reasoner.Portfolio;
import edu.monash.infotech.r2o2.reasoner.R2O2;
import fantail.core.Tools;
import org.apache.commons.math3.util.Pair;
import weka.core.Instances;

import java.io.*;
import java.util.*;

/**
 * Created by ybkan on 25/07/2016.
 */
public class DemoCoreFunction {

    /**
     * This function generates only ontologies with their metric values that are reasoned by any of the given reasoner components
     * from the ore14 ontology set with their metric values
     * @param metricsFile the ore14 ontologies with their metric values
     * @param reasoningTimeDataDir the subset of ore14 ontologies with their metric values reasoned by any of the given reasoner components
     * @return the new ore14 ontology file name
     */
    public String genMetricData(String metricsFile, String reasoningTimeDataDir) throws Exception {
        DataManager dataManager = new DataManager();
        System.out.println("\n#Generate ontologies in the metrics data that are only used in measurement of reasoning time:");
        String newMetricsFile = dataManager.genMetricData(metricsFile, reasoningTimeDataDir);
        return newMetricsFile;
    }

    /**
     * This function shows how to preprocess the givne ontologies with their metric values before building prediction models of
     * the input reasoners. The preprocessing step follows ones presented in the following AAAI 14 paper:
     *      - Title: How Long Will It Take? Accurate Prediction of Ontology Reasoning Performance
     * @param newMetricsFile the new ore14 ontology file name produced from the "genMetricData" function.
     * @return the pair of two components: 1) removed duplicated ontologies, 2) preprocessed training data instances
     */
    public Pair<String, Instances> preprocess(String newMetricsFile) throws Exception {
        System.out.println("\n#Apply preprocessing steps:");

        Preprocessor preprocessor = new Preprocessor();
        Instances data = preprocessor.perform(newMetricsFile);
        String dupOntFileName = preprocessor.getDupOntFileName();

        Pair<String, Instances> pair = new Pair<String, Instances>(dupOntFileName, data);
        return pair;
    }

    /**
     * This function generate actual training data to be used by prediction models and the meta-reasoner. For each reasoner,
     * each instance in the data consists of the set of metric values and the acutal rasoning time measured by the reasoner.
     * @param data the preprocessed ontologies with their metric values
     * @param dupOntFileName the removed duplicted ontology names
     * @param reasoningTimeDataDir the directory containing measured reasoning time for the input ontologies
     */
    public void genMetricAndClsTimeData(Instances data, String dupOntFileName, String reasoningTimeDataDir) throws Exception {
        System.out.println("\n#Generate combined form of metrics and reasoning time data:");
        DataManager dataManager = new DataManager();
        dataManager.genMetricAndClsTimeData(data, dupOntFileName, reasoningTimeDataDir);
    }

    /**
     * Generate two datasets from the single data consisting of metrics + reasoning time for all reasoners for each cross-validation:
     * 1) training data for building prediction models - 90% of the input data used
     * 2) training data for training R2O2 - the same data with 1)
     * 3) testing data for testing R2O2 - 10% of the input data
     *
     * @param reasoningTimeDataDir directory that contains each reasoner's data arff file
     * @param nFold                cross-validation number
     * @throws Exception
     */
    public void genMetaData(String reasoningTimeDataDir, int nFold) throws Exception {
        System.out.println("\n#Generate datasets:");
        DataManager dataManager = new DataManager();
        // generate two ranking matrix tables using all the common ontologies (90%: training, 10%: testing)
        dataManager.genData(reasoningTimeDataDir, nFold);
    }

    /**
     * This function generate prediction models of given reasoner components by Random Forest regression model in Weka.
     * The reasulting prediction models ends with "*.reg". Also, the performance result of prediction models are saved in a
     * file - "pm.performance.csv".
     * @param reasoningTimeDataDir the directory containing the input training data for prediction models (format - r.*.arff).
     * @param cvFoldTest the number of fold for overall cross-validation of R2O2.
     * @param cvFoldForPrediction the number of fold for cross-validation of the produced prediction models
     * @throws Exception
     */
    public void buildPredictionModel(String reasoningTimeDataDir, int cvFoldTest, int cvFoldForPrediction, String inputFilePostfix) throws Exception {
        System.out.println("\n#Generate prediction models:");

        String resultPredFile = "";
        if (inputFilePostfix.endsWith("arff"))
            resultPredFile = "reg";
        else
            resultPredFile = "reg2";

        String resultPerfFile = "";
        if (inputFilePostfix.endsWith("arff"))
            resultPerfFile = "pm.performance.csv";
        else
            resultPerfFile = "pm2.performance.csv";

        for (int fold = 1; fold <= cvFoldTest; fold++) {
            File foldDir = new File(reasoningTimeDataDir + "/" + fold);
            File files[] = foldDir.listFiles();

            StringBuilder performanceStr = new StringBuilder();
            for (File file: files) {
                if (file.getName().startsWith("r.") && file.getName().endsWith(inputFilePostfix)) {
                    RegressionModel regressionModel = new RegressionModel();
                    String reasonerName = regressionModel.getReasonerName(file.getName());
                    regressionModel.buildClassifier(file.getAbsolutePath());
                    regressionModel.saveClassifier(file.getAbsolutePath() + "." + resultPredFile);

                    System.out.println("target file:" + file.getName());
                    Pair<Double, Double> performance = regressionModel.evaluate(file.getAbsolutePath(), cvFoldForPrediction);
                    System.out.println("\treasoner:" + reasonerName + ", r2=" + performance.getKey() + ", rmse=" + performance.getValue());
                    performanceStr.append(reasonerName + "," + performance.getKey() + "," + performance.getValue() + "\n");
                }
            }

            BufferedWriter performanceWriter = new BufferedWriter(new FileWriter(reasoningTimeDataDir + "/" + fold + "/" + resultPerfFile));
            performanceWriter.write(performanceStr.toString());
            performanceWriter.close();
        }
    }

    public void buildRankerOnAcutal(String reasoningTimeDataDir, int cvFoldForRanker) throws Exception {
        System.out.println("\n#Generate rankers:");

        for (int fold = 1; fold <= cvFoldForRanker; fold++) {
            System.out.println("fold:" + fold);

            // Train rankers on the ranking matrix generated using "predicted" reasoning time
            File trainDir = new File(reasoningTimeDataDir + "/" + fold + "/train_mm");

            // Train rankers on the ranking matrix generated using "actual" reasoning time
            R2O2 metaReasoner = new R2O2();
            String trainFile = trainDir + "/meta-rank-actual.arff";
            metaReasoner.buildModel(trainFile, "ranker", "ranker.summary");
        }
    }

    private HashMap<String, RegressionModel> loadPredictionModels(String dir, String predictionModelExtension) throws Exception {

        // load prediction models
        HashMap<String, RegressionModel> predictionModels = new LinkedHashMap<>();
        File foldDir = new File(dir);
        File files[] = foldDir.listFiles();
        for (File file : files) {
            if (file.getName().startsWith("r.") && file.getName().endsWith(predictionModelExtension)) {

                // load prediction models of all reasoners
                RegressionModel regressionModel = new RegressionModel();
                String reasonerName = regressionModel.getReasonerName(file.getName());
                regressionModel.loadClassifier(file.getAbsolutePath());
                predictionModels.put(reasonerName, regressionModel);
            }
        }
        return predictionModels;
    }

    /**
     * This function generate a ranking matrix that R2O2 will learn.
     * @param reasoningTimeDataDir the directory containing training data for each reasoner, where such data will be combined to generate a ranking matrix
     * @param cvFoldTest the number of cross-validation (fold number)
     * @param metaDataRoot the meta data root directory
     * @param trainFileExtension the train file extension - arff.
     * @param predictionModelExtension the prediction model extension - reg.
     */
    public void  buildRankingMatrix (String reasoningTimeDataDir, int cvFoldTest, String metaDataRoot,
                                     String trainFileExtension, String predictionModelExtension) throws Exception {

        for (int fold = 1; fold <= cvFoldTest; fold++) {

            // load prediction models
            String dir = reasoningTimeDataDir + "/" + fold;
            HashMap<String, RegressionModel> predictionModels = loadPredictionModels(dir, predictionModelExtension);

            // read meta data which has actual ranking of reasoner components.
            Instances metaRankActualInstances = Tools.loadFantailARFFInstances(dir + "/" + metaDataRoot + "/meta-rank-actual." + trainFileExtension);
            metaRankActualInstances.setClassIndex(metaRankActualInstances.numAttributes()-1);

            // output instances
            Instances metaRankPredInstances = new Instances (metaRankActualInstances, 0);
            Instances metaTimePredInstances = new Instances (metaRankActualInstances, 0);

            String testFileName = dir + "/" + metaDataRoot + "/input-pred." + trainFileExtension;
            Instances testInstances = new Instances(new BufferedReader(new FileReader(testFileName)));
            testInstances.setClassIndex(testInstances.numAttributes()-1);

            // predict the test instance using all prediction models
            HashMap<String, Double> preds = new HashMap<>();
            for (int i = 0; i < testInstances.numInstances(); i++) {
                Set<String> reasonerNames = predictionModels.keySet();
                for (String reasonerName : reasonerNames) {
                    double pred = predictionModels.get(reasonerName).classifyInstance(testInstances.instance(i));
                    preds.put(reasonerName, pred);
                }

                // read ranking of the reasoners of this instance
                String names[] = Tools.getTargetNames(metaRankActualInstances.instance(i));
                double predictions[] = DataManager.summarizePrediction(preds, names);
                double predRanking[] = DataManager.rank(predictions);
                DataManager.addPredictedOutput(predRanking, metaRankActualInstances.instance(i), metaRankPredInstances); // add predicted rank output
                DataManager.addPredictedOutput(predictions, metaRankActualInstances.instance(i), metaTimePredInstances); // add predicted time output
            }

            // save outputs
            DataManager.createArffFile(dir + "/" + metaDataRoot + "/meta-rank-pred." + trainFileExtension, metaRankPredInstances);
            DataManager.createArffFile(dir + "/" + metaDataRoot + "/meta-time-pred." + trainFileExtension, metaTimePredInstances);
        }
    }

    /**
     * This function measure the performance of portfoli-based reasoner
     * @param reasoningTimeDataDir the directory containing the ranking matrix
     * @param cvFoldForRanker the number of cross-validation for testing the portfolio-based reasoner
     */
    public void measurePortfolioPerformance(String reasoningTimeDataDir, int cvFoldForRanker, String inputPredFilePostfix) throws Exception {
        System.out.println("\n#Measure portfolio-based approach:");

        String result = "";
        if (inputPredFilePostfix.endsWith("arff")) result = "portfolio.summary";

        for (int fold = 1; fold <= cvFoldForRanker; fold++) {
            //System.out.println("fold:" + fold);

            File trainDir = new File(reasoningTimeDataDir + "/" + fold + "/train_mm");

            Portfolio portfolio = new Portfolio();
            String predFile = trainDir + "/meta-rank-pred." + inputPredFilePostfix;
            String actualFile = trainDir + "/meta-rank-actual." + inputPredFilePostfix;

            portfolio.measurePrecision(predFile, actualFile, trainDir + "/" + result);
        }
    }

    public void evaluateR2O2 (String reasoningTimeDataDir, int cvFoldForRanker, String resultFileName) throws Exception {

        List<RankingEvalResult> evalResults = new ArrayList<RankingEvalResult>();

        // for each fold, we load and apply the trained rankers
        String reasonerNames[] = null;
        List<RankingEvalResult> evalResultSet = new ArrayList<RankingEvalResult>();

        for (int fold = 1; fold <= cvFoldForRanker; fold++) {
            //System.out.println("fold:" + fold);

            String currentDir = reasoningTimeDataDir + "/" + fold + "/test_mm/";
            Instances predRankingInstances = Tools.loadFantailARFFInstances(currentDir + "/meta-rank-pred.arff");

            R2O2 metaReasoner = new R2O2(currentDir + "/../train_mm/", "ranker");
            String pmPerfFile = currentDir + "/../pm.performance.csv";
            String avgRankPerfFile = currentDir + "/../train_mm/ranker.summary.avgRank";
            String rankPerfFile = currentDir + "/../train_mm/ranker.summary";
            String portfolioPerfFile = currentDir + "/../train_mm/portfolio.summary";

            // read the ranking matrix profile, performance of the reasoners, performance of rankers, and performance of portfolio
            metaReasoner.readMetaDataProfile(predRankingInstances,
                    pmPerfFile,
                    avgRankPerfFile,
                    rankPerfFile,
                    portfolioPerfFile);

            ReasonerComponent[][] predRankings = metaReasoner.recommendRankings(predRankingInstances); // use the meta-reasoner

            // load actual ranking instances for comparison purpose
            Instances actualRankingInstances = Tools.loadFantailARFFInstances(currentDir + "/meta-rank-actual.arff");

            // load actual reasoning time instances for comparison purpose
            Instances actualReasoningTimeInstances = Tools.loadFantailARFFInstances(currentDir + "/meta-time-actual.arff");

            // perform evaluation based on ranking
            Evaluation evaluator = new Evaluation(predRankingInstances,
                    actualRankingInstances,
                    actualReasoningTimeInstances); // test and actual meta data

            evaluator.setPredictedRankingResult(predRankings); // set predicted ranking result
            evaluator.setMetaReasoner(metaReasoner);
            RankingEvalResult rankingEvalResult = evaluator.evaluatePredictedRanking(); // evaluate
            String evalStr = evaluator.evalToString();

            // add evaluation result of each fold validation
            evalResultSet.add(rankingEvalResult);

            reasonerNames = metaReasoner.getReasonerNames();
        }

        Evaluation.writeEvalResult(reasonerNames, reasoningTimeDataDir + "/" + resultFileName, evalResultSet);
    }
}
