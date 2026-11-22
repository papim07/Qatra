package com.example.qatra;

import java.io.Serializable;

public class Donation implements Serializable {
    private long donationDate;
    private String centre;
    private String donationType;
    private String bloodType;

    public Donation(long donationDate, String centre, String donationType, String bloodType) {
        this.donationDate = donationDate;
        this.centre = centre;
        this.donationType = donationType;
        this.bloodType = bloodType;
    }

    public void record(Donor donor) {
        donor.addDonation(this);
    }

    public long getDonationDate() { return donationDate; }
    public String getCentre() { return centre; }
    public String getDonationType() { return donationType; }
    public String getBloodType() { return bloodType; }
}
