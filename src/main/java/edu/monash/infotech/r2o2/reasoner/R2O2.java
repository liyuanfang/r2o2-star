package edu.monash.infotech.r2o2.reasoner;

import edu.monash.infotech.r2o2.data.DataManager;
import edu.monash.infotech.r2o2.evaluation.ReasonerComponent;
import edu.monash.infotech.r2o2.prediction.Preprocessor;
import edu.monash.infotech.r2o2.prediction.RegressionModel;
import fantail.algorithms.*;
import fantail.core.MultiRunEvaluation;
import fantail.core.NominalConverter;
import fantail.core.Tools;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by Yong-Bin on 23/02/2016.
 */
public class R2O2 implements Cloneable {

    public enum RankerEnum {
        KNNRanker, RegRanker, RPCRanker, PCTRanker, BiARFRanker, ARFRanker;
    };

    private HashMap<String, RegressionModel> _predictionModels;

    // embedded rankers in r2o2;
    private List<AbstractRanker> _rankers;

    private int _numReasoner;
    private String _reasoners[];
    private HashMap<String, Integer> _reasonerNameIndexMap;
    private HashMap<String, ReasonerComponent> _pmPerformanceIndicator;
    private HashMap<String, Double> _rankerPerformanceIndicator;

    private double _minAvgRank, _maxAvgRank;

    public R2O2() {

    }

    public R2O2(String rankerModelDirectory, String modelExtension) throws Exception {
        _rankers = new ArrayList();

        File dir = new File(rankerModelDirectory);
        File[] files = dir.listFiles();
        for (File f: files) {
            if (!f.getName().endsWith("." + modelExtension)) continue;

            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            AbstractRanker ranker = (AbstractRanker) ois.readObject();
            ois.close();
            _rankers.add(ranker);
        }
    }

