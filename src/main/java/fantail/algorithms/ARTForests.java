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
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;

import java.util.Random;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class ARTForests extends AbstractRanker {

    private int m_T = 10;
    private Ranker[] m_WeakRankers;
    private int m_RandomSeed = 1;
    private int m_NumMinInstances = 15;
    private boolean m_UseMedian = false;
    private double m_BaggingPercentage = 100.0;
    private String m_ApproxAvgDisMethod = "S"; // C: center ranking; S: Spearman close form
    private boolean m_IsProperDistance = true;
    private int m_K = 0;

    @Override
    public String rankerName() {
        return this.getClass().getSimpleName();
    }
    
    public void setK(int k) {
        m_K = k;
    }

    public void setIsProperDistance(boolean m) {
        m_IsProperDistance = m;
    }

    public void setApproxAvgDisMethod(String model) {
        m_ApproxAvgDisMethod = model;
    }

    public void setBaggingPercentage(double p) {
        if (m_BaggingPercentage > 100 || m_BaggingPercentage < 5) {
            m_BaggingPercentage = 100;
        } else {
            m_BaggingPercentage = p;
        }
    }

    public void setUseMedian(boolean m) {
        m_UseMedian = m;
    }

    public void setNumMinInstances(int m) {
        m_NumMinInstances = m;
    }

    @Override
    public void buildRanker(Instances metaData) throws Exception {

        Random r = new Random(m_RandomSeed);
        Instances workingData = new Instances(metaData);
        m_WeakRankers = new Ranker[m_T];

        for (int i = 0; i < m_T; i++) {
            Instances baggingSample = workingData.resampleWithWeights(r);

            if (m_BaggingPercentage < 100.0) {
                weka.filters.unsupervised.instance.Resample res = new weka.filters.unsupervised.instance.Resample();
                res.setSampleSizePercent(m_BaggingPercentage);
                res.setNoReplacement(false);
                res.setInputFormat(baggingSample);
                baggingSample = Filter.useFilter(baggingSample, res);
            }

            BinaryART ranker = new BinaryART();           
            ranker.setMiniLeaf(m_NumMinInstances);
            ranker.setK(m_K);
            ranker.setRandomSeed(i);
            ranker.setUseMedian(m_UseMedian);

            m_WeakRankers[i] = ranker;
            m_WeakRankers[i].buildRanker(baggingSample);
        }
    }

    public void setNumIterations(int t) {
        m_T = t;
    }

    @Override
    public double[] recommendRanking(Instance metaInst) throws Exception {
        double[] pred = m_WeakRankers[0].recommendRanking(metaInst);
        if (m_T > 1) {
            for (int i = 1; i < m_T; i++) {
                double[] pred_i = m_WeakRankers[i].recommendRanking(metaInst);
                for (int j = 0; j < pred.length; j++) {
                    pred[j] += pred_i[j];
                }
            }
        }
        for (int j = 0; j < pred.length; j++) {
            pred[j] /= (1.0 * m_T);
        }
        return Tools.doubleArrayToRanking(pred);
    }
}
