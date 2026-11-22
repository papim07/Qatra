package com.example.qatra;

import java.io.Serializable;

/** <<entity>> Donation: date, centre, type and blood type (FR-3, FR-6). */
public class Donation implements Serializable {
    private long donationDate;     // day number
    private String centre;
    private String donationType;   // e.g. "Whole blood"
    private String bloodType;

    public Donation(long donationDate, String centre, String donationType, String bloodType) {
        this.donationDate = donationDate;
        this.centre = centre;
        this.donationType = donationType;
        this.bloodType = bloodType;
    }

    /** Saves this donation to the donor's history. */
    public void record(Donor donor) {
        donor.addDonation(this);
    }

    public long getDonationDate() { return donationDate; }
    public String getCentre() { return centre; }
    public String getDonationType() { return donationType; }
    public String getBloodType() { return bloodType; }
}
