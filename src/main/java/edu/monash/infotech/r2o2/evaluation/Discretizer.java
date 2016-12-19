package edu.monash.infotech.r2o2.evaluation;

import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Microsoft
 * Date: 13. 12. 30
 * Time: 오후 3:59
 * To change this template use File | Settings | File Templates.
 */
public class Discretizer {

    public static String [] DISC_LABEL = {"A", "B", "C", "D", "E", "F", "G", "H"};

    public static int discIndex(String label) {
        for (int i = 0; i < DISC_LABEL.length; i++) {
            if (label.equalsIgnoreCase(DISC_LABEL[i])) return i;
        }
        return -1;
    }

    public static String getDiscValue(double cls_time, int disc_num) {

        // if the time unit is sec, then we transform it to the ms.
        cls_time /= 1000d;
        String res = "";

        if (disc_num == 3) {
            if (cls_time < 10) {
                res = DISC_LABEL[0];
            } else if (cls_time >= 10 && cls_time < 100) {
                res = DISC_LABEL[1];
            } else if (cls_time >= 100) {
                res = DISC_LABEL[2];
            }
        } else if (disc_num == 4) {
            if (cls_time <= 1) {
                res = DISC_LABEL[0];
            } else if ((cls_time > 1) && (cls_time <= 10)) {
                res = DISC_LABEL[1];
            } else if ((cls_time > 10) && (cls_time <= 100)) {
                res = DISC_LABEL[2];
            } else if (cls_time > 100) {
                res = DISC_LABEL[3];
            }
        } else if (disc_num == 5) {
            if ((cls_time <= 0.01)) {
                res = DISC_LABEL[0];
            } else if ((cls_time > 0.01) && (cls_time <= 1)) {
                res = DISC_LABEL[1];
            } else if ((cls_time > 1) && (cls_time <= 10)) {
                res = DISC_LABEL[2];
            } else if ((cls_time > 10) && (cls_time <= 100)) {
                res = DISC_LABEL[3];
            } else if (cls_time > 100) {
                res = DISC_LABEL[4];
            }
        }
        return res;
    }

}
