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
import jsc.util.Rank;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;

/**
 *
 * @author Quan Sun quan.sun.nz@gmail.com
 */
public class Tools {

    public static boolean isClassAttributeRelational(Instances data) throws Exception {
        if (data == null) {
            throw new Exception("data can't be null.");
        }
        return data.classAttribute().isRelationValued();
    }

    public static int getNumberTargets(Instances data) throws Exception {
        if (data == null) {
            throw new Exception("data can't be null.");
        }
        if (data.numInstances() <= 0) {
            throw new Exception("data can't be empty.");
        }
        if (data.classIndex() < 0) {
            throw new Exception("class index is not set.");
        }
        Instance tempInst = data.instance(0);
        Instances targets = tempInst.relationalValue(data.classIndex());
        return targets.numAttributes();
    }

    public static int getNumberTargets(Instance inst) throws Exception {
        if (inst == null) {
            throw new Exception("inst can't be null.");
        }
        Instances targets = inst.relationalValue(inst.classIndex());
        return targets.numAttributes();
    }

    public static double[] getTargetVector(Instance inst) {
        Instances targetBag = inst.relationalValue(inst.classIndex());
        double[] values = new double[targetBag.numAttributes()];
        for (int i = 0; i < values.length; i++) {
            values[i] = targetBag.instance(0).value(i);
        }
        return values;
    }
    
    public static String[] getTargetNames(Instance inst) {
        Instances targetBag = inst.relationalValue(inst.classIndex());
        String[] names = new String[targetBag.numAttributes()];
        for (int i = 0; i < names.length; i++) {
            names[i] = targetBag.attribute(i).name();
        }
        return names;
    }

    public static ReasonerComponent[] getTargetObjects(Instance inst) {

        Instances targetBag = inst.relationalValue(inst.classIndex());
        String[] names = new String[targetBag.numAttributes()];
        ReasonerComponent res[] = new ReasonerComponent[names.length];

        for (int i = 0; i < names.length; i++) {
            ReasonerComponent cl = new ReasonerComponent(i, targetBag.instance(0).value(i));
            res[i] = cl;
        }
        return res;
    }

    public static double[] doubleArrayToRanking(double[] values) {
        Rank rank = new Rank(values, 0.001);
        double ranks[] = rank.getRanks();
        //for (int i = 0; i < ranks.length; i++) {
            //ranks[i] += 1;
        //}
        return ranks;
    }

    public static Instances loadFantailARFFInstances(String arffPath) throws Exception {
        ArffLoader loader = new ArffLoader();
        loader.setSource(new File(arffPath));
        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);
        if (data.classAttribute().isRelationValued() != true) {
            throw new Exception("The last attribute needs to be 'RelationValued'");
        }
        return data;
    }
}
