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

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class AverageRanking extends AbstractRanker {

    private double[] m_DefRanking;

    @Override
    public String rankerName() {
        return "AvgRanking";
    }

    @Override
    public void buildRanker(Instances data) throws Exception {
        Instances workingData = new Instances(data);
        int numLabels = Tools.getNumberTargets(workingData);
        m_DefRanking = new double[numLabels];
        for (int m = 0; m < workingData.numInstances(); m++) {
            Instance inst = workingData.instance(m);
            double[] targetValues = Tools.getTargetVector(inst);
            for (int j = 0; j < targetValues.length; j++) {
                m_DefRanking[j] += (targetValues[j]);
            }
        }
    }

    @Override
    public double[] recommendRanking(Instance metaInst) throws Exception {
        return Tools.doubleArrayToRanking(m_DefRanking);
    }
}
