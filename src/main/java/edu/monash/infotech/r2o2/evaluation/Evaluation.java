package edu.monash.infotech.r2o2.evaluation;

import edu.monash.infotech.r2o2.reasoner.R2O2;
import fantail.core.Tools;
import javafx.util.Pair;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ybkan on 24/02/2016.
 */
public class Evaluation {

    private int _numReasoner;
    private String _reasonerNames[];
    private String _extRasonerNames[];
    private ReasonerComponent [][]_predRankingResult;
    private Instances _predRankingInstances;
    private Instances _actualRankingInstances;
    private Instances _actualReasoningTimeInstances;

    private double _precisionSum[];
    private double _reasoningTimeSum[];
    private double _reasoningTimeDiffSum[];

    private R2O2 _metaReasoner;

    public Evaluation(Instances predRankingInstances, Instances actualRankingInstances, Instances actualReasoningTimeInstances) {
        _predRankingInstances = predRankingInstances;
        _actualRankingInstances = actualRankingInstances;
        _actualReasoningTimeInstances = actualReasoningTimeInstances;

        _reasonerNames = Tools.getTargetNames(_actualRankingInstances.firstInstance());
        _numReasoner = _reasonerNames.length;
        _extRasonerNames = new String[_numReasoner+2];
        for (int i = 0; i < _numReasoner; i++) {
            _extRasonerNames[i] = _reasonerNames[i];
        }
        _extRasonerNames[_numReasoner] = "portR";
        _extRasonerNames[_numReasoner+1] = "metaR";

        // the variable that contains the final evaluation result in terms of P@1
        _precisionSum = new double[_numReasoner+3];
        // the variable that contains the final evaluation result in terms of reasoning time sum
        _reasoningTimeSum = new double[_numReasoner+3];
        // the variable that contains the final evaluation result in terms of resoning time difference sum
        _reasoningTimeDiffSum = new double[_numReasoner+3];
    }

    public void setPredictedRankingResult (ReasonerComponent[][] predRankingResult) {
        _predRankingResult = predRankingResult;
    }

    public void setMetaReasoner(R2O2 metaReasoner) throws CloneNotSupportedException {
        _metaReasoner = (R2O2) metaReasoner.clone();
    }

    public int getReasonerNum() {
        return _numReasoner;
    }

    private List<ReasonerComponent> findBestReasoner(List<ReasonerComponent> ranking) {

        List<ReasonerComponent> besReasoners = new ArrayList();
        for (ReasonerComponent c : ranking) {
            if (c.getRank() == 1) { // Assume that the first rank starts from 1.
                besReasoners.add(c);
            }
        }
        return besReasoners;
    }

