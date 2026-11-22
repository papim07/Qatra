package com.example.qatra;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

/** <<entity>> Donor (a User). Has 0..* Deferral and makes 0..* Donation. */
public class Donor extends User {
    private long dateOfBirth;           // day number (days since 1 Jan 1970)
    private String bloodType;
    private String location;            // area, e.g. "Abu Dhabi - Al Reem"
    private String contactPreference;   // "Push" or "SMS"
    private String eligibilityStatus = "Unknown";
    private long nextEligibleDate = 0;  // day number
    private boolean healthChecked;      // UC-2 extension 2a answered
    private ArrayList<Donation> donations = new ArrayList<>();
    private ArrayList<Deferral> deferrals = new ArrayList<>();

    public Donor(String userId, String name, String contact, String password, long dateOfBirth,
                 String bloodType, String location, String contactPreference) {
        super(userId, name, contact, password);
        this.dateOfBirth = dateOfBirth;
        this.bloodType = bloodType;
        this.location = location;
        this.contactPreference = contactPreference;
    }

    @Override
    public String getRole() { return "donor"; }

    /**
     * Sequence diagram "Check Eligibility": returns {donations, deferrals}.
     * Index 0 = ArrayList of Donation, index 1 = ArrayList of Deferral.
     */
    public Object[] listDonationsAndDeferrals() {
        return new Object[]{donations, deferrals};
    }

    public ArrayList<Donation> listDonations() { return donations; }

    public ArrayList<Donation> listDonationsNewestFirst() {
        ArrayList<Donation> copy = new ArrayList<>(donations);
        Collections.sort(copy, (a, b) -> Long.compare(b.getDonationDate(), a.getDonationDate()));
        return copy;
    }

    public void updateEligibility(String status, long nextDate) {
        this.eligibilityStatus = status;
        this.nextEligibleDate = nextDate;
    }

    /** Returns true if every health answer is OK. */
    public boolean answerHealthQuestions(boolean[] answers) {
        healthChecked = true;
        for (boolean a : answers) if (!a) return false;
        return true;
    }

    public void addDonation(Donation d) {
        donations.add(d);
        healthChecked = true;
    }

    public void addDeferral(Deferral d) { deferrals.add(d); }

    /** The deferral that ends last, or null. */
    public Deferral latestDeferral() {
        Deferral latest = null;
        for (Deferral d : deferrals) {
            if (latest == null || d.getEndDate() > latest.getEndDate()) latest = d;
        }
        return latest;
    }

    public long lastDonationDate() {
        long last = -1;
        for (Donation d : donations) if (d.getDonationDate() > last) last = d.getDonationDate();
        return last;
    }

    public void updateProfile(String contact, String location, String contactPreference) {
        setContact(contact);
        this.location = location;
        this.contactPreference = contactPreference;
    }

    /** Age in whole years on the given day. */
    public int getAge(long today) {
        return yearsBetween(dateOfBirth, today);
    }

    public static int yearsBetween(long fromDay, long toDay) {
        Calendar a = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        a.setTimeInMillis(fromDay * 86400000L);
        Calendar b = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        b.setTimeInMillis(toDay * 86400000L);
        int years = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
        if (b.get(Calendar.DAY_OF_YEAR) < a.get(Calendar.DAY_OF_YEAR)) years--;
        return years;
    }

    public long getDateOfBirth() { return dateOfBirth; }
    public String getBloodType() { return bloodType; }
    public String getLocation() { return location; }
    public String getContactPreference() { return contactPreference; }
    public String getEligibilityStatus() { return eligibilityStatus; }
    public long getNextEligibleDate() { return nextEligibleDate; }
    public boolean isHealthChecked() { return healthChecked; }
}
