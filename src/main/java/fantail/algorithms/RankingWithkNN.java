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

import fantail.algorithms.weka.IBkEnhanced;
import fantail.core.Tools;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class RankingWithkNN extends AbstractRanker {

    private IBkEnhanced m_kNN;
    private int m_K = 5;
    
    public void setK(int k) {
        m_K = k;
    }
    
    @Override
    public String rankerName() {
        return "kNN";
    }
    
    @Override
    public void buildRanker(Instances metaData) throws Exception {

        Instances workingData = new Instances(metaData);
        workingData.setClassIndex(workingData.numAttributes() - 1);
        m_kNN = new IBkEnhanced();
        // EuclideanDistance, ChebyshevDistance, ManhattanDistance
        String ops = "-W 0 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -R first-last\\\"\"";
        m_kNN.setOptions(weka.core.Utils.splitOptions(ops));
        m_kNN.setKNN(m_K);
        m_kNN.buildClassifier(workingData);
        workingData.setClassIndex(-1);
    }

    @Override
    public double[] recommendRanking(Instance inst) throws Exception {
        Instances nnbrs = m_kNN.getNearestNeighbourSearchAlgorithm().kNearestNeighbours(inst, m_K);

        double[] predictedRanks = new double[Tools.getNumberTargets(inst)];

        double sumWeights = 0;
        for (int k = 0; k < m_K; k++) {           
            Instance nn = (Instance)nnbrs.instance(k);
            sumWeights += nn.weight();
        }
        
        for (int k = 0; k < m_K; k++) {           
            Instance nn = (Instance)nnbrs.instance(k);
            double[] rankingNN = Tools.getTargetVector(nn);
            for (int j = 0; j < predictedRanks.length; j++) {
                predictedRanks[j] += (rankingNN[j] * nn.weight() / sumWeights);
            }
        }

        return Tools.doubleArrayToRanking(predictedRanks);
    }
}

