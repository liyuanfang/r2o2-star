package org.semanticweb.ore.evaluation;

import java.util.ArrayList;

public class EvaluationChartPrintingData {
	
	
	protected ArrayList<ArrayList<String>> mDataSeries = new ArrayList<ArrayList<String>>();
	protected ArrayList<String> mDataValuesNames = new ArrayList<String>();
	protected ArrayList<String> mDataSeriesNames = new ArrayList<String>();
	protected String mTitle = null;
	protected String mDataTitle = null;
	protected String mValuesTitle = null;
	protected int mDataSeriesCount = 0;
	protected int mDataSeriesLength = 0;

	
	public EvaluationChartPrintingData() {		
	}
	
	public EvaluationChartPrintingData(String titleString) {
		mTitle = titleString;
	}
	
	
	
	public ArrayList<ArrayList<String>> getDataSeries() {
		return mDataSeries;
	}
	
	public ArrayList<String> getDataValuesNames() {
		return mDataValuesNames;
	}

	public ArrayList<String> getDataSeriesNames() {
		return mDataSeriesNames;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getDataTitle() {
		return mDataTitle;
	}
	
	public String getValuesTitle() {
		return mValuesTitle;
	}
	
	
	public int getDataSeriesCount() {
		return mDataSeriesCount;
	}
	
	public int getDataSeriesLength() {
		return mDataSeriesLength;
	}
	
	

	
	public void addDataSerie(ArrayList<String> dataSerie, String dataNameString) {
		mDataSeries.add(dataSerie);
		mDataSeriesLength = Math.max(mDataSeries.size(),dataSerie.size());
		mDataSeriesCount = mDataSeries.size();
		addDataSerieName(dataNameString);
	}
	
	
	
	public void setDataSeries(ArrayList<ArrayList<String>> dataSeries) {
		mDataSeries = dataSeries;
		for (ArrayList<String> dataSerie : mDataSeries) {
			mDataSeriesLength = Math.max(mDataSeries.size(),dataSerie.size());
		}
		mDataSeriesCount = mDataSeries.size();
	}
	
	public void setDataValuesNames(ArrayList<String> dataValuesNames) {
		mDataValuesNames = dataValuesNames;
	}
	
	
	public void addDataValueName(String dataValueName) {
		mDataValuesNames.add(dataValueName);
	}
	
	

	public void setDataSeriesNames(ArrayList<String> dataValueName) {
		mDataSeriesNames = dataValueName;
	}
	
	
	public void addDataSerieName(String dataSerieName) {
		mDataSeriesNames.add(dataSerieName);
	}
	
	public void setTitle(String title) {
		mTitle = title;
	}
	
	public void setDataTitle(String title) {
		mDataTitle = title;
	}
	
	public void setValuesTitle(String title) {
		mValuesTitle = title;
	}

	

}
