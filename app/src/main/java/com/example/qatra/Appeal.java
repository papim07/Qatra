package com.example.qatra;

import java.io.Serializable;
import java.util.ArrayList;

public class Appeal implements Serializable {
    public static final String PENDING = "Pending";
    public static final String APPROVED = "Approved";
    public static final String REJECTED = "Rejected";

    private int appealId;
    private String bloodType;
    private String urgency;
    private String status = PENDING;
    private int unitsNeeded;
    private ArrayList<Donor> recipients = new ArrayList<>();
    private String rejectReason;
    private long createdDate;

    public Appeal(int appealId, String bloodType, String urgency, int unitsNeeded, long createdDate) {
        this.appealId = appealId;
        this.bloodType = bloodType;
        this.urgency = urgency;
        this.unitsNeeded = unitsNeeded;
        this.createdDate = createdDate;
    }

    public int assignRecipients(ArrayList<Donor> donors) {
        recipients = new ArrayList<>(donors);
        return recipients.size();
    }

    public void approve() { status = APPROVED; }

    public void reject(String reason) {
        status = REJECTED;
        rejectReason = reason;
    }

    public void markPending() { status = PENDING; }

    public String logAppeal() {
        return "Appeal #" + appealId + " (" + bloodType + ", " + urgency + ") " + status
                + ", " + getDonorCount() + " donors";
    }

    public int getDonorCount() { return recipients.size(); }

    public int getAppealId() { return appealId; }
    public String getBloodType() { return bloodType; }
    public String getUrgency() { return urgency; }
    public String getStatus() { return status; }
    public int getUnitsNeeded() { return unitsNeeded; }
    public ArrayList<Donor> getRecipients() { return recipients; }
    public String getRejectReason() { return rejectReason; }
    public long getCreatedDate() { return createdDate; }
}
