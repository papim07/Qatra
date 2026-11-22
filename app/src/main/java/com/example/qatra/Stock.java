package com.example.qatra;

import java.io.Serializable;
import java.util.ArrayList;

public class Stock implements Serializable {
    private String bloodType;
    private long recordDate;
    private int unitsInStock;
    private int unitsUsed;
    private int unitsDonated;
    private int unitsExpiring;
    private long expiryDate;

    public Stock(String bloodType, long recordDate, int unitsInStock, int unitsUsed,
                 int unitsDonated, int unitsExpiring, long expiryDate) {
        this.bloodType = bloodType;
        this.recordDate = recordDate;
        this.unitsInStock = unitsInStock;
        this.unitsUsed = unitsUsed;
        this.unitsDonated = unitsDonated;
        this.unitsExpiring = unitsExpiring;
        this.expiryDate = expiryDate;
    }

    public static Stock currentLevel(ArrayList<Stock> records, String bloodType) {
        Stock latest = null;
        for (Stock s : records) {
            if (s.bloodType.equals(bloodType) && (latest == null || s.recordDate >= latest.recordDate)) latest = s;
        }
        return latest;
    }

    public void addUnit() { unitsInStock++; unitsDonated++; }

    public String getBloodType() { return bloodType; }
    public long getRecordDate() { return recordDate; }
    public int getUnitsInStock() { return unitsInStock; }
    public int getUnitsUsed() { return unitsUsed; }
    public int getUnitsDonated() { return unitsDonated; }
    public int getUnitsExpiring() { return unitsExpiring; }
    public long getExpiryDate() { return expiryDate; }
}
