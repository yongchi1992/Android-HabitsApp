package com.example.bluetoothsensor;

import android.content.Context;
import android.graphics.Color;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class LineGraph {
	static public int seconds;
	private GraphicalView view;

	TimeSeries dataset = new TimeSeries("Calories consumed");
	XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYSeriesRenderer renderer = new XYSeriesRenderer();
	public XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

	LineGraph(String color) {
		
		double[] range = {0,10,0,10};
		mDataset.addSeries(dataset);
		renderer.setColor(Color.parseColor(color));
		renderer.setPointStyle(PointStyle.CIRCLE);
		renderer.setFillPoints(true);
		renderer.setChartValuesSpacing(10);
		renderer.setLineWidth(5);
		
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		mRenderer.setBackgroundColor(Color.TRANSPARENT);
		mRenderer.setRange(range);
		mRenderer.setZoomEnabled(true);
		mRenderer.setYAxisMin(0);
		mRenderer.setYAxisMax(25);
		mRenderer.setShowAxes(false);
		mRenderer.setPanEnabled(true);
		mRenderer.setPointSize(6f);
		
		mRenderer.addSeriesRenderer(renderer);

	}

	public GraphicalView getView(Context context) {
		view = ChartFactory.getLineChartView(context, mDataset, mRenderer);
		return view;

	}

	public void addPoints(int[] val) {
		
		for(int i=0; i<val.length; i++){
			dataset.add(i, val[i]);
		}

		//dataset.add(time, mag);
	}

	public XYMultipleSeriesRenderer getmRenderer() {
		return mRenderer;
	}

	public void setmRenderer(XYMultipleSeriesRenderer mRenderer) {
		this.mRenderer = mRenderer;
	}
	
	
	

	/*
	  public void addPoints(AccelerometerData data){
	  
	  dataset.add(data.getTimestamp(), data.getMagnitude()); }
	 

	public Intent getIntent(Context context, float mag, float time) {

		TimeSeries series = new TimeSeries("mag vs time");
		series.add(time, mag);
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(series);

		XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		mRenderer.addSeriesRenderer(renderer);

		Intent intent = ChartFactory.getLineChartIntent(context, dataset,
				mRenderer);
		return intent;
	}
	
	*/
}
