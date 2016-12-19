package edu.monash.infotech.r2o2.evaluation;

import java.util.LinkedHashMap;

public class RankingEvalResult {
    // AR: Actual reasoning time, DAR: discretized actual reasoning time.
    public LinkedHashMap<Integer, Double> precisionUsingAR = new LinkedHashMap<Integer, Double>();
    public LinkedHashMap<Integer, Double> precisionUsingDAR = new LinkedHashMap<Integer, Double>();
    public LinkedHashMap<Integer, Double> reasoningTimeSum = new LinkedHashMap<Integer, Double>();
    public LinkedHashMap<Integer, Double> reasoningTimeDiffFromGoldStd = new LinkedHashMap<Integer, Double>();

    // The first param: disc label, the second param: reasoning time
    public LinkedHashMap<Integer, double[]> precisionUsingARAccordingToDisc = new LinkedHashMap<Integer, double[]>();
    public LinkedHashMap<Integer, double[]> precisionUsingDARAccordingToDisc = new LinkedHashMap<Integer, double[]>();
    public LinkedHashMap<Integer, double[]> reasoningTimeSumAccordingToDisc = new LinkedHashMap<Integer, double[]>();
    public LinkedHashMap<Integer, double[]> reasoningTimeDiffFromGoldStdAccordingToDisc = new LinkedHashMap<Integer, double[]>();

    // Performance
    public ReasoningPerformance[][] performance;
}

