package com.example.cardtally.model;

public class Category {
    private long id;
    private String name;
    private int type; // 0: 支出, 1: 收入

    public Category() {
    }

    public Category(long id, String name, int type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Category(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
