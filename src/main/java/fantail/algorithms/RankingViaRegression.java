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
import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class RankingViaRegression extends AbstractRanker {

    private weka.classifiers.Classifier[] m_Classifiers;
    //private int m_LastFeatureIndex = -1;
    private int m_NumTargets = -1;
    private int m_NumFeatures = -1;
    private Instances m_TempHeader = null;

    @Override
    public String rankerName() {
        return this.getClass().getSimpleName();
    }
    
    @Override
    public void buildRanker(Instances data) throws Exception {

        Instances workingData = new Instances(data);
        //Instance instTemp = workingData.instance(0);

        //m_LastFeatureIndex = workingData.numAttributes() - 1;
        m_NumFeatures = workingData.numAttributes() - 1;
        m_NumTargets = Tools.getNumberTargets(data);
        m_Classifiers = new AbstractClassifier[m_NumTargets];

        for (int i = 0; i < m_NumTargets; i++) {         
            weka.classifiers.functions.LinearRegression lr = new weka.classifiers.functions.LinearRegression();           
            m_Classifiers[i] = AbstractClassifier.makeCopy(lr);
        }

        Instances[] trainingSets = new Instances[m_NumTargets];

        for (int t = 0; t < m_NumTargets; t++) {

            ArrayList attributes = new ArrayList();
            for (int i = 0; i < m_NumFeatures; i++) {
                attributes.add(new Attribute(workingData.attribute(i).name()));
            }

            String targetName = "att-" + (t + 1);
            attributes.add(new Attribute(targetName));

            trainingSets[t] = new Instances("data-" + targetName, attributes, 0);
            
            for (int j = 0; j < workingData.numInstances(); j++) {
                Instance metaInst = workingData.instance(j);
                double[] ranking = Tools.getTargetVector(metaInst);
                double[] values = new double[trainingSets[t].numAttributes()];

                for (int m = 0; m < (trainingSets[t].numAttributes() - 1); m++) {
                    values[m] = metaInst.value(m);
                }
                values[values.length - 1] = ranking[t];
                trainingSets[t].add(new DenseInstance(1.0, values));
            }

            trainingSets[t].setClassIndex(trainingSets[t].numAttributes() - 1);
            m_Classifiers[t].buildClassifier(trainingSets[t]);
        }

        m_TempHeader = new Instances(trainingSets[0], 0);
    }

    @Override
    public double[] recommendRanking(Instance metaInst) throws Exception {

        double[] values = new double[m_NumFeatures + 1];
        for (int i = 0; i < values.length - 1; i++) {
            values[i] = metaInst.value(i);
        }
        values[values.length - 1] = 0;

        Instance inst = new DenseInstance(1.0, values);
        inst.setDataset(m_TempHeader);

        //
        double[] preds = new double[m_NumTargets];

        for (int t = 0; t < m_NumTargets; t++) {

            double pred = m_Classifiers[t].classifyInstance(inst);
            if (pred <= 0) {
                pred = 0;
            }
            if (pred >= m_NumTargets) {
                pred = m_NumTargets;
            }
            preds[t] = pred;            
        }
        
        return Tools.doubleArrayToRanking(preds);
    }
}