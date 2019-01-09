package edu.monash.infotech.r2o2.evaluation;

import java.util.Arrays;

public class EvaluatedReasoner {

    public int n_instances;
    public int n_bins;

    public int model_index;
    public String model_name;

    public String winners[];
    public int winners_indexes [];

    // precision calculations
    public int precisions[];
    public int sum_precisions;
    public int precisions_per_bins[];

    // totol reasoning time of each model
    public double reasoning_times[];
    public double sum_reasoning_times;
    public double reasoning_times_per_bins[];

    // time difference between each reasoner and vb
    public double additive_times[];
    public double sum_additive_times;
    public double additive_times_per_bins[];

    public EvaluatedReasoner(int n_instances, int n_bins) {
        this.n_instances = n_instances;
        this.n_bins = n_bins;

        winners = new String[n_instances];
        winners_indexes = new int[n_instances];

        precisions = new int[n_instances];
        Arrays.fill(precisions, 0);
        sum_precisions = 0;
        precisions_per_bins = new int[n_bins];
        Arrays.fill(precisions_per_bins, 0);

        reasoning_times = new double[n_instances];
        Arrays.fill(reasoning_times, 0);
        sum_reasoning_times = 0;
        reasoning_times_per_bins = new double[n_bins];
        Arrays.fill(reasoning_times_per_bins, 0);

        additive_times = new double[n_instances];
        Arrays.fill(additive_times, 0);
        sum_additive_times = 0;
        additive_times_per_bins = new double[n_bins];
        Arrays.fill(additive_times_per_bins, 0);
    }

    @Override
    public String toString() {
        return "EvaluatedReasoner{" +
                "model_index=" + model_index +
                ", model_name='" + model_name + '\'' +
                //", winners=" + Arrays.toString(winners) +
                //", winners_indexes=" + Arrays.toString(winners_indexes) +
                //", precisions=" + Arrays.toString(precisions) +
                ", sum_precisions=" + sum_precisions +
                ", precisions_per_bins=" + Arrays.toString(precisions_per_bins) +
                //", additive_times=" + Arrays.toString(additive_times) +
                ", sum_additive_times=" + sum_additive_times +
                ", additive_times_per_bins=" + Arrays.toString(additive_times_per_bins) +
                '}';
    }
}
