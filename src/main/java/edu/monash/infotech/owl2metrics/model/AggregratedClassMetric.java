package edu.monash.infotech.owl2metrics.model;

import java.text.DecimalFormat;

/**
 * @author Yuan-Fang Li
 * @version $Id: AggregratedClassMetric.java 64 2012-10-09 02:55:17Z yli $
 */
public class AggregratedClassMetric {
    private int max;
    private int total;
    private double avg;


    public AggregratedClassMetric(int max, int total, double avg) {
        this.max = max;
        this.total = total;
        this.avg = avg;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public double getAvg() {
		return avg;
	}

	public void setAvg(double avg) {
		this.avg = avg;
	}

	@Override
    public String toString() {
		DecimalFormat df = new DecimalFormat("0.00");
        return "Avg = " + df.format(avg) + ", Max = " + max + ". Total = " + total;
    }
}
