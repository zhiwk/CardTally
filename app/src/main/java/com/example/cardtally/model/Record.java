package com.example.cardtally.model;

public class Record {
    private long id;
    private String date;
    private double amount;
    private String category;
    private int type; // 0: 支出, 1: 收入
    private String description;

    public Record() {
    }

    public Record(long id, String date, double amount, String category, int type, String description) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.description = description;
    }

    public Record(String date, double amount, String category, int type, String description) {
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
