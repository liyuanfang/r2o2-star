/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fantail.core;

import edu.monash.infotech.r2o2.evaluation.ReasonerComponent;
import fantail.algorithms.AbstractRanker;
import weka.core.Debug.Random;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class MultiRunEvaluation {

    private Instances m_Data;
    private Random m_Rand;
    private double[] m_ScoreKendall;
    private double[] m_ScoreSpearmanCC;
    private double[] m_ScorePrecision;

    public MultiRunEvaluation(Instances data) {
        m_Data = new Instances(data);
    }

    public double getScoreKendall() {
        return weka.core.Utils.mean(m_ScoreKendall);
    }

    public double getScoreSpearmanCC() {
        return weka.core.Utils.mean(m_ScoreSpearmanCC);
    }

    public double getScorePrecision() {
        return weka.core.Utils.mean(m_ScorePrecision);
    }

    public double getScoreSpearmanCCStd() {
        return Math.sqrt(weka.core.Utils.variance(m_ScoreSpearmanCC));
    }

    public double getScoreKendallStd() {
        return Math.sqrt(weka.core.Utils.variance(m_ScoreKendall));
    }


    public void multiRunEvaluate(AbstractRanker ranker, int randSeed) throws Exception {
        //m_NumRuns = numRuns;
        m_Rand = new Random();

        int nFold = 5;
        //
        m_ScoreKendall = new double[nFold];
        m_ScoreSpearmanCC = new double[nFold];
        m_ScorePrecision = new double[nFold];
        //
        //m_Data.randomize(m_Rand);

        for (int i = 0; i < nFold; i++) {
            Instances train = m_Data.trainCV(nFold, i);
            Instances test = m_Data.testCV(nFold, i);

            //System.out.println("train size:" + train.size() + ", test size:" + test.size());
            ranker.buildRanker(train);

            double localScoreKendall = 0;
            double localScoreSpearmanCC = 0;
            double localScorePrecision = 0;

            for (int m = 0; m < test.numInstances(); m++) {
                Instance inst = test.instance(m);
                double[] pred = ranker.recommendRanking(inst);
                double[] actual = Tools.getTargetVector(inst);

                //System.out.println("test instance:" + inst.toString());

                List<ReasonerComponent> pred_list = sortRanking(pred);
                List<ReasonerComponent> actual_list = sortRanking(actual);
                double precision = computePrecision(pred_list, actual_list);
                localScorePrecision += precision;

                //System.out.println("\tprecision:" + precision + " ==> pred:" + pred_list + ", actual:" + actual_list);
                //System.out.println("\t ==> pred:" + Arrays.toString(pred) + ", actual:" + Arrays.toString(actual));

                //localScoreKendall += EvalTools.computeKendallTau(actual, pred);
                //localScoreSpearmanCC += EvalTools.computeSpearmanCC(actual, pred);
            }

            localScoreKendall /= test.numInstances();
            localScoreSpearmanCC /= test.numInstances();
            localScorePrecision /= test.numInstances();

            m_ScoreKendall[i] += localScoreKendall;
            m_ScoreSpearmanCC[i] += localScoreSpearmanCC;
            m_ScorePrecision[i] += localScorePrecision;
        }
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

    private double computePrecision (List<ReasonerComponent> pred_list, List<ReasonerComponent> actual_list) {

        // ex: pred [4.0, 3.0, 2.0, 1.0], actual:[1.0, 1.0, 1.0, 4.0]

        double res = 0;
        double first_rank_in_pred = pred_list.get(0).getRank();
        double first_rank_in_actual = actual_list.get(0).getRank();

        for (int i = 0; i < actual_list.size(); i++) {

            double actual_rank =  actual_list.get(i).getRank();
            int actual_label = actual_list.get(i).getIndex();

            if (actual_rank == first_rank_in_actual) {
                for (int j = 0; j < pred_list.size(); j++) {
                    double pred_rank =  pred_list.get(j).getRank();
                    int pred_label = pred_list.get(j).getIndex();
                    if (pred_rank == first_rank_in_pred) {
                        if (actual_label == pred_label)
                            return 1;
                    }
                }
            }
        }
        return res;
    }
}

