package ore.evaluation;

import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class EvaluationHighchartPrinter implements EvaluationChartPrinter {
	
	final private static Logger mLogger = LoggerFactory.getLogger(EvaluationHighchartPrinter.class);

	protected Config mConfig = null;
	protected String mBaseTemplateFileString = null;
	protected String mBarChartTemplateFileString = null;
	
	
	protected static AtomicInteger mContinerIDNumber = new AtomicInteger();
	
	
	public EvaluationHighchartPrinter(Config config) {
		mConfig = config;
		
		mBaseTemplateFileString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_TEMPLATES_DIRECTORY);
		mBarChartTemplateFileString = mBaseTemplateFileString+"charts"+ File.separator+"highchart"+ File.separator+"bar-horizontal-chart.html";
	}
	
	

	protected String loadFileIntoString(String fileString) {
		String string = null;
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileString));
			StringBuilder sb = new StringBuilder();
	        byte[] b = new byte[4096];
	        for (int i; (i = inputStream.read(b)) != -1;) {
	        	sb.append(new String(b, 0, i));
	        }		        
			inputStream.close();
			
			string = sb.toString();
			
		} catch (Exception e) {
			mLogger.error("Could not access file '{}', got Exception '{}'",fileString,e.getMessage());
		}	
		return string;
	}
	
	public boolean printBarChart(String outputString, EvaluationChartPrintingData chartData) {
		boolean chartGenerated = false;
		String chartString = loadFileIntoString(mBarChartTemplateFileString);
		
		if (chartString != null) {
			
			String plotLabelsString = getCategoriesString(chartData);
			String dataString = getDataString(chartData);
			String valuesTitleString = chartData.getValuesTitle();
			String categoriesTitleString = chartData.getDataTitle();
			String plotTitleString = chartData.getTitle();
			String containerIDString = getContainerIDString(outputString,chartData);
			
			chartString = chartString.replace("$$_PLOT_TITLE_$$", plotTitleString);
			chartString = chartString.replace("$$_PLOT_LABELS_$$", plotLabelsString);
			chartString = chartString.replace("$$_PLOT_DATA_SERIES_$$", dataString);
			chartString = chartString.replace("$$_VALUES_TITLE_$$", valuesTitleString);
			chartString = chartString.replace("$$_CATEGORIES_TITLE_$$", categoriesTitleString);
			chartString = chartString.replace("$$_CONTAINER_ID_$$", containerIDString);
			
			FileOutputStream fileOutputStream;
			try {
				fileOutputStream = new FileOutputStream( outputString);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
				outputStreamWriter.write(chartString);
				outputStreamWriter.flush();
				fileOutputStream.flush();
				outputStreamWriter.close();
				fileOutputStream.close();
				chartGenerated = true;
			} catch (Exception e) {
				mLogger.error("Failed to write chart to file, got Exception '{}'",e.getMessage());
			}
		}

		
		return chartGenerated;
	}
	
	
	
	protected String getContainerIDString(String outputString, EvaluationChartPrintingData chartData) {
		int nextContainerID = mContinerIDNumber.addAndGet(1);
		DateTime dateTime = new DateTime();
		String containerIDString = "container-"+nextContainerID+"-"+dateTime.getMillis();
		return containerIDString;
	}


	
	protected String getCategoriesString(EvaluationChartPrintingData chartData) {
		ArrayList<String> dataValuesNames = chartData.getDataValuesNames();
		String categoryString = "";
		for (String dataValueNameString : dataValuesNames) {
			if (categoryString.length() > 0) {
				categoryString = categoryString+", ";
			}
			categoryString = categoryString+"'"+dataValueNameString+"'";			
		}
		return categoryString;
	}
	
	
	
	
	protected String getDataString(EvaluationChartPrintingData chartData) {
		String dataString = "";
		ArrayList<ArrayList<String>> dataSeries = chartData.getDataSeries();
		ArrayList<String> dataSeriesNames = chartData.getDataSeriesNames();
		
		Iterator<ArrayList<String>> seriesIt = dataSeries.iterator();
		Iterator<String> serieNamesIt = dataSeriesNames.iterator();
		
		while (seriesIt.hasNext() && serieNamesIt.hasNext()) {
			ArrayList<String> seriesData = seriesIt.next();
			String seriesName = serieNamesIt.next();
			String serieDataString = "";
			for (String dataValueString : seriesData) {
				if (serieDataString.length() > 0) {
					serieDataString = serieDataString+", ";
				}
				serieDataString = serieDataString+dataValueString;
			}
			serieDataString = "{ name: '"+seriesName+"', data: ["+serieDataString+"], dataLabels: {enabled: true, color: '#000', align: 'center', y: 25, style: {fontSize: '15px'} } }";
			if (dataString.length() > 0) {
				dataString = dataString+", ";
			}
			dataString = dataString+serieDataString;
		}
		return dataString;
	}
	
	
	
	
	protected String[] getLegendLabels(EvaluationChartPrintingData chartData) {
		ArrayList<String> dataSeriesNames = chartData.getDataSeriesNames();
		int seriesCount = dataSeriesNames.size();
		String[] legendLabels = new String[seriesCount];
		int dataSerieNameNumber = 0;
		for (String dataSerieNameString : dataSeriesNames) {
			legendLabels[dataSerieNameNumber] = dataSerieNameString;
			++dataSerieNameNumber;
		}
		return legendLabels;
	}
	
	

	
	protected String[] getValueLabels(EvaluationChartPrintingData chartData) {
		ArrayList<String> dataValuesNames = chartData.getDataValuesNames();
		int valuesCount = dataValuesNames.size();
		String[] valueLabels = new String[valuesCount];
		int dataValueNameNumber = 0;
		for (String dataValueNameString : dataValuesNames) {
			valueLabels[dataValueNameNumber] = dataValueNameString;
			++dataValueNameNumber;
		}
		return valueLabels;
	}		
	
	
	
	
	
	

}
