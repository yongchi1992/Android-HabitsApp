package com.example.bluetoothsensor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import org.achartengine.model.TimeSeries;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by William on 10/15/2017.
 */

public class CaloriesGraph extends bargraph {
    public enum Timescale {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    int[] x = { 0,1,2,3,4,5,6,7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24};
    private float X_MAX = 24;
    private float X_MIN = 0;
    private static final float Y_MIN = 0;
    private static final float Y_MAX = 2359 + 1000;
    private static final float THRESHOLD = 500;
    private float HBE = 2000;
    private float totalCals = 0;

    private TimeSeries overEating = new TimeSeries("Overeating episode");
    private TimeSeries underEating = new TimeSeries("Undereating episode");
    private TimeSeries Eating = new TimeSeries("Calorie Intake");

    private XYSeriesRenderer overeatingRenderer = new XYSeriesRenderer();
    private XYSeriesRenderer underEatingRenderer = new XYSeriesRenderer();
    private XYSeriesRenderer EatingRenderer = new XYSeriesRenderer();
    private XYSeriesRenderer thresholdRenderer = new XYSeriesRenderer();
    private XYSeriesRenderer hbRenderer = new XYSeriesRenderer();

    private float nextDayEntry = 0;

    CaloriesGraph(String color, float HBE, Timescale timescale, Context c) {
        //super(color);
        List<FoodDatum> data = new LinkedList<>();
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH)+1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        Float[] cals;
        TimeSeries lowerSeries = new TimeSeries("Lower Threshold");
        TimeSeries upperSeries = new TimeSeries("Upper Threshold");
        TimeSeries hbSeries = new TimeSeries("Harris Benedict");
        switch (timescale) {
            case DAILY:
                this.X_MAX = 24;
                data = FoodData.getInstance(c).getAll(-1, month, day, year);
                mRenderer.setXTitle("Hour of Day");

                // Init graph
                for (int i=0; i<X_MAX+1; i++) {putDayCalories(i,0);}
                this.nextDayEntry = 0;

                cals = new Float[25];
                Arrays.fill(cals, 0f);
                for (FoodDatum d : data) {
                    cals[d.hour] += d.energy;
                }
                for (int i=1; i<25; i++) {
                    putDayCalories(i, cals[i]);
                }

                lowerSeries.add(3.05, 0);
                lowerSeries.add(24, 2059);

                upperSeries.add(0, 300);
                upperSeries.add(24, 2659);

                hbSeries.add(0, 0);
                hbSeries.add(24, 2359);
                break;
            case WEEKLY:
                this.X_MAX = 7;
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                data = FoodData.getInstance(c).getAll(-1, month, -1, year);
                mRenderer.setClickEnabled(true);

                // Accumulate number of calories for each day
                cals = new Float[8];
                Arrays.fill(cals, 0f);
                for (FoodDatum d : data) {
                    if (d.day > day - dayOfWeek )
                       cals[dayOfWeek - (day - d.day)] += d.energy;
                }
                for (int i=1; i<dayOfWeek+1; i++) {
                    putCalories(i, cals[i]);
                }
                mRenderer.addXTextLabel(1, "Sun");
                mRenderer.addXTextLabel(2, "Mon");
                mRenderer.addXTextLabel(3, "Tue");
                mRenderer.addXTextLabel(4, "Wed");
                mRenderer.addXTextLabel(5, "Thu");
                mRenderer.addXTextLabel(6, "Fri");
                mRenderer.addXTextLabel(7, "Sat");
                mRenderer.setXTitle("Day of Week");

                mRenderer.setXLabels(0);
                lowerSeries.add(0, 2059);
                lowerSeries.add(8, 2059);

                upperSeries.add(0, 2659);
                upperSeries.add(8, 2659);

                hbSeries.add(0, 2359);
                hbSeries.add(8, 2359);
                break;
            case MONTHLY:
                this.X_MAX = 31;
                data = FoodData.getInstance(c).getAll(-1, month, -1, year);
                // Accumulate number of calories for each day
                cals = new Float[32];
                Arrays.fill(cals, 0f);
                for (FoodDatum d : data) {
                    cals[d.day] += d.energy;
                }
                for (int i=1; i<32; i++) {
                    putCalories(i, cals[i]);
                }
                mRenderer.setXTitle("Day of Month");
                lowerSeries.add(0, 2059);
                lowerSeries.add(32, 2059);

                upperSeries.add(0, 2659);
                upperSeries.add(32, 2659);

                hbSeries.add(0, 2359);
                hbSeries.add(32, 2359);
                break;
        }

