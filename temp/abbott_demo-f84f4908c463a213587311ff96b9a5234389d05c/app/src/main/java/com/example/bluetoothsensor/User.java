package com.example.bluetoothsensor;

/**
 * Created by William on 10/8/2017.
 */

public class User {
    private int age;
    private float weight;
    private String name;
    private float height;
    private float hbe;

    public User() {
        this.age = -1;
        this.weight = -1;
        this.name = "NO NAME GIVEN";
        this.height = -1;
        this.hbe = 2000;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public float getHbe() {
        return hbe;
    }
}
