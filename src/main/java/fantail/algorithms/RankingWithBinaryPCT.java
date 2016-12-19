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
import java.util.Random;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class RankingWithBinaryPCT extends AbstractRanker {

    private RankingWithBinaryPCT[] m_Successors;
    private Attribute m_Attribute;
    private double[] m_Prototype;
    private double m_SplitPoint;
    private int m_MiniLeaf = 2;
    private double m_MinVariancea = 0.001;
    private int m_K = 0;
    private int m_Seed = 1;
    private boolean m_UseMedian = true;
    private int m_MaxDepth = 0;
    private int m_NumTargetLabels;

    public void setUseMedian(boolean m) {
        m_UseMedian = m;
    }

    @Override
    public String rankerName() {
        return "BinaryPCT";
    }

    public void setNumTargetLabels(int t) {
        m_NumTargetLabels = t;
    }

    @Override
    public void buildRanker(Instances data) throws Exception {

        setNumTargetLabels(Tools.getNumberTargets(data));
        Random rnd = new Random(m_Seed);
        Instances workingData = new Instances(data);

        int depth = 1;
        makeTree(workingData, rnd, depth);
    }

    private double computeVariance(Instances data) throws Exception {
        double[][] targets = new double[data.numInstances()][];
        for (int i = 0; i < data.numInstances(); i++) {
            targets[i] = Tools.getTargetVector(data.instance(i));
        }
        double sumVar = 0;
        for (int i = 0; i < m_NumTargetLabels; i++) {
            double[] target_i = new double[data.numInstances()];

            for (int j = 0; j < data.numInstances(); j++) {
                Instance metaInst = (Instance) data.instance(j);
                target_i[j] = targets[j][i] * metaInst.weight();
            }
            sumVar += weka.core.Utils.variance(target_i);
        }
        return sumVar / m_NumTargetLabels;
    }

    private double computeVarianceReduction(Instances data, int attIndex, double splitPoint) throws Exception {
        //double varianceaE = computeVariance(data); // doesn't make sense to compute this
        Instances[] P = splitData(data, attIndex, splitPoint);
        double variancePk = 0;
        for (int k = 0; k < P.length; k++) {
            variancePk += (1.0 * P[k].numInstances() / data.numInstances() * computeVariance(P[k]));
        }
        return -variancePk;
    }

    private Instances[] splitData(Instances data, int attIndex, double splitPoint) throws Exception {
        Instances[] subsets = new Instances[2];
        subsets[0] = new Instances(data, 0);
        subsets[1] = new Instances(data, 0);
        
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            if (inst.value(attIndex) <= splitPoint && (subsets[0].numInstances() <= 0.5 * data.numInstances())) {
                subsets[0].add(inst);
            } else {
                subsets[1].add(inst);
            }
        }
        // TODO: 
        if (subsets[1].numInstances() == 0) {
            subsets[1].add(subsets[0].instance(0));
        }
        if (subsets[0].numInstances() == 0) {
            subsets[0].add(subsets[1].instance(0));
        }
        return subsets;
    }
    
    private Instances[] splitData2(Instances data, int attIndex, double splitPoint) throws Exception {
        Instances[] subsets = new Instances[2];
        subsets[0] = new Instances(data, 0);
        subsets[1] = new Instances(data, 0);
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            if (inst.value(attIndex) <= splitPoint) {
                subsets[0].add(inst);
            } else {
                subsets[1].add(inst);
            }
        }
        // TODO: 
        if (subsets[1].numInstances() == 0) {
            subsets[1].add(subsets[0].instance(0));
        }
        if (subsets[0].numInstances() == 0) {
            subsets[0].add(subsets[1].instance(0));
        }
        return subsets;
    }

    private double getMedian(Instances data, int attIndex) throws Exception {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = (Instance) data.instance(i);
            stats.addValue(inst.value(attIndex));
        }
        double median = stats.getPercentile(50);
        return median;
    }

    private int getRandomPosition(Random r, int[] attIndice) {
        int randP = -1;
        int index = -1;
        while (randP == -1) {
            index = r.nextInt(attIndice.length);
            randP = attIndice[index];

            if (randP != -1) {
                return index;
            }
        }
        return index;
    }

    private void makeTree(Instances data, Random r, int depth)
            throws Exception {

        if (data.numInstances() <= m_MiniLeaf
                || (depth >= m_MaxDepth && m_MaxDepth != 0)
                || computeVariance(data) <= m_MinVariancea) {
            //|| maxVarianceaReduction <= 0
            //|| data.variance(bestAttIndex) <= 0) { // || data.variance(bestAttIndex) <= 0 ) {   copied from ART, 

            m_Attribute = null;
            m_Prototype = AbstractRanker.getAvgRanking(data);
            return;
        }

        //
        if (m_K > data.numAttributes()) {
            m_K = data.numAttributes();
        }
        if (m_K < 1) {
            m_K = (int) weka.core.Utils.log2(data.numAttributes()) + 1;
        }

        // TODO:
        int[] attIndice = new int[data.numAttributes() - 1];
        for (int i = 0; i < attIndice.length; i++) {
            attIndice[i] = i;
        }
        for (int i = 0; i < attIndice.length; i++) {
            //int randomPosition = getRandomPosition(r, attIndice);
            int randomPosition = r.nextInt(attIndice.length);
            int temp = attIndice[i];
            attIndice[i] = attIndice[randomPosition];
            attIndice[randomPosition] = temp;
        }

        AttScorePair[] attScorePair
                = new AttScorePair[m_K];

        for (int i = 0; i < m_K; i++) {
            int attIndex = attIndice[i];

            double splitPoint = Double.NaN;
            if (!m_UseMedian) {
                splitPoint = data.meanOrMode(attIndex);
            } else {
                splitPoint = getMedian(data, attIndex);
            }
            double varianceReduction = computeVarianceReduction(data, attIndex, splitPoint);
            attScorePair[i] = new AttScorePair(attIndex, varianceReduction);

        }

        Arrays.sort(attScorePair);
        int randAttIndex = 0;
        int bestAttIndex = attScorePair[randAttIndex].index;

        double maxVarianceaReduction = attScorePair[randAttIndex].score;

//        if (data.numInstances() <= 1 * m_MiniLeaf
//                || (depth >= m_MaxDepth && m_MaxDepth != 0)
//                || computeVariance(data) <= m_MinVariancea) {
//                //|| maxVarianceaReduction <= 0
//                //|| data.variance(bestAttIndex) <= 0) { // || data.variance(bestAttIndex) <= 0 ) {   copied from ART, 
//
//            m_Attribute = null;
//            m_Prototype = AbstractRanker.getAvgRanking(data);
//            return;
//        }
        m_Attribute = data.attribute(bestAttIndex);

        if (!m_UseMedian) {
            m_SplitPoint = data.meanOrMode(bestAttIndex);
        } else {
            m_SplitPoint = getMedian(data, bestAttIndex);
        }

        //m_SplitPoint = data.meanOrMode(m_Attribute);
        Instances[] splitData = splitData(data, bestAttIndex, m_SplitPoint);
        
        //System.out.println(splitData[0].numInstances());
        //System.out.println(splitData[1].numInstances());
        //System.out.println();

        m_Successors = new RankingWithBinaryPCT[2];

        for (int j = 0; j < 2; j++) {
            m_Successors[j] = new RankingWithBinaryPCT();
            m_Successors[j].setMiniLeaf(m_MiniLeaf);
            m_Successors[j].setK(m_K);
            m_Successors[j].setUseMedian(m_UseMedian);
            m_Successors[j].setNumTargetLabels(m_NumTargetLabels);
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

    public int getK() {
        return m_K;
    }

    public void setRandomSeed(int s) {
        m_Seed = s;
    }

    @Override
    public double[] recommendRanking(Instance metaInst) throws Exception {
        if (m_Attribute == null) {
            return m_Prototype;
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
