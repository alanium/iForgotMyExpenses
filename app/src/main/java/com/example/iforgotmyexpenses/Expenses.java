package com.example.iforgotmyexpenses;

public class Expenses {
    private int id_expenses;
    private String description;
    private int amount;
    private int amount_total;
    private String date;

    public Expenses(){}

    public Expenses(String description, int amount) {
        this.description = description;
        this.amount = amount;
    }

    public String toString(){
        return description + " $" + amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getAmount_total() {
        return amount_total;
    }

    public void setAmount_total(int amount_total) {
        this.amount_total = amount_total;
    }

    public int getId_expenses() {
        return id_expenses;
    }

    public void setId_expenses(int id_expenses) {
        this.id_expenses = id_expenses;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
