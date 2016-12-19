package ore.evaluation;

import ore.configuration.Config;
import org.jCharts.axisChart.AxisChart;
import org.jCharts.axisChart.customRenderers.axisValue.renderers.ValueLabelPosition;
import org.jCharts.axisChart.customRenderers.axisValue.renderers.ValueLabelRenderer;
import org.jCharts.chartData.AxisChartDataSet;
import org.jCharts.chartData.ChartDataException;
import org.jCharts.chartData.DataSeries;
import org.jCharts.encoders.PNGEncoder;
import org.jCharts.properties.AxisProperties;
import org.jCharts.properties.BarChartProperties;
import org.jCharts.properties.ChartProperties;
import org.jCharts.properties.LegendProperties;
import org.jCharts.types.ChartType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class EvaluationJChartPrinter implements EvaluationChartPrinter {
	
	final private static Logger mLogger = LoggerFactory.getLogger(EvaluationJChartPrinter.class);
	
	private Config mConfig = null;
	
	public EvaluationJChartPrinter(Config config) {
		mConfig = config;
	}
	
	
	
	protected double[][] getData(EvaluationChartPrintingData chartData) {
		ArrayList<ArrayList<String>> dataSeries = chartData.getDataSeries();
		double[][] data = new double[chartData.getDataSeriesCount()][chartData.getDataSeriesLength()];
		int serieNumber = 0;
		for (ArrayList<String> dataSerie : dataSeries) {
			int dataValueNumber = 0;
			for (String dataValueString : dataSerie) {
				Double doubleValue = Double.valueOf(dataValueString);
				if (doubleValue != null) {
					data[serieNumber][dataValueNumber] = doubleValue;
				}
				++dataValueNumber;
			}
			++serieNumber;
		}
		return data;
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
		
	
	public boolean printBarChart(String outputString, EvaluationChartPrintingData chartData) {
		
		boolean chartGenerated = false;
		
		String[] xAxisLabels= getValueLabels(chartData);
		String xAxisTitle= chartData.getDataTitle();
		String yAxisTitle= chartData.getValuesTitle();
		String title= chartData.getTitle();
		DataSeries dataSeries = new DataSeries( xAxisLabels, xAxisTitle, yAxisTitle, title );

        double[][] data = getData(chartData);
		String[] legendLabels= getLegendLabels(chartData);
		Paint[] paints= new Paint[]{ Color.BLUE };
		BarChartProperties barChartProperties= new BarChartProperties();

		ValueLabelRenderer valueLabelRenderer = new ValueLabelRenderer( false, true, -1 );
		valueLabelRenderer.setValueLabelPosition( ValueLabelPosition.ON_TOP );
		valueLabelRenderer.useVerticalLabels( false );
		barChartProperties.addPostRenderEventListener( valueLabelRenderer );


		
		try {
			AxisChartDataSet axisChartDataSet = new AxisChartDataSet( data, legendLabels, paints, ChartType.BAR, barChartProperties );
			dataSeries.addIAxisPlotDataSet( axisChartDataSet );
	
			ChartProperties chartProperties= new ChartProperties();
			AxisProperties axisProperties= new AxisProperties();
			LegendProperties legendProperties= new LegendProperties();
			AxisChart axisChart= new AxisChart( dataSeries, chartProperties, axisProperties, legendProperties, 1024, 768 );
	
			FileOutputStream fileOutputStream;
			try {
				fileOutputStream= new FileOutputStream( outputString);
				PNGEncoder.encode( axisChart, fileOutputStream );
				fileOutputStream.flush();
				fileOutputStream.close();
				chartGenerated = true;
			} catch (Exception e) {
				mLogger.error("Failed to write chart to file, got Exception '{}'",e.getMessage());
			}
		} catch (ChartDataException e) {
			mLogger.error("Failed to write chart to file, got Exception '{}'",e.getMessage());
		}		
		return chartGenerated;		
	}

}