    public void buildModel(String trainFile, String modelExtension, String summaryFileName) throws Exception {

        Instances data = Tools.loadFantailARFFInstances(trainFile);

        StringBuilder log = new StringBuilder();
        log.append("#Num of labels: " + Tools.getNumberTargets(data) + "\n");
        log.append("#Num of instances: " + data.numInstances() + "\n");
        log.append("#Num of attributes (incl. target att): " + data.numAttributes() + "\n\n");
        String strFormat = "%-30s %-30s";
        log.append("#<Algorithms>, <P@1>\n");

        double avgScore = 0;
        AbstractRanker ranker = null;
        for (RankerEnum target : RankerEnum.values()) {
            switch (target) {
                case KNNRanker:
                    ranker = new RankingWithkNN();
                    break;
                case RegRanker:
                    ranker = new RankingViaRegression();
                    break;
                case RPCRanker:
                    ranker = new RankingByPairwiseComparison();
                    break;
                case PCTRanker:
                    ranker = new RankingWithBinaryPCT();
                    break;
                case BiARFRanker:
                    ranker = new BinaryART();
                    ((BinaryART) ranker).setK(9999);
                    ((BinaryART) ranker).setMiniLeaf(1);
                    data = NominalConverter.covertNominalToNumeric(data);
                    break;
                case ARFRanker:
                    ranker = new ARTForests();
                    ((ARTForests) ranker).setNumIterations(50);
                    ((ARTForests) ranker).setK(0);
                    ((ARTForests) ranker).setNumMinInstances(1);
                    data = NominalConverter.covertNominalToNumeric(data);
                    break;
            }

            // build a ranker
            ranker.buildRanker(data);

            // save a ranker
            saveRanker (ranker, trainFile, modelExtension);

            MultiRunEvaluation eval = new MultiRunEvaluation(data);
            eval.multiRunEvaluate(ranker, 1);
            double score = roundDouble(eval.getScorePrecision(), 3);
            avgScore += score;
            log.append(ranker.rankerName() + "," + score + "\n");

            System.out.println("\tbuilding " + ranker.rankerName() + " is done!");
        }
        log.append("ranker," + (double) avgScore/RankerEnum.values().length + "\n");

        // generate the ranker summary file
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(trainFile).getParent() + "/" + summaryFileName));
        writer.write(log.toString());
        writer.close();

        // generate the average ranks of reasoner components
        data = Tools.loadFantailARFFInstances(trainFile);
        _reasoners = Tools.getTargetNames(data.instance(0));
        _numReasoner = _reasoners.length;
        double rank_sum[] = new double[_numReasoner];
        for (int i = 0; i < data.numInstances(); i++) {
            double[] ranking = Tools.getTargetVector(data.instance(i));
            for (int k = 0; k < ranking.length; k++) {
                rank_sum[k] += ranking[k];
            }
        }

        StringBuilder avgRankingStr = new StringBuilder();
        double rank_avg[] = new double[rank_sum.length];
        for (int i = 0; i < rank_sum.length; i++) {
            rank_avg[i] = rank_sum[i] / data.numInstances();
            avgRankingStr.append(_reasoners[i] + "," + rank_avg[i] + "\n");
        }
        writer = new BufferedWriter(new FileWriter(new File(trainFile).getParent() + "/" + summaryFileName + ".avgRank"));
        writer.write(avgRankingStr.toString());
        writer.close();
    }

    public String[] getReasonerNames(String pmPerfFileName)throws Exception {

        List<String> reasonerNames = new ArrayList();

        // read prediction performance of reasoners to see what reasoner components were used
        BufferedReader reader = new BufferedReader(new FileReader(pmPerfFileName));
        String line = "";
        while ((line = reader.readLine()) != null) {

            String em[] = line.split(",");
            String label = em[0];

            reasonerNames.add(label);
        }
        reader.close();

        return reasonerNames.toArray(new String[reasonerNames.size()]);
    }

    public String[] getReasonerNames() {
        return _reasoners;
    }

    public int getReasonerNum() {
        return _numReasoner;
    }

    public ReasonerComponent[][] recommendRankings (Instances testMetaData) throws Exception {

        // the variable to be returned: it contains ranking of reasoners for each instance
        ReasonerComponent[][] predRankings = new ReasonerComponent[testMetaData.numInstances()][_numReasoner];

        for (int m = 0; m < testMetaData.numInstances(); m++) {
            predRankings[m] = recommendRanking(testMetaData.instance(m));
        }
        return predRankings;
    }

    public ReasonerComponent[] recommendRanking (Instance instance) throws Exception {

        // the variable to be returned: it contains ranking of reasoners for each instance
        ReasonerComponent[] predRanking = new ReasonerComponent[_numReasoner];

        double ranking[] = predictRanking(instance);
        ReasonerComponent[] predRankingList = getRanking(ranking);

        for (int j = 0; j < predRankingList.length; j++) {
            ReasonerComponent rc = new ReasonerComponent();
            rc.setIndex(predRankingList[j].getIndex());
            rc.setLabel(predRankingList[j].getLabel());
            rc.setAvgRank(predRankingList[j].getAvgRank());
            rc.setRank(predRankingList[j].getRank());
            rc.setCoeffDetermination(predRankingList[j].getCoeffDetermination());
            predRanking[j] = rc;
        }
        return predRanking;
    }

    public void readMetaDataProfile (Instances testMetaData,
                                     String pmPerfFile,
                                     String avgRankPerfFile,
                                     String rankPerfFile,
                                     String portfolioPerfFile) throws Exception {

        // Get number of reasoners, reasoner names, reasoners names and their indices. These will be used in evaluating R2O2.
        _reasoners = Tools.getTargetNames(testMetaData.firstInstance());
        _numReasoner =_reasoners.length;

        _reasonerNameIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < _reasoners.length; i++) {
            _reasonerNameIndexMap.put(_reasoners[i], i);
        }
        _pmPerformanceIndicator = readReasonerPerformance(pmPerfFile, avgRankPerfFile);

        _rankerPerformanceIndicator = readRankerPerformance(rankPerfFile, portfolioPerfFile);
    }

    public void readMetaDataProfile (String reasonerNames[],
                                     String pmPerfFile,
                                     String avgRankPerfFile,
                                     String rankPerfFile) throws Exception {

        _reasoners = reasonerNames;
        _numReasoner =_reasoners.length;
        _reasonerNameIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < _reasoners.length; i++) {
            _reasonerNameIndexMap.put(_reasoners[i], i);
        }

        _pmPerformanceIndicator = readReasonerPerformance(pmPerfFile, avgRankPerfFile);
        _rankerPerformanceIndicator = readRankerPerformance(rankPerfFile, "");
    }

    private double[] predictRanking (Instance instance) throws Exception {
        double avg_ranking[] = new double[_numReasoner];

        // add the rank of each reasoner by each ranker
        for (AbstractRanker ranker: _rankers) {
            double ranking[] = ranker.recommendRanking(instance);
            for (int i = 0; i < ranking.length; i++) {
                avg_ranking[i] += ranking[i];
            }
            //System.out.println("\t\tranker:" + ranker.rankerName() + ":" + Arrays.toString(ranking));
        }

        // calculate the average ranks of all reasoners
        for (int i = 0; i < avg_ranking.length; i++) {
            avg_ranking[i] /= _rankers.size();
        }
        return avg_ranking;
    }

    public ReasonerComponent[] getRanking(double [] list) {

        double rank[] = new NaturalRanking(TiesStrategy.MINIMUM).rank(list);

        ReasonerComponent[] res = new ReasonerComponent[_numReasoner];
        for (int i = 0; i < list.length; i++) {
            ReasonerComponent reasonerComponent = _pmPerformanceIndicator.get(_reasoners[i]);
            reasonerComponent.setRank(rank[i]);
            res[i] = reasonerComponent;
        }

        return res;
    }

    public ReasonerComponent findBestReasoner(ReasonerComponent[] unsortedMetaRanking) throws Exception {

        ReasonerComponent bestReasoner = null;

        // sort the ranking list by ranks
        List<ReasonerComponent> resList = new ArrayList<>(Arrays.asList(unsortedMetaRanking));
        Collections.sort(resList, Collections.reverseOrder());
        ReasonerComponent[] sortedMetaRanking = resList.toArray(new ReasonerComponent[resList.size()]);

        // Step 1: Find how many reasoners are ranked the highest
        // predRanking is a sorted list of the reasoners according to their ranks.
        List<ReasonerComponent> bestReasonerCandidateSet = new ArrayList();
        double best_rank = 0;
        for (int i = 0; i < sortedMetaRanking.length; i++) {
            if (i==0) {
                best_rank = sortedMetaRanking[i].getRank();
                bestReasonerCandidateSet.add(sortedMetaRanking[i]);
            } else {
                double rank = sortedMetaRanking[i].getRank();
                if (rank == best_rank) {
                    bestReasonerCandidateSet.add(sortedMetaRanking[i]);
                }
            }
        }

        // Step 2: If there are multiple reasoners ranked the top, we find the best reasoner considering their average ranks.
        if (bestReasonerCandidateSet.size() == 1) {
            bestReasoner = bestReasonerCandidateSet.get(0);
        } else {
            double maxConfidence = Double.MIN_VALUE;
            for (ReasonerComponent c: bestReasonerCandidateSet) {
                double confidence = 1-((c.getAvgRank()-_minAvgRank)/(_maxAvgRank-_minAvgRank));
                if (maxConfidence < confidence) {
                    maxConfidence = confidence;
                    bestReasoner = c;
                }
            }
        }

        return bestReasoner;
    }

    public ReasonerComponent findBestReasonerRandomly(ReasonerComponent[] unsortedPredRanking) throws Exception {

        ReasonerComponent bestReasoner = null;

        // sort the ranking list by ranks
        List<ReasonerComponent> resList = new ArrayList<>(Arrays.asList(unsortedPredRanking));
        Collections.sort(resList, Collections.reverseOrder());
        ReasonerComponent[] predRanking = resList.toArray(new ReasonerComponent[resList.size()]);

        // Step 1: Find how many reasoners are ranked the highest
        // predRanking is a sorted list of the reasoners according to their ranks.
        List<ReasonerComponent> bestReasonerCandidateSet = new ArrayList<ReasonerComponent>();
        double best_rank = 0;
        for (int i = 0; i < predRanking.length; i++) {
            if (i==0) {
                best_rank = predRanking[i].getRank();
                bestReasonerCandidateSet.add(predRanking[i]);
            } else {
                double rank = predRanking[i].getRank();
                if (rank == best_rank) {
                    bestReasonerCandidateSet.add(predRanking[i]);
                }
            }
        }
        //System.out.println("bestReasonerCandidateSet:" + bestReasonerCandidateSet);

        // Step 2: If there are multiple reasoners ranked the top, we find the best reasoner randomly.
        if (bestReasonerCandidateSet.size() == 1) {
            bestReasoner = bestReasonerCandidateSet.get(0);
        } else {
            bestReasoner = bestReasonerCandidateSet.get(new Random().nextInt(bestReasonerCandidateSet.size()));
        }

        //System.out.println("Best:" + bestReasoner);
        return bestReasoner;
    }

    private HashMap<String, ReasonerComponent> readReasonerPerformance(String pmPerfFile, String avgRankPerfFile) throws Exception {

        HashMap<String, ReasonerComponent> performance = new LinkedHashMap();

        // read prediction performance of reasoners in terms of r^2
        BufferedReader reader = new BufferedReader(new FileReader(pmPerfFile));
        String line = "";
        while ((line = reader.readLine()) != null) {

            String em[] = line.split(",");
            String label = em[0];
            double performanceScore = Double.parseDouble(em[1]);

            ReasonerComponent c = new ReasonerComponent();
            c.setIndex(_reasonerNameIndexMap.get(label));
            c.setLabel(label);
            c.setRank(0); // default
            c.setCoeffDetermination(performanceScore);
            performance.put(label, c);
        }
        reader.close();

        // read average ranking of reasoners in the ranking matrix
        if (new File(avgRankPerfFile).exists()) {
            reader = new BufferedReader(new FileReader(avgRankPerfFile));
            while ((line = reader.readLine()) != null) {

                String em[] = line.split(",");
                String label = em[0];
                double performanceScore = Double.parseDouble(em[1]);

                ReasonerComponent c = performance.get(label);
                c.setAvgRank(performanceScore);
                _minAvgRank = Math.min(performanceScore, _minAvgRank);
                _maxAvgRank = Math.max(performanceScore, _maxAvgRank);
                performance.put(label, c);
            }
            reader.close();
        }

        return performance;
    }

    private HashMap<String, Double> readRankerPerformance (String rankerPerfFile, String portfolioPerfFile) throws Exception {

        HashMap<String, Double> performance = new LinkedHashMap();

        File file = new File(rankerPerfFile);
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(rankerPerfFile));
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                if (line.trim().length() == 0) continue;

                String em[] = line.split(",");
                performance.put(em[0], Double.parseDouble(em[1]));
            }
            reader.close();
        }

        file = new File(portfolioPerfFile);
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(portfolioPerfFile));
            String line = "";
            while ((line = reader.readLine()) != null) {
                String em[] = line.split(",");
                performance.put(em[0], Double.parseDouble(em[1]));
            }
            reader.close();
        }

        return performance;
    }

    private static double roundDouble(double n, int dPlaces) {
        return weka.core.Utils.roundDouble(n, dPlaces);
    }

    public void saveRanker(AbstractRanker ranker, String trainFile, String modelExtension) throws Exception {
        String dir = new File(trainFile).getParent();
        String outFile = dir + "/" + ranker.rankerName() + "." + modelExtension;
        FileOutputStream file = new FileOutputStream(outFile);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(ranker);
        save.close();
    }


    public void loadPredictionModels(String dir, String predictionModelExtension) throws Exception {

        // load prediction models
        _predictionModels = new LinkedHashMap<>();
        File foldDir = new File(dir);
        File files[] = foldDir.listFiles();
        for (File file : files) {
            if (file.getName().startsWith("r.") && file.getName().endsWith(predictionModelExtension)) {

                // load prediction models of all reasoners
                RegressionModel regressionModel = new RegressionModel();
                String reasonerName = regressionModel.getReasonerName(file.getName());
                regressionModel.loadClassifier(file.getAbsolutePath());
                _predictionModels.put(reasonerName, regressionModel);
            }
        }
    }

    public double predict(ReasonerComponent reasonerComponent, Instance inputInstance) throws Exception {
        double predictedTime = _predictionModels.get(reasonerComponent.getLabel()).classifyInstance(inputInstance);
        return Math.expm1(predictedTime);
    }

    public double run (ReasonerComponent reasonerComponent, String currentResourceDir, Instance inputInstance) throws Exception {

        double startTime = getUserTime();

        /**
         * load the best reasoner and run it on the inputInstance (reasoner index = reasonerComponent.getIndex()+1)
         */

        // load the input ontology
        String ontNamePath = currentResourceDir + "/" + inputInstance.stringValue(0);

        // load the best reasoner
        /**
         * fact
           hermit
           jfact
           konclude
           more
           pellet
           trowl
         */
        ReasonerWrapper.REASONER_ID reasoner_id = null;
        switch (reasonerComponent.getLabel()) {
            case "fact":
                reasoner_id = ReasonerWrapper.REASONER_ID.FACT;
                break;
            case "hermit":
                reasoner_id = ReasonerWrapper.REASONER_ID.HERMIT;
                break;
            case "jfact":
                reasoner_id = ReasonerWrapper.REASONER_ID.JFACT;
                break;
            case "konclude":
                // Konclude's server must be installed for this to work
                reasoner_id = ReasonerWrapper.REASONER_ID.KONCLUDE;
                //reasoner_id = ReasonerWrapper.REASONER_ID.HERMIT;
                break;
            case "more":
                reasoner_id = ReasonerWrapper.REASONER_ID.MORE;
                break;
            case "pellet":
                reasoner_id = ReasonerWrapper.REASONER_ID.PELLET;
                break;
            case "trowl":
                reasoner_id = ReasonerWrapper.REASONER_ID.TROWL;
                break;
        }

        ReasonerRunner reasonerRunner = new ReasonerRunner(reasoner_id);
        reasonerRunner.setOntology(ontNamePath);
        reasonerRunner.createReasoner();
        boolean isSuccess = reasonerRunner.doClassification();
        double endTime = getUserTime();

        if (isSuccess) {
            return (endTime - startTime);
        } else {
            return -1;
        }
    }

    public long getUserTime() {
        ThreadMXBean tb = ManagementFactory.getThreadMXBean();
        return tb.getCurrentThreadUserTime()/(1000000L);
    }

    public Instances transformTestInstances(String testOntMatrixFilename, String trainMetricPropertyFile) throws Exception {

        Preprocessor preprocessor = new Preprocessor();
        Instances testRawInstances = preprocessor.transform(testOntMatrixFilename, trainMetricPropertyFile, "");
        return testRawInstances;
    }

    public Instances readOntology (String testOntFilename) throws Exception {
        Instances testOntInstances = new Instances(new BufferedReader(new FileReader(testOntFilename)));
        testOntInstances.setClassIndex(-1); // default: there is no class index
        return testOntInstances;
    }

    public Instances generatePMInstances(Instances testRawInstances) throws Exception {
        DataManager dataManager = new DataManager();
        Instances newData = dataManager.generatePMInstances(testRawInstances);
        return newData;
    }

    public Instances generateMetaInstances(Instances testRawInstances, String[] reasonerNames) throws Exception {
        DataManager dataManager = new DataManager();
        Instances newData = dataManager.generateMetaInstances(testRawInstances, reasonerNames);
        return newData;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
