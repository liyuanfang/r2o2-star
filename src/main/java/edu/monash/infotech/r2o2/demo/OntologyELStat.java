package edu.monash.infotech.r2o2.demo;

public class OntologyELStat {

    String name;
    boolean isEL;
    double reasoning_time;
    String metrics_str;

    public String get_name() {
        return name;
    }

    public void set_name(String name) {
        this.name = name;
    }

    public boolean isEL() {
        return isEL;
    }

    public void setEL(boolean EL) {
        isEL = EL;
    }

    public double get_reasoning_time() {
        return reasoning_time;
    }

    public void set_reasoning_time(double reasoning_time) {
        this.reasoning_time = reasoning_time;
    }

    public String get_metrics_str() {
        return metrics_str;
    }

    public void set_metrics_str(String metrics_str) {
        this.metrics_str = metrics_str;
    }

    @Override
    public String toString() {
        return name + "," + isEL + "," + reasoning_time + "," + metrics_str;
    }
}
