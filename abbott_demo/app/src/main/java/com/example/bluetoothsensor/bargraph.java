package com.example.bluetoothsensor;

/**
 * Created by Jishnu on 10/20/2017.
 */

import android.content.Context;
import android.graphics.Color;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.LineChart;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class bargraph {
    private GraphicalView view;

    TimeSeries dataset = new TimeSeries("Calories consumed");
    XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

    private XYSeriesRenderer renderer = new XYSeriesRenderer();
    public XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

    void BarChart() {

        mDataset.addSeries(dataset);
        renderer.setColor(Color.RED);
        renderer.setChartValuesSpacing(1f);
        renderer.setFillPoints(true);
        renderer.setLineWidth(5);
        renderer.setDisplayChartValues(true);


        mRenderer.setBackgroundColor(Color.TRANSPARENT);
        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setOrientation(XYMultipleSeriesRenderer.Orientation.HORIZONTAL);
        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(true);
        mRenderer.setShowAxes(true);
        mRenderer.setPanEnabled(true);mRenderer.setChartTitle("Calories");
        mRenderer.setXTitle("Time");
        mRenderer.setYTitle("Calories");
        mRenderer.addSeriesRenderer(renderer);
    }

    public GraphicalView getView(Context context) {
        view = ChartFactory.getCombinedXYChartView(context,
                mDataset,
                mRenderer,
                new String[] {BarChart.TYPE,
                        BarChart.TYPE,
                        BarChart.TYPE,
                        LineChart.TYPE,
                        LineChart.TYPE,
                        LineChart.TYPE});
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