    public RankingEvalResult evaluatePredictedRanking() throws Exception {

        RankingEvalResult rankingEvalResult = new RankingEvalResult();
        ReasoningPerformance performance[][] = new ReasoningPerformance [_predRankingResult.length][_numReasoner+2]; // include {PR,MR,BR}

        // For each test instance, we perform the following:
        for (int i = 0; i < _predRankingResult.length; i++) {

            // read predicted ranking by rankers
            ReasonerComponent[] metaRanking = _predRankingResult[i];

            // read actual ranking & find actual best reasoner dealing with the current testing instance
            double[] actualRankingArray = Tools.getTargetVector(_actualRankingInstances.instance(i));
            List<ReasonerComponent> actualRanking = sortRanking(actualRankingArray);
            List<ReasonerComponent> actualR = findBestReasoner(actualRanking);

            // read pred ranking by prediction models: portfolio approach
            double[] predRankingArray = Tools.getTargetVector(_predRankingInstances.instance(i));
            ReasonerComponent predRanking[] = _metaReasoner.getRanking(predRankingArray);

            // Get the portfolio reasoner dealing with the current testing instance
            ReasonerComponent portR = _metaReasoner.findBestReasonerRandomly(predRanking);

            // find the meta-reasoner dealing with the current testing instance
            ReasonerComponent metaR = _metaReasoner.findBestReasoner(metaRanking);

            // Calculate P@1 by comparing the selected predicted reasoner with the gold-standard
            double precision[] = compPrecisionUsingRegression(actualR, portR, metaR);

            // Calculate reasoning time sum and resoning time difference sum by comparing the selected predicted reasoner with the gold-standard
            Instance actualTimeInstance = _actualReasoningTimeInstances.instance(i);
            Pair<double[], double[]> timeEvalResult = compTimeDiff (actualTimeInstance, actualR, portR, metaR);

            // evaluate predicted reasoning time
            performance[i] = compEvalStatsByRegModel(actualTimeInstance, actualR, portR, metaR);

            for (int p = 0; p < precision.length; p++) {
                _precisionSum[p] += precision[p];
                _reasoningTimeSum[p] += timeEvalResult.getKey()[p];
                _reasoningTimeDiffSum[p] += timeEvalResult.getValue()[p];
            }
        }

        // Set the results to the RankingEvalResult object to be returned.
        for (int i = 0; i < _numReasoner+2; i++) {
            rankingEvalResult.precisionUsingAR.put(i, _precisionSum[i]);
            rankingEvalResult.reasoningTimeSum.put(i, _reasoningTimeSum[i]);
            rankingEvalResult.reasoningTimeDiffFromGoldStd.put(i, _reasoningTimeDiffSum[i]);
        }
        rankingEvalResult.performance = performance;

        return rankingEvalResult;
    }

    public ReasoningPerformance[] compEvalStatsByRegModel(Instance actualTimeInstance,
                                        List<ReasonerComponent> actualR,
                                        ReasonerComponent portR,
                                        ReasonerComponent metaR) throws Exception {

        int portRIndex = _numReasoner;
        int metaRIndex = portRIndex + 1;
        int actualRIndex = metaRIndex + 1;

        double actualTimes[] = Tools.getTargetVector(actualTimeInstance);
        double portTime =  Math.expm1(actualTimes[portR.getIndex()]);
        double metaRTime = Math.expm1((actualTimes[metaR.getIndex()]));
        double actualTime = Math.expm1(actualTimes[actualR.get(0).getIndex()]);

        // Read the discretized index of the labels of PR and MR
        String portTimeD = Discretizer.getDiscValue(portTime, 4);
        String metaRTimeD = Discretizer.getDiscValue(metaRTime, 4);
        String actualTimeD = Discretizer.getDiscValue(actualTime, 4);

        // Set reasoning performance
        ReasoningPerformance portRP = new ReasoningPerformance(portTime, portTimeD);
        ReasoningPerformance metaRP = new ReasoningPerformance(metaRTime, metaRTimeD);
        ReasoningPerformance actualRP = new ReasoningPerformance(actualTime, actualTimeD);

        ReasoningPerformance performance[] = new ReasoningPerformance[actualRIndex+1];
        performance[portRIndex] = portRP;
        performance[metaRIndex] = metaRP;
        performance[actualRIndex] = actualRP;

        // For other reasoner, add their reasoning time
        for (int i = 0; i < _numReasoner; i++) {
            // Read actual reasoning time of each reasoner
            double reasoningTime = Math.expm1(actualTimes[i]);

            // Read the discretized label of the above reasoning time
            String RD = Discretizer.getDiscValue(reasoningTime, 4);
            performance[i] = new ReasoningPerformance(reasoningTime, RD);
        }

        return performance;
    }

