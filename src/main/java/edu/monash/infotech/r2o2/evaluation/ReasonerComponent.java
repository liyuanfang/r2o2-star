package edu.monash.infotech.r2o2.evaluation;

import java.text.DecimalFormat;

public class ReasonerComponent implements Comparable {
    private int index;
    private String label;
    private double rank;
    private double avgRank;
    private double coeffDetermination;

    public ReasonerComponent() {
    }

    public ReasonerComponent(int index, double rank) {
        this.index = index;
        this.rank = rank;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public double getAvgRank() {
        return avgRank;
    }

    public void setAvgRank(double avgRank) {
        this.avgRank = avgRank;
    }

    public double getCoeffDetermination() {
        return coeffDetermination;
    }

    public void setCoeffDetermination(double coeffDetermination) {
        this.coeffDetermination = coeffDetermination;
    }

    @Override
    public int compareTo(Object o) {

        ReasonerComponent other = (ReasonerComponent)o;
        if (this.rank > other.rank) {
            return -1;
        } else if (this.rank < other.rank){
            return 1;
        } else {
            return 0;
        }
    }

    public String toString() {
        DecimalFormat df = new DecimalFormat("0.00");
        return "{" + this.index + "[" + this.label + "]:rank=" + df.format(this.rank) +
                ",avgRank=" + df.format(this.avgRank) + ",conf=" + df.format(this.coeffDetermination) + "}";
    }
}
