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
package fantail.algorithms;

import fantail.core.Tools;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Arrays;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class BinaryART extends AbstractRanker {

    private double[] m_Prototype;
    private BinaryART[] m_Successors;
    private Attribute m_Attribute;
    private double m_SplitPoint;
    private int m_MiniLeaf = 1;
    private int m_K = 0;
    private int m_Seed = 1;
    private boolean m_UseMedian = true;
    private String m_ApproxCenterMethod = "AVG";
    private int m_MaxDepth = 0;
    private int m_NumObjects = 20;

    @Override
    public String rankerName() {
        return "BinaryART";
    }

    public void setUseMedian(boolean m) {
        m_UseMedian = m;
    }

    public void setNumObjects(int m) {
        m_NumObjects = m;
    }

    @Override
    public void buildRanker(Instances metaData) throws Exception {
        //
        java.util.Random r = new java.util.Random(m_Seed);
        Instances workingData = new Instances(metaData);
        m_NumObjects = Tools.getNumberTargets(workingData);
        //System.out.println(m_NumObjects);
        int depth0 = 1;
        makeTree(workingData, r, depth0);
    }

    private double estimateAvgDistanceSpearman(double m, double[] estimatedCenterRanking, double[] avgRankValues) throws Exception {
        double dotProd = dotProduct(estimatedCenterRanking, avgRankValues);
        return m * (m + 1) * (2 * m + 1) / 3 - 2 * dotProd;
    }

    private double estimateR2(Instances data, int attIndex, double splitPoint) throws Exception {
        Instances[] P = splitData(data, attIndex, splitPoint);
        double m = m_NumObjects;
        double sum1 = 0;
        for (int l = 0; l < P.length; l++) {
            //double estimatedCenterRanking[] = AbstractRanker.getCenterRanking(P[l], m_ApproxCenterMethod);          
            // paper version
            double estimatedCenterRanking[] = AbstractRanker.getAvgRankValues(P[l]);
            //double estimatedCenterRanking2[] = AbstractRanker.getAvgRanking(P[l]); // working version
            int n_L = P[l].numInstances();
            double avgDistance_L = estimateAvgDistanceSpearman(m, estimatedCenterRanking, estimatedCenterRanking);
            sum1 += n_L * avgDistance_L;
        }
        //
        double sum2 = 0;

        //double estimatedCenterRankingT[] = AbstractRanker.getCenterRanking(data, m_ApproxCenterMethod);
        double estimatedCenterRankingT[] = AbstractRanker.getAvgRankValues(data);
        double avgDistance_L = estimateAvgDistanceSpearman(m, estimatedCenterRankingT, estimatedCenterRankingT);

        //for (int l = 0; l < P.length; l++) {
        //double avgRankValues[] = AbstractRanker.getAvgRankValues(P[l]);
        //int n_L = P[l].numInstances();
        //double avgDistance_L = estimateAvgDistanceSpearman(P[l], m, estimatedCenterRankingT, avgRankValues);
        //sum2 += n_L * avgDistance_L;
        //}
        sum2 = data.numInstances() * avgDistance_L;
        //double r2 = 1.0 - sum1 / (sum2 + 0.00000001); // paper version
        double r2 = 1.0 - (sum1) / (sum2 + 0.0000); // working version 02 July 2013
        //double r2 = 1.0 - sum2 / (sum1 + 0.00000001);  // works well for larger label sets

        //System.out.println(r2);
        return r2;
    }

    private double dotProduct(double[] a, double[] b) throws Exception {
        double val = 0;
        for (int i = 0; i < a.length; i++) {
            val += a[i] * b[i];
        }
        return val;
    }

    private Instances[] splitData(Instances data, int attIndex, double splitPoint) throws Exception {

        Instances[] subsets = new Instances[2];
        subsets[0] = new Instances(data, 0);
        subsets[1] = new Instances(data, 0);

        // changed on 7 Feb 2013, because for some LR datasets, the Alpo returns NaN
        int halfPoint = (int) (data.numInstances() * 0.50);
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            if (inst.value(attIndex) <= splitPoint && subsets[0].numInstances() < halfPoint) {
                subsets[0].add(inst);
            } else {
                subsets[1].add(inst);
            }
        }

        if (subsets[1].numInstances() == 0) {
            subsets[1].add(subsets[0].instance(0));
        }

        if (subsets[0].numInstances() == 0) {
            subsets[0].add(subsets[1].instance(0));
        }
        return subsets;

        // following were used before 7 Feb 2013
//        for (int i = 0; i < data.numInstances(); i++) {
//            Instance inst = data.instance(i);
//            if (inst.value(attIndex) <= splitPoint) {
//                subsets[0].add(inst);
//            } else {
//                subsets[1].add(inst);
//            }
//        }
//
//        if (subsets[1].numInstances() == 0) {
//            subsets[1].add(subsets[0].instance(0));
//        }
//
//        if (subsets[0].numInstances() == 0) {
//            subsets[0].add(subsets[1].instance(0));
//        }
//        return subsets;
    }

    private double getMedian2(Instances data, int attIndex) throws Exception {
        double[] numArray = new double[data.numInstances()];
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            numArray[i] = inst.value(attIndex);
        }
        Arrays.sort(numArray);
        double median;
        if (numArray.length % 2 == 0) {
            median = ((double) numArray[numArray.length / 2] + (double) numArray[numArray.length / 2 + 1]) / 2;
        } else {
            median = (double) numArray[numArray.length / 2];
        }
        return median;
    }

    private double getMedian(Instances data, int attIndex) throws Exception {

        if (false) {
            return getMedian2(data, attIndex); // added 07-july 2013; actually they are the same
            // removed 17/07/2013 
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = (Instance) data.instance(i);
            stats.addValue(inst.value(attIndex));
        }
        double median = stats.getPercentile(50);
        return median;
    }

    private void makeTree(Instances data, java.util.Random r, int depth) throws Exception {
        if (m_K > data.numAttributes()) {
            m_K = data.numAttributes() - 1;
        }

        if (m_K < 1) {
            m_K = (int) weka.core.Utils.log2(data.numAttributes()) + 1;
        }

        int[] randAtts = new int[data.numAttributes() - 1];
        //TODO: handle class target att
        for (int i = 0; i < randAtts.length; i++) {
            randAtts[i] = i;
        }
        for (int i = 0; i < randAtts.length; i++) {
            int randomPosition = r.nextInt(randAtts.length);
            int temp = randAtts[i];
            randAtts[i] = randAtts[randomPosition];
            randAtts[randomPosition] = temp;
        }

        int bestAttIndex = -1;

        AttScorePair[] attScorePair = new AttScorePair[m_K];

        //double currentR2 = estimateAvgDistanceSpearman(data);
        for (int i = 0; i < m_K; i++) {
            int attIndex = randAtts[i];

            double splitPoint = Double.NaN;

            if (!m_UseMedian) {
                splitPoint = data.meanOrMode(attIndex);
            } else {
                splitPoint = getMedian(data, attIndex);
            }

            double r2 = estimateR2(data, attIndex, splitPoint);
            attScorePair[i] = new AttScorePair(attIndex, r2);
        }

        Arrays.sort(attScorePair);

        bestAttIndex = attScorePair[0].index;
        double maxR2 = attScorePair[0].score;
        boolean stop1 = false;

//        for (int kk = 0; kk < attScorePair.length; kk++) {
//            System.out.println(attScorePair[kk].score);
//        }
//        if (true) {
//            throw new Exception("stop");
//        }
        if (attScorePair[0].score <= attScorePair[m_K - 1].score) {
            stop1 = true;
        }

        if (data.numInstances() <= m_MiniLeaf
                || (depth >= m_MaxDepth && m_MaxDepth != 0)
                //|| maxR2 <= 0.01 // removed 10/01/2013
                || maxR2 >= 0.95
                || stop1 // 11/01/13 the paper version doesn't have this
                || data.variance(bestAttIndex) <= 0) {

            m_Attribute = null;
            m_Prototype = AbstractRanker.getAvgRanking(data);
            //m_Prototype = AbstractRanker.getCenterRanking(data, m_ApproxCenterMethod);
            return;
        }

        m_Attribute = data.attribute(bestAttIndex);
        if (!m_UseMedian) {
            m_SplitPoint = data.meanOrMode(bestAttIndex);
        } else {
            m_SplitPoint = getMedian(data, bestAttIndex);
        }
        Instances[] splitData = splitData(data, bestAttIndex, m_SplitPoint);

        m_Successors = new BinaryART[2];
        for (int j = 0; j < 2; j++) {
            m_Successors[j] = new BinaryART();
            m_Successors[j].setMiniLeaf(m_MiniLeaf);
            m_Successors[j].setK(m_K);
            m_Successors[j].setUseMedian(m_UseMedian);
            m_Successors[j].setNumObjects(m_NumObjects);

            m_Successors[j].makeTree(splitData[j], r, depth + 1);
        }
    }

    public void setMiniLeaf(int n) {
        if (n < 1) {
            m_MiniLeaf = 1;
        }
        m_MiniLeaf = n;
    }

    public int getMiniLeaf() {
        return m_MiniLeaf;
    }

    public void setK(int n) {
        m_K = n;
    }

    public void setRandomSeed(int s) {
        m_Seed = s;
    }

    @Override
    public double[] recommendRanking(Instance metaInst) throws Exception {
        if (m_Attribute == null) {
            return Tools.doubleArrayToRanking(m_Prototype);
        } else {
            if (metaInst.value(m_Attribute) <= m_SplitPoint) {
                return m_Successors[0].recommendRanking(metaInst);
            } else {
                return m_Successors[1].recommendRanking(metaInst);
            }
        }
    }

    private class AttScorePair implements Comparable {

        double score = Double.NaN;
        int index = -1;

        public AttScorePair(int i, double s) {
            score = s;
            index = i;
        }

        @Override
        public int compareTo(Object o) {
            AttScorePair other = (AttScorePair) o;
            if (other.score > score) {
                return 1;
            } else if (other.score == score) {
                return 0;
            } else {
                return -1;
            }
        }
    }
}
