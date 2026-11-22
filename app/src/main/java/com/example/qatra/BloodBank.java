package com.example.qatra;

import java.io.Serializable;
import java.util.ArrayList;

public class BloodBank implements Serializable {
    private String name;
    private String location;
    private int safeStockLevel;
    private ArrayList<Stock> stockRecords = new ArrayList<>();

    public BloodBank(String name, String location, int safeStockLevel) {
        this.name = name;
        this.location = location;
        this.safeStockLevel = safeStockLevel;
    }

    public int safeLevelFor(String bloodType) {
        ArrayList<Stock> history = history(bloodType);
        if (history.isEmpty()) return 1;
        int start = Math.max(0, history.size() - 28);
        double sum = 0;
        for (int i = start; i < history.size(); i++) sum += history.get(i).getUnitsUsed();
        double avgUse = sum / (history.size() - start);
        return Math.max(1, (int) Math.ceil(safeStockLevel * avgUse));
    }

    public void addRecord(Stock s) { stockRecords.add(s); }

    public Stock currentLevel(String bloodType) { return Stock.currentLevel(stockRecords, bloodType); }

    public ArrayList<Stock> history(String bloodType) {
        ArrayList<Stock> list = new ArrayList<>();
        for (Stock s : stockRecords) if (s.getBloodType().equals(bloodType)) list.add(s);
        return list;
    }

    public String getName() { return name; }
    public String getLocation() { return location; }
    public int getSafeStockLevel() { return safeStockLevel; }
    public ArrayList<Stock> getStockRecords() { return stockRecords; }
}
