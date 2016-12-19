package edu.monash.infotech.r2o2.evaluation;

/**
 * Created by ybkan on 24/02/2016.
 */
public class ReasoningPerformance {
    private double reasoningTime;
    private String discLabel;

    ReasoningPerformance (double reasoningTime, String discLabel) {
        this.reasoningTime = reasoningTime;
        this.discLabel = discLabel;
    }

    public double getReasoningTime() {
        return reasoningTime;
    }

    public void setReasoningTime(double reasoningTime) {
        this.reasoningTime = reasoningTime;
    }

    public String getDiscLabel() {
        return discLabel;
    }

    public void setDiscLabel(String discLabel) {
        this.discLabel = discLabel;
    }
}