    public Pair<double[], double[]> compTimeDiff (Instance actualTimeInstance,
                                  List<ReasonerComponent> actualR,
                                  ReasonerComponent portR,
                                  ReasonerComponent metaR) throws Exception {

        double reasoningTime[] = new double[_numReasoner + 2];
        double reasoningTimeDiff[] = new double[_numReasoner + 2];

        int portRIndex = _numReasoner;
        int metaRIndex = portRIndex + 1;

        double actualTimes[] = Tools.getTargetVector(actualTimeInstance);
        double actualTime = Math.expm1(actualTimes[actualR.get(0).getIndex()]);
        double portTime =  Math.expm1(actualTimes[portR.getIndex()]);
        double metaRTime = Math.expm1((actualTimes[metaR.getIndex()]));

        reasoningTime[portRIndex] = portTime;
        reasoningTime[metaRIndex] = metaRTime;

        reasoningTimeDiff[portRIndex] = portTime - actualTime;
        reasoningTimeDiff[metaRIndex] = metaRTime - actualTime;

        // For individual reasoners, add their reasoning time and differences
        for (int i = 0; i < _numReasoner; i++) {
            reasoningTime[i] =  Math.expm1((actualTimes[i]));
            reasoningTimeDiff[i] = reasoningTime[i] - actualTime;
        }

        Pair<double[], double[]> result = new Pair(reasoningTime, reasoningTimeDiff);
        return result;
    }

    private List<ReasonerComponent> sortRanking(double [] list) {
        List<ReasonerComponent> res = new ArrayList<ReasonerComponent>();
        for (int i = 0;i < list.length; i++) {
            ReasonerComponent cr = new ReasonerComponent(i, list[i]);
            res.add(cr);
        }
        Collections.sort(res, Collections.reverseOrder());
        return res;
    }

    public String evalToString() {

        DecimalFormat df = new DecimalFormat("0.000");

        // Print evaluation results
        StringBuilder resultStr = new StringBuilder();

        resultStr.append("#evaluation result:\n");
        for (int i = 0; i < _numReasoner+2; i++) {
            resultStr.append(_extRasonerNames[i] + ", " +
                    df.format(_precisionSum[i]/_predRankingResult.length) + "," +
                    df.format(_reasoningTimeSum[i]/_predRankingResult.length) + "," +
                    df.format(_reasoningTimeDiffSum[i]/_predRankingResult.length) + "\n");
        }

        return resultStr.toString();
    }


    public double[] compPrecisionUsingRegression(List<ReasonerComponent> actualR,
                                                 ReasonerComponent portR,
                                                 ReasonerComponent metaR) throws Exception {

        double precision[] = new double[_numReasoner+2];
        int pr_index = _numReasoner;
        int mr_index = pr_index + 1;

        // find portR's precision@1s
        for (ReasonerComponent r : actualR) {
            if (r.getIndex() == portR.getIndex()) {
                precision[pr_index]++;
                break;
            }
        }

        // find metaR's precision@1
        for (ReasonerComponent r : actualR) {
            if (r.getIndex() == metaR.getIndex()) {
                precision[mr_index]++;
                break;
            }
        }


        // find actual best reasoner's precision@1
        for (int i = 0; i < _numReasoner; i++) {
            for (ReasonerComponent r : actualR) {
                if (r.getIndex() == i) {
                    precision[i]++;
                    break;
                }
            }
        }

        return precision;
    }

