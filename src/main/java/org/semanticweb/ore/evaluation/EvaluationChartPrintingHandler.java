package org.semanticweb.ore.evaluation;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.semanticweb.ore.competition.Competition;
import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.competition.CompetitionEvaluationType;
import org.semanticweb.ore.competition.CompetitionStatusUpdater;
import org.semanticweb.ore.configuration.Config;

import java.util.ArrayList;


public class EvaluationChartPrintingHandler {
	
	protected Config mConfig = null;
	protected CompetitionStatusUpdater mStatusUpdater = null;
	protected String mBaseOutputString = null;
	
	protected EvaluationJChartPrinter mJChartPrinter = null;
	protected EvaluationHighchartPrinter mHighchartPrinter = null;
	
	public EvaluationChartPrintingHandler(Config config, String baseOutputString, CompetitionStatusUpdater statusUpdater) {		
		mConfig = config;
		mStatusUpdater = statusUpdater;
		mBaseOutputString = baseOutputString;
		
		mJChartPrinter = new EvaluationJChartPrinter(mConfig);
		mHighchartPrinter = new EvaluationHighchartPrinter(mConfig);
	}
	
	public boolean printBarChart(Competition competition, String evaluationNameString, EvaluationChartPrintingData chartData) {
		try {
			CompetitionEvaluationStatus jChartEvalStat = new CompetitionEvaluationStatus(competition,evaluationNameString+"-JChart",mBaseOutputString+evaluationNameString+"-JChart.png",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_PNG,new DateTime(DateTimeZone.UTC));
			if (mJChartPrinter.printBarChart(mBaseOutputString+evaluationNameString+"-JChart.png", chartData)) {
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(jChartEvalStat);
				}
			}
		} catch (Exception e) {			
		}
		try {
			CompetitionEvaluationStatus highChartEvalStat = new CompetitionEvaluationStatus(competition,evaluationNameString+"-Highchart",mBaseOutputString+evaluationNameString+"-Highchart.html",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_HTML,new DateTime(DateTimeZone.UTC));
			if (mHighchartPrinter.printBarChart(mBaseOutputString+evaluationNameString+"-Highchart.html", chartData)) {
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(highChartEvalStat);
				}
			}
		} catch (Exception e) {			
		}
		return false;
	}
	
	
	

	public boolean printCactusChart(Competition competition, String evaluationNameString, EvaluationChartPrintingData chartData) {
		try {
			CompetitionEvaluationStatus jChartEvalStat = new CompetitionEvaluationStatus(competition,evaluationNameString+"-JChart",mBaseOutputString+evaluationNameString+"-JChart.png",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_PNG,new DateTime(DateTimeZone.UTC));
			if (mJChartPrinter.printCactusChart(mBaseOutputString+evaluationNameString+"-JChart.png", chartData)) {
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(jChartEvalStat);
				}
			}
		} catch (Exception e) {			
		}
		try {
			CompetitionEvaluationStatus highChartEvalStat = new CompetitionEvaluationStatus(competition,evaluationNameString+"-Highchart",mBaseOutputString+evaluationNameString+"-Highchart.html",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_HTML,new DateTime(DateTimeZone.UTC));
			if (mHighchartPrinter.printCactusChart(mBaseOutputString+evaluationNameString+"-Highchart.html", chartData)) {
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(highChartEvalStat);
				}
			}
		} catch (Exception e) {			
		}
		return false;
	}
	
	
	


	public boolean printLogarithmicCactusChart(Competition competition, String evaluationNameString, EvaluationChartPrintingData chartData) {
		try {
			CompetitionEvaluationStatus jChartEvalStat = new CompetitionEvaluationStatus(competition,evaluationNameString+"-JChart",mBaseOutputString+evaluationNameString+"-JChart.png",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_PNG,new DateTime(DateTimeZone.UTC));
			if (mJChartPrinter.printLogarithmicCactusChart(mBaseOutputString+evaluationNameString+"-JChart.png", chartData)) {
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(jChartEvalStat);
				}
			}
		} catch (Exception e) {			
		}
		try {
			CompetitionEvaluationStatus highChartEvalStat = new CompetitionEvaluationStatus(competition,evaluationNameString+"-Highchart",mBaseOutputString+evaluationNameString+"-Highchart.html",CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_HTML,new DateTime(DateTimeZone.UTC));
			if (mHighchartPrinter.printLogarithmicCactusChart(mBaseOutputString+evaluationNameString+"-Highchart.html", chartData)) {
				if (mStatusUpdater != null) {
					mStatusUpdater.updateCompetitionEvaluationStatus(highChartEvalStat);
				}
			}
		} catch (Exception e) {			
		}
		return false;
	}
	
	public boolean printBarChart(Competition competition, String evaluationNameString, String title, ArrayList<String> dataList, ArrayList<String> valueNameList, String dataName, String dataTitle, String valueTitle) {
		
		EvaluationChartPrintingData chartData = new EvaluationChartPrintingData(title); 
		chartData.addDataSerie(dataList,dataName);		
		chartData.setDataValuesNames(valueNameList);
		chartData.setDataTitle(dataTitle);
		chartData.setValuesTitle(valueTitle);
		
		printBarChart(competition,evaluationNameString,chartData);
		return false;
	}	

}