        this.HBE = HBE;


        this.mRenderer.setShowAxes(true);
        this.mRenderer.setBackgroundColor(Color.TRANSPARENT);
        this.mRenderer.setApplyBackgroundColor(true);
        this.mRenderer.setOrientation(XYMultipleSeriesRenderer.Orientation.HORIZONTAL);
//        this.mRenderer.setXTitle("Time (in Hours)");
        this.mRenderer.setYTitle("Calories Consumed");

        //setting text size of the axis title
        this.mRenderer.setAxisTitleTextSize(45);
        //setting text size of the graph lable
        this.mRenderer.setLabelsTextSize(45);
        //setting zoom buttons visiblity
        this.mRenderer.setZoomButtonsVisible(false);
        //setting pan enablity which uses graph to move on both axis
        this.mRenderer.setPanEnabled(false, false);
        //setting click false on graph
        //setting zoom to false on both axis
        this.mRenderer.setZoomEnabled(false, false);
        //setting lines to display on y axis
        this.mRenderer.setShowGridY(false);
        //setting lines to display on x axis
        this.mRenderer.setShowGridX(false);
        //setting legend to fit the screen size
        this.mRenderer.setFitLegend(true);
        //setting displaying line on grid
        this.mRenderer.setShowGrid(false);
        //setting zoom to false
        this.mRenderer.setZoomEnabled(true);
        //setting external zoom functions to false
        this.mRenderer.setExternalZoomEnabled(false);
        //setting displaying lines on graph to be formatted(like using graphics)
        this.mRenderer.setAntialiasing(true);
        //setting to in scroll to false
        this.mRenderer.setInScroll(false);
        //setting to set legend height of the graph
        this.mRenderer.setLegendHeight(40);
        //setting x axis label align
        this.mRenderer.setXLabelsAlign(Paint.Align.CENTER);
        //setting y axis label to align
        this.mRenderer.setYLabelsAlign(Paint.Align.LEFT);
        //setting text style
        this.mRenderer.setTextTypeface("sans_serif", Typeface.NORMAL);
        //setting no of values to display in y axis
        this.mRenderer.setYLabels(10);
        // setting y axis max value, Since i'm using static values inside the graph so i'm setting y max value to 4000.
        // if you use dynamic values then get the max y value and set here
        this.mRenderer.setYAxisMax(Y_MAX);
        this.mRenderer.setYAxisMin(Y_MIN);
        //setting used to move the graph on xaxiz to .5 to the right
        this.mRenderer.setXAxisMin(X_MIN);
        this.mRenderer.setFitLegend(true);
        this.mRenderer.setLegendTextSize(40);
        //setting max values to be display in x axis
        this.mRenderer.setXAxisMax(X_MAX);
        //setting bar size or space between two bars
        this.mRenderer.setBarSpacing(0);

        this.mRenderer.setMargins(new int[] {50,70,135,50});

