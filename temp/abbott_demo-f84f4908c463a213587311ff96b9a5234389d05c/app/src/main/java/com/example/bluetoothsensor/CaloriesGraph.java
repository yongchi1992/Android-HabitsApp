package com.example.bluetoothsensor;

import android.graphics.Color;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by William on 10/15/2017.
 */

public class CaloriesGraph extends LineGraph {
    public enum Timescale {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    private float X_MAX = 24 * 60;
    private static final float X_MIN = 0;
    private static final float Y_MIN = -1000;
    private static final float Y_MAX = 1000;
    private static final float THRESHOLD = 500;
    private float HBE = 2000;
    private float totalCals = 0;

    private TimeSeries upperThreshold = new TimeSeries("");
    private TimeSeries middleLine = new TimeSeries("");
    private TimeSeries lowerThreshold = new TimeSeries("");
    private TimeSeries overEating = new TimeSeries("Overeating episodes");
    private XYSeriesRenderer thresholdRenderer = new XYSeriesRenderer();
    private XYSeriesRenderer middleRenderer = new XYSeriesRenderer();
    private XYSeriesRenderer overeatingRenderer = new XYSeriesRenderer();

    CaloriesGraph(String color, float HBE, Timescale timescale) {
        super(color);

        switch (timescale) {
            case DAILY:
                this.X_MAX = 60 * 24;
                break;
            case WEEKLY:
                this.X_MAX = 7;
                HBE = 0;
                break;
            case MONTHLY:
                this.X_MAX = 31;
                HBE = 0;
                break;
        }

        this.HBE = HBE;

        double[] range = {X_MIN, X_MAX, Y_MIN, Y_MAX};
        this.mRenderer.setRange(range);

        this.mRenderer.setShowAxes(true);

        this.mDataset.addSeries(upperThreshold);
        prepareRenderer(this.thresholdRenderer, "#FFFFFF");
        this.mRenderer.addSeriesRenderer(this.thresholdRenderer);
        this.upperThreshold.add(X_MIN, THRESHOLD);
        this.upperThreshold.add(X_MAX, THRESHOLD);


        this.mDataset.addSeries(middleLine);
        prepareRenderer(this.middleRenderer, "#aaaaaa");
        this.mRenderer.addSeriesRenderer(this.middleRenderer);
        this.middleLine.add(X_MIN, 0);
        this.middleLine.add(X_MAX, 0);

        this.mDataset.addSeries(lowerThreshold);
        this.mRenderer.addSeriesRenderer(this.thresholdRenderer);
        this.lowerThreshold.add(X_MIN, -THRESHOLD);
        this.lowerThreshold.add(X_MAX, -THRESHOLD);

        this.mDataset.addSeries(overEating);
        prepareRenderer(this.overeatingRenderer, "#ff6666");
        this.mRenderer.addSeriesRenderer(this.overeatingRenderer);

        // Fill up to 0600 with 0 calories
        for (float i = 0; i < 6 * 60; i++) {
            addCalories(i, 0);
        }
    }

    /**
     * Adds a number of calories consumed at time time
     *
     * @param time     the time (in # of minutes today)
     * @param calories number of calories consumed at time
     */
    public void addCalories(float time, float calories) {
        totalCals += calories;
        float HBNow = HBE * time / X_MAX;

        float delta = totalCals - HBNow;

        this.dataset.add(time, delta);
    }

    public void addOverEating(float time) {
        float HBNow = HBE * time/X_MAX;
        float delta = totalCals - HBNow;
        this.overEating.add(time, delta);
    }

    private void prepareRenderer(XYSeriesRenderer renderer, String color) {

        renderer.setColor(Color.parseColor(color));
        renderer.setPointStyle(PointStyle.CIRCLE);
        renderer.setFillPoints(true);
        renderer.setChartValuesSpacing(10);
        renderer.setLineWidth(5);
    }

    public void setDefaultDailyGraph() {

        float time = 0;
        this.totalCals = 0;
        // Fill up to 0600 with 0 calories
        for (; time < 6 * 60; time++) {
            this.addCalories(time, 0);
        }
        // 250 calorie bagel, 39 cal OJ over 15 minutes
        for (; time < 6 * 60 + 15; time++) {
            this.addCalories(time, (250 + 39) / 7);
        }
        // Nothing for commute (1hr)
        float commute = time + 60;
        for (;time < commute; time++) {
            this.addCalories(time, 0f);
        }
        // Muffin at work across 1hr meeting (426 cal)
        float meeting = time+60;
        for (;time < meeting; time++) {
            // Take a bite every five minutes
            float calsPerBite = 426/12;
            if (time % 5 == 0) this.addCalories(time, calsPerBite);
            else this.addCalories(time, 0);
        }
        // 0815 right now, nothing for 3.5 hours (1145)
        float lunch_start = (float) (time + 3.5*60);
        for (; time<lunch_start; time++) this.addCalories(time, 0);
        // Goes to McDonalds and gets a big mac, fries, coke
        float lunch_end = time + 60;
        for (;time<lunch_end; time++) {
            this.addCalories(time, 1120/60f);
            this.addOverEating(time);
        }
        // Do nothing until 1800
        for (; time<18*60; time++) {
            this.addCalories(time, 0);
        }


    }

    private static List<Float> weekCals = null;

    private void setWeeklyCals() {
        weekCals = new LinkedList<Float>();
        weekCals.add(0f);
        weekCals.add(200f);
        weekCals.add(-100f);
        weekCals.add(100f);
    }

    public void setDefaultWeeklyGraph() {
        if (weekCals == null) {
            setWeeklyCals();
        }

        this.totalCals = 0;
        for (int i = 0; i < weekCals.size(); i++) {
            this.addCalories(i, weekCals.get(i));
        }
    }

    private static List<Float> monthCals = null;

    public void setDefaultMonthlyGraph() {
        if (weekCals == null) setWeeklyCals();
        if (monthCals == null) {
            monthCals = new LinkedList<Float>();
            for (float time = 0; time < this.X_MAX - weekCals.size(); time++) {
                double r = Math.random();
                r -= .5;
                double cals = r * 500;
                monthCals.add((float) cals);
            }

            for (int time = 0; time < weekCals.size(); time++) {
                monthCals.add(weekCals.get(time));
            }
        }

        this.totalCals = 0;
        for (int i = 0; i < monthCals.size(); i++) {
            this.addCalories(i, monthCals.get(i));
        }
    }
}
