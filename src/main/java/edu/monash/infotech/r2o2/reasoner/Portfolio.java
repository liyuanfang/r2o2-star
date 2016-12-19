package edu.monash.infotech.r2o2.reasoner;

import edu.monash.infotech.r2o2.evaluation.ReasonerComponent;
import fantail.core.Tools;
import weka.core.Instances;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Yong-Bin on 3/04/2016.
 */
public class Portfolio {

    public void measurePrecision(String predFile, String actualFile, String outFile) throws Exception {

        Instances predData = Tools.loadFantailARFFInstances(predFile);
        Instances actualData = Tools.loadFantailARFFInstances(actualFile);

        int localScorePrecision = 0;
        for (int m = 0; m < actualData.numInstances(); m++) {
            double[] pred = Tools.getTargetVector(predData.instance(m));
            double[] actual = Tools.getTargetVector(actualData.instance(m));

            List<ReasonerComponent> pred_list = sortRanking(pred);
            List<ReasonerComponent> actual_list = sortRanking(actual);
            double precision = computePrecision(pred_list, actual_list);
            localScorePrecision += precision;
        }

        // generate the ranker summary file
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outFile)));
        writer.write("pr," + roundDouble((double)localScorePrecision/actualData.numInstances(), 3));
        writer.close();
    }

    private double roundDouble(double n, int dPlaces) {
        return weka.core.Utils.roundDouble(n, dPlaces);
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
