package com.example.qatra;

import java.util.ArrayList;

public class NotificationService {
    private final QatraDatabase db;
    private boolean available = true;

    public NotificationService(QatraDatabase db) {
        this.db = db;
    }

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
