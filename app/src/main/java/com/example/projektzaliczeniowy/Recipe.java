package com.example.projektzaliczeniowy;

public class Recipe {
    private String name;
    private float rating;
    private String imageUrl;
    private int calories;
    private int totalTimeMinutes;
    private String id;

    public Recipe(String name, float rating, String imageUrl, int calories, int totalTimeMinutes, String id) {
        this.name = name;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.calories = calories;
        this.totalTimeMinutes = totalTimeMinutes;
        this.id = id;
    }

    // Getters
    public String getName() {
        return name;
    }

    public float getRating() {
        return rating;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getCalories() {
        return calories;
    }

    public int getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    public String getId() {
        return id;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public void setTotalTimeMinutes(int totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }

    public void setId(String id) {
        this.id = id;
    }
}