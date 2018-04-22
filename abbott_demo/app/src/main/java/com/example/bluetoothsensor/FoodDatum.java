package com.example.bluetoothsensor;

/**
 * Created by William on 10/17/2017.
 */

public class FoodDatum {
    int year;
    int month;
    int day;
    int hour;
    String description;
    float energy;
    float fat;
    float CHO;
    float protein;

    public FoodDatum(String name, String energy,
                     String fat, String CHO,
                     String protein, String time,
                     String date) {
        description = name;
        this.energy = Float.valueOf(energy);
        this.fat = Float.valueOf(fat);
        this.CHO = Float.valueOf(CHO);
        this.protein = Float.valueOf(protein);
        this.hour = timeTohour(time);

        int firstSlash = date.indexOf('/');
        int secondSlash = date.indexOf('/', firstSlash +1);
//        Log.v("Datum ", "String: " + date + " FirstSlash: " + Integer.toString(firstSlash) + " SecondSlash: " + Integer.toString(secondSlash));
        this.year = yearFromDate(date, firstSlash, secondSlash);
        this.month = monthFromDate(date, firstSlash, secondSlash);
        this.day = dayFromDate(date, firstSlash, secondSlash);

    }

    private int timeTohour(String time) {
        int colIndex = time.indexOf(':');
        int pIndex = time.indexOf('p');
        int aIndex = time.indexOf('a');
        String hr = time.substring(0, colIndex);
        int hour = Integer.valueOf(hr);
        if (hour == 12 && aIndex != -1) hour = 0;
        if (hour != 12 && pIndex != -1) hour += 12;
        return hour;
    }

    private int yearFromDate(String date, int firstSlash, int secondSlash) {
        String year = date.substring(secondSlash+1, date.length());
        return Integer.valueOf(year);
    }

    private int monthFromDate(String date, int firstSlash, int secondSlash) {
        String year = date.substring(0,firstSlash);
        return Integer.valueOf(year);
    }

    private int dayFromDate(String date, int firstSlash, int secondSlash) {
        String day = date.substring(firstSlash+1, secondSlash);
        return Integer.valueOf(day);
    }

    public String getTime() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(hour))
                .append(":")
                .append("00");
        return sb.toString();
    }

    public String getDate() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(month))
                .append("/")
                .append(Integer.toString(day))
                .append("/")
                .append(Integer.toString(year));
        return sb.toString();
    }
}
