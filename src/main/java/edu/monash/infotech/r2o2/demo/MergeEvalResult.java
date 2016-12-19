package edu.monash.infotech.r2o2.demo;

import com.csvreader.CsvReader;
import edu.monash.infotech.r2o2.evaluation.Discretizer;
import fantail.core.Tools;
import weka.core.Instances;

import java.io.*;
import java.text.DecimalFormat;

/**
 * Created by ybkan on 8/04/2016.
 */
public class MergeEvalResult {

    public void mergeResultFiles(String currentDir, String evalResultFileName, int foldNum, int reasonerComponentNum) throws Exception {

        String outFilename = currentDir + "/" + evalResultFileName + ".merged.precision.csv";
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFilename));

        boolean isFirstFile = false;
        File directory = new File(currentDir);
        File files[] = directory.listFiles();
        for (File file: files) {

            // each subdirectory has a cross-validation result (e.g. cv fold number)
            if (!file.isDirectory()) continue;

            String evalResultFilePath = file.getAbsolutePath() + "/" + evalResultFileName;

            BufferedReader reader = new BufferedReader(new FileReader(evalResultFilePath));
            String line = "";
            int N = 0;

            int reasonerNum = reasonerComponentNum+3;
            double precision[] = new double[reasonerNum];
            double rt[] = new double[reasonerNum];
            double rtDiff[] = new double[reasonerNum];

            // read the evaluation output file for each fold
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Fold")) continue;
                String em[] = line.split(",");

                // write header info
                if (em[0].trim().length() == 0) {
                    if (!isFirstFile) writer.write(line + "\n");
                } else {
                    for (int i = 1; i < em.length; i++) {
                        if (i <= reasonerNum) {
                            precision[i-1] += Double.parseDouble(em[i]);
                        } else if (i > reasonerNum+1 && i <= reasonerNum*2+1) {
                            rt[i - reasonerNum-2] += Double.parseDouble(em[i]);
                        } else if (i > reasonerNum*2+2 && i <= reasonerNum*2+12) {
                            rtDiff[i - reasonerNum*2-3] += Double.parseDouble(em[i]);
                        }
                    }
                    N++;
                }
                isFirstFile = true;
            }

            // read the total number of test cases for each fold - each fold also has 10 cv
            int totalTestNum = 0;
            for (int i = 1; i <= 10; i++) {
                Instances testInstances = Tools.loadFantailARFFInstances(file.getAbsoluteFile() + "/" + i + "/test_mm/meta-rank-actual.arff");
                totalTestNum += testInstances.numInstances();
            }

            for (int i = 0; i < reasonerNum; i++) {
                precision[i] = precision[i] / (totalTestNum); // 1000 : to convert the milliseconds to seconds
                rt[i] = rt[i] / (totalTestNum*1000);
                rtDiff[i] = rtDiff[i] / (totalTestNum*1000);
            }
            reader.close();

            writer.write(",");
            for (int i = 0; i < reasonerNum; i++) {
                writer.write(precision[i] + ",");
            }
            writer.write(",");
            for (int i = 0; i < reasonerNum; i++) {
                writer.write(rt[i] + ",");
            }
            writer.write(",");
            for (int i = 0; i < reasonerNum; i++) {
                if (i < reasonerNum-1)
                    writer.write(rtDiff[i] + ",");
                else
                    writer.write(rtDiff[i] + "\n");
            }
        }
        writer.close();
    }

    public void mergeDetailedResultFiles(String currentDir, String evalResultFileName, int rawReasonerNum) throws Exception {

        int binNum = 4;
        String outFilename = currentDir + "/" + evalResultFileName + ".merged.csv";
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFilename));

        File directory = new File(currentDir);
        File files[] = directory.listFiles();

        String headers[] = null;
        double reasonerStatisticsSum[][] = new double[binNum][rawReasonerNum+3]; // 4: the number of discretized bins
        int allReasonerNum = 0;

        for (File file: files) {

            // each subdirectory has a cross-validation result (e.g. cv fold number)
            if (!file.isDirectory()) continue;

            String evalResultFilePath = file.getAbsolutePath() + "/" + evalResultFileName;

            CsvReader reader = new CsvReader(evalResultFilePath);
            reader.readHeaders();
            headers = reader.getHeaders();

            allReasonerNum = headers.length-1; // including portR, metaR, metaHR, actualR

            /*
            Disc Label	fact	hermit	jfact	konclude	more	pellet	trowl	portR	metaR	metaHR
             */
            double reasonerStatistics[][] = new double[binNum][rawReasonerNum+3]; // 4: the number of discretized bins
            int totalTestInstanceNum[] = new int[binNum]; // keep the number of test instances in each bin
            while (reader.readRecord()) {
                int binIndex = Discretizer.discIndex(reader.get(0));

                // set reasoning times of all reasoners
                double reasoners[] = new double[allReasonerNum];
                for (int i = 0; i < allReasonerNum; i++) {
                    reasoners[i] = Double.parseDouble(reader.get(headers[i+1]));
                }

                // compare the best reasoning time with reasoners, and count the best reasoner in each bin
                for (int i = 0; i < allReasonerNum-1; i++) {
                    if (reasoners[allReasonerNum-1] == reasoners[i]) {
                        reasonerStatistics[binIndex][i]++;
                    }
                }
                totalTestInstanceNum[binIndex] ++;
            }

            for (int i = 0; i < allReasonerNum-1; i++) {
                for (int j = 0; j < binNum; j++) {
                    reasonerStatisticsSum[j][i] += reasonerStatistics[j][i]/totalTestInstanceNum[j];
                }
            }
        }

        // write headers.
        DecimalFormat df = new DecimalFormat("0.00");
        writer.write("Reasoner,A,B,C,D, AVG\n");
        for (int i = 0; i < allReasonerNum-1; i++) {
            writer.write(headers[i+1] + ",");
            double sum = 0;
            for (int j = 0; j < binNum; j++) {
                double value = reasonerStatisticsSum[j][i]/10*100;
                sum += value;
                writer.write(df.format(value) + "%,");
            }
            writer.write(df.format(sum/binNum) + "%\n");
        }
        writer.close();
    }

    public static void main (String args[]) throws Exception {
        String dir = "H:\\meta_reasoner";

        String rootDir = dir + "/2015_ore14_subset12_10cv_without_trowl_without_timeout_test/";
        String resultFileName = "r2o2_eval2_app_without_trowl_without_time_test.csv";

        int reasonerNum = 6;
        int foldNum = 10;
        MergeEvalResult mergeEvalResult = new MergeEvalResult();
        mergeEvalResult.mergeResultFiles(rootDir, resultFileName, foldNum, reasonerNum);

        // generate more detailed reasoning time comparison according to discretized bins.
        mergeEvalResult.mergeDetailedResultFiles(rootDir, resultFileName + ".rp.csv", reasonerNum);

    }
}
