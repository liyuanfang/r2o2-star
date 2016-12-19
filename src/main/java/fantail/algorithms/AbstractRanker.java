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

import java.io.Serializable;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public abstract class AbstractRanker implements Ranker, Serializable {
    protected boolean m_Debug = true;
    
    public void setDebug(boolean d) {
        m_Debug = d;
    }
    @Override
    abstract public String rankerName();
    
    //
    public static double[] getAvgRankValues(Instances data) throws Exception {

        if (data.numInstances() == 0) {
            throw new Exception("data can't be empty.");
        }
        int numLabels = Tools.getNumberTargets(data);
        double[] avgVals = new double[numLabels];
        for (int m = 0; m < data.numInstances(); m++) {
            Instance inst = data.instance(m);
            double[] targetValues = Tools.getTargetVector(inst);

            for (int j = 0; j < targetValues.length; j++) {
                avgVals[j] += (targetValues[j] * inst.weight());
            }
        }
        for (int i = 0; i < avgVals.length; i++) {
            avgVals[i] /= data.numInstances();
        }
        return avgVals;
    }
    
    public static double[] getAvgRanking(Instances data) throws Exception {
        double[] avgVals = getAvgRankValues(data);
        return Tools.doubleArrayToRanking(avgVals);
    }
}
