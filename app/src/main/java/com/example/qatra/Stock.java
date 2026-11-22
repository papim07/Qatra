package com.example.qatra;

import java.io.Serializable;
import java.util.ArrayList;

/** <<entity>> Stock: one daily stock record for one blood type (FR-7). */
public class Stock implements Serializable {
    private String bloodType;
    private long recordDate;       // day number
    private int unitsInStock;
    private int unitsUsed;
    private int unitsDonated;
    private int unitsExpiring;     // units that expire on expiryDate
    private long expiryDate;       // nearest expiry date of units in stock

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

    /** The latest record for a blood type, or null. */
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
