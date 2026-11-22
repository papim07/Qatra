package com.example.qatra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MatchingModule {
    public static final double[] DISTANCE_STEPS = {25, 50, 150, 200};

    private static final HashMap<String, List<String>> COMPATIBLE = new HashMap<>();
    static {
        COMPATIBLE.put("O-", Arrays.asList("O-"));
        COMPATIBLE.put("O+", Arrays.asList("O+", "O-"));
        COMPATIBLE.put("A-", Arrays.asList("A-", "O-"));
        COMPATIBLE.put("A+", Arrays.asList("A+", "A-", "O+", "O-"));
        COMPATIBLE.put("B-", Arrays.asList("B-", "O-"));
        COMPATIBLE.put("B+", Arrays.asList("B+", "B-", "O+", "O-"));
        COMPATIBLE.put("AB-", Arrays.asList("AB-", "A-", "B-", "O-"));
        COMPATIBLE.put("AB+", Arrays.asList("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"));
    }

    private double searchDistanceKm = DISTANCE_STEPS[0];

    private final QatraDatabase db;
    private final EligibilityModule eligibility;
    private final MapsService maps;
    private final NotificationService notification;

    public MatchingModule(QatraDatabase db, EligibilityModule eligibility,
                          MapsService maps, NotificationService notification) {
        this.db = db;
        this.eligibility = eligibility;
        this.maps = maps;
        this.notification = notification;
    }

    public Appeal prepareAppeal(Forecast f) {
        for (Appeal a : db.getAppeals()) {
            if (a.getBloodType().equals(f.getBloodType()) && a.getStatus().equals(Appeal.PENDING)) return a;
        }
        String urgency;
        if (f.getDaysUntilShort() == 0) urgency = "Critical";
        else if (f.getRiskLevel().equals("High")) urgency = "High";
        else urgency = "Normal";
        Appeal a = new Appeal(db.nextAppealId(), f.getBloodType(), urgency,
                Math.max(1, f.getUnitsNeeded()), QatraDatabase.today());
        db.getAppeals().add(a);
        db.log("system", "Prepared " + urgency + " appeal #" + a.getAppealId() + " for " + a.getBloodType());
        return a;
    }

    public ArrayList<Donor> matchDonors(int appealId) {
        Appeal appeal = db.findAppeal(appealId);
        ArrayList<Donor> matched = new ArrayList<>();
        String bankLocation = db.getBank().getLocation();
        for (Donor d : db.getDonors()) {
            if (db.hasOpenAlert(d)) continue;
            if (!eligibility.checkEligibility(d.getUserId()).equals(EligibilityModule.ELIGIBLE)) continue;
            if (!isCompatible(d.getBloodType(), appeal.getBloodType())) continue;
            if (maps.getDistance(d.getLocation(), bankLocation) > searchDistanceKm) continue;
            matched.add(d);
        }
        appeal.assignRecipients(matched);
        return matched;
    }

    public boolean isCompatible(String donorType, String neededType) {
        List<String> ok = COMPATIBLE.get(neededType);
        return ok != null && ok.contains(donorType);
    }

    public ArrayList<Donor> widenSearch(int appealId, double distanceKm) {
        searchDistanceKm = distanceKm;
        db.log("staff", "Widened appeal #" + appealId + " to " + (int) distanceKm + " km");
        return matchDonors(appealId);
    }

    public double nextDistance() {
        for (double d : DISTANCE_STEPS) if (d > searchDistanceKm) return d;
        return -1;
    }

    public void resetSearch() { searchDistanceKm = DISTANCE_STEPS[0]; }

    public int sendAppeal(int appealId, String by) {
        Appeal appeal = db.findAppeal(appealId);
        matchDonors(appealId);
        appeal.approve();
        boolean delivered = notification.sendAlerts(appeal.getRecipients(), appeal);
        if (!delivered) {
            appeal.markPending();
            db.log(by, "Notification Service unavailable, appeal #" + appealId + " kept pending");
            return -1;
        }
        db.log(by, appeal.logAppeal());
        return appeal.getDonorCount();
    }

    public void rejectAppeal(int appealId, String reason, String by) {
        Appeal appeal = db.findAppeal(appealId);
        appeal.reject(reason);
        db.log(by, appeal.logAppeal() + ": " + reason);
    }

    public double getSearchDistanceKm() { return searchDistanceKm; }
    public NotificationService getNotificationService() { return notification; }
}
