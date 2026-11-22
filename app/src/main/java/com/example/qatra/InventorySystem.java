package com.example.qatra;

import java.util.Random;

public class InventorySystem {
    private final QatraDatabase db;
    private final Random random = new Random();

    public InventorySystem(QatraDatabase db) {
        this.db = db;
    }

    public int importStock(long date) {
        BloodBank bank = db.getBank();
        int count = 0;
        for (String type : QatraDatabase.BLOOD_TYPES) {
            Stock last = bank.currentLevel(type);
            if (last == null) continue;
            int used = Math.max(0, last.getUnitsUsed() + random.nextInt(5) - 2);
            int donated = Math.max(0, last.getUnitsDonated() + random.nextInt(5) - 2);
            int units = Math.max(0, last.getUnitsInStock() + donated - used);
            int expiring = Math.min(units, last.getUnitsExpiring());
            bank.addRecord(new Stock(type, date, units, used, donated, expiring, date + 5));
            count++;
        }
        return count;
    }
}
