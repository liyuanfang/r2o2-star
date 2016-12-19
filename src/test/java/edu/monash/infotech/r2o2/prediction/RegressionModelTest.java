package edu.monash.infotech.r2o2.prediction;

import org.junit.Test;

/**
 * Created by ybkan on 15/02/2016.
 */
public class RegressionModelTest {

    @Test
    public void testEvaluate() throws Exception {
        String dir = System.getProperty("user.dir");
        String arff_fname = dir + "/data/test.arff";

        int cvFold = 10;
        RegressionModel rm = new RegressionModel();
        rm.evaluate(arff_fname, cvFold);
    }
}