        // Due to bug the color must be set as this for transparent
        this.mRenderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));

        this.mDataset.addSeries(underEating);
        this.mRenderer.addSeriesRenderer(this.underEatingRenderer);
        prepareRenderer(this.underEatingRenderer, "#6666ff");

        this.mDataset.addSeries(overEating);
        this.mRenderer.addSeriesRenderer(this.overeatingRenderer);
        prepareRenderer(this.overeatingRenderer, "#ff6666");

        this.mDataset.addSeries(Eating);
        this.mRenderer.addSeriesRenderer(this.EatingRenderer);
        prepareRenderer(this.EatingRenderer, "#66FF66");

        // Add Harris Benedict thresholds
        this.mDataset.addSeries(lowerSeries);
        this.thresholdRenderer.setStroke(BasicStroke.SOLID);
        this.thresholdRenderer.setFillBelowLine(false);
        this.thresholdRenderer.setLineWidth(5);
        this.thresholdRenderer.setColor(Color.WHITE);
        this.mRenderer.addSeriesRenderer(this.thresholdRenderer);
        this.mDataset.addSeries(upperSeries);
        this.mRenderer.addSeriesRenderer(this.thresholdRenderer);

        this.mDataset.addSeries(hbSeries);
        this.hbRenderer.setStroke(BasicStroke.DASHED);
        this.hbRenderer.setLineWidth(5);
        this.hbRenderer.setColor(Color.WHITE);
        this.mRenderer.addSeriesRenderer(this.hbRenderer);
    }

    /**
     * Adds a number of calories consumed at time time
     *
     * @param time     the time (in # of minutes today)
     * @param calories number of calories consumed at time
     */
    public void addCalories(float time, float calories) {
        totalCals += calories;
        float HBNow = HBE * (time) / X_MAX;
        float delta = totalCals - HBNow;
        this.Eating.add(time, totalCals);
        Log.d("each plot", "delta=" + delta);
    }

    public void putCalories(float time, float calories) {
        this.overEating.add(time, 0);
        this.underEating.add(time,0);
        this.Eating.add(time, 0);
        if (isOvereating(calories)) {
            this.overEating.add(time, calories);
        } else if (isUndereating(calories)) {
            this.underEating.add(time, calories);
        } else {
            this.Eating.add(time, calories);
        }
    }

    public boolean isDayOvereating(float time, float calories) {
        float hbSlope = 2359f/24f;
        float timeToEnd = 24 - time;
        float thresholdNow = 2659 - hbSlope*timeToEnd;
        return calories > thresholdNow;
    }

    public boolean isDayUndereating(float time, float calories) {
        float hbSlope = 2359f/24f;
        float timeToEnd = 24 - time;
        float thresholdNow = 2059 - hbSlope*timeToEnd;
        return calories < thresholdNow;
    }

    public void putDayCalories(float time, float calories) {
        // Update unfilled points with same value
        if (this.nextDayEntry < time) {
            putDayCalories(time-1, 0);
        }

        totalCals += calories;
        calories = totalCals;


        if (isDayOvereating(time, calories)) {
            this.overEating.add(time, calories);
        } else if (isDayUndereating(time, calories)) {
            this.underEating.add(time, calories);
        } else {
            this.Eating.add(time, calories);
        }
        this.nextDayEntry = time+1;
    }

    public void addOverEating(float time) {
        float HBNow = HBE * (time)/X_MAX;
        float delta = totalCals - HBNow;
        this.overEating.add(time, totalCals);
    }

    private void prepareRenderer(XYSeriesRenderer renderer, String color) {

        renderer.setColor(Color.parseColor(color));
        renderer.setFillPoints(true);
        renderer.setChartValuesSpacing(0);
    }

    private boolean isOvereating(float energy) {
        return energy > 2659;
    }

    private boolean isUndereating(float energy) {
        return energy < 2059;
    }
    public static CaloriesGraph getDefaultDailyGraph(User user, Context context) {
        return new CaloriesGraph("#FFFB00", user.getHbe(), CaloriesGraph.Timescale.DAILY, context);
    }

    public void setDefaultDailyGraph() {

        float time = 0;
        this.totalCals = 0;

        // Fill up to 0600 with 0 calories
        for (; time < 6; time++) {
            this.addCalories(time, 0);
            Log.d("before 6", "time=" + time);

        }


        // 250 calorie bagel, 39 cal OJ over 15 minutes
        for (;time < 7; time++) {
            this.addCalories(time, (250 + 39));
            Log.d("before 7", "time=" + time);
        }

        // Nothing for commute (1hr)
        float commute = time + 1;
        for (;time < commute; time++) {
            this.addCalories(time, 0);
        }

        // Muffin at work across 1hr meeting (426 cal)
        float meeting = time + 1;
        for (;time < meeting; time++) {
            this.addCalories(time, 426);
        }


        // 0815 right now, nothing for 3.5 hours (1145)
        float lunch_start = (float) (time + 3.5);
        for (; time<lunch_start; time++) this.addCalories(time, 0);
        // Goes to McDonalds and gets a big mac, fries, coke
        float lunch_end = time + 1;
        for (;time<lunch_end; time++) {
            this.addCalories(time, 1120/1f);
            this.addOverEating(time);
        }
        // Do nothing until 1800
        for (; time<18; time++) {
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
