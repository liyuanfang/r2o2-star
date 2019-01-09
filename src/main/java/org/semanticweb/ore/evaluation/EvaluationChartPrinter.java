package org.semanticweb.ore.evaluation;


public interface EvaluationChartPrinter {
	
	public boolean printBarChart(String outputString, EvaluationChartPrintingData chartData);

	public boolean printCactusChart(String outputString, EvaluationChartPrintingData chartData);

	public boolean printLogarithmicCactusChart(String outputString, EvaluationChartPrintingData chartData);
}
