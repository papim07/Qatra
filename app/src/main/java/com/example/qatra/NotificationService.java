package com.example.qatra;

import java.util.ArrayList;

/**
 * <<boundary>> NotificationService (external system, simulated).
 * Creates one Alert per recipient; the donor sees it in the app (Push/SMS is simulated).
 */
public class NotificationService {
    private final QatraDatabase db;
    private boolean available = true;   // can be switched off to test UC-7 extension 5a

    public NotificationService(QatraDatabase db) {
        this.db = db;
    }

    /** Returns true if delivered. */
    public boolean sendAlerts(ArrayList<Donor> recipients, Appeal appeal) {
        if (!available) return false;
        for (Donor d : recipients) {
            db.getAlerts().add(new Alert(appeal, d, db.getBank().getName()));
        }
        return true;
    }

    public void setAvailable(boolean available) { this.available = available; }
    public boolean isAvailable() { return available; }
}
