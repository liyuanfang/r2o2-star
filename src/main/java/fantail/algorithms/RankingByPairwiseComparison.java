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

import java.util.ArrayList;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class RankingByPairwiseComparison extends AbstractRanker {

    private int m_NumLabels;
    private ArrayList<weka.classifiers.AbstractClassifier> m_Classifiers;
    private String m_BaseClassifierName;
    private ArrayList<String> m_AlgoPairs = new ArrayList<String>();

    private weka.filters.unsupervised.attribute.Add m_Add;

    public void setNumLabels(int numL) {
        m_NumLabels = numL;
    }

    @Override
    public String rankerName() {
        return "RPC (" + m_BaseClassifierName + ")";
    }

    private static boolean hasPair(ArrayList<String> algoPairs, String pairStr) {
        String[] parts1 = pairStr.split("\\|");
        for (int i = 0; i < algoPairs.size(); i++) {
            String p = algoPairs.get(i);
            String[] parts2 = p.split("\\|");
            if (parts1[0].equals(parts2[0]) && parts1[1].equals(parts2[1])) {
                return true;
            }
            if (parts1[0].equals(parts2[1]) && parts1[1].equals(parts2[0])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void buildRanker(Instances data) throws Exception {
        m_Classifiers = new ArrayList<weka.classifiers.AbstractClassifier>();
        m_AlgoPairs = new ArrayList<String>();
        m_NumLabels = Tools.getNumberTargets(data);

        // build pb datasets
        for (int a = 0; a < m_NumLabels; a++) {
            for (int b = 0; b < m_NumLabels; b++) {

                String pairStr = a + "|" + b;
                if (!hasPair(m_AlgoPairs, pairStr) && a != b) {
                    m_AlgoPairs.add(pairStr);

                    Instances d = new Instances(data);
                    d.setClassIndex(-1);
                    d.deleteAttributeAt(d.numAttributes() - 1);

                    weka.filters.unsupervised.attribute.Add add = new weka.filters.unsupervised.attribute.Add();
                    add.setInputFormat(d);
                    add.setOptions(weka.core.Utils.splitOptions("-T NOM -N class -L " + ((int) a) + "," + ((int) b) + " -C last"));

                    d = Filter.useFilter(d, add);
                    d.setClassIndex(d.numAttributes() - 1);

                    for (int i = 0; i < d.numInstances(); i++) {

                        Instance metaInst = (Instance) data.instance(i);
                        Instance inst = d.instance(i);

                        double[] rankVector = Tools.getTargetVector(metaInst);

                        double rank_a = rankVector[a];
                        double rank_b = rankVector[b];

                        if (rank_a < rank_b) {
                            inst.setClassValue(0.0);
                        } else {
                            inst.setClassValue(1.0);
                        }
                    }

                    //weka.classifiers.functions.SMO cls = new weka.classifiers.functions.SMO();
                    //String ops = "weka.classifiers.functions.SMO -C 1.0 -L 0.001 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.RBFKernel -C 250007 -G 0.01\"";
                    //cls.setOptions(weka.core.Utils.splitOptions(ops));                   
                    //cls.buildClassifier(d);
                    //weka.classifiers.functions.Logistic cls = new weka.classifiers.functions.Logistic();
                    //weka.classifiers.trees.J48 cls = new weka.classifiers.trees.J48();
                    //weka.classifiers.rules.ZeroR cls = new weka.classifiers.rules.ZeroR();
                    weka.classifiers.trees.DecisionStump cls = new weka.classifiers.trees.DecisionStump();
                    cls.buildClassifier(d);
                    m_Classifiers.add(cls);
                    m_BaseClassifierName = cls.getClass().getSimpleName();
                    m_Add = add;
                }
            }
        }
    }

    // Borda count
    @Override
    public double[] recommendRanking(Instance testInst) throws Exception {
        Instances tempData = new Instances(testInst.dataset(), 0);
        tempData.add((Instance) testInst.copy());
        // remove the relation att
        tempData.setClassIndex(-1);
        tempData.deleteAttributeAt(tempData.numAttributes() - 1);
        tempData = Filter.useFilter(tempData, m_Add);
        tempData.setClassIndex(tempData.numAttributes() - 1);
        double predRanking[] = new double[m_NumLabels];
        for (int i = 0; i < predRanking.length; i++) {
            predRanking[i] = m_NumLabels - 1;
        }
        for (int i = 0; i < m_Classifiers.size(); i++) {
            double predIndex = m_Classifiers.get(i).classifyInstance(tempData.instance(0));
            String algoPair = m_AlgoPairs.get(i);
            String[] parts = algoPair.split("\\|");
            int trueIndex = Integer.parseInt(parts[(int) predIndex]);
            predRanking[trueIndex] -= 1;
        }
        predRanking = Tools.doubleArrayToRanking(predRanking);      
        return predRanking;
    }

    // Soft ranking (using prob)
    //@Override
    public double[] recommendRanking2(Instance testInst) throws Exception {
        Instances tempData = new Instances(testInst.dataset(), 0);
        tempData.add((Instance) testInst.copy());
        // remove the relation att
        tempData.setClassIndex(- 1);
        tempData.deleteAttributeAt(tempData.numAttributes() - 1);
        tempData = Filter.useFilter(tempData, m_Add);
        tempData.setClassIndex(tempData.numAttributes() - 1);
        double predRanking[] = new double[m_NumLabels];
        for (int i = 0; i < m_Classifiers.size(); i++) {
            double predIndex = m_Classifiers.get(i).classifyInstance(tempData.instance(0));
            double predProb = m_Classifiers.get(i).distributionForInstance(tempData.instance(0))[0];
            String algoPair = m_AlgoPairs.get(i);
            String[] parts = algoPair.split("\\|");
            int trueIndex = Integer.parseInt(parts[(int) predIndex]);
            predRanking[trueIndex] -= predProb;
        }
        return Tools.doubleArrayToRanking(predRanking);
    }
}
