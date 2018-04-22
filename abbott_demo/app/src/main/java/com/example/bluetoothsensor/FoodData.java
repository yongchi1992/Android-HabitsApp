package com.example.bluetoothsensor;

import android.content.Context;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by William on 10/17/2017.
 */

public class FoodData {
    private static FoodData instance = null;

    public static FoodData getInstance(Context context) {
        if (instance == null) {
            instance = new FoodData(context);
        }
        return instance;
    }

    public void put(FoodDatum datum) {

        SparseArray<SparseArray<SparseArray<List<FoodDatum>>>> y;
        SparseArray<SparseArray<List<FoodDatum>>> m;
        SparseArray<List<FoodDatum>> day;
        List<FoodDatum> l;

        y = data.get(datum.year);
        if (y==null) y = new SparseArray<SparseArray<SparseArray<List<FoodDatum>>>>();

        m = y.get(datum.month);
        if (m==null) m = new SparseArray<SparseArray<List<FoodDatum>>>();

        day = m.get(datum.day);
        if (day==null) day = new SparseArray<List<FoodDatum>>();

        l = day.get(datum.hour);
        if (l == null) l = new LinkedList<>();

        l.add(datum);
        day.put(datum.hour, l);
        m.put(datum.day, day);
        y.put(datum.month, m);
        data.put(datum.year, y);

    }

    public void remove(int hour, int day, int month, int year) {
        if (hour == -1) {
            // Delete all entries in this day
            data.get(year).get(month).put(day, new SparseArray<List<FoodDatum>>());
        }
    }

    public List<FoodDatum> getAll(int hour, int month, int day, int year) {
        try {
            LinkedList<FoodDatum> out = new LinkedList<FoodDatum>();
            SparseArray<SparseArray<SparseArray<List<FoodDatum>>>> y = data.get(year);

            if (month == -1) {
                // Return all entries in year
                int size = y.size();
                for (int i = 0; i < size; i++) {
                    // All months
                    SparseArray<SparseArray<List<FoodDatum>>> m = y.valueAt(i);
                    int size2 = m.size();
                    for (int j = 0; j < size2; j++) {
                        // All days
                        SparseArray<List<FoodDatum>> d = m.valueAt(j);
                        int size3 = d.size();
                        for (int k = 0; k < size3; k++) {
                            out.addAll(d.valueAt(k));
                        }
                    }
                }
            } else if (day == -1) {
                // Get all entries in month
                SparseArray<SparseArray<List<FoodDatum>>> m = y.get(month);
                int size2 = m.size();
                for (int j = 0; j < size2; j++) {
                    // All days
                    SparseArray<List<FoodDatum>> d = m.valueAt(j);
                    int size3 = d.size();
                    for (int k = 0; k < size3; k++) {
                        out.addAll(d.valueAt(k));
                    }
                }
            } else if (hour == -1) {
                SparseArray<List<FoodDatum>> d = y.get(month).get(day);
                // All hours
                int size3 = d.size();
                for (int k = 0; k < size3; k++) {
                    out.addAll(d.valueAt(k));
                }
            } else {
                out.addAll(y.get(month).get(day).get(hour));
            }
            return out;
        } catch (NullPointerException e) {
            return new LinkedList<>();
        }
    }

    // Year, Month, Day, Hour
    SparseArray<SparseArray<SparseArray<SparseArray<List<FoodDatum>>>>>
        data = new SparseArray<SparseArray<SparseArray<SparseArray<List<FoodDatum>>>>>();
    private FoodData(Context context) {
        // Open CSV file
        try {
            InputStream is = context.getAssets().open("p121.csv");
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isr);
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");;
                FoodDatum d = new FoodDatum(row[0], row[1], row[2], row[3], row[4], row[5], row[6]);

                this.put(d);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
