package edu.monash.infotech.r2o2.demo;

import edu.monash.infotech.r2o2.evaluation.ReasonerComponent;
import fantail.algorithms.*;
import fantail.core.MultiRunEvaluation;
import fantail.core.NominalConverter;
import fantail.core.Tools;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class R2O2 implements Cloneable {

    public enum RankerEnum {
        KNNRanker, RegRanker, RPCRanker, PCTRanker, ARFRanker;
    };


    // embedded rankers in r2o2;
    private List<AbstractRanker> _rankers;

    private int _numReasoner;
    private String _reasoners[];
    private HashMap<String, ReasonerComponent> _pmPerformanceIndicator;


    public R2O2() {

    }

    public R2O2(String rankerModelDirectory, String modelExtension) throws Exception {
        _rankers = new ArrayList<AbstractRanker>();

        File dir = new File(rankerModelDirectory);
        File[] files = dir.listFiles();
        for (File f: files) {
            if (!f.getName().endsWith("." + modelExtension)) continue;
            if (f.getName().endsWith("RPC (DecisionStump)." + modelExtension)) continue;
            if (f.getName().endsWith("ARTForests." + modelExtension)) continue;
            if (f.getName().endsWith("BinaryART." + modelExtension)) continue;
            if (f.getName().endsWith("RankingViaRegression." + modelExtension)) continue;
            if (f.getName().endsWith("BinaryPCT." + modelExtension)) continue;
            if (f.getName().endsWith("kNN." + modelExtension)) continue;

            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            AbstractRanker ranker = (AbstractRanker) ois.readObject();
            ois.close();
            _rankers.add(ranker);
        }
    }

    public void buildModel(String trainFile, String modelExtension, String summaryFileName, boolean needEvaluation) throws Exception {

        Instances data = Tools.loadFantailARFFInstances(trainFile);

        StringBuilder log = new StringBuilder();
        log.append("#Num of labels: " + Tools.getNumberTargets(data) + "\n");
        log.append("#Num of instances: " + data.numInstances() + "\n");
        log.append("#Num of attributes (incl. target att): " + data.numAttributes() + "\n\n");
        String strFormat = "%-30s %-30s";
        log.append("#<Algorithms>, <P@1>\n");

        HashMap<String, AbstractRanker> rankers = new LinkedHashMap<>();
        HashMap<String, List<double[]>> rankers_predictions = new LinkedHashMap<>();
        HashMap<String, Double> rankers_performance = new LinkedHashMap<>();

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

            // save a ranker to the hashmap: ranker
            rankers.put(target.name(), ranker);

            if (target.name().equalsIgnoreCase("ARFRanker"))
                saveRanker(ranker, trainFile, modelExtension);

            if (needEvaluation) {
                // put the prediction results for each ranker into the hashmap: rankers_predictions
                // We will use this hashmap to calculate the correlations between rankers
                MultiRunEvaluation eval = new MultiRunEvaluation(data);
                List<double[]> predictions = eval.multiRunEvaluate(ranker, 1);
                rankers_predictions.put(target.name(), predictions);

                //log.append(printResult2(strFormat, ranker, eval) + "\n");
                double score = roundDouble(eval.getScorePrecision(), 3);

                rankers_performance.put(target.name(), score);
                log.append(target.name() + "," + score + "\n");
                System.out.println("\t" + target.name() + "," + score);
            }
        }

        if (needEvaluation) {
            // generate the ranker summary file
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(trainFile).getParent() + "/" + summaryFileName));
            writer.write(log.toString());
            writer.close();
        }

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
        String dir = this.getClass().getClassLoader().getResource(new File(trainFile).getParent()).getPath();

        BufferedWriter writer = new BufferedWriter(new FileWriter(dir + "/" + summaryFileName + ".avgRank"));
        writer.write(avgRankingStr.toString());
        writer.close();

    }

    public ReasonerComponent[] recommendRanking (Instance instance) throws Exception {

        // the variable to be returned: it contains ranking of reasoners for each instance
        ReasonerComponent[] predRanking = new ReasonerComponent[_numReasoner];

        ReasonerComponent predRankingList[] = predictRankingByAverage(instance);

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

    private ReasonerComponent[] predictRankingByAverage(Instance instance) throws Exception {
        double avg_ranking[] = new double[_numReasoner];

        // add the rank of each reasoner by each ranker
        for (AbstractRanker ranker: _rankers) {
            double ranking[] = ranker.recommendRanking(instance);
            for (int i = 0; i < ranking.length; i++) {
                avg_ranking[i] += ranking[i];
            }
        }

        // calculate the average ranks of all reasoners
        for (int i = 0; i < avg_ranking.length; i++) {
            avg_ranking[i] /= _rankers.size();
        }

        // sort the ranks and the highest rank has the lowest value.
        double rank[] = new NaturalRanking(TiesStrategy.MINIMUM).rank(avg_ranking);
        ReasonerComponent[] rankList = new ReasonerComponent[_numReasoner];
        for (int i = 0; i < avg_ranking.length; i++) {
            ReasonerComponent reasonerComponent = _pmPerformanceIndicator.get(_reasoners[i]);
            reasonerComponent.setRank(rank[i]);
            rankList[i] = reasonerComponent;
        }
        return rankList;
    }

    private static double roundDouble(double n, int dPlaces) {
        return weka.core.Utils.roundDouble(n, dPlaces);
    }

    public void saveRanker(AbstractRanker ranker, String trainFile, String modelExtension) throws Exception {
        String dir = new File(trainFile).getParent();
        String dirPath = this.getClass().getClassLoader().getResource(dir).getPath();
        String outFile = dirPath + "/" + ranker.rankerName() + "." + modelExtension;
        FileOutputStream file = new FileOutputStream(outFile);
        ObjectOutputStream save = new ObjectOutputStream(file);
        save.writeObject(ranker);
        save.close();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
