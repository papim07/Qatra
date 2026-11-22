package com.example.qatra;

import java.io.Serializable;

public class Alert implements Serializable {
    public static final String SENT = "Sent";
    public static final String BOOKED = "Booked";
    public static final String DONATED = "Donated";
    public static final String NOT_ACCEPTED = "Not accepted";

    private String bloodType;
    private String location;
    private String bookingLink;
    private String channel;
    private Appeal appeal;
    private Donor donor;
    private String status = SENT;
    private long appointmentDate = -1;
    private String appointmentTime;

    public Alert(Appeal appeal, Donor donor, String location) {
        this.appeal = appeal;
        this.donor = donor;
        this.bloodType = appeal.getBloodType();
        this.location = location;
        this.channel = donor.getContactPreference();
        this.bookingLink = "qatra://book/" + appeal.getAppealId() + "/" + donor.getUserId();
    }

    public void book(long date, String time) {
        status = BOOKED;
        appointmentDate = date;
        appointmentTime = time;
    }

    public boolean isOpen() { return status.equals(SENT) || status.equals(BOOKED); }

    public void setStatus(String s) { status = s; }

    public String getBloodType() { return bloodType; }
    public String getLocation() { return location; }
    public String getBookingLink() { return bookingLink; }
    public String getChannel() { return channel; }
    public Appeal getAppeal() { return appeal; }
    public Donor getDonor() { return donor; }
    public String getStatus() { return status; }
    public long getAppointmentDate() { return appointmentDate; }
    public String getAppointmentTime() { return appointmentTime; }
}