    public static void writeEvalResult(String reasonerNames[], String result_fname, List<RankingEvalResult> resultSet) throws Exception {

        final int N = 2;

        StringBuilder result = new StringBuilder();

        int reasonerNum = reasonerNames.length;

        // set reasoner names to be compared including portR and metaR
        String evalReasonerNames[] = new String[reasonerNum+N];
        for (int i = 0; i < reasonerNames.length; i++) {
            evalReasonerNames[i] = reasonerNames[i];
        }
        evalReasonerNames[reasonerNum] = "portR";
        evalReasonerNames[reasonerNum+1] = "metaR";

        // writer header string
        result.append("Fold,P@1");
        for (int i = 0; i < reasonerNum+N; i++) result.append(",");
        result.append(",RT");
        for (int i = 0; i < reasonerNum+N; i++) result.append(",");
        result.append(",RT Diff");
        for (int i = 0; i < reasonerNum+N; i++) result.append(",");
        result.append(",\n");

        result.append(",");
        for (int i = 0; i < reasonerNum+N; i++)
            result.append(evalReasonerNames[i] + ",");
        result.append(",");
        for (int i = 0; i < reasonerNum+N; i++)
            result.append(evalReasonerNames[i] + ",");
        result.append(",");
        for (int i = 0; i < reasonerNum+N; i++)
            result.append(evalReasonerNames[i] + ",");
        result.append("\n");

        // write evaluation result
        for (int k = 0; k < resultSet.size(); k++) {

            RankingEvalResult rankingEvalResult = resultSet.get(k);

            StringBuilder sb_p1 = new StringBuilder();
            StringBuilder sb_rt = new StringBuilder();
            StringBuilder sb_rt_diff = new StringBuilder();

            for (int i = 0; i < reasonerNum+N; i++) {
                if (i == reasonerNum+1) {
                    sb_p1.append(rankingEvalResult.precisionUsingAR.get(i) + ",,");
                    sb_rt.append(rankingEvalResult.reasoningTimeSum.get(i) + ",,");
                    sb_rt_diff.append(rankingEvalResult.reasoningTimeDiffFromGoldStd.get(i));
                } else {
                    sb_p1.append(rankingEvalResult.precisionUsingAR.get(i) + ",");
                    sb_rt.append(rankingEvalResult.reasoningTimeSum.get(i) + ",");
                    sb_rt_diff.append(rankingEvalResult.reasoningTimeDiffFromGoldStd.get(i) + ",");
                }
            }
            result.append(k + "," + sb_p1.toString() + sb_rt.toString() + sb_rt_diff.toString() + "\n");
        }

        // write the evaluation result to a file
        BufferedWriter writer = new BufferedWriter(new FileWriter(result_fname));
        writer.write(result.toString());
        writer.close();

        // Write reasoning performance
        StringBuilder detailedResult = new StringBuilder();

        detailedResult.append("Disc Label,");
        for (int i = 0; i < evalReasonerNames.length; i++)
            detailedResult.append(evalReasonerNames[i] + ",");
        detailedResult.append("\n");

        StringBuilder sb_a = new StringBuilder();
        StringBuilder sb_b = new StringBuilder();
        StringBuilder sb_c = new StringBuilder();
        StringBuilder sb_d = new StringBuilder();

        int actualReasonerIndex = reasonerNum + N;
        for (int k = 0; k < resultSet.size(); k++) {
            RankingEvalResult rankingEvalResult = resultSet.get(k);
            ReasoningPerformance performance[][] = rankingEvalResult.performance;

            int numInstance = performance.length;
            //System.out.println("instance num:" + num_instance);
            for (int i = 0; i < numInstance; i++) {
                ReasoningPerformance actualR = performance[i][actualReasonerIndex];
                StringBuilder currentResult = new StringBuilder();

                if (actualR.getDiscLabel().equalsIgnoreCase(Discretizer.DISC_LABEL[0])) {
                    currentResult = sb_a;
                } else if (actualR.getDiscLabel().equalsIgnoreCase(Discretizer.DISC_LABEL[1])) {
                    currentResult = sb_b;
                } else if (actualR.getDiscLabel().equalsIgnoreCase(Discretizer.DISC_LABEL[2])) {
                    currentResult = sb_c;
                } else if (actualR.getDiscLabel().equalsIgnoreCase(Discretizer.DISC_LABEL[3])) {
                    currentResult = sb_d;
                }
                currentResult.append(actualR.getDiscLabel() + ",");
                for (int j = 0; j <= actualReasonerIndex; j++) {
                    ReasoningPerformance pr = performance[i][j];
                    currentResult.append(pr.getReasoningTime() + ",");
                }
                currentResult.append("\n");
            }
        }

        BufferedWriter rp_writer = new BufferedWriter(new FileWriter(result_fname + ".rp.csv"));
        rp_writer.write(detailedResult.toString());
        rp_writer.write(sb_a.toString() + sb_b.toString() + sb_c.toString() + sb_d.toString());
        rp_writer.close();
    }
}